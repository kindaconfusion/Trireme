package secure.team4;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;


public class Main {

    public static void main(String[] args) throws IOException {
        if (args[0].equals("send")) {
            send("127.0.0.1", 15420, args[1]);
        } else {
            receive();
        }
    }
    public static void send(String hostName, int portNumber, String file) throws IOException {
        Scanner fileIn = new Scanner(new FileReader(file));
        try (
            Socket sendSocket = new Socket(hostName, portNumber);
            DataOutputStream out = new DataOutputStream(
                sendSocket.getOutputStream())) {
            while(fileIn.hasNext()) {
                out.writeUTF(fileIn.next());
            }
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