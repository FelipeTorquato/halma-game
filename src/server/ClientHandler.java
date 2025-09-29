package server;

import shared.Protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler extends Thread {
    private final Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private GameSession gameSession;

    private static final int CHAT_PORT = 12346;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    public void setGameSession(GameSession gameSession) {
        this.gameSession = gameSession;
    }

    @Override
    public void run() {
        boolean isChatHandler = clientSocket.getLocalPort() == CHAT_PORT;

        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            if (gameSession == null) {
                if (!isChatHandler) {
                    sendMessage(Protocol.INFO + Protocol.SEPARATOR + "Aguardando oponente...");
                }
            }

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (gameSession != null) {
                    if (isChatHandler) {
                        gameSession.processChatMessage(inputLine, this);
                    } else {
                        gameSession.processGameMessage(inputLine, this);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Cliente desconectado: " + clientSocket.getInetAddress());
            if (gameSession != null) {
                gameSession.handleDisconnect(this);
            }
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public void shutdown() {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Erro durante o desligamento do cliente: " + e.getMessage());
        }
    }
}