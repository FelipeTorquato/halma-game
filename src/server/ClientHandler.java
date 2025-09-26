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

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    public void setGameSession(GameSession gameSession) {
        this.gameSession = gameSession;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            if (gameSession == null) {
                sendMessage(Protocol.INFO + Protocol.SEPARATOR + "Aguardando oponente...");
            }

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (gameSession != null) {
                    gameSession.processMessage(inputLine, this);
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