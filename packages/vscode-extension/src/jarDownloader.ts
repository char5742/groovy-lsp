import * as vscode from 'vscode';
import * as fs from 'fs';
import * as path from 'path';
import * as https from 'https';
import * as crypto from 'crypto';
import { promisify } from 'util';

const mkdir = promisify(fs.mkdir);
const writeFile = promisify(fs.writeFile);
const readFile = promisify(fs.readFile);
const unlink = promisify(fs.unlink);
const readdir = promisify(fs.readdir);
const stat = promisify(fs.stat);

interface GitHubRelease {
    tag_name: string;
    assets: Array<{
        name: string;
        browser_download_url: string;
        size: number;
    }>;
}

interface GitHubChecksumAsset {
    name: string;
    browser_download_url: string;
}

export class JarDownloader {
    private readonly githubRepo = 'char5742/groovy-lsp';
    private readonly jarFileName: string;

    constructor(private context: vscode.ExtensionContext) {
        const packageJson = JSON.parse(
            fs.readFileSync(
                path.join(context.extensionPath, 'package.json'),
                'utf8'
            )
        );
        const version = packageJson.version;
        this.jarFileName = `groovy-language-server-${version}.jar`;
    }

    async ensureJarExists(): Promise<string | undefined> {
        const config = vscode.workspace.getConfiguration('groovyLanguageServer');
        const configuredJar = config.get<string>('serverJar');
        
        if (configuredJar && fs.existsSync(configuredJar)) {
            return configuredJar;
        }

        // Check bundled JAR
        const bundledJar = this.context.asAbsolutePath(path.join('server', 'groovy-language-server.jar'));
        if (fs.existsSync(bundledJar)) {
            return bundledJar;
        }

        // Clean up old JAR files before checking/downloading
        await this.cleanupOldJars();

        // Check global storage
        const globalJar = path.join(this.context.globalStorageUri.fsPath, this.jarFileName);
        if (fs.existsSync(globalJar)) {
            return globalJar;
        }

        // Need to download
        const shouldDownload = await vscode.window.showInformationMessage(
            'Groovy Language Server JAR not found. Would you like to download it?',
            'Download',
            'Cancel'
        );

        if (shouldDownload === 'Download') {
            return await this.downloadJar();
        }

        return undefined;
    }

    private async downloadJar(): Promise<string | undefined> {
        const progress = await vscode.window.withProgress({
            location: vscode.ProgressLocation.Notification,
            title: 'Downloading Groovy Language Server',
            cancellable: true
        }, async (progress, token) => {
            try {
                // Get the release info
                const release = await this.getLatestRelease();
                if (!release) {
                    throw new Error('Could not find release');
                }

                const asset = release.assets.find(a => a.name === this.jarFileName);
                if (!asset) {
                    throw new Error(`Could not find ${this.jarFileName} in release ${release.tag_name}`);
                }

                // Ensure global storage directory exists
                const storageDir = this.context.globalStorageUri.fsPath;
                await mkdir(storageDir, { recursive: true });

                // Download the JAR with verification
                const jarPath = path.join(storageDir, this.jarFileName);
                const tempPath = `${jarPath}.tmp`;
                
                try {
                    // Download to temporary file first
                    await this.downloadFile(asset.browser_download_url, tempPath, progress, token, asset.size);

                    // Download and verify checksum if available
                    const checksumAsset = release.assets.find(a => a.name === `${this.jarFileName}.sha256`);
                    if (checksumAsset) {
                        const isValid = await this.verifyChecksum(tempPath, checksumAsset.browser_download_url);
                        if (!isValid) {
                            throw new Error('Checksum verification failed');
                        }
                    }

                    // Verify file size
                    const stats = await stat(tempPath);
                    if (stats.size !== asset.size) {
                        throw new Error(`File size mismatch: expected ${asset.size}, got ${stats.size}`);
                    }

                    // Move to final location
                    await fs.promises.rename(tempPath, jarPath);
                    
                    vscode.window.showInformationMessage('Groovy Language Server downloaded and verified successfully');
                    return jarPath;
                } catch (error) {
                    // Clean up temporary file on error
                    try {
                        await unlink(tempPath);
                    } catch {}
                    throw error;
                }
            } catch (error) {
                const message = error instanceof Error ? error.message : 'Unknown error';
                vscode.window.showErrorMessage(`Failed to download Groovy Language Server: ${message}`);
                return undefined;
            }
        });

        return progress;
    }

    private async getLatestRelease(): Promise<GitHubRelease | undefined> {
        return new Promise((resolve, reject) => {
            const options = {
                hostname: 'api.github.com',
                path: `/repos/${this.githubRepo}/releases/tags/v${this.getExtensionVersion()}`,
                headers: {
                    'User-Agent': 'groovy-language-server-vscode',
                    Accept: 'application/vnd.github.v3+json'
                }
            };

            https.get(options, (res) => {
                let data = '';
                res.on('data', chunk => data += chunk);
                res.on('end', () => {
                    if (res.statusCode === 200) {
                        try {
                            const release = JSON.parse(data) as GitHubRelease;
                            resolve(release);
                        } catch (e) {
                            reject(new Error('Failed to parse release data'));
                        }
                    } else if (res.statusCode === 404) {
                        // Try to get latest release if specific version not found
                        this.getAnyLatestRelease().then(resolve).catch(reject);
                    } else {
                        reject(new Error(`GitHub API returned ${res.statusCode}`));
                    }
                });
            }).on('error', reject);
        });
    }

    private async getAnyLatestRelease(): Promise<GitHubRelease | undefined> {
        return new Promise((resolve, reject) => {
            const options = {
                hostname: 'api.github.com',
                path: `/repos/${this.githubRepo}/releases/latest`,
                headers: {
                    'User-Agent': 'groovy-language-server-vscode',
                    Accept: 'application/vnd.github.v3+json'
                }
            };

            https.get(options, (res) => {
                let data = '';
                res.on('data', chunk => data += chunk);
                res.on('end', () => {
                    if (res.statusCode === 200) {
                        try {
                            const release = JSON.parse(data) as GitHubRelease;
                            resolve(release);
                        } catch (e) {
                            reject(new Error('Failed to parse release data'));
                        }
                    } else {
                        reject(new Error(`GitHub API returned ${res.statusCode}`));
                    }
                });
            }).on('error', reject);
        });
    }

    private async downloadFile(
        url: string,
        destination: string,
        progress: vscode.Progress<{ message?: string; increment?: number }>,
        token: vscode.CancellationToken,
        totalSize: number
    ): Promise<void> {
        return new Promise((resolve, reject) => {
            if (token.isCancellationRequested) {
                reject(new Error('Download cancelled'));
                return;
            }

            https.get(url, { headers: { 'User-Agent': 'groovy-language-server-vscode' } }, (res) => {
                // Handle redirects
                if (res.statusCode === 302 || res.statusCode === 301) {
                    const redirectUrl = res.headers.location;
                    if (redirectUrl) {
                        this.downloadFile(redirectUrl, destination, progress, token, totalSize)
                            .then(resolve)
                            .catch(reject);
                        return;
                    }
                }

                if (res.statusCode !== 200) {
                    reject(new Error(`Download failed with status ${res.statusCode}`));
                    return;
                }

                const chunks: Buffer[] = [];
                let downloadedSize = 0;

                res.on('data', (chunk: Buffer) => {
                    if (token.isCancellationRequested) {
                        res.destroy();
                        reject(new Error('Download cancelled'));
                        return;
                    }

                    chunks.push(chunk);
                    downloadedSize += chunk.length;
                    const percentage = Math.round((downloadedSize / totalSize) * 100);
                    progress.report({
                        message: `Downloading... ${percentage}%`,
                        increment: chunk.length / totalSize * 100
                    });
                });

                res.on('end', async () => {
                    try {
                        const buffer = Buffer.concat(chunks);
                        await writeFile(destination, buffer);
                        resolve();
                    } catch (error) {
                        reject(error);
                    }
                });

                res.on('error', reject);
            }).on('error', reject);
        });
    }

    private getExtensionVersion(): string {
        const packageJson = JSON.parse(
            fs.readFileSync(
                path.join(this.context.extensionPath, 'package.json'),
                'utf8'
            )
        );
        return packageJson.version;
    }

    private async verifyChecksum(filePath: string, checksumUrl: string): Promise<boolean> {
        return new Promise((resolve) => {
            https.get(checksumUrl, { headers: { 'User-Agent': 'groovy-language-server-vscode' } }, (res) => {
                let checksumData = '';
                res.on('data', chunk => checksumData += chunk);
                res.on('end', async () => {
                    if (res.statusCode !== 200) {
                        console.warn('Checksum file not found, skipping verification');
                        resolve(true); // Skip verification if no checksum available
                        return;
                    }

                    try {
                        // Expected format: "<hash>  <filename>"
                        const expectedChecksum = checksumData.trim().split(/\s+/)[0];
                        const fileContent = await readFile(filePath);
                        const actualChecksum = crypto.createHash('sha256').update(fileContent).digest('hex');
                        
                        if (expectedChecksum === actualChecksum) {
                            console.log('Checksum verification passed');
                            resolve(true);
                        } else {
                            console.error(`Checksum mismatch: expected ${expectedChecksum}, got ${actualChecksum}`);
                            resolve(false);
                        }
                    } catch (error) {
                        console.error('Error during checksum verification:', error);
                        resolve(false);
                    }
                });
            }).on('error', (error) => {
                console.error('Error downloading checksum:', error);
                resolve(true); // Skip verification on error
            });
        });
    }

    private async cleanupOldJars(): Promise<void> {
        try {
            const storageDir = this.context.globalStorageUri.fsPath;
            if (!fs.existsSync(storageDir)) {
                return;
            }

            const files = await readdir(storageDir);
            const jarFiles = files.filter(f => f.startsWith('groovy-language-server-') && f.endsWith('.jar'));
            
            // Keep only the current version and delete others
            const currentVersion = this.getExtensionVersion();
            const deletePromises = jarFiles
                .filter(f => f !== this.jarFileName)
                .map(async f => {
                    const filePath = path.join(storageDir, f);
                    try {
                        await unlink(filePath);
                        console.log(`Cleaned up old JAR: ${f}`);
                    } catch (error) {
                        console.warn(`Failed to delete old JAR ${f}:`, error);
                    }
                });

            await Promise.all(deletePromises);
        } catch (error) {
            console.error('Error during JAR cleanup:', error);
        }
    }
}