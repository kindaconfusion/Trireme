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
        byte[] fileContent = Files.readAllBytes(Path.of(file)); // read in file
        try (
                Socket socket = new Socket(hostName, port); // open socket
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(
                        socket.getOutputStream())) {
            while(fileContent.length > fileIndex) {
                if (fileContent.length > fileIndex + PACKET_SIZE) {
                    out.write(fileContent, fileIndex, PACKET_SIZE); // write full-size packet
                }
                else {
                    byte[] finalContent = new byte[PACKET_SIZE];
                    // if we reach the end of file, create a full-size packet
                    // with EOF char at the end
                    // (there may be a better way to do this)
                    for (int i = 0; i < PACKET_SIZE-1; i++) {
                        finalContent[i] = (fileIndex + i < fileContent.length) ? fileContent[fileIndex+i] : 0;
                    }
                    finalContent[7] = 0x06;
                    out.write(finalContent, 0, PACKET_SIZE);
                }
                // acknowledge OK signal
                in.readNBytes(2);
                fileIndex = fileIndex + 8;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
