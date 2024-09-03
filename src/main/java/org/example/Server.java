package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
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
    public String root = ".";
    public Boolean isRunning = true;

    public Server() {
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

    private String responseStatus(String status) {
        return "HTTP/1.1 " + status + CLRF;
    }

    public byte[] getResponse(InputStream inputStream) throws IOException, InterruptedException {
        var request = getRequest(inputStream);
        var filePath = getPath(request);
        var indexHTML = new File(root + filePath + "/index.html");
        var file = new File(root + filePath);

        ByteArrayOutputStream responseStream = new ByteArrayOutputStream();

        if (filePath.endsWith("/ping")) {
            Thread.sleep(1000);
            return buildResponse(responseStream, getContentType("index.html"), getCurrentTime().getBytes());
        }

        if (indexHTML.exists()) {
            return buildResponse(responseStream, getContentType(filePath + "index.html"), getFileContent(indexHTML));
        }

        if (file.isFile()){
            return buildResponse(responseStream, getContentType(filePath), getFileContent(file));
        }

        if (file.isDirectory()) {
            return buildResponse(responseStream, getContentType(filePath), buildDirectoryListing(file).getBytes());
        }

        var fileNotFound = new File(root + "/404/index.html");
        return buildResponse(responseStream, "404 Not Found", getContentType(filePath), getFileContent(fileNotFound));
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

    public String getPath(String request) throws IOException {
       return splitHeader(request)[1];
    }

    public String getMethod(String request) throws IOException {
        return splitHeader(request)[0];
    }

    private String getHeader(String request) {
        return request.split("\r\n")[0];
    }

    private String[] splitHeader(String request) throws IOException {
        return getHeader(request).split("\\s");
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

    public String buildDirectoryListing(File directory) throws IOException {
        var listing = directory.list();
        var rootPath = new File(root).getCanonicalPath();
        var path = directory.getCanonicalPath().substring(rootPath.length());
        var stringBuilder = new StringBuilder();

        stringBuilder.append("<h1>Directory Listing for ");
        stringBuilder.append(path);
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

        for (int i = 0; i < args.length; i++) {
            if (Objects.equals(args[i], "-p"))
                port = Integer.parseInt(args[i + 1]);

            if (Objects.equals(args[i], "-r"))
                root = args[i + 1];
        }
    }

    public String getRequest(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
}