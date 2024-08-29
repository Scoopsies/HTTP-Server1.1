package org.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketAddress;

public class Server {
    public ServerSocket serverSocket;
    public SocketAddress socketAddress;
    public int port = 80;

    public Server() {
        createServer();
    }

    public  Server(int portNumber) {
        this.port = portNumber;
        createServer();
    }

    private void createServer() {
        try {
            serverSocket = new ServerSocket(port);
            socketAddress = serverSocket.getLocalSocketAddress();
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
    }

    public void run() {
        try{
         serverSocket.accept();
        } catch (IOException ioe){
            System.out.println(ioe.getMessage());
        }
    }

    public void stop() {
        try{
            serverSocket.close();
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
    }
}
