package game;

/**
 * Gera o tabuleiro de tamanho 8, posiciona as peças de acordo com as cores e jogadores.
 */
public class Board {
    public static final int SIZE = 8;
    private final Piece[][] grid;

    public Board() {
        grid = new Piece[SIZE][SIZE];
        setupPieces();
    }

    private void setupPieces() {
        // Player 1 (Peças pretas no canto superior esquerdo do tabuleiro)
        grid[0][0] = new Piece(1);
        grid[0][1] = new Piece(1);
        grid[0][2] = new Piece(1);
        grid[1][0] = new Piece(1);
        grid[1][1] = new Piece(1);
        grid[1][2] = new Piece(1);
        grid[2][0] = new Piece(1);
        grid[2][1] = new Piece(1);
        grid[3][0] = new Piece(1);

        // Player 2 (Peças brancas no canto inferior direito do tabuleiro)
        grid[SIZE - 1][SIZE - 1] = new Piece(2);
        grid[SIZE - 1][SIZE - 2] = new Piece(2);
        grid[SIZE - 1][SIZE - 3] = new Piece(2);
        grid[SIZE - 2][SIZE - 1] = new Piece(2);
        grid[SIZE - 2][SIZE - 2] = new Piece(2);
        grid[SIZE - 2][SIZE - 3] = new Piece(2);
        grid[SIZE - 3][SIZE - 1] = new Piece(2);
        grid[SIZE - 3][SIZE - 2] = new Piece(2);
        grid[SIZE - 4][SIZE - 1] = new Piece(2);
    }

    /**
     * Verifica se a peça selecionada está em posição válida
     * @param row A linha da peça clicada
     * @param col A coluna da peça clicada
     * @return A peça se a posição for válida, senão, retorna null
     */
    public Piece getPieceAt(int row, int col) {
        if (isValidCoordinate(row, col)) {
            return grid[row][col];
        }
        return null;
    }

    /**
     * Função responsável por executar o movimento da peça no tabuleiro. Utilizada pela função de atualizar o estado do
     * tabuleiro. Primeiro valida a posição da peça clicada, e altera o estado do tabuleiro.
     * @param startRow Linha inicial
     * @param startCol Coluna inicial
     * @param endRow Linha final
     * @param endCol Coluna final
     */
    public void performMove(int startRow, int startCol, int endRow, int endCol) {
        // Verificação de segurança da peça selecionada
        if (getPieceAt(startRow, startCol) == null) {
            return;
        }
        Piece piece = grid[startRow][startCol];
        grid[startRow][startCol] = null;
        grid[endRow][endCol] = piece;
    }

    /**
     * Função responsável por mover a peça e atualizar o estado dela.
     * @param startRow Linha inicial
     * @param startCol Coluna inicial
     * @param endRow Linha final
     * @param endCol Coluna final
     * @param player
     * @return Retorna se é válido o movimento da peça e altera a posição da peça.
     */
    public boolean movePiece(int startRow, int startCol, int endRow, int endCol, int player) {
        return movePiece(startRow, startCol, endRow, endCol, player, false) ;
    }

    /**
     * Metodo sobrecarregado para validar apenas pulos.
     * @param jumpOnly Se verdadeiro, apenas movimentos de pulo são permitidos.
     * @return Retorna se é válido o movimento da peça e altera a posição da peça.
     */
    public boolean movePiece(int startRow, int startCol, int endRow, int endCol, int player, boolean jumpOnly) {
        // Valida o movimento
        if (!isValidMove(startRow, startCol, endRow, endCol, player, jumpOnly)) {
            return false;
        }
        Piece piece = grid[startRow][startCol];
        grid[startRow][startCol] = null;
        grid[endRow][endCol] = piece;
        return true;
    }

    // Verifica se o movimento enviado é válido
    private boolean isValidMove(int startRow, int startCol, int endRow, int endCol, int player, boolean jumpOnly) {
        // Validaçao básica
        if (!isValidCoordinate(startRow, startCol) || !isValidCoordinate(endRow, endCol)) {
            return false;
        }

        if (grid[endRow][endCol] != null) {
            return false; // O endereço de destino deve ser null, não ter nenhuma peça
        }

        Piece piece = grid[startRow][startCol];
        if (piece == null || piece.getPlayerId() != player) {
            return false; // Um player só pode mexer se a peça for sua
        }

        // Verifica se há movimento adjacente
        int rowDiff = Math.abs(startRow - endRow);
        int colDiff = Math.abs(startCol - endCol);
        boolean isJump = (rowDiff == 2 && colDiff == 0) || (rowDiff == 0 && colDiff == 2) || (rowDiff == 2 && colDiff == 2);

        if (isJump) {
            int jumpedRow = startRow + (endRow - startRow) / 2;
            int jumpedCol = startCol + (endCol - startCol) / 2;
            // Um pulo é válido se houver uma peça para pular por cima.
            return grid[jumpedRow][jumpedCol] != null;
        }

        if (jumpOnly) {
            return false;
        }

        if (rowDiff <= 1 && colDiff <= 1) {
            return true; // Valida movimento adjacente
        }

        return false; // Invalid move
    }

    // Sequência de pulos
    public boolean canJumpFrom(int row, int col) {
        // Verifica as 8 direções para potenciais pulos
        for (int r = -2; r <= 2; r += 2) {
            for (int c = -2; c <= 2; c += 2) {
                if (r == 0 && c == 0) {
                    continue; // Pula o ponto central
                }

                int destRow = row + r;
                int destCol = col + c;
                int jumpedRow = row + r / 2;
                int jumpedCol = col + c / 2;

                // Verifica se o destino é válido e se está vazio
                if (isValidCoordinate(destRow, destCol) && getPieceAt(destRow, destCol) == null) {
                    // Verifica se há um peça para pular por cima
                    if (isValidCoordinate(jumpedRow, jumpedCol) && getPieceAt(jumpedRow, jumpedCol) != null) {
                        return true; // Foi encontrado um pulo válido
                    }
                }
            }
        }
        return false; // Pulos não foram encontrados
    }

    /**
     * Responsável por verificar se, após uma jogada, o movimento realizado levou a uma vitória
     * @param player Id do player
     * @return Se um jogador foi dado com ganhador depois de uma jogada
     */
    public boolean checkForWinner(int player) {
        if (player == 1) { // Player 2
            // Verifica se as peças do player 1 estão na posição inicial do player 2
            if (getPieceAt(SIZE - 1, SIZE - 1) == null || getPieceAt(SIZE - 1, SIZE - 1).getPlayerId() != 1) {
                return false;
            }
            if (getPieceAt(SIZE - 1, SIZE - 2) == null || getPieceAt(SIZE - 1, SIZE - 2).getPlayerId() != 1) {
                return false;
            }
            if (getPieceAt(SIZE - 1, SIZE - 3) == null || getPieceAt(SIZE - 1, SIZE - 3).getPlayerId() != 1) {
                return false;
            }
            if (getPieceAt(SIZE - 2, SIZE - 1) == null || getPieceAt(SIZE - 2, SIZE - 1).getPlayerId() != 1) {
                return false;
            }
            if (getPieceAt(SIZE - 2, SIZE - 2) == null || getPieceAt(SIZE - 2, SIZE - 2).getPlayerId() != 1) {
                return false;
            }
            if (getPieceAt(SIZE - 2, SIZE - 3) == null || getPieceAt(SIZE - 2, SIZE - 3).getPlayerId() != 1) {
                return false;
            }
            if (getPieceAt(SIZE - 3, SIZE - 1) == null || getPieceAt(SIZE - 3, SIZE - 1).getPlayerId() != 1) {
                return false;
            }
            if (getPieceAt(SIZE - 3, SIZE - 2) == null || getPieceAt(SIZE - 3, SIZE - 2).getPlayerId() != 1) {
                return false;
            }
            if (getPieceAt(SIZE - 4, SIZE - 1) == null || getPieceAt(SIZE - 4, SIZE - 1).getPlayerId() != 1) {
                return false;
            }
            return true;
        } else { // Player 2
            // Verifica se as peças do player 2 estão na posição inicial do player 1
            if (getPieceAt(0, 0) == null || getPieceAt(0, 0).getPlayerId() != 2) return false;
            if (getPieceAt(0, 1) == null || getPieceAt(0, 1).getPlayerId() != 2) return false;
            if (getPieceAt(0, 2) == null || getPieceAt(0, 2).getPlayerId() != 2) return false;
            if (getPieceAt(1, 0) == null || getPieceAt(1, 0).getPlayerId() != 2) return false;
            if (getPieceAt(1, 1) == null || getPieceAt(1, 1).getPlayerId() != 2) return false;
            if (getPieceAt(1, 2) == null || getPieceAt(1, 2).getPlayerId() != 2) return false;
            if (getPieceAt(2, 0) == null || getPieceAt(2, 0).getPlayerId() != 2) return false;
            if (getPieceAt(2, 1) == null || getPieceAt(2, 1).getPlayerId() != 2) return false;
            if (getPieceAt(3, 0) == null || getPieceAt(3, 0).getPlayerId() != 2) return false;
            return true;
        }
    }

    // Verifica se a coordenada é válida, estando dentro dos limites do tabuleiro
    private boolean isValidCoordinate(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }
}
