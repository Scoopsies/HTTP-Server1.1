package org.example;

import org.junit.jupiter.api.AfterEach;
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
    }

    @AfterEach
    void cleanUp() throws IOException {
        server.stop();
    }

    @Test
    void serverSocketCreatedAtPort80byDefault() {
        assertEquals(80, server.port);
    }

    @Test
    void serverSocketCreatedAtPort5432() {
        var server = new Server(5432);
        assertEquals(5432, server.port);
    }

    @Test
    void serverSocketCreatedAtPort3212() {
        var server = new Server(3212);
        assertEquals(3212, server.port);
    }

    @Test
    void localSocketAddressCreated() {
        assertEquals("0.0.0.0/0.0.0.0:80", server.socketAddress.toString());
    }

    @Test
    void runListensForAConnection() throws InterruptedException {
        Thread thread = new Thread(() -> server.run());
        thread.start();

        try(var socket = new Socket()) {
            assertDoesNotThrow(() -> socket.connect(server.socketAddress));
        } catch (IOException ioe) {
            System.out.println();
        }

        thread.join();
    }

    @Test
    void stopClosesTheConnectionToPort() throws InterruptedException, IOException {
        Thread thread = new Thread(() -> server.run());
        thread.start();
        assertFalse(server.serverSocket.isClosed());
        server.stop();
        thread.join();
        assertTrue(server.serverSocket.isClosed());
    }

    @Test
    void getPathReturnsSlash() throws IOException {
        var bais = new ByteArrayInputStream("GET / HTTP/1.1\r\n\r\n".getBytes());
        assertEquals("/", server.getPath(bais));
    }

    @Test
    void getPathReturnsSlashHello() throws IOException {
        var bais = new ByteArrayInputStream("GET /hello HTTP/1.1\r\n\r\n".getBytes());
        assertEquals("/hello", server.getPath(bais));
    }

    @Test
    void getFileReturnsTextInHello() {
        var bais = new ByteArrayInputStream("GET /hello HTTP/1.1\r\n\r\n".getBytes());
        assertEquals("<h1>Hello!</h1>", server.getFile(bais));
    }

    @Test
    void getFileReturnsTextInGoodBye() {
        var bais = new ByteArrayInputStream("GET /goodbye HTTP/1.1\r\n\r\n".getBytes());
        assertEquals("<h1>Goodbye</h1>", server.getFile(bais));
    }

    @Test
    void getResponseHello() throws IOException, InterruptedException {
        var inputStream = new ByteArrayInputStream("GET /hello HTTP/1.1".getBytes());
        var expected = """
                HTTP/1.1 200 OK\r
                Content-Type: text/html\r
                \r
                <h1>Hello!</h1>""";
        var result = new String(server.getResponse(inputStream));
        assertEquals(expected, result);
    }

    @Test
    void getResponseGoodBye() throws IOException, InterruptedException {
        var inputStream = new ByteArrayInputStream("GET /goodbye HTTP/1.1".getBytes());
        var expected = """
                HTTP/1.1 200 OK\r
                Content-Type: text/html\r
                \r
                <h1>Goodbye</h1>""";
        var result = new String(server.getResponse(inputStream));
        assertEquals(expected, result);
    }

    @Test
    void getResponseIf404() throws IOException, InterruptedException {
        var inputStream = new ByteArrayInputStream("GET /hamburger HTTP/1.1".getBytes());
        var expected = """
                HTTP/1.1 404 Not Found\r
                Content-Type: text/html\r
                \r
                <h1>404: This isn't the directory you are looking for.</h1>""";
        var result = new String(server.getResponse(inputStream));
        assertEquals(expected, result);
    }

    @Test
    void getResponseForNoIndex() throws IOException, InterruptedException {
        var inputStream = new ByteArrayInputStream("GET /noIndex HTTP/1.1".getBytes());
        var directory = new File(server.root + "/noIndex");
        var expected ="""
                HTTP/1.1 200 OK\r
                Content-Type: text/html\r
                \r
                %s""".formatted(server.getDirectoryListing(directory));
        var result = new String(server.getResponse(inputStream));
        assertEquals(expected, result);
    }

    @Test
    void getResponseForNotIndexHTML() throws IOException, InterruptedException {
        var inputStream = new ByteArrayInputStream("GET /noIndex/notIndex.html HTTP/1.1".getBytes());
        var file = new File(server.root + "/noIndex/notIndex.html");
        var expected ="""
                HTTP/1.1 200 OK\r
                Content-Type: text/html\r
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
                Content-Type: text/html\r
                \r
                %s""".formatted(server.getDirectoryListing(directory));
        var result = new String(server.getResponse(inputStream));
        assertEquals(expected, result);
    }

    @Test
    void getDirectoryListingForThings() {
        var directory = new File(server.root + "/things");
        var result = """
        <h1>Directory Listing for /things</h1>
        <ul>
        <li><a href="things/miata.pdf">miata.pdf</a></li>
        <li><a href="things/miata.jpg">miata.jpg</a></li>
        <li><a href="things/miata.png">miata.png</a></li>
        <li><a href="things/miata.txt">miata.txt</a></li>
        <li><a href="things/miata.gif">miata.gif</a></li>
        </ul>""";
        assertEquals(result, server.getDirectoryListing(directory));
    }

    @Test
    void getDirectoryListingForNoIndex() {
        var directory = new File(server.root + "/noIndex");
        var notIndexPath = "noIndex/notIndex.html";
        var textTxtPath = "noIndex/text.txt";
        var result = """
        <h1>Directory Listing for /noIndex</h1>
        <ul>
        <li><a href="%s">notIndex.html</a></li>
        <li><a href="%s">text.txt</a></li>
        </ul>""".formatted(notIndexPath, textTxtPath);
        assertEquals(result, server.getDirectoryListing(directory));
    }

    @Test
    void getHTMLContentReturnsHello() throws IOException {
        var file = new File(server.root + "/hello/index.html");
        var result = "<h1>Hello!</h1>";
        assertEquals(result, server.getTextFileContent(file));
    }

    @Test
    void getHTMLContentReturnsGoodbye() throws IOException {
        var file = new File(server.root + "/goodbye/index.html");
        var result = "<h1>Goodbye</h1>";
        assertEquals(result, server.getTextFileContent(file));
    }

    @Test
    void getHTMLContentReturnsNoIndex() throws IOException {
        var file = new File(server.root + "/noIndex/notIndex.html");
        var result = "<h1>Not Index.html</h1>";
        assertEquals(result, server.getTextFileContent(file));
    }

    @Test
    void getHTMLContentReturnsGoodBye2() throws IOException {
        var file = new File(server.root + "/goodbye/goodbye2/index.html");
        var result = "<h1>goodbye2</h1>\n<p>Bonus Line</p>";
        assertEquals(result, server.getTextFileContent(file));
    }

    @Test
    void getExtensionOfGetsHTML() {
        assertEquals("html", server.getExtensionOf("index.html"));
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
        assertEquals("Content-Type: text/html", server.getContentType("/hello/index.html"));
    }

    @Test
    void getContentTypeReturnsPNG() {
        assertEquals("Content-Type: image/png", server.getContentType("/things/miata.png"));
    }

    @Test
    void getContentTypeReturnsGIF() {
        assertEquals("Content-Type: image/gif", server.getContentType("/things/miata.gif"));
    }

    @Test
    void getContentTypeReturnsJPEG() {
        assertEquals("Content-Type: image/jpeg", server.getContentType("/things/miata.jpeg"));
    }

    @Test
    void getContentTypeReturnsPDF() {
        assertEquals("Content-Type: application/pdf", server.getContentType("/things/miata.pdf"));
    }

    @Test
    void getContentTypeReturnsHtmlForDirectory() {
        assertEquals("Content-Type: text/html", server.getContentType("/things"));
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

}