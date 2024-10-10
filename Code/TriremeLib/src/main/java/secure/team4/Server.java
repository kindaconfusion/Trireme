package secure.team4;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public Server(int port) throws IOException {
        try(ServerSocket server = new ServerSocket(15420);
            Socket clientSocket = server.accept();
            DataInputStream in = new DataInputStream(
                    new BufferedInputStream(clientSocket.getInputStream()))) {
            String line = "";
            while (!line.equals("Over"))
            {
                line = in.readUTF();
                System.out.println(line);
            }
        }
    }
}
