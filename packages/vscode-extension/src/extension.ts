import * as path from 'path';
import * as vscode from 'vscode';
import * as fs from 'fs';
import {
    LanguageClient,
    LanguageClientOptions,
    ServerOptions,
    TransportKind
} from 'vscode-languageclient/node';
import { JarDownloader } from './jarDownloader';

let client: LanguageClient;

export async function activate(context: vscode.ExtensionContext) {
    console.log('Groovy Language Server extension activating...');
    
    const config = vscode.workspace.getConfiguration('groovyLanguageServer');
    const javaHome = config.get<string>('javaHome') || process.env.JAVA_HOME || process.env.JDK_HOME;
    
    if (!javaHome) {
        const message = 'Java runtime not found. Please set groovyLanguageServer.javaHome in settings or JAVA_HOME environment variable.';
        console.error(message);
        vscode.window.showErrorMessage(message);
        return;
    }
    
    console.log(`Using Java home: ${javaHome}`);

    const javaExecutable = path.join(javaHome, 'bin', 'java');
    
    // Use JarDownloader to ensure JAR exists
    const jarDownloader = new JarDownloader(context);
    const jarPath = await jarDownloader.ensureJarExists();
    
    if (!jarPath) {
        console.error('Failed to get Groovy Language Server JAR');
        return;
    }
    
    console.log(`Using server JAR: ${jarPath}`);

    // Add logging for Java executable
    console.log(`Java executable: ${javaExecutable}`);
    console.log(`JAR exists: ${fs.existsSync(jarPath)}`);
    
    const serverOptions: ServerOptions = {
        run: {
            command: javaExecutable,
            args: ['-jar', jarPath],
            transport: TransportKind.stdio,
            options: {
                env: {
                    ...process.env,
                    'groovy.lsp.debug': 'true'
                }
            }
        },
        debug: {
            command: javaExecutable,
            args: ['-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005', '-jar', jarPath],
            transport: TransportKind.stdio,
            options: {
                env: {
                    ...process.env,
                    'groovy.lsp.debug': 'true'
                }
            }
        }
    };

    const clientOptions: LanguageClientOptions = {
        documentSelector: [
            { scheme: 'file', language: 'groovy' }
        ],
        synchronize: {
            fileEvents: vscode.workspace.createFileSystemWatcher('**/*.groovy')
        },
        outputChannelName: 'Groovy Language Server',
        traceOutputChannel: vscode.window.createOutputChannel('Groovy Language Server Trace')
    };

    client = new LanguageClient(
        'groovyLanguageServer',
        'Groovy Language Server',
        serverOptions,
        clientOptions
    );

    client.start().then(() => {
        console.log('Groovy Language Server started successfully');
        vscode.window.showInformationMessage('Groovy Language Server started');
    }, (error) => {
        console.error('Failed to start Groovy Language Server:', error);
        vscode.window.showErrorMessage(`Failed to start Groovy Language Server: ${error.message}`);
    });
    
    console.log('Groovy Language Server starting...');
    
    context.subscriptions.push(
        vscode.commands.registerCommand('groovy.restartLanguageServer', () => {
            console.log('Restarting Groovy Language Server...');
            vscode.window.showInformationMessage('Restarting Groovy Language Server...');
            client.stop().then(() => {
                client.start();
                console.log('Groovy Language Server restarted');
            });
        })
    );
}

export function deactivate(): Thenable<void> | undefined {
    console.log('Groovy Language Server extension deactivating...');
    if (!client) {
        return undefined;
    }
    return client.stop();
}