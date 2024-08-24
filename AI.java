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
        Board.Token[][] boardCopy = board.cloneBoard();
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
    private double[] chooseColHelper(Board.Token[][] board, Boolean isPlayerOne, Map<String, double[]> memoMap) {
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
        // Check if we have the key in the memo map, if we do then return
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
        memoMap.put(memoKey, scores);
        return scores;
    }

    private double calculateColScore(Board.Token[][] board, int col, Boolean isPlayerOne) {
        // Skip columns that are full. Give it non-valid score
        if (board[0][col] != Board.Token.EMPTY) {
            return -1;
        }
        // Place token in bottom-most possible row
        int row = Board.ROWS - 1;
        while (row >= 0 && board[row][col] != Board.Token.EMPTY) {
            row--;
        }
        // Modifying the board. Need to reset after modify
        if (isPlayerOne) {
            board[row][col] = Board.Token.PLAYER_1;
        } else {
            board[row][col] = Board.Token.PLAYER_2;
        }
        // Calculate the score for this move
        double score = -1;
        // Check if automatic win
        Board.State state = Board.getGameState(board);
        if (state != Board.State.LIVE) {
            if ((isPlayerOne && state == Board.State.PLAYER_1_WON)
                    || (!isPlayerOne && state == Board.State.PLAYER_2_WON)) {
                // If we are playing as the current player, win probably is 1
                if (this.isPlayerOne == isPlayerOne) {
                    score = 1;
                } else {
                    // If we are playing as the opposite player, win probability is 0
                    score = 0;
                }
            }
            // Check if tie
            if (state == Board.State.TIED) {
                score = 0.5;
            }
        } else {
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
            score = totalScore / totalMoves;
        }
        board[row][col] = Board.Token.EMPTY;
        return score;
    }
}
