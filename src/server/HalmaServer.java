package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class HalmaServer {
    private static final int GAME_PORT = 12345;
    private static final int CHAT_PORT = 12346;
    private static final List<ClientHandler> waitingGameClients = new ArrayList<>();
    private static final List<ClientHandler> waitingChatClients = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("Halma Server em execução...");

        Thread gameThread = new Thread(() -> listen(GAME_PORT, waitingGameClients, "Jogo"));
        Thread chatThread = new Thread(() -> listen(CHAT_PORT, waitingChatClients, "Chat"));

        gameThread.start();
        chatThread.start();

//        try (ServerSocket serverSocket = new ServerSocket(GAME_PORT)) {
//            while (true) {
//                Socket clientSocket = serverSocket.accept();
//                System.out.println("Novo cliente conectado: " + clientSocket.getInetAddress());
//                ClientHandler clientHandler = new ClientHandler(clientSocket);
//
//                clientHandler.start();
//
//                synchronized (waitingGameClients) {
//                    waitingGameClients.add(clientHandler);
//                    if (waitingGameClients.size() >= 2) {
//                        ClientHandler player1 = waitingGameClients.remove(0);
//                        ClientHandler player2 = waitingGameClients.remove(0);
//
//                        System.out.println("Pareando jogadores e iniciando nova partida.");
//                        GameSession gameSession = new GameSession(player1, player2);
//                        new Thread(gameSession).start();
//                    } else {
//                        //clientHandler.sendMessage(shared.Protocol.INFO + shared.Protocol.SEPARATOR + "Waiting for an opponent...");
//                    }
//                }
//            }
//        } catch (IOException e) {
//            System.err.println("Erro no servidor: " + e.getMessage());
//            e.printStackTrace();
//        }
    }

    private static void listen(int port, List<ClientHandler> waitingList, String type) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Servidor de " + type + " escutando na porta " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Novo cliente de " + type + " conectado: " + clientSocket.getInetAddress());
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandler.start();

                waitingList.add(clientHandler);
                checkForPair();
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor de " + type + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void checkForPair() {
        // Garante que a verificação é atômica
        synchronized (waitingGameClients) {
            if (waitingGameClients.size() >= 2 && waitingChatClients.size() >= 2) {
                System.out.println("Pareando jogadores e iniciando nova partida.");

                // Pega os dois primeiros de cada lista
                ClientHandler player1Game = waitingGameClients.remove(0);
                ClientHandler player2Game = waitingGameClients.remove(0);
                ClientHandler player1Chat = waitingChatClients.remove(0);
                ClientHandler player2Chat = waitingChatClients.remove(0);

                // Cria a sessão com as 4 conexões
                GameSession gameSession = new GameSession(player1Game, player2Game, player1Chat, player2Chat);
                new Thread(gameSession).start();
            }
        }
    }
}
