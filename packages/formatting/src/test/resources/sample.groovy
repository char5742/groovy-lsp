// Sample Groovy file for formatting tests

class Person {
    String name
    int age

    def Person(String name, int age) {
        this.name = name
        this.age = age
    }

    def greet() {
        println "Hello, my name is ${name} and I am ${age} years old."
    }

    static void main(String[] args) {
        def people = [
            new Person("Alice", 30),
            new Person("Bob", 25),
            new Person("Charlie", 35)
        ]

        people.each { person ->
            person.greet()
        }

        // Using triple-quoted strings
        def message = '''
        This is a multi-line
        string that preserves
        formatting
        '''

        println message
    }
}
