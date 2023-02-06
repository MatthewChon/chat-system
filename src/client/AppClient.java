import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class AppClient implements Runnable {
    private String ipAddress;
    private int portnumber;
    private String certificate;
    private String _TRUSTSTORE_PASS;

    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private ChatGUI window;
    
    public AppClient(String certPath, String pass, String ipAddress, int port) {
        certificate = certPath;
        _TRUSTSTORE_PASS = pass;
        this.ipAddress = ipAddress;
        portnumber = port;
    }
    @Override
    public void run() {
        try {
            client = setupSSLClientSocket();
            out = new PrintWriter(client.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            window = new ChatGUI(this);
            Thread inputThread = new Thread(window);
            inputThread.start();
            String incomingMessage;
            while ((incomingMessage = in.readLine()) != null) {
                window.updateLog(incomingMessage);
            }
            shutdown();
        } catch (Exception e) {
            shutdown();
        }
    }
    private SSLSocket setupSSLClientSocket() throws IOException {
        System.setProperty("javax.net.ssl.trustStore", certificate);
        System.setProperty("javax.net.ssl.trustStorePassword", _TRUSTSTORE_PASS);

        SSLSocketFactory socketFactory = (SSLSocketFactory)SSLSocketFactory.getDefault();
        SSLSocket socket = (SSLSocket) socketFactory.createSocket(ipAddress, portnumber);

        return socket;
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
        } catch (IOException e) {
            // ignore
        }
    }
    public static void main(String[] args) {
        if (args.length < 3) {
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