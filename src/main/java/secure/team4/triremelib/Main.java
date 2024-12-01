package secure.team4.triremelib;

public class Main {

    public static void main(String[] args) {
        if (args[0].equals("send")) {
            // send [hostname] [port] [filepath]
            System.out.println("Sending file to " + args[1] + " on port " + args[2]);
            Client c = new Client(args[1], Integer.parseInt(args[2]), args[3]);
            c.start();
        } else if (args[0].equals("receive")) {
            // receive [port]
            System.out.println("Starting server on port " + args[1]);
            Server s = new Server(Integer.parseInt(args[1]));
            s.start();
        }
    }
}