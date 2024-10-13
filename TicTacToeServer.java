package TicTacToe;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

public class TicTacToeServer extends Application implements TicTacToeConstants {
    private int sessionNo = 1; // Number a session

    @Override
    public void start(Stage primaryStage) {
        TextArea taLog = new TextArea();

        // Create a scene and place it in the stage
        Scene scene = new Scene(new ScrollPane(taLog), 450, 200);
        primaryStage.setTitle("TicTacToeServer"); // Set the stage title
        primaryStage.setScene(scene); // Place the scene in the stage
        primaryStage.show(); // Display the stage

        new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(8000);     // Create a server socket
                Platform.runLater(() -> taLog.appendText(new Date() +
                        ": Server started at socket 8000\n"));
                // Ready to create a session for every two players
                while (true) {
                    Platform.runLater(() -> taLog.appendText(new Date() +
                            ": Wait for players to join session " + sessionNo + '\n'));
                    Socket player1 = serverSocket.accept();     // Connect to player 1
                    Platform.runLater(() -> {
                        taLog.appendText(new Date() + ": Player 1 joined session "
                                + sessionNo + '\n');
                        taLog.appendText("Player 1's IP address" +
                                player1.getInetAddress().getHostAddress() + '\n');
                    });
                    new DataOutputStream(player1.getOutputStream()).writeInt(PLAYER1);
                    // Connect to player 2
                    Socket player2 = serverSocket.accept();
                    Platform.runLater(() -> {
                        taLog.appendText(new Date() +
                                ": Player 2 joined session " + sessionNo + '\n');
                        taLog.appendText("Player 2's IP address" +
                                player2.getInetAddress().getHostAddress() + '\n');
                    });
                    new DataOutputStream(player2.getOutputStream()).writeInt(PLAYER2);
//                     Display this session and increment session number
                    Platform.runLater(() -> taLog.appendText(new Date() +
                            ": Start a thread for session " + sessionNo++ + '\n'));
                    // Launch a new thread for this session of two players
                    new Thread(new HandleASession(player1, player2)).start();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    // Define the thread class for handling a new session for two players
    class HandleASession implements Runnable, TicTacToeConstants {
        private Socket player1;
        private Socket player2;

        // Create and initialize cells
        private char[][] cell = new char[3][3];

        // Continue to play
        private boolean continueToPlay = true;

        /**
         * Construct a thread
         */
        public HandleASession(Socket player1, Socket player2) {
            this.player1 = player1;
            this.player2 = player2;
            // Initialize cells
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    cell[i][j] = ' ';
                }
            }
        }

        /**
         * Implement the run() method for the thread
         */
        public void run() {
            try {
                // Create data input and output streams
                DataInputStream fromPlayer1 = new DataInputStream(player1.getInputStream());
                DataOutputStream toPlayer1 = new DataOutputStream(player1.getOutputStream());
                DataInputStream fromPlayer2 = new DataInputStream(player2.getInputStream());
                DataOutputStream toPlayer2 = new DataOutputStream(player2.getOutputStream());
                // Write anything to notify player 1 to start
                toPlayer1.writeInt(1);
                // Continuously serve the players and determine the game status
                while (true) {
                    // Receive a move from player 1
                    int row = fromPlayer1.readInt();
                    int column = fromPlayer1.readInt();
                    cell[row][column] = 'X';
                    // Check if Player 1 wins
                    if (isWon('X')) {
                        toPlayer1.writeInt(PLAYER1_WON);
                        toPlayer2.writeInt(PLAYER1_WON);
                        sendMove(toPlayer2, row, column);
                        break; // Break the loop
                    } else if (isFull()) { // Check if all cells are filled
                        toPlayer1.writeInt(DRAW);
                        toPlayer2.writeInt(DRAW);
                        sendMove(toPlayer2, row, column);
                        break;
                    } else {
                        // Notify player 2 to take the turn
                        toPlayer2.writeInt(CONTINUE);
                        sendMove(toPlayer2, row, column);
                    }

                    // Receive a move from player 2
                    row = fromPlayer2.readInt();
                    column = fromPlayer2.readInt();
                    cell[row][column] = 'O';

                    // Check if Player 2 wins
                    if (isWon('O')) {
                        toPlayer1.writeInt(PLAYER2_WON);
                        toPlayer2.writeInt(PLAYER2_WON);
                        sendMove(toPlayer1, row, column);
                        break;
                    } else {
                        // Notify player 1 to take the turn
                        toPlayer1.writeInt(CONTINUE);
                        sendMove(toPlayer1, row, column);
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        /**
         * Send the move to the other player
         */
        private void sendMove(DataOutputStream out, int row, int column) throws IOException {
            out.writeInt(row); // Send row index
            out.writeInt(column); // Send column index
        }

        /**
         * Determine if the cells are all occupied
         */
        private boolean isFull() {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (cell[i][j] == ' ') return false; // At least one cell is not filled
                }
            }
            return true; // All cells are filled
        }

        /** Determine if the player with the specified token wins */
        private boolean isWon(char token) {
            // Check all rows
            for (int i = 0; i < 3; i++) {
                if ((cell[i][0] == token) && (cell[i][1] == token) && (cell[i][2] == token)) {
                    return true; // Player wins by filling a row
                }
            }

            // Check all columns
            for (int j = 0; j < 3; j++) {
                if ((cell[0][j] == token) && (cell[1][j] == token) && (cell[2][j] == token)) {
                    return true; // Player wins by filling a column
                }
            }

            // Check major diagonal
            if ((cell[0][0] == token) && (cell[1][1] == token) && (cell[2][2] == token)) {
                return true; // Player wins by filling the major diagonal
            }

            // Check subdiagonal
            if ((cell[0][2] == token) && (cell[1][1] == token) && (cell[2][0] == token)) {
                return true; // Player wins by filling the subdiagonal
            }

            // No winner
            return false;
        }
    }
}
