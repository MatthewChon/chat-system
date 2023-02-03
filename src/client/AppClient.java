import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class AppClient implements Runnable {
    private String ipAddress;
    private int portnumber;

    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private ChatGUI window;
    
    public AppClient(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        portnumber = port;
    }
    @Override
    public void run() {
        try {
            client = new Socket(ipAddress, portnumber);
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
        if (args.length < 2) {
            System.err.println("Please execute in this format:\n\t... AppClient [ip_address][port_number]");
            return;
        }
        AppClient client = new AppClient(args[0], Integer.parseInt(args[1]));
        client.run();
    }
}