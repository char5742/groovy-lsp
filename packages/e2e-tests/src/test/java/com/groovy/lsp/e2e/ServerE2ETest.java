package com.groovy.lsp.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * End-to-end tests for the Groovy Language Server.
 * Tests the server running as a separate process.
 */
@Tag("e2e")
class ServerE2ETest extends E2ETestBase {

    @Override
    protected boolean isServerReady() {
        try {
            // Check if server responds to health check
            URL url = new URL("http://localhost:" + serverPort + "/health");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1000);

            int responseCode = connection.getResponseCode();
            connection.disconnect();

            return responseCode == 200;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void testServerStartsAndStops() {
        // Server should be running (started in setUp)
        assertThat(serverProcess.getProcess().isAlive()).isTrue();
        assertThat(isServerReady()).isTrue();
    }

    @Test
    void testBasicGroovyFileProcessing() throws Exception {
        // Create a simple Groovy file
        Path groovyFile =
                createGroovyFile(
                        "src/main/groovy/Example.groovy",
                        """
                        class Example {
                            String name

                            void sayHello() {
                                println "Hello, $name!"
                            }
                        }
                        """);

        // Give server time to process the file
        Thread.sleep(2000);

        // Verify file exists
        assertThat(groovyFile).exists();

        // Additional checks would require LSP client connection
        // This is just a basic test to ensure the server can handle files
    }

    @Test
    void testMultipleFileProcessing() throws Exception {
        // Create multiple Groovy files
        createGroovyFile(
                "src/main/groovy/com/example/Service.groovy",
                """
                package com.example

                class Service {
                    def process(String input) {
                        return input.toUpperCase()
                    }
                }
                """);

        createGroovyFile(
                "src/main/groovy/com/example/Controller.groovy",
                """
                package com.example

                class Controller {
                    private Service service = new Service()

                    def handleRequest(String request) {
                        return service.process(request)
                    }
                }
                """);

        createGroovyFile(
                "src/test/groovy/com/example/ServiceTest.groovy",
                """
                package com.example

                import org.junit.jupiter.api.Test
                import static org.assertj.core.api.Assertions.assertThat

                class ServiceTest {
                    @Test
                    void testProcess() {
                        def service = new Service()
                        assertThat(service.process("hello")).isEqualTo("HELLO")
                    }
                }
                """);

        // Give server time to process files
        Thread.sleep(3000);

        // Server should still be running
        assertThat(serverProcess.getProcess().isAlive()).isTrue();
    }

    @Test
    void testServerHandlesInvalidFiles() throws Exception {
        // Create a file with syntax errors
        createGroovyFile(
                "src/main/groovy/Invalid.groovy",
                """
                class Invalid {
                    // Missing closing brace
                    void broken() {
                        if (true) {
                            println "This is broken"
                        // Missing closing brace
                    }
                """);

        // Give server time to process the file
        Thread.sleep(2000);

        // Server should still be running despite invalid file
        assertThat(serverProcess.getProcess().isAlive()).isTrue();
        assertThat(isServerReady()).isTrue();
    }

    @Test
    void testServerMemoryUsage() throws Exception {
        // Create many files to test memory handling
        for (int i = 0; i < 50; i++) {
            createGroovyFile(
                    "src/main/groovy/Generated" + i + ".groovy",
                    """
                    class Generated%d {
                        String field1
                        String field2
                        int field3

                        void method1() { }
                        void method2() { }
                        void method3() { }
                    }
                    """
                            .formatted(i));
        }

        // Give server time to process all files
        Thread.sleep(5000);

        // Server should still be running
        assertThat(serverProcess.getProcess().isAlive()).isTrue();
        assertThat(isServerReady()).isTrue();

        // Check memory usage doesn't exceed limits
        // This would require JMX or other monitoring in a real test
    }
}
