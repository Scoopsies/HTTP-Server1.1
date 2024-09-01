package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.util.HashMap;

public class Server {
    public ServerSocket serverSocket;
    public SocketAddress socketAddress;
    public int port = 80;
    public String root = "/Users/scoops/Projects/httpServer1.1/src/PathFiles";
    public Boolean isRunning = true;

    public Server() {
        createServer();
    }

    public  Server(int portNumber) {
        port = portNumber;
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
        while (isRunning) {
            try {
                var clientSocket = serverSocket.accept();
                var in = clientSocket.getInputStream();
                var out = clientSocket.getOutputStream();
                var response = getResponse(in);
                out.write(response.getBytes());
                clientSocket.close();
            } catch (IOException ioe) {
                System.out.println(ioe.getMessage());
            }
        }
    }

    public void stop() throws IOException {
        isRunning = false;
        serverSocket.close();
    }

    public String getResponse(InputStream inputStream) throws IOException {
        var filePath = getPath(inputStream);
        var indexHTML = new File(root + filePath + "/index.html");
        var directory = new File(root + filePath);
        String CLRF = "\r\n";
        String htmlContent = "Content-Type: text/html" + CLRF;
        String status404 = "HTTP/1.1 404 Not Found" + CLRF;
        String status200 = "HTTP/1.1 200 OK" + CLRF;

        if (indexHTML.exists())
            return status200
                    + htmlContent
                    + CLRF
                    + getHtmlContent(indexHTML);

        if (directory.exists()) {
            return status200
                    + htmlContent
                    + CLRF
                    + getDirectoryListing(directory);
        }

        var fileNotFound = new File(root + "/404/index.html");

        return status404
                + htmlContent
                + CLRF
                + getHtmlContent(fileNotFound);
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
        StringBuilder htmlContent = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                htmlContent.append(line).append("\n");
            }
        }

        if (!htmlContent.isEmpty()) {
            htmlContent.setLength(htmlContent.length() - 1);
        }

        return htmlContent.toString();
    }

    public String getDirectoryListing(File directory) {
        var listing = directory.list();
        var stringBuilder = new StringBuilder();

        stringBuilder.append("<h1>Directory Listing for /");
        stringBuilder.append(directory.getName());
        stringBuilder.append("</h1>\n");
        stringBuilder.append("<ul>\n");

        assert listing != null;
        for (String fileName : listing) {
            stringBuilder.append("<li>");
            stringBuilder.append("<a href=\"");
            stringBuilder.append(directory.getPath());
            stringBuilder.append("/");
            stringBuilder.append(fileName);
            stringBuilder.append("\">");
            stringBuilder.append(fileName);
            stringBuilder.append("</a>");
            stringBuilder.append("</li>\n");
        }

        stringBuilder.append("</ul>");

        return stringBuilder.toString();
    }

    public String getContentType(String path) {
        var extension = getExtensionOf(path);

        var contentTypes = new HashMap<String, String>();
        contentTypes.put("html", "Content-Type: text/html");
        contentTypes.put("png", "Content-Type: image/png");
        contentTypes.put("gif", "Content-Type: image/gif");
        contentTypes.put("jpeg", "Content-Type: image/jpeg");
        contentTypes.put("pdf", "Content-Type: application/pdf");

        return contentTypes.get(extension);
    }

    public String getExtensionOf(String path) {
        int lastDotIndex = path.lastIndexOf(".");
        return path.substring(lastDotIndex + 1);
    }
}