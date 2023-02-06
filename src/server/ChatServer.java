import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

public class ChatServer implements Runnable {
    private static int portnumber;
    private ServerSocket server;
    private ArrayList<ConnectionHandler> connections;
    private boolean done;
    private ExecutorService threadPool;
    private DateFormat textDateFormatter = new SimpleDateFormat("hh:mm:ss a zzz 'on' MM:dd:yyyy");
    private String _KEYSTORE;
    private String _STORE_PASS;


    private DateFormat dateFormat = new SimpleDateFormat("E yyyy.MM.dd 'at' hh:mm:ss a zzz");

    public ChatServer(String certPath, String pass, int port) {
        portnumber = port;
        connections = new ArrayList<>();
        done = false;
        _KEYSTORE = certPath;
        _STORE_PASS = pass;
    }
    @Override
    public void run() {
        try {
            server = setupSSLServerSocket();
            threadPool = Executors.newCachedThreadPool();
            while (!done) {
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                threadPool.execute(handler);
            }
        } catch (IOException e) {
            shutdown();
        }
    }
    private SSLServerSocket setupSSLServerSocket() throws IOException {
        System.setProperty("javax.net.ssl.keyStore", _KEYSTORE);
        System.setProperty("javax.net.ssl.keyStorePassword", _STORE_PASS);

        SSLServerSocketFactory socketFactory = (SSLServerSocketFactory)SSLServerSocketFactory.getDefault();
        SSLServerSocket server = (SSLServerSocket)socketFactory.createServerSocket(portnumber);

        return server;
    }
    protected void broadcast(String message) {
        Date now = new Date();
        for (ConnectionHandler clientConnection: connections) {
            if (clientConnection != null) {
                clientConnection.sendMessage(String.format("[ %s ] %s", textDateFormatter.format(now),
                                                                            message));
            }
        }
    }
    protected void systemBroadcast(String message) {
        Date now = new Date();
        System.out.println(String.format("%s : %s", dateFormat.format(now), message));
        for (ConnectionHandler clientConnection: connections) {
            if (clientConnection != null) {
                clientConnection.sendMessage(String.format("[System] %s",message));
            }
        }
    }
    public void shutdown() {
        try {
            done = true;
            threadPool.shutdown();
            if (!server.isClosed()) {
                server.close();
            }
            for (ConnectionHandler clientConnection: connections) {
                clientConnection.shutdown();
            }
        } catch (IOException e) {
            // ignore
        }
    }
    class ConnectionHandler implements Runnable {
        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String clientName;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }
        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                clientName = in.readLine();
                systemBroadcast(String.format("%s joined the chat!", clientName));

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/nick ")) {
                        String[] messageSplit = message.split(" ", 2);
                        if (messageSplit.length == 2) {
                            systemBroadcast(String.format("%s renamed to %s", clientName, messageSplit[1]));
                            clientName = messageSplit[1];
                            out.println(String.format("[System] Successfully change to %s!", clientName));
                        } else {
                            out.println("No nickname provided!");
                        }
                    } else if (message.equals("/quit")) {
                        systemBroadcast(String.format("%s left the chat!", clientName));
                        shutdown();
                    } else {
                        broadcast(String.format("%s : %s", clientName, message));
                    }
                }
            } catch (Exception e) {
                shutdown();
            }
        }
        public String getName() {
            return clientName;
        }
        public void sendMessage(String message) {
            out.println(message);
        }
        public void shutdown() {
            try {
                in.close();
                out.close();
                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }
    }
    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.print("Insufficient argument: Missing");
            if (args.length < 1) {
                System.err.print(" [Certificate Path]");
            }
            if (args.length < 2) {
                System.err.print(" [Certificate Password]");
            }
            if (args.length < 3) {
                System.err.print(" [Port Number]");
            }
            System.err.println();
            return;
        }
        ChatServer server = new ChatServer(args[0], args[1], Integer.parseInt(args[2]));
        server.run();
    }
}
