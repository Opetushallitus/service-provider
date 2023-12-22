package fi.vm.sade.saml;

import org.apache.log4j.Logger;
import org.opensaml.saml2.encryption.Decrypter;
import org.opensaml.saml2.encryption.EncryptedElementTypeEncryptedKeyResolver;
import org.opensaml.xml.encryption.ChainingEncryptedKeyResolver;
import org.opensaml.xml.encryption.InlineEncryptedKeyResolver;
import org.opensaml.xml.encryption.SimpleRetrievalMethodEncryptedKeyResolver;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.keyinfo.KeyInfoCredentialResolver;
import org.opensaml.xml.security.keyinfo.StaticKeyInfoCredentialResolver;
import org.opensaml.xml.security.x509.BasicX509Credential;
import org.springframework.security.saml.context.SAMLContextProviderLB;
import org.springframework.security.saml.context.SAMLMessageContext;

import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SAMLContextProvider extends SAMLContextProviderLB {
    private final Logger log = Logger.getLogger(getClass());
    private List<String> certificates;

    public void setCertificates(List<String> certificates) {
        this.certificates = certificates;
    }

    private static final ChainingEncryptedKeyResolver encryptedKeyResolver = new ChainingEncryptedKeyResolver();

    static {
        encryptedKeyResolver.getResolverChain().add(new InlineEncryptedKeyResolver());
        encryptedKeyResolver.getResolverChain().add(new EncryptedElementTypeEncryptedKeyResolver());
        encryptedKeyResolver.getResolverChain().add(new SimpleRetrievalMethodEncryptedKeyResolver());
    }
    protected void populateDecrypter(SAMLMessageContext samlContext) {
        // Instead of single credentials we also add the secondary key for the StaticKeyInfoCredentialResolver
        // This is the only difference from SAMLContextProviderLB and SAMLContextProviderImpl
        List<Credential> encryptionCredentials = validCredentials();

        // Locate encryption key for this entity
        if (samlContext.getLocalExtendedMetadata().getEncryptionKey() != null) {
            encryptionCredentials.add(keyManager.getCredential(samlContext.getLocalExtendedMetadata().getEncryptionKey()));
        } else {
            encryptionCredentials.add(keyManager.getDefaultCredential());
        }

        // Entity used for decrypting of encrypted XML parts
        // Extracts EncryptedKey from the encrypted XML using the encryptedKeyResolver and attempts to decrypt it
        // using private keys supplied by the resolver.
        KeyInfoCredentialResolver resolver = new StaticKeyInfoCredentialResolver(encryptionCredentials);

        Decrypter decrypter = new Decrypter(null, resolver, encryptedKeyResolver);
        decrypter.setRootInNewDocument(true);

        samlContext.setLocalDecrypter(decrypter);
    }

    private List<Credential> validCredentials() {
        List<Credential> credentials = this.certificates.stream()
                .flatMap(this::getCredential)
                .collect(Collectors.toList());
        if (credentials.isEmpty()) {
            log.error("No valid credentials found for decryption, check certificate validity");
        }
        return credentials;
    }

    private Stream<Credential> getCredential(String keyName) {
        Credential c = this.keyManager.getCredential(keyName);
        if (c == null) {
            log.error(String.format("Credential not found for key name: %s", keyName));
            return Stream.empty();
        }

        if (!isValidCredential(c)) {
            log.error("Skipping key " + keyName + " because it is not valid");
            return Stream.empty();
        }

        return Stream.of(c);
    }

    private boolean isValidCredential(Credential c) {
        if (c instanceof BasicX509Credential) {
            return isValidCert(((BasicX509Credential) c).getEntityCertificate());
        }
        log.error(String.format("Unsupported credential type: %s, %s, expected X509Certificate", c.getClass(), c.getPublicKey().getClass().getName()));
        return false;
    }

    private boolean isValidCert(X509Certificate cert) {
        try {
            cert.checkValidity();
            return true;
        } catch (CertificateNotYetValidException | CertificateExpiredException e) {
            return false;
        }
    }
}