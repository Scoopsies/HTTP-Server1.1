package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.util.List;
import java.util.Scanner;

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
          var clientSocket = serverSocket.accept();
          var in = clientSocket.getInputStream();
          var out = clientSocket.getOutputStream();
          var response = getResponse(in);
          out.write(response.getBytes());
          clientSocket.close();
        } catch (IOException ioe){
            System.out.println(ioe.getMessage());
        }
    }

    public String getResponse(InputStream inputStream) throws IOException {
        var filePath = getPath(inputStream);
        var indexHTML = new File(root + filePath + "/index.html");
        String htmlContent = "Content-Type: text/html\r\n";
        if (indexHTML.exists()) {
            String status200 = "HTTP/1.1 200 OK\r\n";
            return status200
                    + htmlContent
                    + "\r\n"
                    + getHtmlContent(indexHTML)
                    + "\r\n";
        }

        String status404 = "HTTP/1.1 404 Not Found\r\n";
        return status404
                + htmlContent
                + "\r\n"
                + "<h1>404: This isn't the directory you are looking for.</h1>"
                + "\r\n";
    }

    public void stop() {
        try {
            serverSocket.close();
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
    }

    public List<String> readIn(InputStream in) {
        var reader = new BufferedReader(new InputStreamReader(in));
        return reader.lines().toList();
    }

    public String getHeader(InputStream input) throws IOException {
        var reader = new BufferedReader(new InputStreamReader(input));
        return reader.readLine();
    }


    public String getPath(InputStream input) throws IOException {
       return getHeader(input).split("\\s")[1];
    }

    public String getFile(InputStream input) {
        String path;
        String result = "";

        try {
            path = root + getPath(input) + "/index.html";
            var reader = new BufferedReader(new FileReader(path));
            result = String.join("\n", reader.lines().toList());
            reader.close();

        } catch (IOException ioe) {
            //noinspection CallToPrintStackTrace
            ioe.printStackTrace();
        }

        return result;
    }


    public String getHtmlContent(File file) throws IOException {
        var scanner = new Scanner(file);
        var htmlContent = "";

        while (scanner.hasNextLine()) {
           htmlContent = htmlContent.concat(scanner.nextLine() + "\n");
        }
        return htmlContent.substring(0, htmlContent.length() - 1);
    }
}
