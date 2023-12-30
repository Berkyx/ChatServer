import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static ConcurrentHashMap<String, ClientHandler> clientHandlers = new ConcurrentHashMap<>();
    private static ServerSocket serverSocket;

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Syntax: java ChatServer <port-number>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        serverSocket = new ServerSocket(port);
        System.out.println("Server started on port: " + port);

        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                 System.out.println("New client connected: " + clientSocket.getInetAddress());
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandler.start();
            }
        } catch (IOException e) {
            System.out.println("Server exception: " + e.getMessage());
        } finally {
            serverSocket.close();
        }
    }

    public static void broadcastMessage(String clientId, String message) {
        for (ClientHandler handler : clientHandlers.values()) {
            if (!handler.getClientId().equals(clientId)) {
                handler.sendMessage(message);
            }
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private String clientId;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public String getClientId() {
            return clientId;
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    // Initialize clientId for the first message
                    if (clientId == null) {
                        clientId = inputLine;
                        System.out.println("Client connected with ID: " + clientId);
                        clientHandlers.put(clientId, this);
                        continue; // Skip the rest of the loop to wait for the next message
                    }

                    // Broadcast the message to other clients
                    System.out.println(clientId + ": " + inputLine);
                    ChatServer.broadcastMessage(clientId, clientId + ": " + inputLine);
                }
            } catch (IOException e) {
                System.out.println("Error handling client " + clientId + ": " + e.getMessage());
            } finally {
                System.out.println("Client disconnected: " + clientId);
                clientHandlers.remove(clientId);
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Couldn't close a socket for client " + clientId);
                }
            }
        }

    }
}
