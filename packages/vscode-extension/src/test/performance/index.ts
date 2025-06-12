import * as path from 'path';
import * as Mocha from 'mocha';
import { glob } from 'glob';

export function run(): Promise<void> {
    const mocha = new Mocha({
        ui: 'tdd',
        color: true,
        timeout: 120000, // 2 minutes timeout for performance tests
        reporter: 'json',
        reporterOptions: {
            output: path.join(__dirname, '../../../performance-results/results.json')
        }
    });

    const testsRoot = path.resolve(__dirname, '.');

    return new Promise((c, e) => {
        glob('**/*PerformanceTest.js', { cwd: testsRoot }).then((files: string[]) => {
            files.forEach((f: string) => mocha.addFile(path.resolve(testsRoot, f)));

            try {
                mocha.run(failures => {
                    if (failures > 0) {
                        e(new Error(`${failures} tests failed.`));
                    } else {
                        c();
                    }
                });
            } catch (err) {
                console.error(err);
                e(err);
            }
        }).catch((err: Error) => {
            return e(err);
        });
    });
}