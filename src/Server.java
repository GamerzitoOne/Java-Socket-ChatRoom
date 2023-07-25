import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {

    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;

    private boolean done;

    private ExecutorService pool; // Pool de threads para lidar com as conexões dos clientes de forma concorrente

    public Server() {
        connections = new ArrayList<>();
        done = false;
    }

    // Classe interna para lidar com cada conexão individual de cliente
    class ConnectionHandler implements Runnable {

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                // Escolher apelido
                out.println("Escolha um nickname: ");
                nickname = in.readLine();
                System.out.println(nickname + " conectou-se");
                broadcast(nickname + " entrou no chat");
                String message;
                while ((message = in.readLine()) != null){
                    if (message.startsWith("/nick")){ // trocar de nick
                        String[] messageSplit = message.split(" ", 2);
                        if (messageSplit.length == 2){
                            broadcast(nickname + "trocou de nick para: " + messageSplit[1]);
                            System.out.println(nickname + "trocou de nick para: " + messageSplit[1]);
                            nickname = messageSplit[1];
                            out.println("Nick trocado com sucesso para: " + nickname);
                        } else{
                            out.println("Nickname não fornecido");
                        }
                    } else if (message.startsWith("/quit")){
                        broadcast(nickname + " se desconectou");
                        shutdown();
                    } else {
                        broadcast(nickname + ": " + message);
                    }
                }
            } catch (IOException e) {
                shutdown();
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public void closeConnection() throws IOException {
            in.close();
            out.close();
            if (!client.isClosed()) {
                client.close();
            }
        }
    }

    @Override
    public void run() {
        try {
            Integer port = 3333;
            server = new ServerSocket(port);
            pool = Executors.newCachedThreadPool();
            System.out.println("Servidor rodando na porta: " + port);

            while (!done) {
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (Exception e) {
            shutdown();
        }
    }

    // Envia a mesma mensagem para todos os clientes conectados
    public void broadcast(String message) {
        for (ConnectionHandler ch : connections) {
            if (ch != null) {
                ch.sendMessage(message);
            }
        }
    }

    public void shutdown() {
        done = true;
        pool.shutdown();
        if (!server.isClosed()) {
            try {
                server.close();
                for (ConnectionHandler ch : connections) {
                    ch.closeConnection();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public static void main(String[] args) {
        Server server = new Server();
        Thread serverThread = new Thread(server);
        serverThread.start();
    }
}
