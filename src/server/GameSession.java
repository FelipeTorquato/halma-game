package server;

import game.Board;
import shared.Protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class GameSession implements Runnable {
    private final ClientHandler player1;
    private final ClientHandler player2;
    private final Board board;
    private int currentPlayer;

    private int player1MoveCount = 0;
    private int player2MoveCount = 0;
    private int player1InvalidAttempts = 0;
    private int player2InvalidAttempts = 0;
    private final List<String> chatHistory = new ArrayList<>();
    private String winnerInfo = "O jogo encerrou inesperadamente.";
    private boolean gameEnded = false;

    private boolean isChainJumpActive = false;
    private int chainJumpRow;
    private int chainJumpCol;

    public GameSession(ClientHandler player1, ClientHandler player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.board = new Board();
        this.currentPlayer = 1; // Jogador 1 inicia o jogo

        // Linka essa sessão de jogo para os jogadores
        this.player1.setGameSession(this);
        this.player2.setGameSession(this);
    }

    @Override
    public void run() {
        // Notificando jogadores que o jogo esta iniciando
        player1.sendMessage(Protocol.WELCOME + Protocol.SEPARATOR + "1");
        player2.sendMessage(Protocol.WELCOME + Protocol.SEPARATOR + "2");

        player1.sendMessage(Protocol.OPPONENT_FOUND);
        player2.sendMessage(Protocol.OPPONENT_FOUND);

        player1.sendMessage(Protocol.GAME_START);
        player2.sendMessage(Protocol.GAME_START);

        // Inicia turno
        updateTurn();
    }

    private void updateTurn() {
        if (currentPlayer == 1) {
            player1.sendMessage(Protocol.SET_TURN + Protocol.SEPARATOR + "YOUR_TURN");
            player2.sendMessage(Protocol.SET_TURN + Protocol.SEPARATOR + "OPPONENT_TURN");
        } else {
            player2.sendMessage(Protocol.SET_TURN + Protocol.SEPARATOR + "YOUR_TURN");
            player1.sendMessage(Protocol.SET_TURN + Protocol.SEPARATOR + "OPPONENT_TURN");
        }
    }

    // Realiza a troca de turno
    private void switchTurn() {
        currentPlayer = (currentPlayer == 1) ? 2 : 1;
        updateTurn();
    }

    public synchronized void processMessage(String message, ClientHandler sender) {
        String[] parts = message.split(Protocol.SEPARATOR, 2);
        String command = parts[0];
        int senderId = (sender == player1) ? 1 : 2;

        switch (command) {
            case Protocol.MOVE:
                if (senderId == currentPlayer) {
                    handleMove(parts[1], sender);
                } else {
                    sender.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Não é o seu turno.");
                    if (senderId == 1) player1InvalidAttempts++;
                    else player2InvalidAttempts++;
                }
                break;
            case Protocol.CHAT:
                broadcastChat(parts[1], senderId);
                break;
            case Protocol.FORFEIT:
                handleForfeit(sender);
                break;
            case Protocol.END_CHAIN_JUMP:
                if (isChainJumpActive && senderId == currentPlayer) {
                    isChainJumpActive = false;
                    ClientHandler opponent = (sender == player1) ? player2 : player1;
                    if (board.checkForWinner(currentPlayer)) {
                        winnerInfo = "Jogador " + currentPlayer + " ganhou por chegar no destino!";
                        endGame(sender, opponent, Protocol.VICTORY, Protocol.DEFEAT);
                    } else {
                        switchTurn();
                    }
                }
                break;
        }
    }

    private void handleMove(String moveData, ClientHandler sender) {
        try {
            String[] coords = moveData.split(Protocol.SEPARATOR);
            int startRow = Integer.parseInt(coords[0]);
            int startCol = Integer.parseInt(coords[1]);
            int endRow = Integer.parseInt(coords[2]);
            int endCol = Integer.parseInt(coords[3]);
            int senderId = (sender == player1) ? 1 : 2;

            if (isChainJumpActive) {
                if (startRow != chainJumpRow || startCol != chainJumpCol) {
                    sender.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Você deve continuar pulando com a mesma peça.");
                    return;
                }
            }

            if (board.movePiece(startRow, startCol, endRow, endCol, currentPlayer, isChainJumpActive)) {
                if (senderId == 1) player1MoveCount++;
                else player2MoveCount++;
//                String moveMessage = Protocol.VALID_MOVE + Protocol.SEPARATOR + moveData;

                boolean wasJump = Math.abs(startRow - endRow) > 1 || Math.abs(startCol - endCol) > 1;

//                sender.sendMessage(moveMessage);

                ClientHandler opponent = (sender == player1) ? player2 : player1;
//                opponent.sendMessage(Protocol.OPPONENT_MOVED + Protocol.SEPARATOR + moveData);

                if (wasJump && board.canJumpFrom(endRow, endCol)) {
                    isChainJumpActive = true;
                    chainJumpRow = endRow;
                    chainJumpCol = endCol;

                    String jumpMessage = Protocol.JUMP_MOVE + Protocol.SEPARATOR + moveData;
                    sender.sendMessage(jumpMessage);

                    opponent.sendMessage(Protocol.OPPONENT_MOVED + Protocol.SEPARATOR + moveData);

                    // Informa o jogador e aguarda a sua decisão
                    sender.sendMessage(Protocol.CHAIN_JUMP_OFFER + Protocol.SEPARATOR + endRow + Protocol.SEPARATOR + endCol);
                } else {
                    isChainJumpActive = false;

                    String moveMessage = Protocol.VALID_MOVE + Protocol.SEPARATOR + moveData;
                    sender.sendMessage(moveMessage);
                    opponent.sendMessage(Protocol.OPPONENT_MOVED + Protocol.SEPARATOR + moveData);

                    if (board.checkForWinner(currentPlayer)) {
                        if (gameEnded) return;
                        gameEnded = true;

                        opponent = (sender == player1) ? player2 : player1;
                        winnerInfo = "Jogador " + currentPlayer + " ganhou por chegar no destino!";

                        endGame(sender, opponent, Protocol.VICTORY, Protocol.DEFEAT);
                    } else {
                        switchTurn();
                    }

                }
            } else {
                if (senderId == 1) player1InvalidAttempts++;
                else player2InvalidAttempts++;
                sender.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Movimento inválido.");
            }
        } catch (Exception e) {
            sender.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Comando de movimento malformado.");
        }
    }

    private void broadcastChat(String chatMessage, int senderId) {
        String formattedMessage = Protocol.CHAT_MESSAGE + Protocol.SEPARATOR + "Jogador " + senderId + ": " + chatMessage;
        player1.sendMessage(formattedMessage);
        player2.sendMessage(formattedMessage);
        String newFormattedMessage = "Jogador " + senderId + ": " + chatMessage;
        chatHistory.add(newFormattedMessage);
    }

    private void handleForfeit(ClientHandler forfeiter) {
        if (gameEnded) return;
        gameEnded = true;

        ClientHandler winner = (forfeiter == player1) ? player2 : player1;
        int winnerId = (winner == player1) ? 1 : 2;
        winnerInfo = "Jogador " + winnerId + " ganhou pela desistência do oponente.";

        endGame(winner, forfeiter, Protocol.OPPONENT_FORFEIT, Protocol.DEFEAT + Protocol.SEPARATOR + "Você desistiu da partida.");
    }

    public synchronized void handleDisconnect(ClientHandler disconnectedPlayer) {
        if (gameEnded) return;
        gameEnded = true;

        ClientHandler winner = (disconnectedPlayer == player1) ? player2 : player1;
        int winnerId = (winner == player1) ? 1 : 2;
        winnerInfo = "Jogador " + winnerId + " ganhou porque o oponente se desconectou.";
        sendGameOverStats();

        winner.sendMessage(Protocol.OPPONENT_FORFEIT);

        winner.shutdown();
        disconnectedPlayer.shutdown();
    }

    private void shutdownAllConnections() {
        player1.shutdown();
        player2.shutdown();
    }

    private void endGame(ClientHandler winner, ClientHandler loser, String winMessage, String loseMessage) {
        if (gameEnded) return;
        gameEnded = true;

        sendGameOverStats();

        winner.sendMessage(winMessage);
        if (!loseMessage.isEmpty()) {
            loser.sendMessage(loseMessage);
        }

        shutdownAllConnections();
    }

    private void sendGameOverStats() {
        String chatLog = String.join("|", chatHistory);

        StringJoiner stats = new StringJoiner(Protocol.SEPARATOR);
        stats.add(winnerInfo);
        stats.add(String.valueOf(player1MoveCount));
        stats.add(String.valueOf(player1InvalidAttempts));
        stats.add(String.valueOf(player2MoveCount));
        stats.add(String.valueOf(player2InvalidAttempts));
        stats.add(chatLog);

        String message = Protocol.GAME_OVER_STATS + Protocol.SEPARATOR + stats.toString();
        player1.sendMessage(message);
        player2.sendMessage(message);
    }
}
