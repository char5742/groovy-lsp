package com.example

import com.example.model.User
import spock.lang.Specification

class ServiceTest extends Specification {
    Service service

    def setup() {
        service = new Service()
    }

    def "should add valid user"() {
        given:
        def user = new User("1", "John", "Doe", "john@example.com")

        when:
        service.addUser(user)

        then:
        service.getUserCount() == 1
    }

    def "should find user by id"() {
        given:
        def user = new User("123", "Jane", "Smith", "jane@example.com")
        service.addUser(user)

        when:
        def foundUser = service.findUserById("123")

        then:
        foundUser != null
        foundUser.getFullName() == "Jane Smith"
    }
}
