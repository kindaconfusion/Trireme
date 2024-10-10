package secure.team4;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Server {
    private final int PACKET_SIZE = 8;
    public Server(int port) throws IOException {
        try(ServerSocket server = new ServerSocket(port);
            Socket clientSocket = server.accept(); // open socket
            DataInputStream in = new DataInputStream(
                    new BufferedInputStream(clientSocket.getInputStream()))) {
            DataOutputStream out = new DataOutputStream(
                    clientSocket.getOutputStream()); {
            byte[] packet = new byte[PACKET_SIZE];
            Arrays.fill(packet, (byte) 0x00);
            while (true)
            {
                packet = in.readNBytes(PACKET_SIZE); // read in a packet
                out.writeUTF("OK"); // packet received successfully
                System.out.print(new String(packet, StandardCharsets.US_ASCII));
                if (packet[7] == 0x06) break; // break at EOF
            }
        }
    }
}
}
