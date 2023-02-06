import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class ChatGUI extends JPanel implements Runnable {
    private int chatLogRowSize = 30;

    AppClient client_socket;
    private JPanel chat_component;
    private JTextArea groupchat_log;
    private JTextField user_input;

    public ChatGUI(AppClient client) {
        super(new BorderLayout());
        client_socket = client;
        chat_component = createChatComponent();
    }
    private JPanel createChatComponent() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        groupchat_log = createChatLog();
        user_input = createUserTextField();
        panel.add(groupchat_log);
        panel.add(user_input);
        return panel;
    }
    private JTextArea createChatLog() {
        JTextArea text_field = new JTextArea();
        text_field.setRows(chatLogRowSize);
        text_field.setEditable(false);
        text_field.getCaret().setVisible(false);
        text_field.getCaret().setBlinkRate(0);
        return text_field;
    }
    private JTextField createUserTextField() {
        JTextField input_field = new JTextField();
        input_field.addKeyListener(new InputController());
        return input_field;
    }
    public void updateLog(String message) {
        groupchat_log.append(message + "\n");
    }

    @Override
    public void run() {
        add(chat_component, BorderLayout.CENTER);
    }
    class InputController implements KeyListener {
        public InputController() {
        }
        @Override
        public void keyTyped(KeyEvent e) {
            // ignore
        }

        @Override
        public void keyPressed(KeyEvent e) {
            // ignore
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                String message = user_input.getText();
                if (message.startsWith("/nick ")) {
                    String[] messageSplit = message.split(" ", 2);
                    if (messageSplit.length == 2) {
                        client_socket.setName(messageSplit[1]);
                    }
                } else if (message.equals("/quit")) {
                    client_socket.shutdown();
                    System.exit(0);
                }
                client_socket.sendMessage(message);
                user_input.setText("");
            }
        }
    }
}
