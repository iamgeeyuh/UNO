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

  static boolean reverse = false;
  static int currentPlayer = 0;
  static CardFront currentCard;

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
              gameStart = true;
              nextTurn();
              createDeck();
              currentCard = deck.remove(deck.size() - 1);
              for (ProcessConnection connection : connections) {
                String[] message = { currentCard.toString() };
                connection.sendMessage(message);
              }
              for (ProcessConnection connection : connections) {
                for (int i = 0; i < 7; i++) {
                  connection.drawCard();
                }
              }
            }
          } else if (gameStart) {
            if (line.equals("play")) {
              String[] cardParams = sin.nextLine().split(" ");
              currentCard = new CardFront(cardParams[1], cardParams[0]);

              String action = sin.nextLine();

              if (action.equals("plusDraw")) {
                int draws = Integer.valueOf(sin.nextLine());
                for (int i = 0; i < draws; i++) {
                  connections.get(currentPlayer).drawCard();
                }
                nextTurn();
              } else if (action.equals("block")) {
                nextTurn();
              } else if (action.equals("reverse")) {
                System.out.println(currentPlayer);
                reverse = !reverse;
                if (reverse) {
                  currentPlayer -= 2;
                  if (currentPlayer < 0) {
                    currentPlayer = connections.size() + currentPlayer;
                  }
                } else {
                  currentPlayer += 2;
                  if (currentPlayer >= connections.size()) {
                    currentPlayer -= connections.size();
                  }
                }
                System.out.println(currentPlayer);
              }
              nextTurn();

              String[] message = {
                "newCard",
                currentCard.toString(),
                "play",
                username,
              };
              for (ProcessConnection connection : connections) {
                connection.sendMessage(message);
              }
            } else if (line.equals("draw")) {
              drawCard();
              nextTurn();
            } else if (line.equals("win")) {
              String[] message = { "win", username };
              for (ProcessConnection connection : connections) {
                connection.sendMessage(message);
              }
            }
          }
        }
        sin.close();
        socket.close();
      }
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
    if (deck.size() == 0) {
      createDeck();
    }

    CardFront card = deck.remove(deck.size() - 1);
    String[] message = { "draw", username, card.toString() };
    for (ProcessConnection connection : connections) {
      connection.sendMessage(message);
    }
  }

  private void nextTurn() {
    for (ProcessConnection connection : connections) {
      String[] message = { "turn", connections.get(currentPlayer).username };
      connection.sendMessage(message);
    }
    if (!reverse) {
      currentPlayer++;
      if (currentPlayer == connections.size()) {
        currentPlayer = 0;
      }
    } else {
      currentPlayer--;
      if (currentPlayer == -1) {
        currentPlayer = connections.size() - 1;
      }
    }
  }
}
