package secure.team4;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Client {
    private final int PACKET_SIZE = 1000;
    public Client(String hostName, int port, String file) throws IOException, NoSuchAlgorithmException {
        int fileIndex = 0;
        // TODO files are limited to 2 GiB
        byte[] fileContent = Files.readAllBytes(Path.of(file)); // read in file
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(fileContent);
        try (
                Socket socket = new Socket(hostName, port); // open socket
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(
                        socket.getOutputStream())) {
            out.writeInt(fileContent.length);
            out.write(hash);
            while(fileContent.length > fileIndex) {
                out.write(fileContent, fileIndex, Math.min(PACKET_SIZE, fileContent.length - fileIndex)); // write full-size packet
                // acknowledge OK signal
                in.readNBytes(2);
                fileIndex = fileIndex + PACKET_SIZE;
            }
        } catch (SocketException e) {
            // When the server closes the socket, exit
        }
    }
}
