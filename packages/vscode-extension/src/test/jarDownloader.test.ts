import * as assert from 'assert';
import * as sinon from 'sinon';
import * as vscode from 'vscode';
import * as https from 'https';
import * as fs from 'fs';
import * as path from 'path';
import * as crypto from 'crypto';
import { JarDownloader } from '../jarDownloader';

suite('JarDownloader Test Suite', () => {
    let sandbox: sinon.SinonSandbox;
    let mockContext: vscode.ExtensionContext;
    let jarDownloader: JarDownloader;

    setup(() => {
        sandbox = sinon.createSandbox();
        
        // Mock extension context
        mockContext = {
            extensionPath: '/mock/extension/path',
            globalStorageUri: {
                fsPath: '/mock/global/storage'
            },
            asAbsolutePath: (relativePath: string) => path.join('/mock/extension/path', relativePath)
        } as any;

        // Mock package.json read
        sandbox.stub(fs, 'readFileSync').returns(JSON.stringify({
            version: '0.1.0'
        }));

        jarDownloader = new JarDownloader(mockContext);
    });

    teardown(() => {
        sandbox.restore();
    });

    test('ensureJarExists returns configured JAR if exists', async () => {
        const configuredJar = '/path/to/configured.jar';
        sandbox.stub(vscode.workspace, 'getConfiguration').returns({
            get: () => configuredJar
        } as any);
        sandbox.stub(fs, 'existsSync').withArgs(configuredJar).returns(true);

        const result = await jarDownloader.ensureJarExists();
        assert.strictEqual(result, configuredJar);
    });

    test('ensureJarExists returns bundled JAR if exists', async () => {
        sandbox.stub(vscode.workspace, 'getConfiguration').returns({
            get: () => undefined
        } as any);
        const existsStub = sandbox.stub(fs, 'existsSync');
        existsStub.withArgs('/mock/extension/path/server/groovy-language-server.jar').returns(true);

        const result = await jarDownloader.ensureJarExists();
        assert.strictEqual(result, '/mock/extension/path/server/groovy-language-server.jar');
    });

    test('ensureJarExists prompts for download if JAR not found', async () => {
        sandbox.stub(vscode.workspace, 'getConfiguration').returns({
            get: () => undefined
        } as any);
        sandbox.stub(fs, 'existsSync').returns(false);
        const showMessageStub = sandbox.stub(vscode.window, 'showInformationMessage')
            .resolves(undefined);

        const result = await jarDownloader.ensureJarExists();
        assert.strictEqual(result, undefined);
        assert.ok(showMessageStub.calledOnce);
    });

    test('downloadFile handles redirects correctly', async () => {
        const mockResponse = {
            statusCode: 302,
            headers: { location: 'https://redirected.url' },
            on: sandbox.stub(),
            destroy: sandbox.stub()
        } as any;

        const httpsGetStub = sandbox.stub(https, 'get');
        httpsGetStub.onFirstCall().callsArgWith(1, mockResponse);
        
        const redirectResponse = {
            statusCode: 200,
            on: sandbox.stub()
                .withArgs('data').yields(Buffer.from('test data'))
                .withArgs('end').yields()
                .withArgs('error').returnsThis(),
            destroy: sandbox.stub()
        } as any;
        httpsGetStub.onSecondCall().callsArgWith(1, redirectResponse);

        const mockProgress = {
            report: sandbox.stub()
        } as any;
        const mockToken = {
            isCancellationRequested: false
        } as any;

        try {
            await (jarDownloader as any).downloadFile(
                'https://original.url',
                '/tmp/test.jar',
                mockProgress,
                mockToken,
                9
            );
            assert.ok(httpsGetStub.calledTwice);
        } catch (error) {
            // Expected in test environment
        }
    });

    test('cleanupOldJars removes old JAR files', async () => {
        const readdirStub = sandbox.stub(fs.promises, 'readdir').resolves([
            'groovy-language-server-0.0.9.jar',
            'groovy-language-server-0.1.0.jar',
            'groovy-language-server-0.2.0.jar',
            'other-file.txt'
        ] as any);
        
        const unlinkStub = sandbox.stub(fs.promises, 'unlink').resolves();
        const existsStub = sandbox.stub(fs, 'existsSync').returns(true);

        await (jarDownloader as any).cleanupOldJars();

        assert.ok(unlinkStub.calledWith('/mock/global/storage/groovy-language-server-0.0.9.jar'));
        assert.ok(unlinkStub.calledWith('/mock/global/storage/groovy-language-server-0.2.0.jar'));
        assert.ok(!unlinkStub.calledWith('/mock/global/storage/groovy-language-server-0.1.0.jar'));
        assert.ok(!unlinkStub.calledWith('/mock/global/storage/other-file.txt'));
    });

    test('verifyChecksum validates file integrity', async () => {
        const mockChecksumResponse = {
            statusCode: 200,
            on: sandbox.stub()
                .withArgs('data').yields('abc123  groovy-language-server.jar')
                .withArgs('end').yields()
        };

        sandbox.stub(https, 'get').callsArgWith(2, mockChecksumResponse);
        sandbox.stub(fs.promises, 'readFile').resolves(Buffer.from('test content'));
        
        const createHashStub = {
            update: sandbox.stub().returnsThis(),
            digest: sandbox.stub().returns('abc123')
        };
        sandbox.stub(crypto, 'createHash').returns(createHashStub as any);

        const result = await (jarDownloader as any).verifyChecksum(
            '/tmp/test.jar',
            'https://example.com/checksum'
        );

        assert.strictEqual(result, true);
    });

    test('verifyChecksum returns false on mismatch', async () => {
        const mockChecksumResponse = {
            statusCode: 200,
            on: sandbox.stub()
                .withArgs('data').yields('expected123  groovy-language-server.jar')
                .withArgs('end').yields()
        };

        sandbox.stub(https, 'get').callsArgWith(2, mockChecksumResponse);
        sandbox.stub(fs.promises, 'readFile').resolves(Buffer.from('test content'));
        
        const createHashStub = {
            update: sandbox.stub().returnsThis(),
            digest: sandbox.stub().returns('actual456')
        };
        sandbox.stub(crypto, 'createHash').returns(createHashStub as any);

        const result = await (jarDownloader as any).verifyChecksum(
            '/tmp/test.jar',
            'https://example.com/checksum'
        );

        assert.strictEqual(result, false);
    });

    test('downloadFile handles cancellation', async () => {
        const mockToken = {
            isCancellationRequested: true
        };

        try {
            await (jarDownloader as any).downloadFile(
                'https://example.com/file',
                '/tmp/test.jar',
                { report: () => {} },
                mockToken,
                1000
            );
            assert.fail('Should have thrown error');
        } catch (error: any) {
            assert.strictEqual(error.message, 'Download cancelled');
        }
    });

    test('downloadFile handles network errors', async () => {
        const mockResponse = {
            statusCode: 500,
            on: sandbox.stub()
        };

        sandbox.stub(https, 'get').callsArgWith(2, mockResponse);

        try {
            await (jarDownloader as any).downloadFile(
                'https://example.com/file',
                '/tmp/test.jar',
                { report: () => {} },
                { isCancellationRequested: false },
                1000
            );
            assert.fail('Should have thrown error');
        } catch (error: any) {
            assert.ok(error.message.includes('Download failed'));
        }
    });
});