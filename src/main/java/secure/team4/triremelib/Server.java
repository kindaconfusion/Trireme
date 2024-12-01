package secure.team4.triremelib;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;

import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

public class Server extends Thread {
    private final int port;
    private volatile boolean isRunning = false;
    private SSLServerSocket serverSocket;

    // Paths to keystore and truststore
    private final String keystorePath = "keystore.p12";
    private final String truststorePath = "truststore.p12";

    public Server(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        Security.addProvider(new BouncyCastleProvider());
        System.out.println("Server running on port " + port);
        isRunning = true;

        try {
            // Initialize SSL Server Socket
            serverSocket = createSSLServerSocket(port);

            while (isRunning) {
                try {
                    System.out.println("Waiting for a connection...");
                    SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                    System.out.println("Client connected: " + clientSocket.getInetAddress());

                    // Start handshake to ensure mutual SSL
                    clientSocket.startHandshake();

                    // Handle the client connection in a separate thread
                    new Thread(() -> handleClientConnection(clientSocket)).start();
                } catch (SSLHandshakeException e) {
                    System.err.println("SSL Handshake failed: " + e.getMessage());
                    showAlert(Alert.AlertType.ERROR, "SSL Handshake Failed", "Failed to establish a secure connection.");
                } catch (IOException e) {
                    if (!isRunning) {
                        System.out.println("Server stopped listening on port " + port);
                        break;
                    }
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Connection Error", "An error occurred: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Server Error", "Failed to start the server: " + e.getMessage());
        } finally {
            closeServerSocket();
        }
    }

    /**
     * Creates an SSLServerSocket with mutual SSL configuration.
     */
    private SSLServerSocket createSSLServerSocket(int port) throws Exception {
        // Load the keystore
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        File ksFile = new File(keystorePath);
        if (ksFile.exists()) {
            try (InputStream ksIs = new FileInputStream(keystorePath)) {
                keyStore.load(ksIs, null);
            }
        } else {
            // Generate key and certificate
            generateKeyAndCertificate();
            try (InputStream ksIs = new FileInputStream(keystorePath)) {
                keyStore.load(ksIs, null);
            }
        }

        // Initialize KeyManagerFactory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keyStore, null);

        // Load the truststore
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        File tsFile = new File(truststorePath);
        if (tsFile.exists()) {
            try (InputStream tsIs = new FileInputStream(truststorePath)) {
                trustStore.load(tsIs, null);
            }
        } else {
            trustStore.load(null, null);
        }

        // Initialize TrustManagerFactory
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(trustStore);

        // Initialize SSLContext with KeyManagers and TrustManagers
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

        // Create SSLServerSocketFactory
        SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();

        // Create and return the SSLServerSocket
        SSLServerSocket serverSocket = (SSLServerSocket) ssf.createServerSocket(port);

        // Require client authentication
        serverSocket.setNeedClientAuth(true);

        return serverSocket;
    }

    /**
     * Generates the user's key pair and self-signed certificate.
     */
    private void generateKeyAndCertificate() throws GeneralSecurityException, IOException, OperatorCreationException {
        // Generate self-signed certificate for the user
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        // Generate certificate
        X509Certificate certificate = SelfSignedCertificateGenerator.generateSelfSignedCertificate(keyPair, "CN=Trireme User");

        // Create Keystore
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry("userkey", keyPair.getPrivate(), null, new Certificate[]{certificate});
        try (FileOutputStream fos = new FileOutputStream(keystorePath)) {
            keyStore.store(fos, null);
        }
    }

    /**
     * Handles an individual client connection.
     */
    private void handleClientConnection(SSLSocket clientSocket) {
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(clientSocket.getInputStream()));
             DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {

            // Read the file name from the client
            String fileName = in.readUTF();
            System.out.println("File Name: " + fileName);

            // Read the file size
            long fileSize = in.readLong();

            // 2GB file size limit
            if (fileSize > (2L * 1024 * 1024 * 1024)) {
                out.writeUTF("File size exceeds 2GB limit. Transfer rejected.");
                System.out.println("File size exceeds 2GB limit. Transfer rejected.");
                clientSocket.close();
                return;
            }

            // Prompt user to accept or reject the file and choose save location
            File saveDirectory = showTransferRequestDialog(fileName);

            if (saveDirectory != null) {
                // Receive and save the file
                receiveFile(in, out, fileName, fileSize, saveDirectory);
                System.out.println("File received successfully.");
            } else {
                // Notify the client that the transfer was rejected
                out.writeUTF("Transfer rejected by the receiver.");
                clientSocket.close();
            }

        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Connection Error", "An error occurred: " + e.getMessage());
        }
    }

    /**
     * Displays a dialog to the user to accept or reject the incoming file transfer and choose save location.
     */
    private File showTransferRequestDialog(String fileName) {
        final CountDownLatch latch = new CountDownLatch(1);
        final File[] result = new File[1];

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Transfer Request");
            alert.setHeaderText("File Incoming");
            alert.setContentText("Someone is trying to send \"" + fileName + "\". Would you like to accept?");
            Optional<ButtonType> response = alert.showAndWait();

            if (response.isPresent() && response.get() == ButtonType.OK) {
                // Show DirectoryChooser
                DirectoryChooser directoryChooser = new DirectoryChooser();
                directoryChooser.setTitle("Select Save Location");
                File selectedDir = directoryChooser.showDialog(null);
                if (selectedDir != null) {
                    result[0] = selectedDir;
                } else {
                    // User canceled the directory selection
                    showAlert(Alert.AlertType.WARNING, "No Directory Selected",
                            "File transfer was accepted but no directory was selected. Transfer canceled.");
                }
            }
            latch.countDown();
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }

        return result[0];
    }

    /**
     * Receives the file from the client and saves it to the specified directory.
     */
    private void receiveFile(DataInputStream in, DataOutputStream out, String fileName, long fileSize, File saveDir)
            throws IOException, NoSuchAlgorithmException {

        final int PACKET_SIZE = 4096;
        long remaining = fileSize;

        // Sanitize the file name to prevent path traversal attacks
        String safeFileName = new File(fileName).getName();

        // Ensure the file does not overwrite an existing file
        File outputFile = new File(saveDir, safeFileName);
        if (outputFile.exists()) {
            // Prepends timestamp to the file name to prevent overwriting if it already exists
            outputFile = new File(saveDir, System.currentTimeMillis() + "_" + safeFileName);
        }

        // Open file output stream
        try (FileOutputStream fileOut = new FileOutputStream(outputFile);
             DigestOutputStream digestOut = new DigestOutputStream(fileOut, MessageDigest.getInstance("SHA-512"))) {

            byte[] buffer = new byte[PACKET_SIZE];
            // Initialize progress tracking
            while (remaining > 0) {
                int bytesRead = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (bytesRead == -1) {
                    throw new EOFException("Unexpected end of stream");
                }
                digestOut.write(buffer, 0, bytesRead);
                remaining -= bytesRead;

                // Acknowledge packet reception
                out.writeUTF("OK");
            }

            // Finalize the digest
            byte[] fileDigest = digestOut.getMessageDigest().digest();

            // Read the hash sent by the client
            String clientHash = in.readUTF();

            // Convert the file digest to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : fileDigest) {
                hexString.append(String.format("%02x", b));
            }
            String calculatedHash = hexString.toString();

            // Compare hashes
            if (calculatedHash.equals(clientHash)) {
                out.writeUTF("File received and verified successfully.");
                System.out.println("File checksum verified.");
            } else {
                out.writeUTF("File received but checksum does not match.");
                System.out.println("Warning: File checksum does not match. The file may be corrupt or tampered with.");
            }
        }
    }

    /**
     * Displays an alert to the user.
     */
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType, message, ButtonType.OK);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.showAndWait();
        });
    }

    /**
     * Properly closes the server socket.
     */
    private void closeServerSocket() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                System.out.println("Server socket closed.");
            } catch (IOException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Server Error", "Failed to close the server socket: " + e.getMessage());
            }
        }
    }

    /**
     * Stops the server by closing the server socket and interrupting the thread.
     */
    public void closeServer() {
        isRunning = false;
        closeServerSocket();
        this.interrupt();
        System.out.println("Server has been stopped.");
    }
}