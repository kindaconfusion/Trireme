package secure.team4;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class Main {

    public static void main(String[] args) throws IOException {
        if (args[0].equals("send")) {
            send("127.0.0.1", 15420);
        } else {
            receive();
        }
    }
    public static void send(String hostName, int portNumber) throws IOException {
        try (
            Socket sendSocket = new Socket(hostName, portNumber);
            DataOutputStream out = new DataOutputStream(
                sendSocket.getOutputStream())) {
            out.writeUTF("test");
            out.writeUTF("Over");
        }
    }
    public static void receive() throws IOException {
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