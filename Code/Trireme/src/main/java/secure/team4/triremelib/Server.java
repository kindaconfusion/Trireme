package secure.team4.triremelib;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class Server extends Thread {
    private final int port;
    public SimpleBooleanProperty received = new SimpleBooleanProperty(false);
    public Server(int p) {
        port = p;
    }
    public void run() {
        System.out.println("Server running");
        try (ServerSocket server = new ServerSocket(port);
             Socket clientSocket = server.accept(); // open socket
             DataInputStream in = new DataInputStream(
                     new BufferedInputStream(clientSocket.getInputStream()))
        ) {
            String fileName = in.readUTF();
            received.set(true);
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicInteger dialogResult = new AtomicInteger(0);
            Platform.runLater(() -> {
                Dialog<Boolean> dialog = new Dialog<>();
                dialog.setTitle("Transfer Request");
                ButtonType yesBtn = new ButtonType("Yes", ButtonBar.ButtonData.YES);
                dialog.getDialogPane().getButtonTypes().addAll(yesBtn, ButtonType.NO);
                dialog.setHeaderText("File Incoming");
                dialog.setContentText("Somebody is trying to send \"" + fileName + "\". Would you like to accept?");
                dialog.setResultConverter(dialogButton -> {
                    if (dialogButton == yesBtn) {
                        return true;
                    }
                    return null;
                });
                Optional<Boolean> result = dialog.showAndWait();
                if (result.isPresent() && result.get()) {
                    System.out.println("yes");
                    dialogResult.set(1);
                } else {
                    dialogResult.set(2);
                }
                latch.countDown();
            });
            latch.await();
            if (dialogResult.get() == 1) {
                accept(clientSocket);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void accept(Socket socket) {
        int PACKET_SIZE = 1000;
        try {
            DataInputStream in = new DataInputStream(
                    new BufferedInputStream(socket.getInputStream()));
            DataOutputStream out = new DataOutputStream(
                    socket.getOutputStream());
                long fileSize;
                byte[] packet = new byte[1000];
                Arrays.fill(packet, (byte) 0x00);
                fileSize = in.readLong();
                long remaining = fileSize;
                String inHash;
                String name = in.readUTF();
                FileOutputStream fileOut = new FileOutputStream(name);
                DigestOutputStream digest = new DigestOutputStream(fileOut, MessageDigest.getInstance("SHA-512"));
                while (true) {
                    if (remaining < 0) {
                        digest.close();
                        inHash = in.readUTF();
                        System.out.println(inHash);
                        StringBuilder hexString = new StringBuilder();
                        for (byte b : digest.getMessageDigest().digest()) {
                            // Convert each byte to a 2-digit hex string and append it to the builder
                            hexString.append(String.format("%02x", b));
                        }
                        if (!hexString.toString().equals(inHash)) {
                            System.out.println("Warning! File checksum does not match original file. File may be corrupt or tampered with.");
                        }
                        System.out.println(hexString);
                        out.writeUTF("OK");
                        socket.close();
                        break;
                    }
                    packet = in.readNBytes((int) Math.min(remaining, PACKET_SIZE)); // read in a packet
                    remaining -= PACKET_SIZE;
                    out.writeUTF("OK"); // packet received successfully
                    digest.write(packet);
                }
            } catch (IOException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

}
