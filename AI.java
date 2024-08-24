import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

// AI class determines the column to place its token using a min-max algorithm
public class AI {
    // Determine whether AI is player 1 or player 2
    private Boolean isPlayerOne;
    // Memoization map to improve performance of choosing column
    private Map<String, double[]> memoMap;

    public AI(Boolean isPlayerOne) {
        this.isPlayerOne = isPlayerOne;
        this.memoMap = new HashMap<String, double[]>();
    }

    // chooseCol takes in the connect4 board as input and returns the column
    // to place a token in
    public int chooseCol(Board board) {
        Board.Space[][] boardCopy = board.cloneBoard();
        // Calculate time duration to for AI to choose the column
        Instant start = Instant.now();
        double[] scores = chooseColHelper(boardCopy, this.isPlayerOne, this.memoMap);
        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        System.out.println("Method duration: " + duration.toString());

        // Print scores for debugging purposes
        String[] scoresString = new String[scores.length];
        for (int i = 0; i < scores.length; i++) {
            scoresString[i] = String.format("%.2f", scores[i]);
        }
        System.out.println(String.join(",", scoresString));
        // Choose index with max score
        double bestScore = -1;
        int bestScoreIndex = 0;
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > bestScore) {
                bestScore = scores[i];
                bestScoreIndex = i;
            }
        }
        return bestScoreIndex;
    }

    // chooseColHelper returns a list of scores that represents the win/not-lose
    // probability for that move and that player
    private double[] chooseColHelper(Board.Space[][] board, Boolean isPlayerOne, Map<String, double[]> memoMap) {
        // Get memoization key
        String[] boardStrings = new String[Board.ROWS];
        for (int i = 0; i < board.length; i++) {
            String[] rowString = new String[Board.COLS];
            for (int j = 0; j < board[i].length; j++) {
                rowString[j] = board[i][j].toString();
            }
            boardStrings[i] = String.join("", rowString);
        }
        String memoKey = String.format("%s|%s", isPlayerOne, String.join("", boardStrings));
        if (memoMap.containsKey(memoKey)) {
            return memoMap.get(memoKey);
        }
        // Create our scores table that lists the scores for each possible move
        double[] scores = new double[Board.COLS];
        // Initialize our scores table with -1 indicating there is no score
        for (int i = 0; i < scores.length; i++) {
            scores[i] = -1;
        }
        // Can choose one of 7 columns to place a token.
        for (int col = 0; col < Board.COLS; col++) {
            scores[col] = calculateColScore(board, col, isPlayerOne);
        }
        // Return all the possible scores
        // We only care about the move that is the best so we only have to return
        // that index
        memoMap.put(memoKey, scores);
        return scores;
    }

    private double calculateColScore(Board.Space[][] board, int col, Boolean isPlayerOne) {
        // Skip columns that are full. Give it non-valid score
        if (board[0][col] != Board.Space.EMPTY) {
            return -1;
        }
        // Place token in bottom-most possible row
        int row = Board.ROWS - 1;
        while (row >= 0 && board[row][col] != Board.Space.EMPTY) {
            row--;
        }
        // Modifying the board. Need to reset after modify
        if (isPlayerOne) {
            board[row][col] = Board.Space.PLAYER_1;
        } else {
            board[row][col] = Board.Space.PLAYER_2;
        }
        double score = calculateColScoreHelper(board, row, col, isPlayerOne);
        board[row][col] = Board.Space.EMPTY;
        return score;
    }

    private double calculateColScoreHelper(Board.Space[][] board, int row, int col, Boolean isPlayerOne) {
        // Check if automatic win
        Board.State state = getGameState(board);
        if (state != Board.State.LIVE) {
            if ((isPlayerOne && state == Board.State.PLAYER_1_WON)
                    || (!isPlayerOne && state == Board.State.PLAYER_2_WON)) {
                // If we are playing as the current player, win probably is 1
                if (this.isPlayerOne == isPlayerOne) {
                    return 1;
                } else {
                    // If we are playing as the opposite player, win probability is 0
                    return 0;
                }
            }
            // Check if tie
            if (state == Board.State.TIED) {
                return 0.5;
            }
            // Should never get to this point
            return -1;
        }
        // If the game is not over, we want to accumulate the average of scores for this
        // move. We want to change the player to perform min-max
        double[] newScores = chooseColHelper(board, !isPlayerOne, memoMap);
        double totalScore = 0;
        double totalMoves = 0;
        for (int scoreIndex = 0; scoreIndex < newScores.length; scoreIndex++) {
            // Do not count the scores that are non-valid
            if (newScores[scoreIndex] == -1) {
                continue;
            }
            totalScore += newScores[scoreIndex];
            totalMoves++;
        }
        return totalScore / totalMoves;
    }

    private Board.State getGameState(Board.Space[][] board) {
        int[][][] visited = new int[Board.ROWS][Board.COLS][4];
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
                if (board[i][j] == Board.Space.EMPTY) {
                    hasEmpty = true;
                    continue;
                }
                if (dfs(board, i, j, visited, directions)) {
                    // Check if player 1 or player 2
                    if (board[i][j] == Board.Space.PLAYER_1) {
                        return Board.State.PLAYER_1_WON;
                    }
                    return Board.State.PLAYER_2_WON;
                }
            }
        }
        if (hasEmpty) {
            return Board.State.LIVE;
        }
        return Board.State.TIED;
    }

    // Do DFS
    // Store (current position, how many tokens we've seen, and direction traveled)
    // Direction can be between -1 to 3 where 1 is no direction and 0 to 3 are the
    // cardinal directions N, NE, E, SE
    // Returns whether a player has won or not
    private Boolean dfs(Board.Space[][] board, int x, int y, int[][][] visited, int[][] directions) {
        for (int d = 0; d < directions.length; d++) {
            int[] direction = directions[d];
            int newX = x + direction[0];
            int newY = y + direction[1];
            if (0 <= newX && newX < Board.ROWS && 0 <= newY && newY < Board.COLS) {
                // Check if new space is same as current space
                if (board[x][y] == board[newX][newY]) {
                    // Check if new space is already visited
                    if (visited[newX][newY][d] == 0) {
                        // dfs will populate the space with a value
                        dfs(board, newX, newY, visited, directions);
                    }
                    visited[x][y][d] = visited[newX][newY][d] + 1;
                    // Win condition if we have a streak of 4 or more
                    if (visited[x][y][d] >= Board.WIN_CONDITION) {
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
}
