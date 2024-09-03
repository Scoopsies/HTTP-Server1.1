package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

class ServerTest {
    private Server server;

    @BeforeEach
    void setup() {
        server = new Server();
        var baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
    }


    @Test
    void serverSocketCreatedAtPort80byDefault() {
        assertEquals(80, server.port);
    }

    @Test
    void localSocketAddressCreated() throws IOException, InterruptedException {
        server.run();
        Thread.sleep(10);
        assertEquals("0.0.0.0/0.0.0.0:80", server.socketAddress.toString());
        server.stop();
    }

    @Test
    void runListensForAConnection() throws IOException {
        server.run();

        try (var socket = new Socket()) {
            assertDoesNotThrow(() -> socket.connect(server.socketAddress));
            var outputStream = socket.getOutputStream();
            outputStream.write("GET / HTTP/1.1 \r\n".getBytes());
            outputStream.flush();

        } catch (IOException ioe) {
            fail("IOException occurred: " + ioe.getMessage());
        }
        server.stop();
    }

    @Test
    void stopClosesTheConnectionToPort() throws IOException, InterruptedException {
        server.run();
        Thread.sleep(10);
        assertFalse(server.serverSocket.isClosed());
        server.stop();
        assertTrue(server.serverSocket.isClosed());
    }

    @Test
    void getPathReturnsSlash() {
        var request = "GET / HTTP/1.1\r\n\r\n";
        assertEquals("/", server.getPath(request));
    }

    @Test
    void getPathReturnsSlashHello() {
        var request = "GET /hello HTTP/1.1\r\n\r\n";
        assertEquals("/hello", server.getPath(request));
    }

    @Test
    void getResponseHello() throws IOException, InterruptedException {
        var inputStream = new ByteArrayInputStream("GET /hello HTTP/1.1".getBytes());
        var expected = """
                HTTP/1.1 200 OK\r
                Content-Type: text/html
                Server: httpServer1.1\r
                \r
                <h1>Hello!</h1>
                """;
        var result = new String(server.getResponse(inputStream));
        assertEquals(expected, result);
    }

    @Test
    void getResponseGoodBye() throws IOException, InterruptedException {
        var inputStream = new ByteArrayInputStream("GET /goodbye HTTP/1.1".getBytes());
        var expected = """
                HTTP/1.1 200 OK\r
                Content-Type: text/html
                Server: httpServer1.1\r
                \r
                <h1>Goodbye</h1>
                """;
        var result = new String(server.getResponse(inputStream));
        assertEquals(expected, result);
    }

    @Test
    void getResponseIf404() throws IOException, InterruptedException {
        var inputStream = new ByteArrayInputStream("GET /hamburger HTTP/1.1".getBytes());
        var expected = """
                HTTP/1.1 404 Not Found\r
                Content-Type: text/html
                Server: httpServer1.1\r
                \r
                <h1>404: This isn't the directory you are looking for.</h1>
                """;
        var result = new String(server.getResponse(inputStream));
        assertEquals(expected, result);
    }

    @Test
    void getResponseForNoIndex() throws IOException, InterruptedException {
        var inputStream = new ByteArrayInputStream("GET /noIndex HTTP/1.1".getBytes());
        var directory = new File(server.root + "/noIndex");
        var expected ="""
                HTTP/1.1 200 OK\r
                Content-Type: text/html
                Server: httpServer1.1\r
                \r
                %s""".formatted(server.buildDirectoryListing(directory));
        var result = new String(server.getResponse(inputStream));
        assertEquals(expected, result);
    }

    @Test
    void getResponseForNotIndexHTML() throws IOException, InterruptedException {
        var inputStream = new ByteArrayInputStream("GET /noIndex/notIndex.html HTTP/1.1".getBytes());
        var file = new File(server.root + "/noIndex/notIndex.html");
        var expected ="""
                HTTP/1.1 200 OK\r
                Content-Type: text/html
                Server: httpServer1.1\r
                \r
                %s""".formatted(server.getTextFileContent(file));
        var result = new String(server.getResponse(inputStream));
        assertEquals(expected, result);
    }

    @Test
    void getResponseForThings() throws IOException, InterruptedException {
        var inputStream = new ByteArrayInputStream("GET /noIndex HTTP/1.1".getBytes());
        var directory = new File(server.root + "/noIndex");
        var expected ="""
                HTTP/1.1 200 OK\r
                Content-Type: text/html
                Server: httpServer1.1\r
                \r
                %s""".formatted(server.buildDirectoryListing(directory));
        var result = new String(server.getResponse(inputStream));
        assertEquals(expected, result);
    }

    @Test
    void getDirectoryListingForThings() throws IOException {
        var directory = new File(server.root + "/things");
        var result =
        "<h1>Directory Listing for ./things</h1>\n"
        + "<ul>"
        + "<li><a href=\"/things/miata.pdf\">miata.pdf</a></li>"
        + "<li><a href=\"/things/miata.jpg\">miata.jpg</a></li>"
        + "<li><a href=\"/things/miata.png\">miata.png</a></li>"
        + "<li><a href=\"/things/miata.txt\">miata.txt</a></li>"
        + "<li><a href=\"/things/miata.gif\">miata.gif</a></li>"
        + "</ul>";
        assertEquals(result, server.buildDirectoryListing(directory));
    }

    @Test
    void getDirectoryListingForNoIndex() throws IOException {
        var directory = new File(server.root + "/noIndex");
        var result =
        "<h1>Directory Listing for ./noIndex</h1>\n"
        + "<ul>"
        + "<li><a href=\"/noIndex/notIndex.html\">notIndex.html</a></li>"
        + "<li><a href=\"/noIndex/text.txt\">text.txt</a></li>"
        + "</ul>";
        assertEquals(result, server.buildDirectoryListing(directory));
    }

    @Test
    void getHTMLContentReturnsHello() throws IOException {
        var file = new File("./hello/index.html");
        var result = "<h1>Hello!</h1>\n";
        assertEquals(result, server.getTextFileContent(file));
    }

    @Test
    void getHTMLContentReturnsGoodbye() throws IOException {
        var file = new File("./goodbye/index.html");
        var result = "<h1>Goodbye</h1>\n";
        assertEquals(result, server.getTextFileContent(file));
    }

    @Test
    void getHTMLContentReturnsNoIndex() throws IOException {
        var file = new File("./noIndex/notIndex.html");
        var result = "<h1>Not Index.html</h1>\n";
        assertEquals(result, server.getTextFileContent(file));
    }

    @Test
    void getHTMLContentReturnsGoodBye2() throws IOException {
        var file = new File("./goodbye/goodbye2/index.html");
        var result = "<h1>goodbye2</h1>\n<p>Bonus Line</p>\n";
        assertEquals(result, server.getTextFileContent(file));
    }

    @Test
    void getExtensionOfGetsHTML() {
        assertEquals("html", server.getExtensionOf("welcome.html"));
        assertEquals("html", server.getExtensionOf("notIndex.html"));
    }

    @Test
    void getExtensionOfGetsPng() {
        assertEquals("png", server.getExtensionOf("miata.png"));
    }

    @Test
    void getExtensionOfGetsGif() {
        assertEquals("gif", server.getExtensionOf("miata.gif"));
    }

    @Test
    void getExtensionOfGetsJpeg() {
        assertEquals("jpeg", server.getExtensionOf("miata.jpeg"));
    }

    @Test
    void getExtensionOfGetsPdf() {
        assertEquals("pdf", server.getExtensionOf("miata.pdf"));
    }

    @Test
    void getContentTypeReturnsHTML() {
        assertEquals("Content-Type: text/html\n", server.getContentType("/hello/welcome.html"));
    }

    @Test
    void getContentTypeReturnsPNG() {
        assertEquals("Content-Type: image/png\n", server.getContentType("/things/miata.png"));
    }

    @Test
    void getContentTypeReturnsGIF() {
        assertEquals("Content-Type: image/gif\n", server.getContentType("/things/miata.gif"));
    }

    @Test
    void getContentTypeReturnsJPEG() {
        assertEquals("Content-Type: image/jpeg\n", server.getContentType("/things/miata.jpeg"));
    }

    @Test
    void getContentTypeReturnsPDF() {
        assertEquals("Content-Type: application/pdf\n", server.getContentType("/things/miata.pdf"));
    }

    @Test
    void getContentTypeReturnsHtmlForDirectory() {
        assertEquals("Content-Type: text/html\n", server.getContentType("/things"));
    }

    @Test
    void parseArgsSetsPort8080() {
        String[] args = {"-p", "8080"};
        server.parseArgs(args);
        assertEquals(8080, server.port);
    }

    @Test
    void parseArgsSetsPort800() {
        String[] args = {"-p", "800"};
        server.parseArgs(args);
        assertEquals(800, server.port);
    }

    @Test
    void parseArgsSetsRootToHello() {
        String[] args = {"-r", "./hello"};
        server.parseArgs(args);
        assertEquals("./hello", server.root);
    }

    @Test
    void parseArgsSetsRootToGoodbye() {
        String[] args = {"-r", "./goodbye"};
        server.parseArgs(args);
        assertEquals("./goodbye", server.root);
    }

    @Test
    void parseArgsSetsRootToHelloAndPortTo8080() {
        String[] args = {"-r", "./hello", "-p", "8080"};
        server.parseArgs(args);
        assertEquals("./hello", server.root);
        assertEquals(8080, server.port);
    }

    @Test
    void getMethodReturnsGet() {
        var request = "GET / HTTP/1.1";
        assertEquals("GET", server.getMethod(request));
    }

    @Test
    void getMethodReturnsPost() {
        var request = "POST / HTTP/1.1";
        assertEquals("POST", server.getMethod(request));
    }

    @Test
    void getRequestReturnsGetHello() throws IOException {
        var result = """
                GET /hello HTTP/1.1 \r
                """;
        var inputStream = new ByteArrayInputStream(result.getBytes());
        assertEquals(result, server.getRequest(inputStream));
    }

    @Test
    void getRequestReturnsPostHello() throws IOException {
        var result = """
                POST /hello HTTP/1.1 \r
                \r
                "message"
                """;
        var inputStream = new ByteArrayInputStream(result.getBytes());
        assertEquals(result, server.getRequest(inputStream));
    }

}