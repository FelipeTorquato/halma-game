package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class HalmaServer {
    private static final int PORT = 12345;
    private static final List<ClientHandler> waitingClients = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("Halma Server em execução...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Novo cliente conectado: " + clientSocket.getInetAddress());
                ClientHandler clientHandler = new ClientHandler(clientSocket);

                clientHandler.start();

                synchronized (waitingClients) {
                    waitingClients.add(clientHandler);
                    if (waitingClients.size() >= 2) {
                        ClientHandler player1 = waitingClients.remove(0);
                        ClientHandler player2 = waitingClients.remove(0);

                        System.out.println("Pareando jogadores e iniciando nova partida.");
                        GameSession gameSession = new GameSession(player1, player2);
                        new Thread(gameSession).start();
                    } else {
                        //clientHandler.sendMessage(shared.Protocol.INFO + shared.Protocol.SEPARATOR + "Waiting for an opponent...");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
