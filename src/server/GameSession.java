package server;

import game.Board;
import shared.Protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class GameSession implements Runnable {
    private final ClientHandler player1Game;
    private final ClientHandler player2Game;
    private final ClientHandler player1Chat;
    private final ClientHandler player2Chat;
    private final Board board;
    private int currentPlayer;

    private int player1GameMoveCount = 0;
    private int player2GameMoveCount = 0;
    private int player1GameInvalidAttempts = 0;
    private int player2GameInvalidAttempts = 0;
    private final List<String> chatHistory = new ArrayList<>();
    private String winnerInfo = "O jogo encerrou inesperadamente.";
    private boolean gameEnded = false;

    private boolean isChainJumpActive = false;
    private int chainJumpRow;
    private int chainJumpCol;

    public GameSession(ClientHandler player1Game, ClientHandler player2Game, ClientHandler player1Chat, ClientHandler player2Chat) {
        this.player1Game = player1Game;
        this.player1Chat = player1Chat;
        this.player2Game = player2Game;
        this.player2Chat = player2Chat;
        this.board = new Board();
        this.currentPlayer = 1; // Jogador 1 inicia o jogo

        // Linka essa sessão de jogo para os jogadores
        this.player1Game.setGameSession(this);
        this.player1Chat.setGameSession(this);
        this.player2Game.setGameSession(this);
        this.player2Chat.setGameSession(this);
    }

    @Override
    public void run() {
        // Notificando jogadores que o jogo esta iniciando
        player1Game.sendMessage(Protocol.WELCOME + Protocol.SEPARATOR + "1");
        player2Game.sendMessage(Protocol.WELCOME + Protocol.SEPARATOR + "2");

        player1Game.sendMessage(Protocol.OPPONENT_FOUND);
        player2Game.sendMessage(Protocol.OPPONENT_FOUND);

        player1Game.sendMessage(Protocol.GAME_START);
        player2Game.sendMessage(Protocol.GAME_START);

        // Inicia turno
        updateTurn();
    }

    private void updateTurn() {
        if (currentPlayer == 1) {
            player1Game.sendMessage(Protocol.SET_TURN + Protocol.SEPARATOR + "YOUR_TURN");
            player2Game.sendMessage(Protocol.SET_TURN + Protocol.SEPARATOR + "OPPONENT_TURN");
        } else {
            player2Game.sendMessage(Protocol.SET_TURN + Protocol.SEPARATOR + "YOUR_TURN");
            player1Game.sendMessage(Protocol.SET_TURN + Protocol.SEPARATOR + "OPPONENT_TURN");
        }
    }

    // Realiza a troca de turno
    private void switchTurn() {
        currentPlayer = (currentPlayer == 1) ? 2 : 1;
        updateTurn();
    }

    public synchronized void processGameMessage(String message, ClientHandler sender) {
        if (gameEnded) return;
        String[] parts = message.split(Protocol.SEPARATOR, 2);
        String command = parts[0];
        int senderId = (sender == player1Game) ? 1 : 2;

        switch (command) {
            case Protocol.MOVE:
                if (senderId == currentPlayer) {
                    handleMove(parts[1], sender);
                } else {
                    sender.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Não é o seu turno.");
                    if (senderId == 1) player1GameInvalidAttempts++;
                    else player2GameInvalidAttempts++;
                }
                break;
            case Protocol.FORFEIT:
                handleForfeit(sender);
                break;
            case Protocol.END_CHAIN_JUMP:
                if (isChainJumpActive && senderId == currentPlayer) {
                    isChainJumpActive = false;
                    ClientHandler opponent = (sender == player1Game) ? player2Game : player1Game;
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

    // Metodo para processar apenas mensagens de chat
    public synchronized void processChatMessage(String message, ClientHandler sender) {
        if (gameEnded) return;

        String[] parts = message.split(Protocol.SEPARATOR, 2);
        int senderId = (sender == player1Chat) ? 1 : 2;

        if (Protocol.CHAT.equals(parts[0])) {
            broadcastChat(parts[1], senderId);
        }
    }


    private void handleMove(String moveData, ClientHandler sender) {
        try {
            String[] coords = moveData.split(Protocol.SEPARATOR);
            int startRow = Integer.parseInt(coords[0]), startCol = Integer.parseInt(coords[1]);
            int endRow = Integer.parseInt(coords[2]), endCol = Integer.parseInt(coords[3]);
            int senderId = (sender == player1Game) ? 1 : 2;

            if (isChainJumpActive) {
                if (startRow != chainJumpRow || startCol != chainJumpCol) {
                    sender.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Você deve continuar pulando com a mesma peça.");
                    return;
                }
            }

            if (board.movePiece(startRow, startCol, endRow, endCol, currentPlayer, isChainJumpActive)) {
                if (senderId == 1) player1GameMoveCount++;
                else player2GameMoveCount++;
//                String moveMessage = Protocol.VALID_MOVE + Protocol.SEPARATOR + moveData;

                ClientHandler opponent = (sender == player1Game) ? player2Game : player1Game;
                boolean wasJump = Math.abs(startRow - endRow) > 1 || Math.abs(startCol - endCol) > 1;

//                sender.sendMessage(moveMessage);

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
                        winnerInfo = "Jogador " + currentPlayer + " ganhou por chegar no destino!";
                        endGame(sender, opponent, Protocol.VICTORY, Protocol.DEFEAT);
                    } else {
                        switchTurn();
                    }
                }
            } else {
                if (senderId == 1) player1GameInvalidAttempts++;
                else player2GameInvalidAttempts++;
                sender.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Movimento inválido.");
            }
        } catch (Exception e) {
            sender.sendMessage(Protocol.ERROR + Protocol.SEPARATOR + "Comando de movimento malformado.");
        }
    }

    private void broadcastChat(String chatMessage, int senderId) {
        String formattedMessage = Protocol.CHAT_MESSAGE + Protocol.SEPARATOR + "Jogador " + senderId + ": " + chatMessage;
        player1Chat.sendMessage(formattedMessage);
        player2Chat.sendMessage(formattedMessage);
        String newFormattedMessage = "Jogador " + senderId + ": " + chatMessage;
        chatHistory.add(newFormattedMessage);
    }

    private void handleForfeit(ClientHandler forfeiterGame) {
        ClientHandler winnerGame = (forfeiterGame == player1Game) ? player2Game : player1Game;
        int winnerId = (winnerGame == player1Game) ? 1 : 2;
        winnerInfo = "Jogador " + winnerId + " ganhou pela desistência do oponente.";
        endGame(winnerGame, forfeiterGame, Protocol.OPPONENT_FORFEIT, Protocol.DEFEAT + Protocol.SEPARATOR + "Você desistiu da partida.");
    }

    private void shutdownAllConnections() {
        player1Game.shutdown();
        player2Game.shutdown();
        player1Chat.shutdown();
        player2Chat.shutdown();
    }

    public synchronized void handleDisconnect(ClientHandler disconnectedHandler) {
        if (gameEnded) return;
        gameEnded = true;

        ClientHandler winnerGame = (disconnectedHandler == player1Game || disconnectedHandler == player1Chat) ? player2Game : player1Game;
        ClientHandler loserGame = (winnerGame == player1Game) ? player2Game : player1Game;
        int winnerId = (winnerGame == player1Game) ? 1 : 2;

        winnerInfo = "Jogador " + winnerId + " ganhou porque o oponente se desconectou.";
        endGame(winnerGame, loserGame, Protocol.OPPONENT_FORFEIT, ""); // Loser is already disconnected
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
        stats.add(String.valueOf(player1GameMoveCount));
        stats.add(String.valueOf(player1GameInvalidAttempts));
        stats.add(String.valueOf(player2GameMoveCount));
        stats.add(String.valueOf(player2GameInvalidAttempts));
        stats.add(chatLog);

        String message = Protocol.GAME_OVER_STATS + Protocol.SEPARATOR + stats.toString();
        player1Game.sendMessage(message);
        player2Game.sendMessage(message);
    }
}
