import org.codehaus.groovy.control.*
import org.codehaus.groovy.control.io.StringReaderSource

def config = new CompilerConfiguration()
def unit = new CompilationUnit(config)

def sourceCode = '''
class TestClass {
    String name
    
    void sayHello() {
        println "Hello, $name"
    }
}
'''

def sourceUnit = new SourceUnit(
    "TestClass.groovy",
    new StringReaderSource(sourceCode, config),
    config,
    unit.getClassLoader(),
    new ErrorCollector(config)
)

unit.addSource(sourceUnit)

// Try PARSING phase
try {
    unit.compile(Phases.PARSING)
    println "Compiled to PARSING phase"
    println "AST: ${sourceUnit.getAST()}"
    println "Classes: ${sourceUnit.getAST()?.getClasses()}"
} catch (Exception e) {
    println "Error during compilation: ${e.message}"
    e.printStackTrace()
}