package org.example;

public class Main {
    public static void main(String[] args) {
        var server = new Server();


//        String[] argues = {"-x", "-p", "1234"};
//        server.parseArgs(argues);

        server.parseArgs(args);
        if (server.isRunning)
            server.run();
    }
}