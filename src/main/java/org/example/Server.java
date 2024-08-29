package org.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketAddress;

public class Server {
    public ServerSocket serverSocket;
    public SocketAddress socketAddress;
    public int socketPort;

    public Server() {
        int port = 80;

        try {
            serverSocket = new ServerSocket(port);
            socketPort = serverSocket.getLocalPort();
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
