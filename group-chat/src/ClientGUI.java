import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ClientGUI {

    private JFrame frame;
    private JTextPane messagePane;
    private JTextField inputField;
    private JButton sendButton;

    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String username;

    // Her kullanıcıya rastgele bir renk atamak için map
    private static final Map<String, Color> userColors = new HashMap<>();
    private static final Random random = new Random();

    public ClientGUI(Socket socket, String username) {
        this.socket = socket;
        this.username = username;

        try {
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            bufferedWriter.write(username);
            bufferedWriter.newLine();
            bufferedWriter.flush();

            createGUI();
            listenForMessages();

        } catch (IOException e) {
            showError("Bağlantı kurulamadı: " + e.getMessage());
            closeEverything();
        }
    }

    private void createGUI() {
        frame = new JFrame("Sohbet - " + username);
        frame.setSize(500, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        messagePane = new JTextPane();
        messagePane.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(messagePane);
        frame.add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendButton = new JButton("Gönder");

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        frame.add(inputPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        frame.setVisible(true);
    }

    private void sendMessage() {
        String messageToSend = inputField.getText().trim();
        if (!messageToSend.isEmpty()) {
            String fullMessage = username + ": " + messageToSend;

            appendMessage(fullMessage);

            try {
                bufferedWriter.write(fullMessage);
                bufferedWriter.newLine();
                bufferedWriter.flush();
            } catch (IOException e) {
                showError("Mesaj gönderilemedi.");
                closeEverything();
            }

            inputField.setText("");
        }
    }

    private void listenForMessages() {
        new Thread(() -> {
            String messageFromServer;
            while (socket.isConnected()) {
                try {
                    messageFromServer = bufferedReader.readLine();
                    if (messageFromServer != null) {
                        appendMessage(messageFromServer);
                    }
                } catch (IOException e) {
                    showError("Bağlantı koptu.");
                    closeEverything();
                    break;
                }
            }
        }).start();
    }

    // Mesajı kullanıcı ismine göre renklendirerek ekleyen metot
    private void appendMessage(String message) {
        // Mesaj formatı: "username: mesaj içeriği"
        int colonIndex = message.indexOf(":");
        String user;
        String msgText;
        if (colonIndex != -1) {
            user = message.substring(0, colonIndex).trim();
            msgText = message.substring(colonIndex + 1).trim();
        } else {
            user = "SERVER";
            msgText = message;
        }

        // Renk yoksa yeni renk ata
        userColors.putIfAbsent(user, getRandomColor());

        Color userColor = userColors.get(user);

        StyledDocument doc = messagePane.getStyledDocument();

        // Kullanıcı adı için stil
        Style userStyle = messagePane.addStyle("UserStyle", null);
        StyleConstants.setForeground(userStyle, userColor);
        StyleConstants.setBold(userStyle, true);

        // Mesaj metni için stil (siyah normal)
        Style messageStyle = messagePane.addStyle("MessageStyle", null);
        StyleConstants.setForeground(messageStyle, Color.BLACK);

        try {
            doc.insertString(doc.getLength(), user + ": ", userStyle);
            doc.insertString(doc.getLength(), msgText + "\n", messageStyle);

            // Otomatik kaydır
            messagePane.setCaretPosition(doc.getLength());

        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    // Rastgele hoş renk oluştur
    private Color getRandomColor() {
        float hue = random.nextFloat();
        float saturation = 0.5f + random.nextFloat() * 0.5f; // 0.5 - 1.0 arası
        float brightness = 0.7f + random.nextFloat() * 0.3f; // 0.7 - 1.0 arası
        return Color.getHSBColor(hue, saturation, brightness);
    }

    private void showError(String errorMessage) {
        JOptionPane.showMessageDialog(frame, errorMessage, "Hata", JOptionPane.ERROR_MESSAGE);
    }

    private void closeEverything() {
        try {
            if (bufferedReader != null) bufferedReader.close();
            if (bufferedWriter != null) bufferedWriter.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String username = JOptionPane.showInputDialog(null, "Kullanıcı adınızı girin:", "Giriş", JOptionPane.PLAIN_MESSAGE);
            if (username != null && !username.trim().isEmpty()) {
                try {
                    Socket socket = new Socket("localhost", 1234);
                    new ClientGUI(socket, username.trim());
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null, "Sunucuya bağlanılamadı.", "Hata", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }
}
