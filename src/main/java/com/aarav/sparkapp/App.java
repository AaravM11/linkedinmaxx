package com.aarav.sparkapp;

import static spark.Spark.*;

public class App {
    public static void main(String[] args) {
        System.out.println(">>> Starting Spark on port 4567");
        port(4567);

        // Log every incoming request to the console
        before((req, res) -> System.out.println("→ " + req.requestMethod() + " " + req.uri()));

        // A single test route
        get("/ping", (req, res) -> {
            System.out.println("Handling /ping");
            res.type("text/plain");
            return "pong";
        });
    }
}
