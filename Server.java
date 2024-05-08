import java.io.*;
import java.net.*;
import java.util.*;

public class Server {

  private static int PORT = 5190;

  public static void main(String[] args) {
    ServerSocket ss;
    try {
      ss = new ServerSocket(PORT);
      while (true) {
        Socket socket = ss.accept();
        new ProcessConnection(socket).start();
      }
    } catch (IOException e) {
      System.out.println("Could not get the socket");
    }
  }
}

class ProcessConnection extends Thread {

  static ArrayList<ProcessConnection> connections = new ArrayList<ProcessConnection>();

  static ArrayList<CardFront> deck = new ArrayList<>();

  private Socket socket;
  private PrintStream sout;
  private Scanner sin;

  private String username;
  private String ready;
  static boolean gameStart;

  static int currentPlayer = 0;

  public ProcessConnection(Socket socket) {
    this.socket = socket;
  }

  @Override
  public void run() {
    try {
      sout = new PrintStream(socket.getOutputStream());
      sin = new Scanner(socket.getInputStream());

      username = sin.nextLine();
      ready = "false";
      gameStart = false;

      if (connections.size() == 4) {
        sout.println("500");
        sout.println("Room has reached max occupancy of 4 players.");
      } else if (usernameExists(username)) {
        sout.println("500");
        sout.println("Username already exists in this room.");
      } else {
        connections.add(this);

        for (ProcessConnection connection : connections) {
          sout.println(connection.username);
          sout.println(connection.ready);
          if (!connection.username.equals(username)) {
            String[] message = { username, ready };
            connection.sendMessage(message);
          }
        }

        sout.println("200");

        while (sin.hasNext()) {
          String line = sin.nextLine().trim();
          if (!gameStart) {
            ready = line;
            for (ProcessConnection connection : connections) {
              String[] message = { username, ready };
              connection.sendMessage(message);
            }
            if (connections.size() > 1 && checkPartyReady()) {
              for (ProcessConnection connection : connections) {
                String[] message = { "200" };
                connection.sendMessage(message);
              }
              nextTurn();
              createDeck();
              for (ProcessConnection connection : connections) {
                for (int i = 0; i < 7; i++) {
                  connection.drawCard();
                }
              }
            }
          }
        }
      }
      sin.close();
      socket.close();
    } catch (IOException e) {
      System.out.println("Could not get input from client");
    }
  }

  private void sendMessage(String[] messages) {
    for (String message : messages) {
      sout.println(message);
    }
  }

  private boolean usernameExists(String username) {
    for (ProcessConnection connection : connections) {
      if (connection.username.equals(username)) {
        return true;
      }
    }
    return false;
  }

  private boolean checkPartyReady() {
    for (ProcessConnection connection : connections) {
      if (connection.ready.equals("false")) {
        return false;
      }
    }
    return true;
  }

  private void createDeck() {
    String[] colors = { "red", "orange", "blue", "purple" };
    for (String color : colors) {
      for (int i = 0; i < 10; i++) {
        deck.add(new CardFront(String.valueOf(i), color));
        if (i != 0) {
          deck.add(new CardFront(String.valueOf(i), color));
        }
      }
      for (int j = 0; j < 2; j++) {
        deck.add(new CardFront("block", color));
        deck.add(new CardFront("reverse", color));
        deck.add(new CardFront("plusTwo", color));
      }
      deck.add(new CardFront("plusFour"));
      deck.add(new CardFront("wild"));
    }
    Collections.shuffle(deck);
  }

  private void drawCard() {
    CardFront card = deck.remove(deck.size() - 1);
    for (ProcessConnection connection : connections) {
      String[] message = { "draw", username, card.toString() };
      connection.sendMessage(message);
    }
  }

  private void nextTurn() {
    if (currentPlayer == connections.size()) {
      currentPlayer = 0;
    }
    for (ProcessConnection connection : connections) {
      String[] message = { "turn", connections.get(currentPlayer).username };
      connection.sendMessage(message);
    }
    currentPlayer++;
  }
}
