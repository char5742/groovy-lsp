package com.example.model

/**
 * User model class
 */
class User {
    String id
    String firstName
    String lastName
    String email

    User(String id, String firstName, String lastName, String email) {
        this.id = id
        this.firstName = firstName
        this.lastName = lastName
        this.email = email
    }

    String getFullName() {
        return "${firstName} ${lastName}"
    }

    boolean isValidEmail() {
        return email?.contains('@')
    }
}
