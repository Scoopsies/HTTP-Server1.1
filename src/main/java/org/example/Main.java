package org.example;

public class Main {
    public static void main(String[] args) {
        System.out.println(System.getProperty("user.dir"));
        var server = new Server();
        server.run();
    }
}