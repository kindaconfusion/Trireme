package secure.team4;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

public class Client {
    private final int PACKET_SIZE = 8;
    public Client(String hostName, int port, String file) throws IOException {
        int fileIndex = 0;
        // TODO files are limited to 2 GiB
        byte[] fileContent = Files.readAllBytes(Path.of(file));
        try (
                Socket socket = new Socket(hostName, port);
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(
                        socket.getOutputStream())) {
            while(fileContent.length > fileIndex) {
                if (fileContent.length > fileIndex + PACKET_SIZE) {
                    out.write(fileContent, fileIndex, PACKET_SIZE);
                }
                else {
                    byte[] finalContent = new byte[PACKET_SIZE];
                    for (int i = 0; i < PACKET_SIZE-1; i++) {
                        finalContent[i] = (fileIndex + i < fileContent.length) ? fileContent[fileIndex+i] : 0;
                    }
                    finalContent[7] = 0x06;
                    out.write(finalContent, 0, PACKET_SIZE);
                }
                in.readNBytes(2);
                fileIndex = fileIndex + 8;
            }
            //out.write(new byte[]{0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
