package secure.team4;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;


public class Main {

    public static void main(String[] args) throws IOException {
        if (args[0].equals("send")) {
            new Client("127.0.0.1", 15420, args[1]);
        } else {
            new Server(15420);
        }
    }
}