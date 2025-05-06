package com.aarav.sparkapp;

import java.util.HashSet;
import java.util.Set;

/**
 * Simple in-memory user model for profile management.
 */
public class User {
    public String username;
    public String email;
    public String name;
    public Set<User> friends = new HashSet<>();
    public Set<String> interests = new HashSet<>();

    public User(String username, String email, String name) {
        this.username = username;
        this.email = email;
        this.name = name;
    }
}
