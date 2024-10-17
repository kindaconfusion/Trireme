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
        byte[] contentBuf = new byte[PACKET_SIZE];
        Path path = Path.of(filePath);
        try (
                RandomAccessFile file = new RandomAccessFile(new File(filePath), "r"); // read in file
                Socket socket = new Socket(host, hostPort); // open socket
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(
                        socket.getOutputStream())) {
                out.writeLong(file.length());
                String fileName = String.valueOf(path.getFileName());
                out.writeUTF(fileName);
            while(file.length() > file.getFilePointer()) {
                file.read(contentBuf); // write either PACKET_SIZE length packet or remaining bytes
                out.write(contentBuf);
                // acknowledge OK signal
                in.readNBytes(2);
                file.seek(1000);
            }
            // TODO Checksumming currently disabled
        } catch (ConnectException e) {
            System.out.println("Error: Failed to connect to the server (is it running?)");
        } catch (SocketException e) {
            // When the server closes the socket, exit
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
