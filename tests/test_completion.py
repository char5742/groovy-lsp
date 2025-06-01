"""Test code completion functionality using pytest-lsp."""
import os
from pathlib import Path
from pytest_lsp import LanguageClient
import pytest


def test_completion_for_string_methods(tmp_path):
    """Test that completion provides String methods in Groovy code."""
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
    
    def example() {
        name.
    }
}""")
        
        # Open the document
        doc = srv.text_document_did_open(
            uri=test_file.as_uri(),
            language_id="groovy",
            text=test_file.read_text()
        )
        
        # Request completion at position after "name."
        completion = srv.text_document_completion(
            text_document={"uri": test_file.as_uri()},
            position={"line": 4, "character": 13}
        )
        
        # Verify completion response
        assert completion is not None
        
        # Extract items from completion response
        if isinstance(completion, dict) and "items" in completion:
            items = completion["items"]
        elif isinstance(completion, list):
            items = completion
        else:
            items = []
        
        assert len(items) > 0
        
        # Check for String methods
        labels = [item.get("label", "") for item in items]
        assert any("length" in label for label in labels)
        assert any("toLowerCase" in label for label in labels)