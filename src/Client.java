import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client implements Runnable{

    private Socket client;
    private BufferedReader in;
    private PrintWriter out;

    private boolean done;

    @Override
    public void run() {
        try {
            client = new Socket("127.0.0.1", 3333); // Conecta-se ao servidor local na porta 3333
            out = new PrintWriter(client.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            InputHandler inHandler = new InputHandler();
            Thread thread = new Thread(inHandler);
            thread.start();

            String inMessage;
            while ((inMessage = in.readLine()) != null) {
                System.out.println(inMessage); // Exibe mensagens recebidas do servidor no console
            }
        } catch (IOException e) {
            shutdown();
        }
    }

    public void shutdown(){
        done = true;
        try {
            in.close();
            out.close();
            if (!client.isClosed()) {
                client.close();
            }
        } catch (IOException e) {
            //ignore
        }
    }
    class InputHandler implements Runnable{

        @Override
        public void run() {
            try {
                BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));
                while (!done) {
                    String message = inReader.readLine(); // Lê mensagens digitadas pelo usuário no console
                    if (message.equals("/quit")) {
                        out.println(message);
                        inReader.close();
                        shutdown(); // Fecha a conexão e encerra o cliente
                    } else {
                        out.println(message);
                    }
                }
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }
}
