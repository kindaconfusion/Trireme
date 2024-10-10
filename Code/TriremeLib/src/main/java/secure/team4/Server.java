package secure.team4;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Server {
    private final int PACKET_SIZE = 1000;

    public Server(int port) throws IOException {
        int fileSize = 0;
        int index = 0;
        try (ServerSocket server = new ServerSocket(port);
             Socket clientSocket = server.accept(); // open socket
             DataInputStream in = new DataInputStream(
                     new BufferedInputStream(clientSocket.getInputStream()));
             DataOutputStream out = new DataOutputStream(
                     clientSocket.getOutputStream());
             FileOutputStream fileOut = new FileOutputStream("output");
        )
        {
            {
                byte[] packet = new byte[PACKET_SIZE];
                Arrays.fill(packet, (byte) 0x00);
                fileSize = in.readInt();
                while (true) {
                    packet = in.readNBytes(Math.min(fileSize - index, PACKET_SIZE)); // read in a packet
                    out.writeUTF("OK"); // packet received successfully
                    fileOut.write(packet);
                    if (fileSize - index < PACKET_SIZE) {
                        clientSocket.close();
                        break;
                    }
                    index += PACKET_SIZE;
                }
            }
        } catch (SocketException e) {
            // When the client closes the socket, exit
        }
    }
}
