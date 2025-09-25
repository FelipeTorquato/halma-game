package client;

import shared.Protocol;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class HalmaClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final GameFrame gameFrame;
    private String lastGameStats;

    public HalmaClient() {
        gameFrame = new GameFrame(this);
        gameFrame.setVisible(true);

        String serverAddress = JOptionPane.showInputDialog(gameFrame, "Entre com o endereço IP:", "localhost");
        if (serverAddress != null && !serverAddress.trim().isEmpty()) {
            connect(serverAddress, 12345);
        } else {
            System.exit(0);
        }

    }

    public void shutdown() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Erro durante fechamento do socket cliente: " + e.getMessage());
        }
    }

    public void connect(String serverAddress, int port) {
        try {
            socket = new Socket(serverAddress, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            new Thread(new ServerListener()).start();
            gameFrame.updateStatus("Conectado. Aguardando por um oponente...");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(gameFrame, "Não foi possível se conectar ao servidor.", "Erro de conexão", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    public void sendMove(int startRow, int startCol, int endRow, int endCol) {
        String message = Protocol.MOVE + Protocol.SEPARATOR + startRow + Protocol.SEPARATOR + startCol + Protocol.SEPARATOR + endRow + Protocol.SEPARATOR + endCol;
        out.println(message);
    }

    public void sendChatMessage(String message) {
        out.println(Protocol.CHAT + Protocol.SEPARATOR + message);
    }

    public void sendForfeit() {
        out.println(Protocol.FORFEIT);
    }

    public void sendEndChainJump() {
        out.println(Protocol.END_CHAIN_JUMP);
    }

    /**
     * Thread dedicada a escutar mensagens do servidor.
     * Previne da interface congelar enquanto aguarda dados de entrada e saída da rede.
     */
    private class ServerListener implements Runnable {
        @Override
        public void run() {
            try {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                    processServerMessage(serverMessage);
                }
            } catch (IOException e) {
                gameFrame.updateStatus("Conexão com o servidor perdida.");
                System.err.println("Erro ao ler do servidor: " + e.getMessage());
            }
        }

        private void processServerMessage(String message) {
            String[] parts = message.split(Protocol.SEPARATOR, 2);
            String command = parts[0];

            //
            SwingUtilities.invokeLater(() -> {
                switch (command) {
                    case Protocol.GAME_OVER_STATS:
                        lastGameStats = parts[1];
                        break;
                    case Protocol.WELCOME:
                        int playerId = Integer.parseInt(parts[1]);
                        gameFrame.setPlayerId(playerId);
                        break;
                    case Protocol.INFO:
                        gameFrame.updateStatus(parts[1]);
                        break;
                    case Protocol.OPPONENT_FOUND:
                        gameFrame.updateStatus("Oponente encontrado. Iniciando partida...");
                        break;
                    case Protocol.SET_TURN:
                        boolean isMyTurn = "YOUR_TURN".equals(parts[1]);
                        gameFrame.setMyTurn(isMyTurn);
                        break;
                    case Protocol.VALID_MOVE:
                    case Protocol.OPPONENT_MOVED:
                        String[] coords = parts[1].split(Protocol.SEPARATOR);
                        int startRow = Integer.parseInt(coords[0]);
                        int startCol = Integer.parseInt(coords[1]);
                        int endRow = Integer.parseInt(coords[2]);
                        int endCol = Integer.parseInt(coords[3]);
                        gameFrame.updateBoard(startRow, startCol, endRow, endCol);
                        break;
                    case Protocol.CHAT_MESSAGE:
                        gameFrame.addChatMessage(parts[1]);
                        break;
                    case Protocol.CHAIN_JUMP_OFFER:
                        String[] newCoords = parts[1].split(Protocol.SEPARATOR);
                        int newEndRow = Integer.parseInt(newCoords[0]);
                        int newEndCol = Integer.parseInt(newCoords[1]);
                        gameFrame.updateBoardAfterJumpAndPrompt(newEndRow, newEndCol);
                        break;
                    case Protocol.VICTORY:
                        JOptionPane.showMessageDialog(gameFrame, "Parabéns, você ganhou!", "Fim de jogo", JOptionPane.INFORMATION_MESSAGE);
                        showResultsAndExit();
                        break;
                    case Protocol.DEFEAT:
                        JOptionPane.showMessageDialog(gameFrame, "Você perdeu a partida.", "Fim de jogo", JOptionPane.WARNING_MESSAGE);
                        showResultsAndExit();
                        break;
                    case Protocol.OPPONENT_FORFEIT:
                        JOptionPane.showMessageDialog(gameFrame, "seu oponente dessitiu. Você ganhou!", "Vitória", JOptionPane.INFORMATION_MESSAGE);
                        showResultsAndExit();
                        break;
                    case Protocol.ERROR:
                        JOptionPane.showMessageDialog(gameFrame, parts[1], "Erro", JOptionPane.ERROR_MESSAGE);
                        break;
                }
            });
        }

        private void showResultsAndExit() {
            if (lastGameStats != null) {
                ResultsDialog resultsDialog = new ResultsDialog(gameFrame, lastGameStats);
                resultsDialog.setVisible(true);
            }

            shutdown();
            gameFrame.closeApplication();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(HalmaClient::new);
    }
}
