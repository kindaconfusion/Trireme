package secure.team4.triremelib;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.*;
import org.bouncycastle.cert.jcajce.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.*;
import org.bouncycastle.operator.jcajce.*;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Date;

public class SelfSignedCertificateGenerator {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Generates a self-signed X.509 certificate.
     */
    public static X509Certificate generateSelfSignedCertificate(KeyPair keyPair, String dn)
            throws GeneralSecurityException, OperatorCreationException, CertIOException {

        long now = System.currentTimeMillis();
        Date startDate = new Date(now);

        // Valid for 10 years
        Date endDate = new Date(now + 3650L * 24 * 60 * 60 * 1000L);

        // Serial number
        BigInteger serialNumber = BigInteger.valueOf(now);

        // Subject and Issuer DN
        X500Name issuer = new X500Name(dn);
        X500Name subject = new X500Name(dn);

        // Using BouncyCastle's builder
        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer, serialNumber, startDate, endDate, subject, keyPair.getPublic());

        // Add extensions
        // Key Usage
        certBuilder.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));

        // Extended Key Usage
        KeyPurposeId[] keyPurposes = new KeyPurposeId[]{
                KeyPurposeId.id_kp_serverAuth,
                KeyPurposeId.id_kp_clientAuth
        };
        ExtendedKeyUsage extendedKeyUsage = new ExtendedKeyUsage(keyPurposes);
        certBuilder.addExtension(Extension.extendedKeyUsage, false, extendedKeyUsage);

        // Basic Constraints (not a CA)
        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));

        // Content Signer
        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.getPrivate());

        // Build the certificate
        X509CertificateHolder certHolder = certBuilder.build(contentSigner);
        X509Certificate certificate = new JcaX509CertificateConverter()
                .setProvider("BC").getCertificate(certHolder);

        // Validate the certificate
        certificate.verify(keyPair.getPublic());

        return certificate;
    }
}