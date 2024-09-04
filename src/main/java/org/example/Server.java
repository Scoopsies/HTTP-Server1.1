package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Objects;

public class Server {
    private static final String CLRF = "\r\n";
    private ServerSocket serverSocket;
    private int port = 80;
    private String root = ".";
    public Boolean isRunnable = true;
    private GuessingGame guessingGame = new GuessingGame();

    public Server() {}

    public void run() {
        printStartupConfig();
        new Thread(this::handleIO).start();
    }

    public void handleIO() {
        try {
            this.serverSocket = new ServerSocket(port);
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }

        while (isRunnable) {
            try {
                var clientSocket = this.serverSocket.accept();

                new Thread(() -> {
                    try {
                        var in = clientSocket.getInputStream();
                        var out = clientSocket.getOutputStream();
                        var response = getResponse(in);
                        out.write(response);
                        clientSocket.close();
                    } catch (IOException | InterruptedException ioe) {
                        System.out.println(ioe.getMessage());
                    }
                }).start();

            } catch (IOException ioe) {
                System.out.println(ioe.getMessage());
            }
        }
    }

    public void stop() throws IOException {
        isRunnable = false;
        this.serverSocket.close();
    }

    private String responseStatus(String status) {
        return "HTTP/1.1 " + status + CLRF;
    }

    public byte[] getResponse(InputStream inputStream) throws IOException, InterruptedException {
        var request = getRequest(inputStream);
        var filePath = getPath(request);
        var indexHTML = new File(root + filePath + "/index.html");
        var file = new File(root + filePath);
        var responseStream = new ByteArrayOutputStream();

        if (filePath.startsWith("/ping")) {
            var template = getTextFileContent("/Users/scoops/Projects/httpServer1.1/ping/index.html");
            var sleepModifier = filePath.replace("/ping/", "");
            int timeToSleep;

            try {
                timeToSleep = Integer.parseInt(sleepModifier) * 1000;
            } catch (Exception e) {
                timeToSleep = 0;
            }

            var html = template.formatted(getCurrentTime(), "%s");
            Thread.sleep(timeToSleep);
            html = html.formatted(getCurrentTime());
            return buildResponse(responseStream, html.getBytes());
        }

        if (filePath.startsWith("/guess")) {
            if (Objects.equals("POST", getMethod(request))) {
                var requestArray = request.split(CLRF);
                var clientGuess = requestArray[requestArray.length - 1].split("=")[1];
                var guessResponse = guessingGame.handleGuess(Integer.parseInt(clientGuess));
                return buildResponse(responseStream, getTextFileContent(indexHTML).formatted("<p>"+ guessResponse +"</p>").getBytes());
            }
            guessingGame = new GuessingGame();

            return buildResponse(responseStream, getTextFileContent(indexHTML).formatted("<p>Pick a number 1 - 100</p>").getBytes());
        }

        if (filePath.startsWith("/listing")) {
            filePath = filePath.replace("/listing", root);
            return buildResponse(responseStream, buildDirectoryListing(filePath).getBytes());
        }

        if (filePath.startsWith("/form")) {
            var queryMap = parseQuery(filePath);
            var template = getTextFileContent("/Users/scoops/Projects/httpServer1.1/form/getTemplate.html");
            var response = template.formatted(queryMap.get("foo"), queryMap.get("bar")).getBytes();
            return buildResponse(responseStream, response);
        }

        if (indexHTML.exists()) {
            return buildResponse(responseStream, getFileContent(indexHTML));
        }

        if (file.isFile()){
            return buildResponse(responseStream, getContentType(filePath), getFileContent(file));
        }

        if (file.isDirectory()) {
            return buildResponse(responseStream, buildDirectoryListing(file).getBytes());
        }

        var fileNotFound = new File("/Users/scoops/Projects/httpServer1.1/404/index.html");
        return buildResponse(responseStream, "404 Not Found", getContentType(filePath), getFileContent(fileNotFound));
    }

    public String getRequest(InputStream inputStream) throws IOException {
        StringBuilder request = new StringBuilder();
        byte[] buffer = new byte[16384];
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            request.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
            if (bytesRead < buffer.length) {
                break;
            }
        }

        return request.toString();
    }

    private HashMap<String, String> parseQuery(String filePath) {
        var queryMap = new HashMap<String, String>();
        var query = filePath.split("\\?");
        if (query.length > 1) {
            var queryArray = query[1].replace("=", " ").replace("&", " ").split("\\s");
            for (int i = 0; i < queryArray.length; i += 2) {
                queryMap.put(queryArray[i], queryArray[i + 1]);
            }
        }

        return queryMap;
    }

    private byte[] buildResponse(ByteArrayOutputStream responseStream, String status, String contentType, byte[] content) throws IOException {
        responseStream.write(responseStatus(status).getBytes());
        responseStream.write(contentType.getBytes());
        responseStream.write("Server: httpServer1.1".getBytes());
        responseStream.write(CLRF.getBytes());
        responseStream.write(CLRF.getBytes());
        responseStream.write(content);
        return responseStream.toByteArray();
    }

    private byte[] buildResponse(ByteArrayOutputStream responseStream, String contentType, byte[] content) throws IOException {
        return buildResponse(responseStream, "200 OK", contentType, content);
    }

    private byte[] buildResponse(ByteArrayOutputStream responseStream, byte[] content) throws IOException {
        return buildResponse(responseStream, "200 OK", "Content-Type: text/html\n", content);
    }

    public String getCurrentTime() {
        var time = LocalDateTime.now();
        var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return time.format(formatter);
    }

    public HashMap<String, String> parseRequest(String request) {
        var result = new HashMap<String, String>();
        result.put("header", getHeader(request));
        result.put("startLine", getStartLine(request));
        result.put("body", getBody(request));
        result.put("path", getPath(request));
        result.put("method", getMethod(request));

        var headerLines = request.split("\r\n");
        for (int i = 1; i < headerLines.length; i++) {
            var headerLine = headerLines[i].split(": ", 2);
            if (Objects.equals(2, headerLine.length)) {
                var key = headerLine[0];
                var value = headerLine[1];
                result.put(key, value);
            }
        }


        return result;
    }

    public String getPath(String request) {
        return splitStartLine(request)[1];
    }

    public String getMethod(String request) {
        return splitStartLine(request)[0];
    }

    private String getStartLine(String request) {
        return request.split("\r\n")[0];
    }

    private String[] splitStartLine(String request) {
        return getStartLine(request).split("\\s");
    }

    private String getHeader(String request) {
        return request.split("\r\n\r\n", 2)[0];
    }

    private String getBody(String request) {
        try {
            return request.split("\r\n\r\n", 2)[1];
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] getFileContent(File file) throws IOException {
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
        htmlContent.append("\n");

        if (!htmlContent.isEmpty()) {
            htmlContent.setLength(htmlContent.length() - 1);
        }

        return htmlContent.toString();
    }

    public String getTextFileContent(String filePath) throws IOException {
        var file = new File(filePath);
        return getTextFileContent(file);
    }

    public byte[] getBinaryFileContent(File file) throws IOException {
        var path = file.toPath();
        return Files.readAllBytes(path);
    }

    public String buildDirectoryListing(File directory) throws IOException {
        var listing = directory.list();
        var rootPath = new File(root).getCanonicalPath();
        var path = directory.getCanonicalPath().replace(rootPath, ".");
        var stringBuilder = new StringBuilder();

        stringBuilder.append("<h1>Directory Listing for ");
        stringBuilder.append(path);
        stringBuilder.append("</h1>\n<ul>");

        assert listing != null;
        for (String fileName : listing) {
            var currentFile = new File(directory.getPath()  + "/" + fileName);
            var currentFilePath = currentFile.getCanonicalPath().replace(rootPath, "");

            stringBuilder.append("<li><a href=\"");
            if (currentFile.isDirectory())
                stringBuilder.append("/listing");
            stringBuilder.append(currentFilePath);
            stringBuilder.append("\">");
            stringBuilder.append(fileName);
            stringBuilder.append("</a></li>");
        }
        stringBuilder.append("</ul>");

        return stringBuilder.toString();
    }

    public String buildDirectoryListing(String filePath) throws IOException {
        return buildDirectoryListing(new File( filePath));
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

        return "Content-Type: " + contentType + "\n";
    }

    public String getExtensionOf(String path) {
        int lastDotIndex = path.lastIndexOf(".");
        return path.substring(lastDotIndex + 1);
    }

    public void parseArgs(String[] args) {
        boolean isPrintingConfig = false;

        for (int i = 0; i < args.length; i++) {
            if (Objects.equals(args[i], "-p"))
                port = Integer.parseInt(args[i + 1]);

            if (Objects.equals(args[i], "-r"))
                root = args[i + 1];

            if (Objects.equals(args[i], "-h")) {
                isRunnable = false;
                printHelpMenu();
            }

            if (Objects.equals(args[i], "-x")) {
                isRunnable = false;
                isPrintingConfig = true;
            }
        }
        
        if (isPrintingConfig)
            printStartupConfig();
    }

    private void printHelpMenu() {
        System.out.println("  -p     Specify the port.  Default is 80.");
        System.out.println("  -r     Specify the root directory.  Default is the current working directory.");
        System.out.println("  -h     Print this help message");
        System.out.println("  -x     Print the startup configuration without starting the server");
    }

    private void printStartupConfig() {
        var file = new File(root);
        String path;

        try {
            path = file.getCanonicalPath();
        } catch (IOException ioe) {
            path = ".";
        }
        System.out.println("Example Server");
        System.out.println("Running on port: " + port);
        System.out.println("Serving files from: " + path);
    }

    public int getPort() {
        return port;
    }

    public ServerSocket getSocket() {
        return serverSocket;
    }

    public String getRoot() {
        return root;
    }
}