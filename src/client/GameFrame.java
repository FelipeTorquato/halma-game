package client;

import game.Board;
import game.Piece;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class GameFrame extends JFrame {
    private final HalmaClient client;
    private final BoardPanel boardPanel;
    private final JTextArea chatArea;
    private final JTextField chatInput;
    private final JLabel statusLabel;
    private final Board board;
    private int selectedRow = -1;
    private int selectedCol = -1;
    private int playerId;
    private boolean myTurn = false;

    public GameFrame(HalmaClient client) {
        this.client = client;
        this.board = new Board();

        setTitle("Halma Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Painel do tabuleiro
        boardPanel = new BoardPanel();
        add(boardPanel, BorderLayout.CENTER);

        // Painel de controle e chat
        JPanel eastPanel = new JPanel();
        eastPanel.setLayout(new BorderLayout(10, 10));

        // Texto de status do jogador
        statusLabel = new JLabel("Conecte a um servidor para iniciar.", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        eastPanel.add(statusLabel, BorderLayout.NORTH);

        // Chat
        chatArea = new JTextArea(15, 25);
        chatArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        eastPanel.add(chatScrollPane, BorderLayout.CENTER);

        // Botão do Chat
        JPanel chatInputPanel = new JPanel(new BorderLayout());
        chatInput = new JTextField();
        JButton sendButton = new JButton("Enviar");
        sendButton.addActionListener(this::sendChat);
        chatInput.addActionListener(this::sendChat);
        chatInputPanel.add(chatInput, BorderLayout.CENTER);
        chatInputPanel.add(sendButton, BorderLayout.EAST);
        eastPanel.add(chatInputPanel, BorderLayout.SOUTH);

        add(eastPanel, BorderLayout.EAST);

        // Parte de baixo
        JPanel bottomPanel = new JPanel();
        JButton forfeitButton = new JButton("Desistir do jogo");
        forfeitButton.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(this, "Você tem certeza que deseja desistir?", "Desistência", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                client.sendForfeit();

            }
        });
        bottomPanel.add(forfeitButton);
        add(bottomPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    public void updateBoardAfterJumpAndPrompt(int endRow, int endCol) {
        // Find which piece just moved to endRow, endCol. This is complex.
        // A simpler way: The server tells the opponent about the move.
        // The player who moved gets the CHAIN_JUMP_OFFER. They already know their piece moved.
        // We just need to prompt them and force their next selection.

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Outro pulo está disponível. Você deseja continuar pulando?",
                "Sequência de pulos",
                JOptionPane.YES_NO_OPTION
        );

        if (choice == JOptionPane.YES_OPTION) {
            // Seleciona a posição para o próximo pulo
            this.selectedRow = endRow;
            this.selectedCol = endCol;
            updateStatus("Seu turno: Continue pulando com a peça selecionada.");
            boardPanel.repaint();
        } else {
            // Jogador não quis continuar
            client.sendEndChainJump();
            this.selectedRow = -1;
            this.selectedCol = -1;
        }
    }

    public void setPlayerId(int id) {
        this.playerId = id;
        setTitle("Halma Game - Jogador " + id);
    }

    public void setMyTurn(boolean myTurn) {
        this.myTurn = myTurn;
        updateStatus(myTurn ? "Seu turno." : "Turno do oponente.");
    }

    public void updateStatus(String text) {
        statusLabel.setText(text);
    }

    public void addChatMessage(String message) {
        chatArea.append(message + "\n");
    }

    public void updateBoard(int startRow, int startCol, int endRow, int endCol) {
        board.performMove(startRow, startCol, endRow, endCol);
        this.selectedRow = -1; // Garantir que a seleção da peça é limpa depois de um movimento
        this.selectedCol = -1;
        boardPanel.repaint();
    }

    private void sendChat(ActionEvent e) {
        String message = chatInput.getText().trim();
        if (!message.isEmpty()) {
            client.sendChatMessage(message);
            chatInput.setText("");
        }
    }

    private class BoardPanel extends JPanel {
        BoardPanel() {
            setPreferredSize(new Dimension(500, 500));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!myTurn) return;

                    int cellWidth = getWidth() / Board.SIZE;
                    int cellHeight = getHeight() / Board.SIZE;

                    int col = e.getX() / cellWidth;
                    int row = e.getY() / cellHeight;

                    Piece clickedPiece = board.getPieceAt(row, col);

                    if (selectedRow == -1) { // Seleciona uma peça
                        if (clickedPiece != null && clickedPiece.getPlayerId() == playerId) {
                            selectedRow = row;
                            selectedCol = col;
                        }
                    } else { // Move a peça selecionada
                        client.sendMove(selectedRow, selectedCol, row, col);
                        selectedRow = -1;
                        selectedCol = -1;
                    }
                    repaint();
                }
            });
        }

        private boolean isPlayer1BasePrecise(int row, int col) {
            return (row == 0 && col <= 2) || (row == 1 && col <= 2) || (row == 2 && col <= 1) || (row == 3 && col == 0);
        }

        private boolean isPlayer2BasePrecise(int row, int col) {
            int size = Board.SIZE;
            return (row == size - 1 && col >= size - 3) || (row == size - 2 && col >= size - 3) ||
                    (row == size - 3 && col >= size - 2) || (row == size - 4 && col >= size - 1);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int cellWidth = getWidth() / Board.SIZE;
            int cellHeight = getHeight() / Board.SIZE;

            for (int row = 0; row < Board.SIZE; row++) {
                for (int col = 0; col < Board.SIZE; col++) {
                    // Pinta o quadrado de acordo com a cor base de jogador
                    if (isPlayer1BasePrecise(row, col)) {
                        g.setColor(new Color(70, 70, 70)); // Cinza escuro para a base do jogador 1
                    } else if (isPlayer2BasePrecise(row, col)) {
                        g.setColor(new Color(210, 210, 210)); // Cinza claro para a base do jogador 2
                    } else {
                        // Se não for uma base, pinta a cor normal do tabuleiro
                        if ((row + col) % 2 == 0) {
                            g.setColor(new Color(240, 217, 181)); // Quadrado claro
                        } else {
                            g.setColor(new Color(181, 136, 99)); // Quadrado escuro
                        }
                    }
                    g.fillRect(col * cellWidth, row * cellHeight, cellWidth, cellHeight);

                    // Desenha as peças nos quadrados
                    Piece piece = board.getPieceAt(row, col);
                    if (piece != null) {
                        if (piece.getPlayerId() == 1) {
                            g.setColor(Color.BLACK);
                        } else {
                            g.setColor(Color.WHITE);
                        }
                        g.fillOval(col * cellWidth + 5, row * cellHeight + 5, cellWidth - 10, cellHeight - 10);
                    }

                    // Destaca a peça selecionada
                    if (row == selectedRow && col == selectedCol) {
                        g.setColor(Color.CYAN);
                        Graphics2D g2d = (Graphics2D) g;
                        g2d.setStroke(new BasicStroke(3));
                        g.drawRect(col * cellWidth + 1, row * cellHeight + 1, cellWidth - 3, cellHeight - 3);
                        g2d.setStroke(new BasicStroke(1));
                    }
                }
            }
        }

//        @Override
//        protected void paintComponent(Graphics g) {
//            super.paintComponent(g);
//            int cellWidth = getWidth() / Board.SIZE;
//            int cellHeight = getHeight() / Board.SIZE;
//
//            for (int row = 0; row < Board.SIZE; row++) {
//                for (int col = 0; col < Board.SIZE; col++) {
//                    // Draw board squares
//                    if ((row + col) % 2 == 0) {
//                        g.setColor(new Color(240, 217, 181)); // Light square
//                    } else {
//                        g.setColor(new Color(181, 136, 99)); // Dark square
//                    }
//                    g.fillRect(col * cellWidth, row * cellHeight, cellWidth, cellHeight);
//
//                    // Draw pieces
//                    Piece piece = board.getPieceAt(row, col);
//                    if (piece != null) {
//                        if (piece.getPlayerId() == 1) {
//                            g.setColor(Color.BLACK);
//                        } else {
//                            g.setColor(Color.WHITE);
//                        }
//                        g.fillOval(col * cellWidth + 5, row * cellHeight + 5, cellWidth - 10, cellHeight - 10);
//                    }
//
//                    // Highlight selected piece
//                    if (row == selectedRow && col == selectedCol) {
//                        g.setColor(Color.CYAN);
//                        g.drawRect(col * cellWidth, row * cellHeight, cellWidth - 1, cellHeight - 1);
//                    }
//                }
//            }
//        }
    }

    public void closeApplication() {
        dispose();
        System.exit(0);
    }
}
