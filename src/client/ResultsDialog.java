package client;

import shared.Protocol;

import javax.swing.*;
import java.awt.*;

public class ResultsDialog extends JDialog {

    public ResultsDialog(Frame owner, String statsData) {
        super(owner, "Tela de resultados da partida", true);

        // Converte os dados da partida
        String[] parts = statsData.split(Protocol.SEPARATOR, 6);
        String winnerInfo = parts[0];
        String p1Moves = parts[1];
        String p1Invalid = parts[2];
        String p2Moves = parts[3];
        String p2Invalid = parts[4];
        String chatLog;

        if (parts.length > 5 && !parts[5].isEmpty()) {
            chatLog = parts[5].replace("|", "\n");
        } else {
            chatLog = "Sem histórico de conversas.";
        }

        // Configuração dos componentes de interface
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Informações do vencedor
        JLabel winnerLabel = new JLabel(winnerInfo, SwingConstants.CENTER);
        winnerLabel.setFont(new Font("Arial", Font.BOLD, 18));
        mainPanel.add(winnerLabel, BorderLayout.NORTH);

        // Painel de estatísticas
        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 10, 5));
        statsPanel.add(new JLabel("Movimentos do jogador 1: " + p1Moves));
        statsPanel.add(new JLabel("Movimentos do jogador 2: " + p2Moves));
        statsPanel.add(new JLabel("Tentativas inválidas do jogador 1: " + p1Invalid));
        statsPanel.add(new JLabel("Tentativas inválidas do jogador 2: " + p2Invalid));
        mainPanel.add(statsPanel, BorderLayout.CENTER);

        // Histórico de mensagens
        JTextArea chatArea = new JTextArea(10, 30);
        chatArea.setText(chatLog);
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setBorder(BorderFactory.createTitledBorder("Histórico de mensagens"));
        mainPanel.add(chatScrollPane, BorderLayout.SOUTH);

        // Botão de fechar
        JButton closeButton = new JButton("OK");
        closeButton.addActionListener(e -> dispose());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);

        getContentPane().add(mainPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }
}
