package secure.team4.triremelib;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Client extends Thread {
    private final int PACKET_SIZE = 1000;
    private final String host;
    private final int hostPort;
    private final String filePath;
    public Client(String hostName, int port, String file) {
        host = hostName;
        hostPort = port;
        filePath = file;
    }
    public void run() {
        byte[] contentBuf = new byte[PACKET_SIZE];
        Path path = Path.of(filePath);
        System.out.println("Client running");
        try (
                RandomAccessFile file = new RandomAccessFile(new File(filePath), "r"); // read in file
                Socket socket = new Socket(host, hostPort); // open socket
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(
                        socket.getOutputStream());
        DigestOutputStream digest = new DigestOutputStream(out, MessageDigest.getInstance("SHA-256")))
        {
                out.writeLong(file.length());
                long remaining = file.length();
                String fileName = String.valueOf(path.getFileName());
                out.writeUTF(fileName);
                digest.on(true);
            while(file.length() > file.getFilePointer()) {
                Arrays.fill(contentBuf, (byte) 0);
                file.read(contentBuf);
                digest.write(contentBuf, 0, (int) Math.min(PACKET_SIZE, remaining));
                // acknowledge OK signal
                in.readNBytes(2);
                remaining -= PACKET_SIZE;
            }

            StringBuilder hexString = new StringBuilder();
            for (byte b : digest.getMessageDigest().digest()) {
                // Convert each byte to a 2-digit hex string and append it to the builder
                hexString.append(String.format("%02x", b));
            }
            out.writeUTF(hexString.toString());

            // TODO Checksumming currently disabled
        } catch (ConnectException e) {
            System.out.println("Error: Failed to connect to the server (is it running?)");
        } catch (SocketException e) {
            // When the server closes the socket, exit
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
