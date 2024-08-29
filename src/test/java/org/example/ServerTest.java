package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Socket;

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
}