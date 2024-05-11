import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.Border;

public class Client {

  private int PORT = 5190;

  private Color backgroundColor = new Color(235, 252, 235);
  private Color darkRedColor = new Color(171, 26, 33);
  private Color lightRedColor = new Color(250, 205, 215);
  private Color grayColor = new Color(97, 95, 95);
  private Color darkGreenColor = new Color(66, 125, 46);
  private Color lightGreenColor = new Color(205, 250, 190);

  Border darkBorder = BorderFactory.createLineBorder(grayColor, 1);
  Border marginsBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10);
  Border darkMarginsBorder = BorderFactory.createCompoundBorder(
    darkBorder,
    marginsBorder
  );

  private JFrame frame;

  private JPanel joinPanel;
  private JLabel title;
  private JLabel serverLabel;
  private JTextField serverInput;
  private JLabel usernameLabel;
  private JTextField usernameInput;
  private JButton joinButton;
  private JTextArea errorMessage;

  private JPanel partyPanel;
  private JButton readyButton;

  private JPanel gamePanel;
  private JPanel playerHandPanel;
  private TopPanel topHandPanel;
  private JPanel centerPanel;
  private CardBack deck;
  private CardFront lastCard;
  private JLabel playerTurn;

  private JPanel winPanel;
  private JLabel winLabel;

  private GridBagConstraints gbcTop;
  private GridBagConstraints gbcCenter;

  private String server;
  private String username;

  private Socket socket;
  private PrintStream sout;
  private Scanner sin;

  private HashMap<String, PlayerReady> partyReady;
  private HashMap<String, Integer> players;

  private GridBagConstraints gbc;

  private boolean inParty;
  private boolean inGame;

  private boolean wild = false;

  private String currentPlayer;

  public Client() {
    inGame = false;
    inParty = false;

    frame = new JFrame("Uno!");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setVisible(true);

    joinUI();

    frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
  }

  private void joinUI() {
    joinPanel = new JPanel();
    joinPanel.setBackground(backgroundColor);
    joinPanel.setLayout(new GridBagLayout());
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.insets = new Insets(10, 10, 10, 10);
    frame.add(joinPanel);

    gbc.gridx++;
    title = new JLabel();
    incFontSize(title, 100);
    title.setText("UNO");
    joinPanel.add(title, gbc);
    gbc.gridx--;

    gbc.gridy++;
    serverLabel = new JLabel();
    incFontSize(serverLabel, 10);
    serverLabel.setText("Server:");
    joinPanel.add(serverLabel, gbc);

    gbc.gridx++;
    serverInput = new JTextField();
    serverInput.setPreferredSize(new Dimension(300, 30));
    joinPanel.add(serverInput, gbc);
    gbc.gridx--;

    gbc.gridy++;
    usernameLabel = new JLabel();
    incFontSize(usernameLabel, 10);
    usernameLabel.setText("Username:");
    joinPanel.add(usernameLabel, gbc);

    gbc.gridx++;
    usernameInput = new JTextField();
    usernameInput.setPreferredSize(new Dimension(300, 30));
    joinPanel.add(usernameInput, gbc);

    gbc.gridy++;
    joinButton = new JButton("Join Game");
    incFontSize(joinButton, 10);
    joinButton.setBackground(Color.WHITE);
    joinButton.addActionListener(new JoinButtonListener());
    joinPanel.add(joinButton, gbc);

    gbc.gridy++;
    errorMessage = new JTextArea();
    incFontSize(errorMessage, 5);
    errorMessage.setBorder(darkMarginsBorder);
    errorMessage.setForeground(darkRedColor);
    errorMessage.setVisible(false);
    joinPanel.add(errorMessage, gbc);
  }

  private void partyUI() {
    frame.remove(joinPanel);

    partyPanel = new JPanel();
    partyPanel.setLayout(new GridBagLayout());
    partyPanel.setBackground(backgroundColor);
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.insets = new Insets(10, 10, 10, 10);
    frame.add(partyPanel);

    gbc.gridx++;
    title = new JLabel();
    incFontSize(title, 100);
    title.setText("UNO");
    partyPanel.add(title, gbc);

    gbc.gridy++;

    for (String key : partyReady.keySet()) {
      partyPanel.add(partyReady.get(key), gbc);
      gbc.gridy++;
    }

    String readyText;

    if (partyReady.get(username).ready) {
      readyText = "Unready";
    } else {
      readyText = "Ready";
    }

    readyButton = new JButton(readyText);
    readyButton.setBackground(Color.WHITE);
    readyButton.addActionListener(new ReadyButtonListener());
    incFontSize(readyButton, 10);
    partyPanel.add(readyButton, gbc);

    frame.repaint();
    frame.revalidate();
  }

  private void gameUI() {
    frame.remove(partyPanel);

    gamePanel = new JPanel();
    gamePanel.setBackground(backgroundColor);
    gamePanel.setLayout(new BorderLayout());
    frame.add(gamePanel);

    playerHandPanel = new JPanel();
    playerHandPanel.setLayout(new GridBagLayout());
    playerHandPanel.setPreferredSize(new Dimension(100, 250));
    playerHandPanel.setBackground(backgroundColor);
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.insets = new Insets(0, 0, 10, 100);
    gbc.anchor = GridBagConstraints.NORTH;
    gbc.weighty = 1;
    gamePanel.add(playerHandPanel, BorderLayout.SOUTH);

    topHandPanel = new TopPanel();
    topHandPanel.setLayout(new GridBagLayout());
    topHandPanel.setPreferredSize(new Dimension(100, 250));
    topHandPanel.setBackground(backgroundColor);
    gbcTop = new GridBagConstraints();
    gbcTop.gridx = 0;
    gbcTop.gridy = 0;
    gbcTop.insets = new Insets(0, 0, 10, 100);
    gbcTop.anchor = GridBagConstraints.NORTH;
    gbcTop.weighty = 1;
    gamePanel.add(topHandPanel, BorderLayout.NORTH);

    centerPanel = new JPanel();
    centerPanel.setLayout(new GridBagLayout());
    centerPanel.setBackground(backgroundColor);
    gbcCenter = new GridBagConstraints();
    gbcCenter.gridx = 0;
    gbcCenter.gridy = 0;
    gbcCenter.insets = new Insets(0, 50, 0, 100);
    gbcCenter.anchor = GridBagConstraints.NORTH;
    gbcCenter.weighty = 1;
    gamePanel.add(centerPanel, BorderLayout.CENTER);

    playerTurn = new JLabel();
    playerTurn.setBorder(BorderFactory.createEmptyBorder(0, 0, 50, 0));
    incFontSize(playerTurn, 10);
    if (currentPlayer.equals(username)) {
      playerTurn.setText("your turn");
    } else {
      playerTurn.setText(currentPlayer + "'s' turn");
    }
    centerPanel.add(playerTurn);
    gbcCenter.gridx++;

    deck = new CardBack();
    deck.addMouseListener(deckListener);
    centerPanel.add(deck, gbcCenter);
    gbcCenter.gridx++;

    centerPanel.add(lastCard, gbcCenter);

    frame.repaint();
    frame.revalidate();
  }

  private void winUI(String winner) {
    frame.remove(gamePanel);

    winPanel = new JPanel();
    winPanel.setBackground(backgroundColor);
    winPanel.setLayout(new GridBagLayout());
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.insets = new Insets(10, 10, 10, 10);
    frame.add(winPanel);

    winLabel = new JLabel();
    incFontSize(winLabel, 70);
    winLabel.setText(winner + " won!!");
    winPanel.add(winLabel);

    frame.repaint();
    frame.revalidate();
  }

  private void chooseColorUI(String type) {
    gbcTop.gridx = 0;
    gbcTop.gridy = 0;

    wild = true;

    String[] colors = { "red", "orange", "blue", "purple" };
    for (String color : colors) {
      CardFront card = new CardFront(type, color);
      card.addMouseListener(wildListener);
      topHandPanel.add(card, gbcTop);
      gbcTop.gridx++;
    }

    frame.repaint();
    frame.revalidate();
  }

  private void removeChooseColorUI() {
    topHandPanel.removeAll();
    wild = false;

    frame.repaint();
    frame.revalidate();
  }

  private class PlayerReady extends JPanel {

    private JLabel player;
    private JLabel readyLabel;
    private boolean ready;
    private String username;

    private Color readyDarkColor;
    private Color readyLightColor;
    private String readyText;

    public PlayerReady(boolean ready, String username) {
      this.ready = ready;
      this.username = username;

      if (ready) {
        readyDarkColor = darkGreenColor;
        readyLightColor = lightGreenColor;
        readyText = "Ready";
      } else {
        readyDarkColor = darkRedColor;
        readyLightColor = lightRedColor;
        readyText = "Not Ready";
      }

      setLayout(new GridBagLayout());
      setBackground(readyLightColor);
      GridBagConstraints gbcx = new GridBagConstraints();
      gbcx.gridx = 0;
      gbcx.gridy = 0;
      gbcx.insets = new Insets(10, 20, 10, 20);

      player = new JLabel();
      player.setText(this.username);
      incFontSize(player, 7);
      unboldFont(player);
      add(player, gbcx);
      gbcx.gridx++;

      readyLabel = new JLabel();
      readyLabel.setText(readyText);
      readyLabel.setForeground(readyDarkColor);
      incFontSize(readyLabel, 5);
      add(readyLabel, gbcx);
    }

    private void update(boolean ready) {
      this.ready = ready;

      if (this.ready) {
        readyDarkColor = darkGreenColor;
        readyLightColor = lightGreenColor;
        readyText = "Ready";
      } else {
        readyDarkColor = darkRedColor;
        readyLightColor = lightRedColor;
        readyText = "Not Ready";
      }

      setBackground(readyLightColor);
      readyLabel.setText(readyText);
      readyLabel.setForeground(readyDarkColor);
    }
  }

  private void incFontSize(JLabel label, float increase) {
    Font font = label.getFont();
    Font biggerFont = font.deriveFont(font.getSize() + increase);
    label.setFont(biggerFont);
  }

  private void incFontSize(JButton button, float increase) {
    Font font = button.getFont();
    Font biggerFont = font.deriveFont(font.getSize() + increase);
    button.setFont(biggerFont);
  }

  private void incFontSize(JTextArea textArea, float increase) {
    Font font = textArea.getFont();
    Font biggerFont = font.deriveFont(font.getSize() + increase);
    textArea.setFont(biggerFont);
  }

  private void unboldFont(JLabel label) {
    Font font = label.getFont();
    Font unboldFont = font.deriveFont(Font.PLAIN);
    label.setFont(unboldFont);
  }

  private void setError(String error) {
    errorMessage.setText("Error: " + error);
    errorMessage.setVisible(true);
  }

  private class JoinButtonListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent event) {
      server = serverInput.getText().trim();
      username = usernameInput.getText().trim();

      if (server.equals("") || username.equals("")) {
        setError("Please provide a server and username");
      } else {
        new ServerProcesser().start();
      }
    }
  }

  private class ReadyButtonListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent event) {
      sout.println(!partyReady.get(username).ready);
    }
  }

  private MouseListener playCardListener = new MouseListener() {
    @Override
    public void mouseClicked(MouseEvent e) {
      CardFront card = (CardFront) e.getSource();
      String[] cardParams = card.toString().split(" ");
      String[] lastCardParams = lastCard.toString().split(" ");

      if (currentPlayer.equals(username) && !wild) {
        if (cardParams[1].equals("wild") || cardParams[1].equals("plusFour")) {
          if (playerHandPanel.getComponentCount() == 1) {
            sout.println("win");
          }
          chooseColorUI(cardParams[1]);
          playerHandPanel.remove(card);

          frame.repaint();
          frame.revalidate();
        } else if (
          cardParams[0].equals(lastCardParams[0]) ||
          cardParams[1].equals(lastCardParams[1])
        ) {
          if (playerHandPanel.getComponentCount() == 1) {
            sout.println("win");
          }

          sout.println("play");
          sout.println(card);
          playerHandPanel.remove(card);

          frame.repaint();
          frame.revalidate();

          if (cardParams[1].equals("plusTwo")) {
            sout.println("plusDraw");
            sout.println(2);
          } else if (cardParams[1].equals("plusFour")) {
            sout.println("plusDraw");
            sout.println(4);
          } else if (cardParams[1].equals("block")) {
            sout.println("block");
          } else if (cardParams[1].equals("reverse")) {
            sout.println("reverse");
          } else {
            sout.println("none");
          }
        }
      }
    }

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}
  };

  private MouseListener wildListener = new MouseListener() {
    @Override
    public void mouseClicked(MouseEvent e) {
      CardFront card = (CardFront) e.getSource();
      String[] cardParams = card.toString().split(" ");

      sout.println("play");
      if (cardParams[0].equals("red")) {
        cardParams[0] = "purple";
      } else if (cardParams[0].equals("orange")) {
        cardParams[0] = "blue";
      } else if (cardParams[0].equals("blue")) {
        cardParams[0] = "orange";
      } else if (cardParams[0].equals("purple")) {
        cardParams[0] = "red";
      }
      sout.println(cardParams[0] + " " + cardParams[1]);

      frame.repaint();
      frame.revalidate();

      if (cardParams[1].equals("plusFour")) {
        sout.println("plusDraw");
        sout.println(4);
      } else {
        sout.println("none");
      }

      removeChooseColorUI();
    }

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}
  };

  private MouseListener deckListener = new MouseListener() {
    @Override
    public void mouseClicked(MouseEvent e) {
      if (currentPlayer.equals(username) && !wild) {
        sout.println("draw");
      }
    }

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}
  };

  private class ServerProcesser extends Thread {

    @Override
    public void run() {
      try {
        socket = new Socket(server, PORT);
        sout = new PrintStream(socket.getOutputStream());
        sin = new Scanner(socket.getInputStream());

        sout.println(username);

        partyReady = new HashMap<>();
        players = new HashMap<>();

        while (sin.hasNextLine()) {
          String line = sin.nextLine().trim();
          if (line.equals("200")) {
            if (!inParty) {
              partyUI();
              inParty = true;
            } else {
              sin.nextLine();
              currentPlayer = sin.nextLine().trim();
              String lastCardString = sin.nextLine().trim();
              String[] lastCardParams = lastCardString.split(" ");
              lastCard = new CardFront(lastCardParams[1], lastCardParams[0]);
              gameUI();
              inGame = true;
            }
          } else if (line.equals("500")) {
            String error = sin.nextLine();
            setError(error);
            break;
          } else if (!inGame) {
            String incomingUser = line;
            String incomingReady = sin.nextLine().trim();
            if (partyReady.containsKey(incomingUser)) {
              PlayerReady player = partyReady.get(incomingUser);
              player.update(incomingReady.equals("true"));
              if (incomingUser.equals(username)) {
                if (player.ready) {
                  readyButton.setText("Unready");
                } else {
                  readyButton.setText("Ready");
                }
              }
            } else {
              if (!incomingUser.equals(username)) {
                players.put(incomingUser, 0);
              }
              partyReady.put(
                incomingUser,
                new PlayerReady(incomingReady.equals("true"), incomingUser)
              );
              if (partyPanel != null) {
                partyPanel.add(partyReady.get(incomingUser), gbc);
                gbc.gridy++;
                partyPanel.remove(readyButton);
                partyPanel.add(readyButton, gbc);
                frame.repaint();
                frame.revalidate();
              }
            }
          } else if (inGame) {
            if (line.equals("draw")) {
              String incomingUser = sin.nextLine().trim();
              String card = sin.nextLine().trim();
              String[] cardParams = card.split(" ");

              if (incomingUser.equals(username)) {
                CardFront cardObj = new CardFront(cardParams[1], cardParams[0]);
                cardObj.addMouseListener(playCardListener);

                playerHandPanel.add(cardObj, gbc);
                gbc.gridx++;

                frame.repaint();
                frame.revalidate();

                System.out.println(username);
                System.out.println(incomingUser);
                System.out.println(cardObj);
              } else {
                players.put(incomingUser, players.get(incomingUser) + 1);

                if (incomingUser.equals(currentPlayer)) {
                  topHandPanel.add(new CardBack(), gbcTop);
                  gbcTop.gridx++;
                }

                frame.repaint();
                frame.revalidate();
              }
            } else if (line.equals("turn")) {
              currentPlayer = sin.nextLine();
              topHandPanel.removeAll();

              if (currentPlayer.equals(username)) {
                playerTurn.setText("your turn");
              } else {
                playerTurn.setText(currentPlayer + "'s' turn");
                gbcTop.gridx = 0;
                gbcTop.gridy = 0;
                for (int i = 0; i < players.get(currentPlayer); i++) {
                  topHandPanel.add(new CardBack(), gbcTop);
                  gbcTop.gridx++;
                }
                frame.repaint();
                frame.revalidate();
              }
            } else if (line.equals("newCard")) {
              String card = sin.nextLine().trim();
              String[] cardParams = card.split(" ");
              CardFront cardObj = new CardFront(cardParams[1], cardParams[0]);

              centerPanel.remove(lastCard);
              lastCard = cardObj;
              centerPanel.add(lastCard, gbcCenter);

              frame.repaint();
              frame.revalidate();
            } else if (line.equals("play")) {
              String incomingUser = sin.nextLine();
              if (!incomingUser.equals(username)) {
                players.put(incomingUser, players.get(incomingUser) - 1);
              }
            } else if (line.equals("win")) {
              String winner = sin.nextLine();
              winUI(winner);
            }
          }
        }

        socket.close();
        sin.close();
      } catch (UnknownHostException e) {
        System.err.println("Server not found");
        setError("Server not found");
      } catch (IOException e) {
        System.out.println("Check your network connection");
        setError("Check your network connection.");
      }
    }
  }

  public static void main(String[] args) {
    new Client();
  }
}

class CardFront extends JPanel {

  private Color RED = new Color(255, 151, 138);
  private Color ORANGE = new Color(255, 220, 138);
  private Color BLUE = new Color(138, 206, 255);
  private Color PURPLE = new Color(187, 138, 255);

  private Color cardColor;
  private String cardType;

  private int HEIGHT = 250;
  private int WIDTH = 150;
  private int ARC = 20;

  private Graphics2D g2d;

  public CardFront(String type, String color) {
    cardType = type;
    if (color.equals("black")) {
      cardColor = Color.BLACK;
    } else if (color.equals("red")) {
      cardColor = RED;
    } else if (color.equals("orange")) {
      cardColor = ORANGE;
    } else if (color.equals("blue")) {
      cardColor = BLUE;
    } else if (color.equals("purple")) {
      cardColor = PURPLE;
    }
  }

  public CardFront(String type) {
    cardType = type;
    cardColor = Color.BLACK;
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    g2d = (Graphics2D) g;

    g2d.setColor(Color.WHITE);
    g2d.fillRoundRect(0, 0, WIDTH, HEIGHT, ARC, ARC);

    if (cardType.equals("blank")) {
      g2d.setColor(Color.WHITE);
      g2d.fillRoundRect(5, 5, WIDTH - 10, HEIGHT - 10, ARC - 3, ARC - 3);

      g2d.setColor(Color.BLACK);
      g2d.drawRoundRect(0, 0, WIDTH, HEIGHT, ARC, ARC);
    } else {
      g2d.setColor(cardColor);
      g2d.fillRoundRect(5, 5, WIDTH - 10, HEIGHT - 10, ARC - 3, ARC - 3);

      g2d.setColor(Color.BLACK);
      g2d.drawRoundRect(0, 0, WIDTH, HEIGHT, ARC, ARC);

      if (cardType.equals("block")) {
        drawBlockCard();
      } else if (cardType.equals("reverse")) {
        drawReverseCard();
      } else if (cardType.equals("plusTwo")) {
        drawPlusTwoCard();
      } else if (cardType.equals("plusFour")) {
        drawPlusFourCard();
      } else if (cardType.equals("wild")) {
        drawWildCard();
      } else {
        drawNumCard();
      }
    }
    setOpaque(false);
    setSize(new Dimension(WIDTH + 1, HEIGHT + 1));
  }

  private void drawBlockCard() {
    drawBlock(15, 15, 20, new BasicStroke(4));

    AffineTransform old = g2d.getTransform();
    g2d.rotate(Math.PI, WIDTH / 2.0, HEIGHT / 2.0);
    drawBlock(15, 15, 20, new BasicStroke(4));
    g2d.setTransform(old);

    drawBlock(38, 90, 75, new BasicStroke(8));
  }

  private void drawBlock(int x, int y, int diameter, BasicStroke stroke) {
    g2d.setStroke(stroke);
    g2d.setColor(Color.WHITE);
    g2d.drawOval(x, y, diameter, diameter);

    double angle = Math.toRadians(45);
    int x1 = (int) (x + diameter / 2 + diameter / 2 * Math.cos(angle));
    int y1 = (int) (y + diameter / 2 - diameter / 2 * Math.sin(angle));
    int x2 = (int) (x + diameter / 2 - diameter / 2 * Math.cos(angle));
    int y2 = (int) (y + diameter / 2 + diameter / 2 * Math.sin(angle));
    g2d.drawLine(x1, y1, x2, y2);
  }

  private void drawReverseCard() {
    drawReverse(7, 10, 7, 3, 20, 20, 3, 7);

    AffineTransform old = g2d.getTransform();
    g2d.rotate(Math.PI, WIDTH / 2.0, HEIGHT / 2.0);
    drawReverse(7, 10, 7, 3, 20, 20, 3, 7);
    g2d.setTransform(old);

    drawReverse(30, 35, 25, 10, 55, 113, 10, 22);
  }

  private void drawReverse(
    int arrowWidth,
    int arrowHeight,
    int tailWidth,
    int tailHeight,
    int x,
    int y,
    int xDiff,
    int yDiff
  ) {
    g2d.setColor(Color.WHITE);

    drawArrow(x, y, arrowWidth, arrowHeight, tailWidth, tailHeight, true);

    drawArrow(
      x + xDiff,
      y + yDiff,
      arrowWidth,
      arrowHeight,
      tailWidth,
      tailHeight,
      false
    );
  }

  private void drawArrow(
    int x,
    int y,
    int width,
    int height,
    int tailWidth,
    int tailHeight,
    boolean leftFacing
  ) {
    int[] xPoints = new int[3];
    int[] yPoints = new int[3];

    if (leftFacing) {
      xPoints[0] = x;
      yPoints[0] = y - height / 2;
      xPoints[1] = x + width;
      yPoints[1] = y;
      xPoints[2] = x;
      yPoints[2] = y + height / 2;
    } else {
      xPoints[0] = x + width;
      yPoints[0] = y - height / 2;
      xPoints[1] = x;
      yPoints[1] = y;
      xPoints[2] = x + width;
      yPoints[2] = y + height / 2;
    }

    g2d.fillPolygon(xPoints, yPoints, 3);

    if (leftFacing) {
      g2d.fillRect(x - tailWidth, y - tailHeight / 2, tailWidth, tailHeight);
    } else {
      g2d.fillRect(x + width, y - tailHeight / 2, tailWidth, tailHeight);
    }
  }

  private void drawPlusTwoCard() {
    drawPlusNum(12, 30, "+2");

    AffineTransform old = g2d.getTransform();
    g2d.rotate(Math.PI, WIDTH / 2.0, HEIGHT / 2.0);
    drawPlusNum(12, 30, "+2");
    g2d.setTransform(old);

    int x = 50;
    int y = 108;

    drawMiniCard(x, y, cardColor);
    drawMiniCard(x + 20, y - 13, cardColor);
  }

  private void drawPlusFourCard() {
    drawPlusNum(12, 30, "+4");

    AffineTransform old = g2d.getTransform();
    g2d.rotate(Math.PI, WIDTH / 2.0, HEIGHT / 2.0);
    drawPlusNum(12, 30, "+4");
    g2d.setTransform(old);

    int x = 45;
    int y = 117;

    drawMiniCard(x + 10, y - 14, BLUE);
    drawMiniCard(x + 30, y - 21, PURPLE);
    drawMiniCard(x, y, RED);
    drawMiniCard(x + 20, y - 7, ORANGE);
  }

  private void drawPlusNum(int x, int y, String plusNum) {
    g2d.setFont(new Font("Arial", Font.BOLD, 20));
    g2d.setColor(Color.WHITE);
    g2d.drawString(plusNum, x, y);
  }

  private void drawMiniCard(int x, int y, Color color) {
    int width = 29;
    int height = 42;
    int arc = 2;

    g2d.setColor(Color.WHITE);
    g2d.fillRoundRect(x, y, width, height, arc, arc);
    g2d.setColor(color);
    g2d.fillRoundRect(x + 2, y + 2, width - 4, height - 4, arc - 1, arc - 1);
  }

  private void drawWildCard() {
    drawWildCircle(15, 15, 20);

    AffineTransform old = g2d.getTransform();
    g2d.rotate(Math.PI, WIDTH / 2.0, HEIGHT / 2.0);
    drawWildCircle(15, 15, 20);
    g2d.setTransform(old);

    drawWildCircle(38, 90, 75);
  }

  private void drawWildCircle(int x, int y, int diameter) {
    Color[] colors = { RED, ORANGE, BLUE, PURPLE };

    for (int i = 0; i < 4; i++) {
      g2d.setColor(colors[i]);
      g2d.fillArc(x, y, diameter, diameter, i * 90, 90);
    }

    g2d.setColor(Color.BLACK);
    g2d.drawOval(x, y, diameter, diameter);
  }

  private void drawNumCard() {
    drawNum(15, 33, new Font("Arial", Font.BOLD, 24));

    AffineTransform old = g2d.getTransform();
    g2d.rotate(Math.PI, WIDTH / 2.0, HEIGHT / 2.0);
    drawNum(15, 33, new Font("Arial", Font.BOLD, 24));
    g2d.setTransform(old);

    drawNum(46, 158, new Font("Arial", Font.BOLD, 100));

    if (cardType.equals("9")) {
      g2d.fillRect(52, 165, 44, 3);
    } else if (cardType.equals("6")) {
      g2d.fillRect(52, 165, 46, 3);
    }
  }

  private void drawNum(int x, int y, Font font) {
    g2d.setFont(font);
    g2d.setColor(Color.WHITE);
    g2d.drawString(cardType, x, y);
  }

  @Override
  public String toString() {
    String color;
    if (cardColor == RED) {
      color = "red";
    } else if (cardColor == ORANGE) {
      color = "orange";
    } else if (cardColor == BLUE) {
      color = "blue";
    } else if (cardColor == PURPLE) {
      color = "purple";
    } else {
      color = "black";
    }
    return color + " " + cardType;
  }
}

class CardBack extends JPanel {

  private Color RED = new Color(255, 151, 138);
  private Color ORANGE = new Color(255, 220, 138);
  private Color BLUE = new Color(138, 206, 255);
  private Color PURPLE = new Color(187, 138, 255);

  private int HEIGHT = 250;
  private int WIDTH = 150;
  private int ARC = 20;

  private Graphics2D g2d;

  @Override
  public void paintComponent(Graphics g) {
    super.paintComponent(g);

    g2d = (Graphics2D) g;

    g2d.setColor(Color.WHITE);
    g2d.fillRoundRect(0, 0, WIDTH, HEIGHT, ARC, ARC);

    g2d.setColor(Color.BLACK);
    g2d.fillRoundRect(5, 5, WIDTH - 10, HEIGHT - 10, ARC - 3, ARC - 3);

    g2d.setColor(Color.BLACK);
    g2d.drawRoundRect(0, 0, WIDTH, HEIGHT, ARC, ARC);

    int x = 13;
    int y = 100;

    g2d.setStroke(new BasicStroke(17));

    g2d.setColor(RED);
    g2d.drawLine(x, y, x + 123, y);

    y += 16;
    g2d.setColor(BLUE);
    g2d.drawLine(x, y, x + 123, y);

    y += 16;
    g2d.setColor(ORANGE);
    g2d.drawLine(x, y, x + 123, y);

    y += 16;
    g2d.setColor(PURPLE);
    g2d.drawLine(x, y, x + 123, y);

    g2d.setFont(new Font("Arial", Font.BOLD, 45));
    g2d.setColor(Color.WHITE);
    g2d.drawString("UNO", 25, 140);

    setOpaque(false);
    setSize(new Dimension(WIDTH + 1, HEIGHT + 1));
  }
}

class TopPanel extends JPanel {

  @Override
  public void paintComponent(Graphics g) {
    Graphics2D g2d = (Graphics2D) g;
    super.paintComponent(g2d);
    g2d.rotate(Math.PI, getWidth() / 2.0, getHeight() / 2.0);
    g2d.translate(-39, 100);
  }
}
