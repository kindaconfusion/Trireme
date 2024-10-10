package secure.team4;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {
    public Client(String hostName, int port, String file) throws FileNotFoundException {
        Scanner fileIn = new Scanner(new FileReader(file));
        try (
                Socket sendSocket = new Socket(hostName, port);
                DataOutputStream out = new DataOutputStream(
                        sendSocket.getOutputStream())) {
            while(fileIn.hasNext()) {
                out.writeUTF(fileIn.next());
            }
            out.writeUTF("Over");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
