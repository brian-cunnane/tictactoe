// Fig. 18.8: TicTacToeServer.java
// This class maintains a game of Tic-Tac-Toe for two client applets.
import java.awt.*;
import java.net.*;
import java.io.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.swing.*;

public class TicTacToeServer extends JFrame {
    private char[] board;
    private JTextArea outputArea;
    private Player[] players;
    private ServerSocket server;
    private int currentPlayer;
    private final int PLAYER_X = 0, PLAYER_O = 1;
    private final char X_MARK = 'X', O_MARK = 'O';
    public int count = 0;

    // set up logger
    public static final Logger logger = Logger.getLogger(TicTacToeServer.class.getName());

    // set up tic-tac-toe server and GUI that displays messages
    public TicTacToeServer()
    {
        super( "Tic-Tac-Toe Server" );

        try {
            FileHandler fh = new FileHandler("c:\\users\\brian\\desktop\\server.log",true);
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);

        }catch(java.io.IOException IOE){IOE.printStackTrace();}

        board = new char[ 9 ];
        players = new Player[ 2 ];
        currentPlayer = PLAYER_X;

        // set up ServerSocket
        try {
            logger.log(Level.SEVERE,"Creating socket.");
            server = new ServerSocket( 12345, 2 );
        }

        // process problems creating ServerSocket
        catch( IOException ioException ) {
            ioException.printStackTrace();
            System.exit( 1 );
        }

        // set up JTextArea to display messages during execution
        outputArea = new JTextArea();
        getContentPane().add( outputArea, BorderLayout.CENTER );
        outputArea.setText( "Server awaiting connections\n" );

        setSize( 300, 300 );
        setVisible( true );

    } // end TicTacToeServer constructor

    // wait for two connections so game can be played
    public void execute()
    {
        // wait for each client to connect
        for ( int i = 0; i < players.length; i++ ) {

            // wait for connection, create Player, start thread
            try {
                players[ i ] = new Player( server.accept(), i );
                players[ i ].start();
            }

            // process problems receiving connection from client
            catch( IOException ioException ) {
                ioException.printStackTrace();
                System.exit( 1 );
            }
        }

        // Player X is suspended until Player O connects.
        // Resume player X now.
        synchronized ( players[ PLAYER_X ] ) {
            players[ PLAYER_X ].setSuspended( false );
            players[ PLAYER_X ].notify();
        }

    }  // end method execute

    // utility method called from other threads to manipulate
    // outputArea in the event-dispatch thread
    private void displayMessage( final String messageToDisplay )
    {
        // display message from event-dispatch thread of execution
        SwingUtilities.invokeLater(
                new Runnable() {  // inner class to ensure GUI updates properly

                    public void run() // updates outputArea
                    {
                        outputArea.append( messageToDisplay );
                        outputArea.setCaretPosition(
                                outputArea.getText().length() );
                    }

                }  // end inner class

        ); // end call to SwingUtilities.invokeLater
    }

    // Determine if a move is valid. This method is synchronized because
    // only one move can be made at a time.
    public synchronized boolean validateAndMove( int location, int player )
    {
        logger.log(Level.WARNING, "Validating move. Current player = " + currentPlayer + " player = " + player);
        boolean moveDone = false;

        // while not current player, must wait for turn
        while ( player != currentPlayer ) {

            // wait for turn
            try {
                wait();
            }

            // catch wait interruptions
            catch( InterruptedException interruptedException ) {
                interruptedException.printStackTrace();
            }
        }

        // if location not occupied, make move
        if ( !isOccupied( location ) ) {
            count ++;
            logger.log(Level.WARNING,"Valid move for Player: " + currentPlayer);
            // set move in board array
            board[ location ] = currentPlayer == PLAYER_X ? X_MARK : O_MARK;

            // change current player
            currentPlayer = ( currentPlayer + 1 ) % 2;

            // let new current player know that move occurred
            logger.log(Level.WARNING,"notifying player: " + currentPlayer);
            if(! isGameOver())
                players[ currentPlayer ].otherPlayerMoved( location );
            else{
                if(checkForWin()){
                    players[currentPlayer].gameOver(location);
                }
                else
                    players[currentPlayer].draw(location);
            }
            notify(); // tell waiting player to continue

            // tell player that made move that the move was valid
            logger.exiting(getClass().getName(),"Move validated");
            return true;
        }

        // tell player that made move that the move was not valid
        else {
            logger.exiting(getClass().getName(),"Move invalid");
            return false;
        }


    } // end method validateAndMove

    public boolean checkForWin(){

        if((board[0] == X_MARK && board[1] == X_MARK && board[2] == X_MARK
                || board[3] == X_MARK && board[4] == X_MARK && board[5] == X_MARK
                || board[6] == X_MARK && board[7] == X_MARK && board[8] == X_MARK
                || board[0] == X_MARK && board[3] == X_MARK && board[6] == X_MARK
                || board[1] == X_MARK && board[4] == X_MARK && board[7] == X_MARK
                || board[2] == X_MARK && board[5] == X_MARK && board[8] == X_MARK
                || board[0] == X_MARK && board[4] == X_MARK && board[8] == X_MARK
                || board[2] == X_MARK && board[4] == X_MARK && board[6] == X_MARK)
                ||
                (board[0] == O_MARK && board[1] == O_MARK && board[2] == O_MARK
                || board[3] == O_MARK && board[4] == O_MARK && board[5] == O_MARK
                || board[6] == O_MARK && board[7] == O_MARK && board[8] == O_MARK
                || board[0] == O_MARK && board[3] == O_MARK && board[6] == O_MARK
                || board[1] == O_MARK && board[4] == O_MARK && board[7] == O_MARK
                || board[2] == O_MARK && board[5] == O_MARK && board[8] == O_MARK
                || board[0] == O_MARK && board[4] == O_MARK && board[8] == O_MARK
                || board[2] == O_MARK && board[4] == O_MARK && board[6] == O_MARK)) {
            return true;
        }
        return false;
    }

    // determine whether location is occupied
    public boolean isOccupied( int location )
    {
        if ( board[ location ] == X_MARK || board [ location ] == O_MARK )
            return true;
        else
            return false;
    }

    // place code in this method to determine whether game over
    public boolean isGameOver()
    {
        if (checkForWin()) { //there is a winning combination on the board
            logger.log(Level.SEVERE,"Someone won");
            //resetBoard();
            return true;
        }
        else if (count == 9) { //The board is full and there is no winning combination
            logger.log(Level.SEVERE,"Tied game. Count = " + count);
            return true;
        }
        else
            return false;  // this is left as an exercise
    }

    public void resetBoard(){
        for(int i = 0; i < board.length; i ++)
            board[i] = '\u0000';
        count = 0;
        logger.log(Level.SEVERE,"Board was reset at end of game");
    }

    public static void main( String args[] )
    {
        TicTacToeServer application = new TicTacToeServer();
        application.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        application.execute();
    }

    // private inner class Player manages each Player as a thread
    private class Player extends Thread {
        private Socket connection;
        private DataInputStream input;
        private DataOutputStream output;
        private int playerNumber;
        private char mark;
        protected boolean suspended = true;

        // set up Player thread
        public Player( Socket socket, int number )
        {
            playerNumber = number;

            // specify player's mark
            mark = ( playerNumber == PLAYER_X ? X_MARK : O_MARK );

            connection = socket;

            // obtain streams from Socket
            try {
                input = new DataInputStream( connection.getInputStream() );
                output = new DataOutputStream( connection.getOutputStream() );
            }

            // process problems getting streams
            catch( IOException ioException ) {
                ioException.printStackTrace();
                System.exit( 1 );
            }

        } // end Player constructor
        public void gameOver(int location){
            try {
                output.writeUTF("You lose.");
                output.writeInt(location);
            }
            catch(IOException ioException){ioException.printStackTrace();}
        }

        public void draw(int location){
            try {
                output.writeUTF("draw.");
                output.writeInt(location);
            }
            catch(IOException ioException){ioException.printStackTrace();}
        }

        // send message that other player moved
        public void otherPlayerMoved( int location )
        {
            // send message indicating move
            try {
                output.writeUTF( "Opponent moved" );
                output.writeInt( location );
            }

            // process problems sending message
            catch ( IOException ioException ) {
                ioException.printStackTrace();
            }
        }

        // control thread's execution
        public void run()
        {
            // send client message indicating its mark (X or O),
            // process messages from client
            try {
                displayMessage( "Player " + ( playerNumber ==
                        PLAYER_X ? X_MARK : O_MARK ) + " connected\n" );

                output.writeChar( mark ); // send player's mark

                // send message indicating connection
                output.writeUTF( "Player " + ( playerNumber == PLAYER_X ?
                        "X connected\n" : "O connected, please wait\n" ) );

                // if player X, wait for another player to arrive
                if ( mark == X_MARK ) {
                    output.writeUTF( "Waiting for another player" );

                    // wait for player O
                    try {
                        synchronized( this ) {
                            while ( suspended )
                                wait();
                        }
                    }

                    // process interruptions while waiting
                    catch ( InterruptedException exception ) {
                        exception.printStackTrace();
                    }

                    // send message that other player connected and
                    // player X can make a move
                    output.writeUTF( "Other player connected. Your move." );
                }

                // while game not over
                while ( ! isGameOver() ) {

                    // get move location from client
                    int location = input.readInt();

                    // check for valid move
                    if ( validateAndMove( location, playerNumber ) ) {
                        displayMessage( "\nlocation: " + location );
                        output.writeUTF( "Valid move." );
                    }
                    else
                        output.writeUTF( "Invalid move, try again" );
                }
                if(isGameOver()) {
                    if(count > 8)
                        output.writeUTF("draw");
                    else {
                        output.writeUTF("win");
                        connection.close();
                    }

                    resetBoard();
                    //connection.close(); // close connection to client
                    //logger.log(Level.SEVERE,"Closing connection to client: " + currentPlayer);
                    players = new Player[2];
                    for ( int i = 0; i < players.length; i++ ) {

                        // wait for connection, create Player, start thread
                        try {
                            players[ i ] = new Player( server.accept(), i );
                            players[ i ].start();
                            currentPlayer = PLAYER_X;
                        }

                        // process problems receiving connection from client
                        catch( IOException ioException ) {
                            ioException.printStackTrace();
                            System.exit( 1 );
                        }
                    }

                    // Player X is suspended until Player O connects.
                    // Resume player X now.
                    synchronized ( players[ PLAYER_X ] ) {
                        players[ PLAYER_X ].setSuspended( false );
                        players[ PLAYER_X ].notify();
                    }

                }

            } // end try

            // process problems communicating with client
            catch( IOException ioException ) {
                ioException.printStackTrace();
                System.exit( 1 );
            }

        } // end method run

        // set whether or not thread is suspended
        public void setSuspended( boolean status )
        {
            suspended = status;
        }

    } // end class Player

} // end class TicTacToeServer

/**************************************************************************
 * (C) Copyright 1992-2003 by Deitel & Associates, Inc. and               *
 * Prentice Hall. All Rights Reserved.                                    *
 *                                                                        *
 * DISCLAIMER: The authors and publisher of this book have used their     *
 * best efforts in preparing the book. These efforts include the          *
 * development, research, and testing of the theories and programs        *
 * to determine their effectiveness. The authors and publisher make       *
 * no warranty of any kind, expressed or implied, with regard to these    *
 * programs or to the documentation contained in these books. The authors *
 * and publisher shall not be liable in any event for incidental or       *
 * consequential damages in connection with, or arising out of, the       *
 * furnishing, performance, or use of these programs.                     *
 *************************************************************************/
