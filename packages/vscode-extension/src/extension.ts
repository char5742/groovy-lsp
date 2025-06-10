import * as path from 'path';
import * as vscode from 'vscode';
import * as fs from 'fs';
import {
    LanguageClient,
    LanguageClientOptions,
    ServerOptions,
    TransportKind
} from 'vscode-languageclient/node';

let client: LanguageClient;

export function activate(context: vscode.ExtensionContext) {
    const config = vscode.workspace.getConfiguration('groovyLanguageServer');
    const javaHome = config.get<string>('javaHome') || process.env.JAVA_HOME || process.env.JDK_HOME;
    
    if (!javaHome) {
        vscode.window.showErrorMessage('Java runtime not found. Please set groovyLanguageServer.javaHome in settings or JAVA_HOME environment variable.');
        return;
    }

    const javaExecutable = path.join(javaHome, 'bin', 'java');
    
    // Check for server JAR in multiple locations
    let jarPath = config.get<string>('serverJar');
    if (!jarPath) {
        // Try bundled JAR first
        jarPath = context.asAbsolutePath(path.join('server', 'groovy-language-server.jar'));
        if (!fs.existsSync(jarPath)) {
            // Try global storage location
            jarPath = path.join(context.globalStorageUri.fsPath, 'groovy-language-server.jar');
            if (!fs.existsSync(jarPath)) {
                vscode.window.showErrorMessage(
                    'Groovy Language Server JAR not found. Please download it from the releases page and set groovyLanguageServer.serverJar in settings.',
                    'Download'
                ).then(selection => {
                    if (selection === 'Download') {
                        vscode.env.openExternal(vscode.Uri.parse('https://github.com/groovy-lsp/groovy-lsp/releases'));
                    }
                });
                return;
            }
        }
    }

    const serverOptions: ServerOptions = {
        run: {
            command: javaExecutable,
            args: ['-jar', jarPath],
            transport: TransportKind.stdio
        },
        debug: {
            command: javaExecutable,
            args: ['-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005', '-jar', jarPath],
            transport: TransportKind.stdio
        }
    };

    const clientOptions: LanguageClientOptions = {
        documentSelector: [
            { scheme: 'file', language: 'groovy' }
        ],
        synchronize: {
            fileEvents: vscode.workspace.createFileSystemWatcher('**/*.groovy')
        }
    };

    client = new LanguageClient(
        'groovyLanguageServer',
        'Groovy Language Server',
        serverOptions,
        clientOptions
    );

    client.start();
    
    context.subscriptions.push(
        vscode.commands.registerCommand('groovy.restartLanguageServer', () => {
            vscode.window.showInformationMessage('Restarting Groovy Language Server...');
            client.stop().then(() => {
                client.start();
            });
        })
    );
}

export function deactivate(): Thenable<void> | undefined {
    if (!client) {
        return undefined;
    }
    return client.stop();
}