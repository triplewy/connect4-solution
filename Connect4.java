import java.util.Scanner;

public class Connect4 {
    private static final String PVP = "PVP";
    private static final String AI_PLAYER_1 = "AI_PLAYER_1";
    private static final String AI_PLAYER_2 = "AI_PLAYER_2";

    private static int getUserOption(Scanner scanner, String[] optionText, String[] optionInput) {
        String input;
        while (true) {
            // Print out the option text
            for (String text : optionText) {
                System.out.println(text);
            }
            System.out.print(">> ");
            input = scanner.nextLine();
            // Check if the input matches any of the desired inputs
            for (int i = 0; i < optionInput.length; i++) {
                if (input.equals(optionInput[i])) {
                    return i;
                }
            }
            // If it doesn't match any of the inputs go through the loop again
            System.out.println("Invalid input");
        }
    }

    private static void gameLoop(Scanner scanner, String gameMode) {
        // Create the board
        Board board = new Board();
        // Create AI. Instantiate it regardless of whether we actually have AI
        // so that we can avoid compile errors
        AI ai = null;
        if (gameMode != PVP) {
            ai = new AI(gameMode == AI_PLAYER_1);
        }
        // Create a boolean to track the turn
        Boolean turn = false;
        while (true) {
            System.out.println("-----------------------");
            // Print the board
            System.out.println(board.toString());
            int col;
            // Check if AI is choosing the column
            if (gameMode != PVP && ((!turn && gameMode == AI_PLAYER_1) ||
                    (turn && gameMode == AI_PLAYER_2))) {
                col = ai.chooseCol(board.cloneBoard());
                System.out.println(String.format(">> AI chose column: %d", col + 1));
            } else {
                String[] optionTexts = {
                        "Enter column to place token:",
                };
                String[] optionInputs = {
                        "1",
                        "2",
                        "3",
                        "4",
                        "5",
                        "6",
                        "7"
                };
                col = getUserOption(scanner, optionTexts, optionInputs);
            }
            try {
                String state = board.turn(col);
                if (state != Board.LIVE) {
                    System.out.println(board.toString());
                    switch (state) {
                        case Board.TIED:
                            System.out.println("Tied");
                            return;
                        case Board.PLAYER_1_WON:
                            System.out.println("Player 1 won");
                            return;
                        case Board.PLAYER_2_WON:
                            System.out.println("Player 2 won");
                            return;
                        default:
                            break;
                    }
                    return;
                }
                turn = !turn;
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Welcome to Connect4!");

        int input;

        while (true) {
            System.out.println("----------------------------------------------");
            String[] optionTexts = {
                    "Play PvP [1]",
                    "Play AI  [2]",
            };
            String[] optionInputs = {
                    "1",
                    "2",
            };
            input = getUserOption(scanner, optionTexts, optionInputs);
            switch (input) {
                case 0:
                    gameLoop(scanner, PVP);
                    break;
                case 1:
                    String[] playerOptionTexts = {
                            "Play as Player 1 [1]",
                            "Play as Player 2 [2]"
                    };
                    String[] playerOptionInputs = {
                            "1",
                            "2",
                    };
                    input = getUserOption(scanner, playerOptionTexts, playerOptionInputs);
                    switch (input) {
                        case 0:
                            gameLoop(scanner, AI_PLAYER_2);
                            break;
                        case 1:
                            gameLoop(scanner, AI_PLAYER_1);
                            break;
                        default:
                            break;
                    }
                    break;
                default:
                    break;
            }
        }
    }
}