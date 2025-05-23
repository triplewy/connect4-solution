public class Board {
    public static final int ROWS = 4;
    public static final int COLS = 5;
    public static final int WIN_CONDITION = 4;

    public enum State {
        LIVE,
        TIED,
        PLAYER_1_WON,
        PLAYER_2_WON,
    }

    public enum Token {
        EMPTY(" "),
        PLAYER_1("X"),
        PLAYER_2("0");

        private final String name;

        Token(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    // board is the connect4 board
    private Token[][] board;
    // Current player's turn. We can just use boolean to represent player 1 or 2.
    private Boolean turn;

    // Constructor
    public Board() {
        // Initialize connect4 board
        this.board = new Token[ROWS][COLS];

        // Initialize board with empty spaces for presentation purposes
        for (int i = 0; i < this.board.length; i++) {
            for (int j = 0; j < this.board[i].length; j++) {
                this.board[i][j] = Token.EMPTY;
            }
        }

        // Initialize the turn
        this.turn = false;
    }

    public Token[][] cloneBoard() {
        Token[][] clone = new Token[ROWS][COLS];
        for (int i = 0; i < clone.length; i++) {
            for (int j = 0; j < clone[i].length; j++) {
                clone[i][j] = this.board[i][j];
            }
        }
        return clone;
    }

    public State turn(int col) throws Exception {
        // Check if the game is still live
        if (getGameState(this.board) != State.LIVE) {
            throw new Exception("Game is over");
        }
        // Check if can place token on the column by checking if first row has item
        if (this.board[ROWS - 1][col] != Token.EMPTY) {
            throw new Exception("Invalid move");
        }
        // Place token at the bottom most row without token
        int row = ROWS - 1;
        while (row >= 0 && this.board[row][col] == Token.EMPTY) {
            row -= 1;
        }
        row += 1;
        if (this.turn) {
            this.board[row][col] = Token.PLAYER_2;
        } else {
            this.board[row][col] = Token.PLAYER_1;
        }
        // Flip the turn
        this.turn = !this.turn;
        // Return the game state
        return getGameState(this.board);
    }

    public static State getGameState(Token[][] board) {
        int[][][] visited = new int[ROWS][COLS][4];
        for (int i = 0; i < visited.length; i++) {
            for (int j = 0; j < visited[i].length; j++) {
                visited[i][j] = new int[] { 0, 0, 0, 0 };
            }
        }
        int[][] directions = new int[][] { { -1, 0 }, { -1, 1 }, { 0, 1 }, { 1, 1 } };
        Boolean hasEmpty = false;
        // Can optimize this with BFS or DFS
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                // Don't care about spaces that are empty
                if (board[i][j] == Token.EMPTY) {
                    hasEmpty = true;
                    continue;
                }
                if (dfs(board, i, j, visited, directions)) {
                    // Check if player 1 or player 2
                    if (board[i][j] == Token.PLAYER_1) {
                        return State.PLAYER_1_WON;
                    }
                    return State.PLAYER_2_WON;
                }
            }
        }
        if (hasEmpty) {
            return State.LIVE;
        }
        return State.TIED;
    }

    // Do DFS
    // Store (current position, how many tokens we've seen, and direction traveled)
    // Direction can be between -1 to 3 where 1 is no direction and 0 to 3 are the
    // cardinal directions N, NE, E, SE
    // Returns whether a player has won or not
    private static Boolean dfs(Token[][] board, int x, int y, int[][][] visited, int[][] directions) {
        for (int d = 0; d < directions.length; d++) {
            int[] direction = directions[d];
            int newX = x + direction[0];
            int newY = y + direction[1];
            if (0 <= newX && newX < ROWS && 0 <= newY && newY < COLS) {
                // Check if new space is same as current space
                if (board[x][y] == board[newX][newY]) {
                    // Check if new space is already visited
                    if (visited[newX][newY][d] == 0) {
                        // dfs will populate the space with a value
                        dfs(board, newX, newY, visited, directions);
                    }
                    visited[x][y][d] = visited[newX][newY][d] + 1;
                    // Win condition if we have a streak of 4 or more
                    if (visited[x][y][d] >= WIN_CONDITION) {
                        return true;
                    }
                }
            }
            // Set streak to 1 if visited is still 0 for this set of indexes
            if (visited[x][y][d] == 0) {
                visited[x][y][d] = 1;
            }
        }
        return false;
    }

    // Override toString() method
    @Override
    public String toString() {
        String[] result = new String[ROWS + 1];
        for (int row = ROWS - 1; row >= 0; row--) {
            String[] rowString = new String[COLS];
            for (int col = 0; col < COLS; col++) {
                rowString[col] = String.format(" %s ", this.board[row][col].toString());
            }
            result[ROWS - 1 - row] = "|" + String.join("|", rowString) + "|";
        }
        // Add index footer column
        String[] footer = new String[COLS];
        for (int col = 0; col < COLS; col++) {
            footer[col] = String.format(" %d ", col + 1);
        }
        result[ROWS] = " " + String.join(" ", footer) + " ";
        return String.join("\n", result);
    }
}
