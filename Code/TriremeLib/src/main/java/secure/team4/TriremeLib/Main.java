package secure.team4.TriremeLib;

import java.io.*;
import java.security.NoSuchAlgorithmException;


public class Main {

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        if (args[0].equals("send")) {
            // send [hostname] [port] [filepath]
            new Client(args[1], Integer.parseInt(args[2]), args[3]);
        } else if (args[0].equals("receive")) {
            // receive [port]
            new Server(Integer.parseInt(args[1]));
        }
    }
}