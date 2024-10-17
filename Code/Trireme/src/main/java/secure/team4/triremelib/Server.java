package secure.team4.triremelib;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Server extends Thread {
    private final int PACKET_SIZE = 1000;
    private final int port;
    public Server(int p) {
        port = p;
    }
    public void run() {
        long fileSize = 0;
        int index = 0;
        try (ServerSocket server = new ServerSocket(port);
             Socket clientSocket = server.accept(); // open socket
             DataInputStream in = new DataInputStream(
                     new BufferedInputStream(clientSocket.getInputStream()));
             DataOutputStream out = new DataOutputStream(
                     clientSocket.getOutputStream());
        )
        {
            {
                byte[] packet = new byte[PACKET_SIZE];
                Arrays.fill(packet, (byte) 0x00);
                fileSize = in.readLong();
                //String hash = new String(in.readNBytes(32), StandardCharsets.UTF_8);
                String name = in.readUTF();
                FileOutputStream fileOut = new FileOutputStream(name);
                while (true) {
                    packet = in.readNBytes((int) Math.min(fileSize - index, PACKET_SIZE)); // read in a packet
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
