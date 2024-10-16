package secure.team4.triremelib;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
        int fileIndex = 0;
        // TODO files are limited to 2 GiB
        byte[] fileContent = null;
        byte[] hash = null;
        try {
            fileContent = Files.readAllBytes(Path.of(filePath)); // read in file
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            hash = digest.digest(fileContent);
        } catch (IOException e) {
            // do some error handling because the file doesn't exist
        } catch (NoSuchAlgorithmException e) {
            // fuck you
        }
        try (
                Socket socket = new Socket(host, hostPort); // open socket
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(
                        socket.getOutputStream())) {
            if (fileContent != null && hash != null) {
                out.writeInt(fileContent.length);
                out.write(hash);


            while(fileContent.length > fileIndex) {
                out.write(fileContent, fileIndex, Math.min(PACKET_SIZE, fileContent.length - fileIndex)); // write either PACKET_SIZE length packet or remaining bytes
                // acknowledge OK signal
                in.readNBytes(2);
                fileIndex = fileIndex + PACKET_SIZE;
            }
            }
        } catch (ConnectException e) {
            System.out.println("Error: Failed to connect to the server (is it running?)");
        } catch (SocketException e) {
            // When the server closes the socket, exit
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
