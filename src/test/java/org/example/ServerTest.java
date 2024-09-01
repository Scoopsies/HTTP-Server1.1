package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ServerTest {
    private Server server;

    @BeforeEach
    void setup() {
        server = new Server();
    }

    @AfterEach
    void cleanUp() {
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
    void stopClosesTheConnectionToPort() throws InterruptedException {
        Thread thread = new Thread(() -> server.run());
        thread.start();
        assertFalse(server.serverSocket.isClosed());
        server.stop();
        thread.join();
        assertTrue(server.serverSocket.isClosed());
    }

    @Test
    void readInReadsHello() {
        var bais = new ByteArrayInputStream("Hello".getBytes());
        var result = new ArrayList<String>();
        result.add("Hello");
        assertEquals(result, server.readIn(bais));
    }

    @Test
    void readInReadsGoodBye() {
        var bais = new ByteArrayInputStream("Goodbye".getBytes());
        var result = new ArrayList<String>();
        result.add("Goodbye");
        assertEquals(result, server.readIn(bais));
    }

    @Test
    void readInReadsGetRequest() {
        var bais = new ByteArrayInputStream("GET /hello HTTP/1.1\r\n".getBytes());
        var result = new ArrayList<String>();
        result.add("GET /hello HTTP/1.1");
        assertEquals(result, server.readIn(bais));
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
    void getResponseHello() throws IOException {
        var inputStream = new ByteArrayInputStream("GET /hello HTTP/1.1".getBytes());
        var result = """
                HTTP/1.1 200 OK\r
                Content-Type: text/html\r
                \r
                <h1>Hello!</h1>""";
        assertEquals(result, server.getResponse(inputStream));
    }

    @Test
    void getResponseGoodBye() throws IOException {
        var inputStream = new ByteArrayInputStream("GET /goodbye HTTP/1.1".getBytes());
        var result = """
                HTTP/1.1 200 OK\r
                Content-Type: text/html\r
                \r
                <h1>Goodbye</h1>""";
        assertEquals(result, server.getResponse(inputStream));
    }

    @Test
    void getResponseIf404() throws IOException {
        var inputStream = new ByteArrayInputStream("GET /hamburger HTTP/1.1".getBytes());
        var result = """
                HTTP/1.1 404 Not Found\r
                Content-Type: text/html\r
                \r
                <h1>404: This isn't the directory you are looking for.</h1>""";
        assertEquals(result, server.getResponse(inputStream));
    }

    @Test
    void getResponseForNoIndex() throws IOException {
        var inputStream = new ByteArrayInputStream("GET /noIndex HTTP/1.1".getBytes());
        var directory = new File(server.root + "/noIndex");
        var result ="""
                HTTP/1.1 200 OK\r
                Content-Type: text/html\r
                \r
                %s""".formatted(server.getDirectoryListing(directory));
        assertEquals(result, server.getResponse(inputStream));
    }

    @Test
    void getResponseForThings() throws IOException {
        var inputStream = new ByteArrayInputStream("GET /noIndex HTTP/1.1".getBytes());
        var directory = new File(server.root + "/noIndex");
        var result ="""
                HTTP/1.1 200 OK\r
                Content-Type: text/html\r
                \r
                %s""".formatted(server.getDirectoryListing(directory));
        assertEquals(result, server.getResponse(inputStream));
    }

    @Test
    void getDirectoryListingForThings() {
        var directory = new File(server.root + "/things");
        var fileTxtPath = server.root + "/things/file.txt";
        var thingsTxtPath = server.root + "/things/things.txt";
        var result = """
        <ul>
        <li><a href="%s">file.txt</a></li>
        <li><a href="%s">things.txt</a></li>
        </ul>""".formatted(fileTxtPath, thingsTxtPath);
        assertEquals(result, server.getDirectoryListing(directory));
    }

    @Test
    void getDirectoryListingForNoIndex() {
        var directory = new File(server.root + "/noIndex");
        var notIndexPath = server.root + "/noIndex/notIndex.html";
        var textTxtPath = server.root + "/noIndex/text.txt";
        var result = """
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
        assertEquals(result, server.getHtmlContent(file));
    }

    @Test
    void getHTMLContentReturnsGoodbye() throws IOException {
        var file = new File(server.root + "/goodbye/index.html");
        var result = "<h1>Goodbye</h1>";
        assertEquals(result, server.getHtmlContent(file));
    }

    @Test
    void getHTMLContentReturnsNoIndex() throws IOException {
        var file = new File(server.root + "/noIndex/notIndex.html");
        var result = "<h1>Not Index.html</h1>";
        assertEquals(result, server.getHtmlContent(file));
    }

    @Test
    void getHTMLContentReturnsGoodBye2() throws IOException {
        var file = new File(server.root + "/goodbye/goodbye2/index.html");
        var result = "<h1>goodbye2</h1>\n<p>Bonus Line</p>";
        assertEquals(result, server.getHtmlContent(file));
    }
}