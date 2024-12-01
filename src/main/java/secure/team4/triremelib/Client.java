package secure.team4.triremelib;

import javafx.application.Platform;
import javafx.scene.control.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;

import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;

public class Client extends Thread {
    private final String host;
    private final int hostPort;
    private final String filePath;

    // Paths to keystore and truststore
    private final String keystorePath = "keystore.p12";
    private final String truststorePath = "truststore.p12";

    public Client(String hostName, int port, String file) {
        host = hostName;
        hostPort = port;
        filePath = file;
    }

    @Override
    public void run() {
        Security.addProvider(new BouncyCastleProvider());
        int PACKET_SIZE = 4096;
        byte[] contentBuf = new byte[PACKET_SIZE];
        File file = new File(filePath);
        System.out.println("Client running");

        // 2GB file size limit
        if (file.length() > (2L * 1024 * 1024 * 1024)) {
            showAlert(Alert.AlertType.ERROR, "File Too Large", "File size exceeds 2GB limit.");
            return;
        }

        try {
            // Initialize SSL Socket
            SSLSocket socket = createSSLSocket(host, hostPort);

            try (InputStream fis = new FileInputStream(file);
                 DataInputStream in = new DataInputStream(socket.getInputStream());
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                 DigestOutputStream digest = new DigestOutputStream(out, MessageDigest.getInstance("SHA-512"))) {

                String fileName = file.getName();

                // Send file name to server
                out.writeUTF(fileName);

                // Send file length to server
                out.writeLong(file.length());

                digest.on(true);
                int count;

                // Stream file over socket
                while ((count = fis.read(contentBuf)) > 0) {
                    digest.write(contentBuf, 0, count);

                    // Acknowledge packet reception
                    String response = in.readUTF();
                    if (!response.equals("OK")) {
                        System.out.println("Error: Server response not OK.");
                        break;
                    }
                }

                // Send file hash
                StringBuilder hexString = new StringBuilder();
                for (byte b : digest.getMessageDigest().digest()) {
                    hexString.append(String.format("%02x", b));
                }
                out.writeUTF(hexString.toString());
                System.out.println("File hash sent: " + hexString);

                // Read final response from server
                String finalResponse = in.readUTF();
                System.out.println("Server response: " + finalResponse);

                showAlert(Alert.AlertType.INFORMATION, "Transfer Complete", finalResponse);

            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Connection Error", "An error occurred: " + e.getMessage());
        }
    }

    /**
     * Creates an SSLSocket with mutual SSL configuration.
     */
    private SSLSocket createSSLSocket(String host, int port) throws IOException, GeneralSecurityException, OperatorCreationException {
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

        // Create SSLSocketFactory
        SSLSocketFactory ssf = sslContext.getSocketFactory();

        // Create and return the SSLSocket
        SSLSocket socket = (SSLSocket) ssf.createSocket(host, port);

        // Force handshake to initiate SSL
        socket.startHandshake();

        return socket;
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
     * Exports the user's certificate to a file.
     */
    public void exportCertificate(File exportFile) throws Exception {
        System.out.println("Exporting certificate");
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        File ksFile = new File(keystorePath);
        if (!ksFile.exists()) {
            // Generate key and certificate
            generateKeyAndCertificate();
        }
        try (InputStream ksIs = new FileInputStream(keystorePath)) {
            keyStore.load(ksIs, null);
        }

        Certificate cert = keyStore.getCertificate("userkey");

        try (FileOutputStream fos = new FileOutputStream(exportFile)) {
            fos.write(cert.getEncoded());
        }

        showAlert(Alert.AlertType.INFORMATION, "Export Successful", "Certificate exported successfully.");
    }

    /**
     * Imports a peer's certificate into the truststore.
     */
    public void importCertificate(File importFile, String alias) throws Exception {
        System.out.println("Importing certificate");
        // Load or initialize the truststore
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        File tsFile = new File(truststorePath);
        if (tsFile.exists()) {
            try (InputStream tsIs = new FileInputStream(truststorePath)) {
                trustStore.load(tsIs, null);
            } catch (Exception e) {
                trustStore.load(null, null); // Initialize empty if loading fails
            }
        } else {
            trustStore.load(null, null); // Initialize new truststore
        }

        // Load the certificate to import
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert;
        try (FileInputStream fis = new FileInputStream(importFile)) {
            cert = (X509Certificate) cf.generateCertificate(fis);
        }

        // Add the new certificate to the truststore
        trustStore.setCertificateEntry(alias, cert);

        // Save the updated truststore
        try (FileOutputStream fos = new FileOutputStream(truststorePath)) {
            trustStore.store(fos, null);
        }

        showAlert(Alert.AlertType.INFORMATION, "Import Successful", "Certificate imported successfully.");
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
}