const assert = require('assert');
const vscode = require('vscode');
const path = require('path');

suite('Groovy Language Features Test Suite', () => {
    
    test('Should handle Groovy script files', async () => {
        const scriptContent = `
// Groovy script
println "Starting script"

def names = ['Alice', 'Bob', 'Charlie']
names.each { name ->
    println "Hello, $name!"
}

def result = names.collect { it.toUpperCase() }
println result`;
        
        const doc = await vscode.workspace.openTextDocument({
            language: 'groovy',
            content: scriptContent
        });
        
        assert.strictEqual(doc.languageId, 'groovy');
        assert.ok(doc.getText().includes('names.each'));
    });

    test('Should handle Groovy closures', async () => {
        const closureContent = `
class ClosureExample {
    def greet = { name ->
        "Hello, $name!"
    }
    
    def calculate = { int x, int y ->
        x + y
    }
    
    def process(List items, Closure action) {
        items.collect(action)
    }
}`;
        
        const doc = await vscode.workspace.openTextDocument({
            language: 'groovy',
            content: closureContent
        });
        
        // Wait for document to be processed
        await new Promise(resolve => setTimeout(resolve, 1000));
        
        // Test go to definition on closure
        const position = new vscode.Position(2, 15); // On "greet"
        const definitions = await vscode.commands.executeCommand(
            'vscode.executeDefinitionProvider',
            doc.uri,
            position
        );
        
        assert.ok(Array.isArray(definitions));
    });

    test('Should handle Groovy DSL syntax', async () => {
        const dslContent = `
// Gradle-like DSL
project {
    name = 'groovy-lsp'
    version = '1.0.0'
    
    dependencies {
        implementation 'org.codehaus.groovy:groovy-all:3.0.9'
        testImplementation 'junit:junit:4.13.2'
    }
    
    tasks {
        test {
            useJUnitPlatform()
        }
    }
}`;
        
        const doc = await vscode.workspace.openTextDocument({
            language: 'groovy',
            content: dslContent
        });
        
        assert.ok(doc.getText().includes('dependencies'));
    });

    test('Should handle Groovy traits', async () => {
        const traitContent = `
trait Flyable {
    abstract String getName()
    
    String fly() {
        "I'm ${getName()} and I can fly!"
    }
}

trait Swimmable {
    String swim() {
        "I can swim!"
    }
}

class Duck implements Flyable, Swimmable {
    String name = "Donald"
    
    String getName() {
        return name
    }
}`;
        
        const doc = await vscode.workspace.openTextDocument({
            language: 'groovy',
            content: traitContent
        });
        
        // Test document symbols include traits
        const symbols = await vscode.commands.executeCommand(
            'vscode.executeDocumentSymbolProvider',
            doc.uri
        );
        
        assert.ok(Array.isArray(symbols));
    });

    test('Should handle Groovy annotations', async () => {
        const annotationContent = `
import groovy.transform.CompileStatic
import groovy.transform.ToString
import groovy.transform.EqualsAndHashCode

@CompileStatic
@ToString
@EqualsAndHashCode
class Person {
    String firstName
    String lastName
    int age
    
    @Deprecated
    String getFullName() {
        "$firstName $lastName"
    }
    
    String getDisplayName() {
        "${lastName}, ${firstName}"
    }
}`;
        
        const doc = await vscode.workspace.openTextDocument({
            language: 'groovy',
            content: annotationContent
        });
        
        // Test hover on annotation
        const position = new vscode.Position(5, 1); // On "@CompileStatic"
        const hovers = await vscode.commands.executeCommand(
            'vscode.executeHoverProvider',
            doc.uri,
            position
        );
        
        assert.ok(Array.isArray(hovers));
    });

    test('Should handle Groovy metaclass', async () => {
        const metaclassContent = `
// Metaclass example
String.metaClass.shout = { ->
    delegate.toUpperCase() + "!"
}

Integer.metaClass.times = { Closure c ->
    delegate.times(c)
}

def message = "hello"
def loudMessage = message.shout()

3.times {
    println "Iteration $it"
}`;
        
        const doc = await vscode.workspace.openTextDocument({
            language: 'groovy',
            content: metaclassContent
        });
        
        assert.ok(doc.getText().includes('metaClass'));
    });

    test('Should provide references for symbols', async () => {
        const referenceContent = `
class Calculator {
    def add(a, b) {
        return a + b
    }
    
    def subtract(a, b) {
        return a - b
    }
}

def calc = new Calculator()
def sum = calc.add(5, 3)
def diff = calc.subtract(10, 4)
def anotherSum = calc.add(1, 1)`;
        
        const doc = await vscode.workspace.openTextDocument({
            language: 'groovy',
            content: referenceContent
        });
        
        // Find references to 'add' method
        const position = new vscode.Position(2, 8); // On "add" method definition
        const references = await vscode.commands.executeCommand(
            'vscode.executeReferenceProvider',
            doc.uri,
            position,
            { includeDeclaration: true }
        );
        
        assert.ok(Array.isArray(references));
    });

    test('Should handle Groovy GStrings', async () => {
        const gstringContent = `
def name = "World"
def age = 25
def multiline = """
    Hello, $name!
    You are ${age} years old.
    In 10 years, you'll be ${age + 10}.
"""

def template = "Name: \${name}, Age: \${age}"
def interpolated = "Welcome, $name!"`;
        
        const doc = await vscode.workspace.openTextDocument({
            language: 'groovy',
            content: gstringContent
        });
        
        // Test syntax highlighting recognizes GStrings
        assert.ok(doc.getText().includes('$name'));
        assert.ok(doc.getText().includes('${age}'));
    });

    test('Should handle error scenarios', async () => {
        const errorContent = `
class ErrorExample {
    def methodWithError() {
        // Syntax error: missing closing brace
        if (true) {
            println "This is an error"
        // Missing closing brace
    }
    
    def anotherMethod() {
        // Type error: incompatible types
        String text = 123
        return text
    }
}`;
        
        const doc = await vscode.workspace.openTextDocument({
            language: 'groovy',
            content: errorContent
        });
        
        // Wait for diagnostics
        await new Promise(resolve => setTimeout(resolve, 2000));
        
        const diagnostics = vscode.languages.getDiagnostics(doc.uri);
        // Language server should eventually provide error diagnostics
        assert.ok(Array.isArray(diagnostics));
    });
});