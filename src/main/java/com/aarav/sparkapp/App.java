package com.aarav.sparkapp;

import static spark.Spark.*;

public class App {
    public static void main(String[] args) {
        port(4567);
        get("/ping", (req, res) -> "pong");
    }
}