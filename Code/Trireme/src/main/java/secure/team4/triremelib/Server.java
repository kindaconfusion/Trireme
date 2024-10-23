/**
 * Server is the receiving end.
 * It waits for a connection from a Client,
 * then accepts the data and writes it to a file.
 */

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
    public SimpleBooleanProperty received = new SimpleBooleanProperty(false); // This is essentially unused
    public Server(int p) {
        port = p;
    }
    public void run() {
        System.out.println("Server running");
        try (
             // Open server for listening
             ServerSocket server = new ServerSocket(port);
             // Wait until connection is received
             Socket clientSocket = server.accept();
             DataInputStream in = new DataInputStream(
                     new BufferedInputStream(clientSocket.getInputStream()))
        ) {
            // Read filename from client
            String fileName = in.readUTF();
            received.set(true);
            // When we receive a connection attempt, open a dialog box to confirm.
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
            // Wait for dialog box.
            latch.await();
            // If the user clicks "Yes", accept the connection. Otherwise, exit.
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
            // Open in/out data pipes.
            DataInputStream in = new DataInputStream(
                    new BufferedInputStream(socket.getInputStream()));
            DataOutputStream out = new DataOutputStream(
                    socket.getOutputStream());
                long fileSize;
                byte[] packet = new byte[1000];
                Arrays.fill(packet, (byte) 0x00);
                // Read file size
                fileSize = in.readLong();
                long remaining = fileSize;
                String inHash;
                String name = in.readUTF();
                // Open file for writing
                FileOutputStream fileOut = new FileOutputStream(name);
                // Create SHA-512 hash as we stream file
                // DigestOutputStream is a wrapper around a DataOutputStream.
                // As we write data to file, update the MessageDigest.
                DigestOutputStream digest = new DigestOutputStream(fileOut, MessageDigest.getInstance("SHA-512"));
                while (true) {
                    // If we are at the end of file
                    if (remaining < 0) {
                        // Close stream
                        digest.close();
                        // Read hash from client
                        inHash = in.readUTF();
                        System.out.println(inHash);
                        // Convert hash to string
                        StringBuilder hexString = new StringBuilder();
                        for (byte b : digest.getMessageDigest().digest()) {
                            // Convert each byte to a 2-digit hex string and append it to the builder
                            hexString.append(String.format("%02x", b));
                        }
                        if (!hexString.toString().equals(inHash)) {
                            System.out.println("Warning! File checksum does not match original file. File may be corrupt or tampered with.");
                        }
                        System.out.println(hexString);
                        out.writeUTF("Done");
                        socket.close();
                        break;
                    }
                    // Read in a packet
                    packet = in.readNBytes((int) Math.min(remaining, PACKET_SIZE));
                    remaining -= PACKET_SIZE;
                    // Acknowledge packet reception
                    out.writeUTF("OK");
                    // Write data to file
                    digest.write(packet);
                }
            } catch (IOException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

}
