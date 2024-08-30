package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.util.List;

public class Server {
    public ServerSocket serverSocket;
    public SocketAddress socketAddress;
    public int port = 80;
    public String root = "/Users/scoops/Projects/httpServer1.1/src/PathFiles";

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
        try {
          var connection = serverSocket.accept();
          var in = connection.getInputStream();
            System.out.println(readIn(in));

        } catch (IOException ioe){
            System.out.println(ioe.getMessage());
        }
    }

    public void stop() {
        try {
            serverSocket.close();
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
    }

    public List<String> readIn(InputStream in) throws IOException {
        var reader = new BufferedReader(new InputStreamReader(in));
        return reader.lines().toList();
    }


    public String getPath(ByteArrayInputStream bais) throws IOException {
       return readIn(bais).getFirst().split("\\s")[1];
    }

    public String getFile(ByteArrayInputStream bais) {
        String path;
        String result = "";

        try {
            path = root + getPath(bais) + ".html";
            var reader = new BufferedReader(new FileReader(path));
            result = String.join("/n", reader.lines().toList());
            reader.close();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return result;
    }
}
