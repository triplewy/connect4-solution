import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

// AI class determines the column to place its token using a min-max algorithm
public class AI {
    // DIRECTIONS is a cardinal direction array formed in the following order:
    // NE, E, SE, S
    // We skip N because it is impossible for another token to be on top of the
    // token you just placed. We skip SW, W, NW because they are represented by the
    // other directions
    private static int[][] DIRECTIONS = new int[][] { { 1, 1 }, { 0, 1 }, { -1, 1 }, { -1, 0 }, };
    // Boolean that keeps stores if AI is player 1
    private Boolean isPlayerOne;
    // Model to store scores for each possible column given a string
    // representation of the current board
    private Map<Long, double[]> model;

    public AI(Boolean isPlayerOne) {
        this.isPlayerOne = isPlayerOne;
        this.model = new HashMap<Long, double[]>();

        // Create the internal representation for the board, which is a 64 bit long
        // with the values of column taking the first ROW*COL bits. For a standard 6x7
        // board, this would be the first 42 bits. The next bits are the heights of each
        // column which would be 3*COL since Connect4 at most has 6 rows which can be
        // represented with 3 bits. So for a standard 6x7 board, this would be the next
        // 21 bits. Thus for a standard board, we would need 63 bits to encode the
        // information.
        long board = 0L;
        // Calculate time duration to for AI to generate the model
        System.out.println("Generating AI model...");
        Instant start = Instant.now();
        generateModel(board, this.model);
        Instant end = Instant.now();

        Duration duration = Duration.between(start, end);
        System.out.println("Method duration: " + duration.toString());
    }

    // chooseCol takes in the connect4 board as input and returns the column
    // to place a token in
    public int chooseCol(String[][] board) {
        // Only flip the board if AI is player2
        if (!this.isPlayerOne) {
            for (int i = 0; i < board.length; i++) {
                for (int j = 0; j < board[i].length; j++) {
                    if (board[i][j] == Board.PLAYER_1) {
                        board[i][j] = Board.PLAYER_2;
                    } else if (board[i][j] == Board.PLAYER_2) {
                        board[i][j] = Board.PLAYER_1;
                    }
                }
            }
        }
        // Generate the model key
        long modelKey = getModelKey(board);
        // Assert that we have the model key
        if (!this.model.containsKey(modelKey)) {
            System.out.println("model does not have key");
            return -1;
        }
        // We should always have the key in the memo map
        double[] scores = this.model.get(modelKey);
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

    // generateModel returns a list of scores that represents the win/not-lose
    // probability for that move and that player
    private static double[] generateModel(long board, Map<Long, double[]> model) {
        long modelKey = getModelKey(board);
        // Check if we have the board in the model, if we do then return
        if (model.containsKey(modelKey)) {
            return model.get(modelKey);
        }
        // Get our scores table that lists the scores for each possible col
        double[] scores = generateModelScores(board, model);

        // Store the possible scores in the model
        model.put(modelKey, scores);

        return scores;
    }

    // generateModel returns a list of scores that represents the win/not-lose
    // probability for that move and that player
    private static double[] generateModelScores(long board, Map<Long, double[]> model) {
        // Create our scores table that lists the scores for each possible col
        double[] scores = new double[Board.COLS];
        // Initialize our scores table with -1 indicating there is no score
        for (int col = 0; col < Board.COLS; col++) {
            scores[col] = -1;
        }
        // Check if we can automatically win with any of the columns
        // Return early if we find any automatic wins
        for (int col = 0; col < Board.COLS; col++) {
            long height = getColHeight(board, col);
            // Skip columns that are full
            if (height == Board.ROWS) {
                continue;
            }
            // Place token in bottom-most possible row
            board = setToken(board, height, col);
            board = setColHeight(board, col, height + 1);
            // Check if automatic win
            String gameState = getGameState(board, col);
            // Undo the move
            board = setColHeight(board, col, height);
            board = unsetToken(board, height, col);
            // Check the game state if we won
            if (gameState == Board.PLAYER_1_WON) {
                scores[col] = 1;
                return scores;
            }
            // If we tied with this move, set the score to 0.5 but don't immediately return
            else if (gameState == Board.TIED) {
                scores[col] = 0.5;
            }
        }
        // Can choose one of 7 columns to place a token.
        for (int col = 0; col < Board.COLS; col++) {
            long height = getColHeight(board, col);
            // Skip columns that are full or lead to a tie
            if (height == Board.ROWS || scores[col] != -1) {
                continue;
            }
            // Place token in bottom-most possible row
            board = setToken(board, height, col);
            board = setColHeight(board, col, height + 1);
            // Invert the board to represent the board from the other player's perspective
            board = invertSignificantBits(board, Board.ROWS * Board.COLS);
            // Calculate the scores for this move
            double[] newScores = generateModel(board, model);
            // Undo the invert board
            board = invertSignificantBits(board, Board.ROWS * Board.COLS);
            // Undo the move
            board = setColHeight(board, col, height);
            board = unsetToken(board, height, col);

            // Get scores for each col
            double totalScore = 0;
            double totalMoves = 0;
            for (int i = 0; i < Board.COLS; i++) {
                // Do not count the scores that are non-valid
                if (newScores[i] == -1) {
                    continue;
                }
                // Take the inverse of the score as it's our opponent who's taking the turn.
                totalScore += 1 - newScores[i];
                totalMoves++;
            }
            scores[col] = totalScore / totalMoves;
        }
        // Store the possible scores in the model
        return scores;
    }

    private static long getColHeight(long board, long col) {
        // 7 represents 111
        // Get height of the col which are the bits [ROW*COL+3*col,ROW*COL+3*col+3)
        long mask = 7L << (Board.ROWS * Board.COLS + 3 * col);
        return (board & mask) >> (Board.ROWS * Board.COLS + 3 * col);
    }

    private static long setColHeight(long board, long col, long height) {
        // Get height of the col which are the bits [ROW*COL+3*col,ROW*COL+3*col+3)
        long mask = 7L << (Board.ROWS * Board.COLS + 3 * col);
        // Remove the masked bits from the number
        long clearedNumber = board & ~mask;
        // Place the inverted bits into their original position
        return clearedNumber | (height << (Board.ROWS * Board.COLS + 3 * col));
    }

    private static long setToken(long board, long row, long col) {
        // The position of the token is at ROW*col+row
        long mask = 1L << (Board.ROWS * col + row);
        // Place the inverted bits into their original position
        return board | mask;
    }

    private static long unsetToken(long board, long row, long col) {
        // The position of the token is at ROW*col+row
        long mask = 1L << (Board.ROWS * col + row);
        // Remove the masked bit from the number
        return board & ~mask;
    }

    private static long invertSignificantBits(long number, int k) {
        // Create a mask with k significant bits set to 1
        long mask = (1L << k) - 1; // Example: k=4 -> mask=1111 (binary)

        // Extract the significant bits from the number
        long significantBits = number & mask;

        // Invert the significant bits
        long invertedBits = ~significantBits & mask;

        // Combine the inverted bits back into the number
        // Remove the original significant bits from the number
        long clearedNumber = number & ~mask;

        // Place the inverted bits into their original position
        return clearedNumber | invertedBits;
    }

    // Gets game state based on col that a token was just placed in
    private static String getGameState(long board, long col) {
        long row = getColHeight(board, col) - 1;
        // All cardinal directions except N starting from NE going clockwise
        for (int d = 0; d < DIRECTIONS.length; d++) {
            // Calculate the streak
            int streak = 0;

            for (int[] modifier : new int[][] { { 1, 1 }, { -1, -1 } }) {
                long x = row;
                long y = col;
                while (0 <= x && x < Board.ROWS && 0 <= y && y < Board.COLS
                        && (board & (1 << (Board.ROWS * y + x))) > 0 && x < getColHeight(board, y)) {
                    streak += 1;
                    x += DIRECTIONS[d][0] * modifier[0];
                    y += DIRECTIONS[d][1] * modifier[1];
                }
            }
            // Subtract 1 from streak to prevent double counting
            streak -= 1;

            if (streak >= Board.WIN_CONDITION) {
                return Board.PLAYER_1_WON;
            }
        }
        // Check if all cols have maximum height
        for (int i = 0; i < Board.COLS; i++) {
            // If we have a height that is not the max, then the game is still live
            if (getColHeight(board, i) != Board.ROWS) {
                return Board.LIVE;
            }
        }
        // If all heights are at the max, then the game is tied
        return Board.TIED;
    }

    private static long getModelKey(String[][] board) {
        // Convert board into internal representation of board
        long boardLong = 0L;
        for (int col = 0; col < Board.COLS; col++) {
            int row = Board.ROWS - 1;
            while (row >= 0 && board[row][col] == Board.EMPTY) {
                row--;
            }
            boardLong = setColHeight(boardLong, col, row + 1);
            // Create the bit representation of the column
            for (int i = 0; i <= row; i++) {
                // If the token is player2, then skip
                if (board[i][col] == Board.PLAYER_2) {
                    continue;
                }
                boardLong = setToken(boardLong, i, col);
            }
        }
        return boardLong;
    }

    private static long getModelKey(long board) {
        // Get the full heights representation, which is all the bits after the board
        long heights = board & ~((1L << Board.ROWS * Board.COLS) - 1);
        // Generate heights mask
        long heightMask = 0L;
        for (int col = 0; col < Board.COLS; col++) {
            long height = getColHeight(board, col);
            heightMask = heightMask | (((1L << height) - 1) << Board.ROWS * col);
        }
        return heights | (board & heightMask);
    }
}