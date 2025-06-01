"""Test hover functionality using pytest-lsp."""
import os
from pathlib import Path
from pytest_lsp import LanguageClient
import pytest


def test_hover_on_groovy_type(tmp_path):
    """Test that hover provides type information for Groovy code."""
    # Get the path to the server JAR
    server_jar = Path(__file__).parent.parent / "packages" / "server-launcher" / "build" / "libs" / "server-launcher-all.jar"
    
    # Start the Groovy LSP server
    srv = LanguageClient.launch_command(
        ["java", "-cp", str(server_jar), "com.groovy.lsp.server.launcher.Main"],
        root_uri=tmp_path.as_uri()
    )
    
    with srv:
        # Create a test Groovy file
        test_file = tmp_path / "Test.groovy"
        test_file.write_text("""class Test {
    String name = "test"
    
    def greet() {
        println "Hello, ${name}"
    }
}""")
        
        # Open the document
        doc = srv.text_document_did_open(
            uri=test_file.as_uri(),
            language_id="groovy",
            text=test_file.read_text()
        )
        
        # Request hover at position of "String"
        hover = srv.text_document_hover(
            text_document={"uri": test_file.as_uri()},
            position={"line": 1, "character": 4}
        )
        
        # Verify hover response
        assert hover is not None
        assert "contents" in hover
        
        # Check that hover contains information about String type
        hover_text = str(hover["contents"])
        assert "String" in hover_text or "java.lang.String" in hover_text