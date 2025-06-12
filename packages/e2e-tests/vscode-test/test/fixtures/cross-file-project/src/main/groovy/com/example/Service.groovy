package com.example

import com.example.model.User

/**
 * Service class for user management
 */
class Service {
    private List<User> users = []

    void addUser(User user) {
        if (user && user.isValidEmail()) {
            users.add(user)
        }
    }

    User findUserById(String id) {
        return users.find { it.id == id }
    }

    List<User> findUsersByName(String name) {
        return users.findAll {
            it.firstName.contains(name) || it.lastName.contains(name)
        }
    }

    int getUserCount() {
        return users.size()
    }
}
