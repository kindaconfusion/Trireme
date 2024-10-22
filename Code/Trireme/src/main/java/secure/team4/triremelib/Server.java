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
    public String hash = null;
    public Server(int p) {
        port = p;
    }
    public void run() {
        System.out.println("Server running");
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
                long remaining = fileSize;
                String inHash = "";
                String name = in.readUTF();
                FileOutputStream fileOut = new FileOutputStream(name);
                while (true) {
                    if (remaining < 0) {
                        inHash = in.readUTF();
                        System.out.println(inHash);
                        MessageDigest digest = MessageDigest.getInstance("SHA-256");
                        byte[] byteshash = digest.digest(Files.readAllBytes(Path.of(name))); // TODO cannot process files >2gb
                        StringBuilder hexString = new StringBuilder();
                        for (byte b : byteshash) {
                            // Convert each byte to a 2-digit hex string and append it to the builder
                            hexString.append(String.format("%02x", b));
                        }
                        if (!hexString.toString().equals(inHash)) {
                            System.out.println("Warning! File checksum does not match original file. File may be corrupt or tampered with.");
                        }
                        System.out.println(hexString.toString());
                        out.writeUTF("OK");
                        clientSocket.close();
                        break;
                    }
                    packet = in.readNBytes((int) Math.min(remaining, PACKET_SIZE)); // read in a packet
                    remaining -= PACKET_SIZE;
                    out.writeUTF("OK"); // packet received successfully
                    fileOut.write(packet);
                }

            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
