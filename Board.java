public class Board {
    public static final int ROWS = 5;
    public static final int COLS = 4;
    public static final int WIN_CONDITION = 4;

    public static final String LIVE = "LIVE";
    public static final String TIED = "TIED";
    public static final String PLAYER_1_WON = "PLAYER_1_WON";
    public static final String PLAYER_2_WON = "PLAYER_2_WON";

    public static final String EMPTY = " ";
    public static final String PLAYER_1 = "X";
    public static final String PLAYER_2 = "O";

    // board is the connect4 board
    private String[][] board;
    // Current player's turn. We can just use boolean to represent player 1 or 2.
    private Boolean turn;

    // Constructor
    public Board() {
        // Initialize connect4 board
        this.board = new String[ROWS][COLS];

        // Initialize board with empty spaces for presentation purposes
        for (int i = 0; i < this.board.length; i++) {
            for (int j = 0; j < this.board[i].length; j++) {
                this.board[i][j] = EMPTY;
            }
        }

        // Initialize the turn
        this.turn = false;
    }

    public String[][] cloneBoard() {
        String[][] clone = new String[ROWS][COLS];
        for (int i = 0; i < clone.length; i++) {
            for (int j = 0; j < clone[i].length; j++) {
                clone[i][j] = this.board[i][j];
            }
        }
        return clone;
    }

    public String turn(int col) throws Exception {
        // Check if the game is still live
        if (getGameState(this.board) != LIVE) {
            throw new Exception("Game is over");
        }
        // Check if can place String on the column by checking if first row has item
        if (this.board[ROWS - 1][col] != EMPTY) {
            throw new Exception("Invalid move");
        }
        // Place String at the bottom most row without String
        int row = ROWS - 1;
        while (row >= 0 && this.board[row][col] == EMPTY) {
            row -= 1;
        }
        row += 1;
        if (this.turn) {
            this.board[row][col] = PLAYER_2;
        } else {
            this.board[row][col] = PLAYER_1;
        }
        // Flip the turn
        this.turn = !this.turn;
        // Return the game state
        return getGameState(this.board);
    }

    public static String getGameState(String[][] board) {
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
                if (board[i][j] == EMPTY) {
                    hasEmpty = true;
                    continue;
                }
                if (dfs(board, i, j, visited, directions)) {
                    // Check if player 1 or player 2
                    if (board[i][j] == PLAYER_1) {
                        return PLAYER_1_WON;
                    }
                    return PLAYER_2_WON;
                }
            }
        }
        if (hasEmpty) {
            return LIVE;
        }
        return TIED;
    }

    // Do DFS
    // Store (current position, how many Strings we've seen, and direction traveled)
    // Direction can be between -1 to 3 where 1 is no direction and 0 to 3 are the
    // cardinal directions N, NE, E, SE
    // Returns whether a player has won or not
    private static Boolean dfs(String[][] board, int x, int y, int[][][] visited, int[][] directions) {
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