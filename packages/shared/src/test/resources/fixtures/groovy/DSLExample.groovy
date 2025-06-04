// Build script DSL example
project {
    name 'my-awesome-project'
    version '2.0.0'

    dependencies {
        compile 'org.apache.groovy:groovy-all:4.0.0'
        testCompile 'junit:junit:4.13.2'

        implementation {
            group 'com.example'
            name 'library'
            version '1.0'
        }
    }

    tasks {
        compile {
            sourceCompatibility = '11'
            targetCompatibility = '11'
        }

        test {
            useJUnitPlatform()
            maxParallelForks = 4
        }
    }
}

// HTML builder DSL
html {
    head {
        title 'Groovy DSL Example'
        meta(charset: 'UTF-8')
    }
    body {
        h1 'Welcome to Groovy DSL'
        div(class: 'container') {
            p 'This is a paragraph'
            ul {
                li 'Item 1'
                li 'Item 2'
                li 'Item 3'
            }
        }
    }
}
