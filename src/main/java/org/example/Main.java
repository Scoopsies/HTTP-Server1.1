package org.example;

public class Main {
    public static void main(String[] args) {
        var server = new Server();
        server.parseArgs(args);
        server.run();
    }
}