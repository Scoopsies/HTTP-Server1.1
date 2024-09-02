package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Objects;

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
                out.write(response);
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

    public byte[] getResponse(InputStream inputStream) throws IOException {
        var filePath = getPath(inputStream);
        var indexHTML = new File(root + filePath + "/index.html");
        var file = new File(root + filePath);
        String CLRF = "\r\n";
        String htmlContent = "Content-Type: text/html" + CLRF;
        String status404 = "HTTP/1.1 404 Not Found" + CLRF;
        String status200 = "HTTP/1.1 200 OK" + CLRF;

        ByteArrayOutputStream responseStream = new ByteArrayOutputStream();

        if (indexHTML.exists()) {
            responseStream.write(status200.getBytes());
            responseStream.write(getContentType("index.html").getBytes());
            responseStream.write(CLRF.getBytes());
            responseStream.write(CLRF.getBytes());
            responseStream.write(getFileContent(indexHTML));
            return responseStream.toByteArray();
        }

        if (file.isFile()){
            responseStream.write(status200.getBytes());
            responseStream.write(getContentType(filePath).getBytes());
            responseStream.write(CLRF.getBytes());
            responseStream.write(CLRF.getBytes());
            responseStream.write(getFileContent(file));
            return responseStream.toByteArray();
        }

        if (file.isDirectory()) {
            responseStream.write(status200.getBytes());
            responseStream.write(htmlContent.getBytes());
            responseStream.write(CLRF.getBytes());
            responseStream.write(getDirectoryListing(file).getBytes());
            return responseStream.toByteArray();
        }

        var fileNotFound = new File(root + "/404/index.html");
        responseStream.write(status404.getBytes());
        responseStream.write(("Content-Type: text/html" + CLRF).getBytes());
        responseStream.write(CLRF.getBytes());
        responseStream.write(getFileContent(fileNotFound));
        return responseStream.toByteArray();
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

    public byte[] getFileContent(File file) throws IOException {
        String extension = getExtensionOf(file.getName());

        if (isTextFile(extension))
            return getTextFileContent(file).getBytes();

        return getBinaryFileContent(file);

    }

    private boolean isTextFile(String extension) {
        return Objects.equals("html", extension) || Objects.equals("txt", extension);
    }


    public String getTextFileContent(File file) throws IOException {
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

    public byte[] getBinaryFileContent(File file) throws IOException {
        var path = file.toPath();
        return Files.readAllBytes(path);
    }

    public String getDirectoryListing(File directory) {
        var listing = directory.list();
        var path = directory.getPath().substring(root.length() + 1);
        var stringBuilder = new StringBuilder();

        stringBuilder.append("<h1>Directory Listing for /");
        stringBuilder.append(directory.getName());
        stringBuilder.append("</h1>\n");
        stringBuilder.append("<ul>\n");

        assert listing != null;
        for (String fileName : listing) {
            stringBuilder.append("<li>");
            stringBuilder.append("<a href=\"");
            stringBuilder.append(path);
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