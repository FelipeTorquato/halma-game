package game;

/**
 * Cada peça tem o playerId o qual pertence.
 */
public class Piece {
    private final int playerId;

    public Piece(int playerId) {
        this.playerId = playerId;
    }

    public int getPlayerId() {
        return playerId;
    }
}
