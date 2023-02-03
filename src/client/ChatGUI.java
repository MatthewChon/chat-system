import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;

public class ChatGUI extends JFrame implements Runnable {
    private static final int MAXWIDTH = 1200, MAXHEIGHT = 800;
    private static final int MINWIDTH = 800, MINHEIGHT = 600;
    private int chatLogRowSize = 30;

    AppClient clientSocket;
    private JPanel mainContent;
    private JPanel chatComponent;
    private JTextArea groupChatLog;
    private JTextField userInput;
    private String windowName;

    public ChatGUI(AppClient client) {
        super("Chatterbox");
        windowName = JOptionPane.showInputDialog("Enter name for the chat");
        clientSocket = client;
        clientSocket.sendMessage(windowName);
        chatComponent = createChatComponent();
        addWindowListener(new ShutdownHandler());
    }
    private JPanel createChatComponent() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        groupChatLog = createChatLog();
        userInput = createUserTextField();
        panel.add(groupChatLog);
        panel.add(userInput);
        return panel;
    }
    private JTextArea createChatLog() {
        JTextArea textField = new JTextArea();
        textField.setRows(chatLogRowSize);
        textField.setEditable(false);
        textField.getCaret().setVisible(false);
        textField.getCaret().setBlinkRate(0);
        return textField;
    }
    private JTextField createUserTextField() {
        JTextField inputField = new JTextField();
        inputField.addKeyListener(new InputController());
        return inputField;
    }
    public void updateLog(String message) {
        groupChatLog.append(message + "\n");
    }

    @Override
    public void run() {
        mainContent = createPanel();
        add(mainContent);
        configure();
        deploy();
    }
    private JPanel createPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(chatComponent, BorderLayout.CENTER);
        return panel;
    }
    private void configure() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMaximumSize(new Dimension(MAXWIDTH, MAXHEIGHT));
        setMinimumSize(new Dimension(MINWIDTH, MINHEIGHT));
    }
    private void deploy() {
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
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
                String message = userInput.getText();
                if (message.startsWith("/nick ")) {
                    String[] messageSplit = message.split(" ", 2);
                    if (messageSplit.length == 2) {
                        windowName = messageSplit[1];
                    }
                } else if (message.equals("/quit")) {
                    dispose();
                    clientSocket.shutdown();
                    System.exit(0);
                }
                clientSocket.sendMessage(message);
                userInput.setText("");
            }
        }
    }
    class ShutdownHandler implements WindowListener {
        @Override
        public void windowOpened(WindowEvent e) {  
            // ignore          
        }

        @Override
        public void windowClosing(WindowEvent e) {
            clientSocket.shutdown();
        }

        @Override
        public void windowClosed(WindowEvent e) {
            // ignore    
        }

        @Override
        public void windowIconified(WindowEvent e) {   
            // ignore         
        }

        @Override
        public void windowDeiconified(WindowEvent e) {     
            // ignore       
        }

        @Override
        public void windowActivated(WindowEvent e) {     
            // ignore       
        }

        @Override
        public void windowDeactivated(WindowEvent e) {        
            // ignore    
        }

    }
}
