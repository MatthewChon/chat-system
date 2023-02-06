import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import java.awt.Dimension;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class AppClient extends JFrame implements Runnable {
    private static final int MAXWIDTH = 1200, MAXHEIGHT = 800;
    private static final int MINWIDTH = 800, MINHEIGHT = 600;

    private String client_name;
    private String host_address;
    private int port_num;
    private String certificate;
    private String _TRUSTSTORE_PASS;

    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private ChatGUI window;
    
    public AppClient(String certificate_path, String pass, String host_address, int port) {
        super("Chatterbox");
        client_name = JOptionPane.showInputDialog("Enter name for the chat");

        certificate = certificate_path;
        _TRUSTSTORE_PASS = pass;
        this.host_address = host_address;
        port_num = port;

        addWindowListener(new ShutdownHandler());        
    }
    @Override
    public void run() {
        try {
            client = setupSSLClientSocket();
            out = new PrintWriter(client.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            sendMessage(client_name);

            window = new ChatGUI(this);
            add(window);
            Thread input_thread = new Thread(window);
            input_thread.start();
            configure();
            deploy();

            String incoming_message;
            while ((incoming_message = in.readLine()) != null) {
                window.updateLog(incoming_message);
            }
            shutdown();
        } catch (Exception e) {
            shutdown();
        }
    }
    private SSLSocket setupSSLClientSocket() throws IOException {
        System.setProperty("javax.net.ssl.trustStore", certificate);
        System.setProperty("javax.net.ssl.trustStorePassword", _TRUSTSTORE_PASS);

        SSLSocketFactory socket_factory = (SSLSocketFactory)SSLSocketFactory.getDefault();
        SSLSocket socket = (SSLSocket) socket_factory.createSocket(host_address, port_num);

        return socket;
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
    public void sendMessage(String message) {
        out.println(message);
    }
    public void shutdown() {
        try {
            out.println("/quit");
            in.close();
            out.close();
            if (!client.isClosed()) {
                client.close();
            }
            dispose();
        } catch (IOException e) {
            // ignore
        }
    }
    public String getName() {
        return client_name;
    }
    public void changeName(String new_name) {
        client_name = new_name;
    }

    class ShutdownHandler implements WindowListener {
        @Override
        public void windowOpened(WindowEvent e) {  
            // ignore          
        }

        @Override
        public void windowClosing(WindowEvent e) {
            shutdown();
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
    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.print("Insufficient arguments! Missing: ");
            if (args.length < 1) {
                System.err.print(" [Certificate Path]");
            }
            if (args.length < 2) {
                System.err.print(" [Certificate Password]");
            }
            if (args.length < 3) {
                System.err.print(" [Host Address]");
            }
            if (args.length < 4) {
                System.err.print(" [Port Number]");
            }
            System.err.println();
            return;
        }
        AppClient client = new AppClient(args[0], args[1], args[2], Integer.parseInt(args[3]));
        client.run();
    }
}