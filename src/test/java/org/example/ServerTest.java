package org.example;

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

    @Test
    void serverSocketCreatedAtPort80() {
        assertEquals(80, server.socketPort);
        server.stop();
    }

    @Test
    void localSocketAddressCreated() {
        assertEquals("0.0.0.0/0.0.0.0:80", server.socketAddress.toString());
        server.stop();
    }

    @Test
    void runListensForAConnection() throws InterruptedException {
        Thread thread = new Thread(() -> server.run());
        thread.start();

        try(var socket = new Socket()) {
            assertDoesNotThrow(() -> {
                socket.connect(server.socketAddress);
                socket.close();
            });
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }

        thread.join();
        server.stop();
    }

    @Test
    void stopClosesTheConnectionToPort() throws InterruptedException {
        Thread thread = new Thread(() -> server.run());
        thread.start();
        server.stop();
        thread.join();
        assertTrue(server.serverSocket.isClosed());
    }



}