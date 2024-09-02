package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class Server {
    public static final String CLRF = "\r\n";
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
            } catch (IOException | InterruptedException ioe) {
                System.out.println(ioe.getMessage());
            }
        }
    }

    public void stop() throws IOException {
        isRunning = false;
        serverSocket.close();
    }

    // new File("./blah") will look for blah/ in current working directory (where program was run from)
    // new File("/blah") will look for blah/ in ROOT directory of system

    private String responseStatus(String status) {
        return "HTTP/1.1 " + status + CLRF;
    }

    public byte[] getResponse(InputStream inputStream) throws IOException, InterruptedException {
        var filePath = getPath(inputStream);
        var indexHTML = new File(root + filePath + "/index.html");
        var file = new File(root + filePath);
        String htmlContent = "Content-Type: text/html";

        ByteArrayOutputStream responseStream = new ByteArrayOutputStream();

        if (filePath.endsWith("/ping")) {
            Thread.sleep(1000);
            return buildResponse(responseStream, getContentType("index.html"), getCurrentTime().getBytes());
        }

        if (indexHTML.exists()) {
            return buildResponse(responseStream, getContentType("index.html"), getFileContent(indexHTML));
        }

        if (file.isFile()){
            return buildResponse(responseStream, getContentType(filePath), getFileContent(file));
        }

        if (file.isDirectory()) {
            return buildResponse(responseStream, htmlContent, getDirectoryListing(file).getBytes());
        }

        var fileNotFound = new File(root + "/404/index.html");
        return buildResponse(responseStream, "404 Not Found", htmlContent, getFileContent(fileNotFound));
    }

    private byte[] buildResponse(ByteArrayOutputStream responseStream, String status, String contentType, byte[] content) throws IOException {
        responseStream.write(responseStatus(status).getBytes());
        responseStream.write(contentType.getBytes());
        responseStream.write(CLRF.getBytes());
        responseStream.write(CLRF.getBytes());
        responseStream.write(content);
        return responseStream.toByteArray();
    }

    private byte[] buildResponse(ByteArrayOutputStream responseStream, String contentType, byte[] content) throws IOException {
        return buildResponse(responseStream, "200 OK", contentType, content);
    }

    private String getCurrentTime() {
        var time = LocalDateTime.now();
        var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        return time.format(formatter);
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

    public String getContentType(String pathString) {
        var path = Path.of(pathString);
        String contentType;

        try {
            contentType = Files.probeContentType(path);
            if (contentType == null)
                contentType = "text/html";
        } catch (IOException ioe) {
            contentType = "text/html";
            System.out.println(ioe.getMessage());
        }

        return "Content-Type: " + contentType;
    }

    public String getExtensionOf(String path) {
        int lastDotIndex = path.lastIndexOf(".");
        return path.substring(lastDotIndex + 1);
    }

    public void parseArgs(String[] args) {
        port = Integer.parseInt(args[1]);
    }
}