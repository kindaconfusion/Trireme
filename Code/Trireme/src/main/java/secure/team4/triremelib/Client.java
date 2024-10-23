package secure.team4.triremelib;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Client extends Thread {
    private final String host;
    private final int hostPort;
    private final String filePath;

    public Client(String hostName, int port, String file) {
        host = hostName;
        hostPort = port;
        filePath = file;
    }

    public void run() {
        int PACKET_SIZE = 1000;
        byte[] contentBuf = new byte[PACKET_SIZE];
        Path path = Path.of(filePath);
        System.out.println("Client running");
        File file = new File(filePath);
        try (
                InputStream fis = new FileInputStream(file);
                //RandomAccessFile file = new RandomAccessFile(new File(filePath), "r"); // read in file
                Socket socket = new Socket(host, hostPort); // open socket
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(
                        socket.getOutputStream());
                DigestOutputStream digest = new DigestOutputStream(out, MessageDigest.getInstance("SHA-512"))) {
            String fileName = String.valueOf(path.getFileName());
            out.writeUTF(fileName);
            out.writeLong(file.length());


            digest.on(true);
            int count;
            while ((count = fis.read(contentBuf)) > 0) {
                digest.write(contentBuf, 0, count);
                in.readUTF();
            }
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest.getMessageDigest().digest()) {
                // Convert each byte to a 2-digit hex string and append it to the builder
                hexString.append(String.format("%02x", b));
            }
            out.writeUTF(hexString.toString());
            System.out.println(hexString);
            in.readUTF();
        } catch (ConnectException e) {
            System.out.println("Error: Failed to connect to the server (is it running?)");
        } catch (SocketException e) {
            // When the server closes the socket, exit
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
