package secure.team4.triremelib;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Server extends Thread {
    private final int port;
    public Server(int p) {
        port = p;
    }
    public void run() {
        int PACKET_SIZE = 1000;
        System.out.println("Server running");
        long fileSize;
        try (ServerSocket server = new ServerSocket(port);
             Socket clientSocket = server.accept(); // open socket
             DataInputStream in = new DataInputStream(
                     new BufferedInputStream(clientSocket.getInputStream()));
             DataOutputStream out = new DataOutputStream(
                     clientSocket.getOutputStream())
        ) {
            {
                byte[] packet = new byte[PACKET_SIZE];
                Arrays.fill(packet, (byte) 0x00);
                fileSize = in.readLong();
                long remaining = fileSize;
                String inHash;
                String name = in.readUTF();
                FileOutputStream fileOut = new FileOutputStream(name);
                DigestOutputStream digest = new DigestOutputStream(fileOut, MessageDigest.getInstance("SHA-512"));
                while (true) {
                    if (remaining < 0) {
                        digest.close();
                        inHash = in.readUTF();
                        System.out.println(inHash);
                        StringBuilder hexString = new StringBuilder();
                        for (byte b : digest.getMessageDigest().digest()) {
                            // Convert each byte to a 2-digit hex string and append it to the builder
                            hexString.append(String.format("%02x", b));
                        }
                        if (!hexString.toString().equals(inHash)) {
                            System.out.println("Warning! File checksum does not match original file. File may be corrupt or tampered with.");
                        }
                        System.out.println(hexString);
                        out.writeUTF("OK");
                        clientSocket.close();
                        break;
                    }
                    packet = in.readNBytes((int) Math.min(remaining, PACKET_SIZE)); // read in a packet
                    remaining -= PACKET_SIZE;
                    out.writeUTF("OK"); // packet received successfully
                    digest.write(packet);
                }

            }
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
