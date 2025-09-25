package shared;

public class Protocol {
    public static final String SEPARATOR = ":";

    // Comandos do Cliente para o Servidor
    public static final String MOVE = "MOVE";
    public static final String CHAT = "CHAT";
    public static final String FORFEIT = "FORFEIT";
    public static final String END_CHAIN_JUMP = "END_CHAIN_JUMP";

    // Comandos do Servidor para o Cliente
    public static final String GAME_OVER_STATS = "GAME_OVER_STATS";
    public static final String WELCOME = "WELCOME";
    public static final String GAME_START = "GAME_START";
    public static final String OPPONENT_FOUND = "OPPONENT_FOUND";
    public static final String VALID_MOVE = "VALID_MOVE";
    public static final String OPPONENT_MOVED = "OPPONENT_MOVED";
    public static final String SET_TURN = "SET_TURN";
    public static final String CHAT_MESSAGE = "CHAT_MESSAGE";
    public static final String VICTORY = "VICTORY";
    public static final String DEFEAT = "DEFEAT";
    public static final String OPPONENT_FORFEIT = "OPPONENT_FORFEIT";
    public static final String CHAIN_JUMP_OFFER = "CHAIN_JUMP_OFFER";
    public static final String INFO = "INFO";
    public static final String ERROR = "ERROR";
}
