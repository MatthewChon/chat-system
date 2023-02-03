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

public class ChatServer implements Runnable {
    private static int portnumber;
    private ServerSocket server;
    private ArrayList<ConnectionHandler> connections;
    private boolean done;
    private ExecutorService threadPool;
    private DateFormat textDateFormatter = new SimpleDateFormat("hh:mm:ss a zzz 'on' MM:dd:yyyy");


    private DateFormat dateFormat = new SimpleDateFormat("E yyyy.MM.dd 'at' hh:mm:ss a zzz");

    public ChatServer(int port) {
        portnumber = port;
        connections = new ArrayList<>();
        done = false;
    }
    @Override
    public void run() {
        try {
            server = new ServerSocket(portnumber);
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
    protected void broadcast(String message) {
        Date now = new Date();
        for (ConnectionHandler clientConnection: connections) {
            if (clientConnection != null) {
                clientConnection.sendMessage(String.format("%s %s", textDateFormatter.format(now),
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
        if (args.length < 1) {
            System.err.println("Please specify a port number");
            return;
        }
        ChatServer server = new ChatServer(Integer.parseInt(args[0]));
        server.run();
    }
}
