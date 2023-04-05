package fi.vm.sade.saml;

import org.opensaml.saml2.encryption.Decrypter;
import org.opensaml.saml2.encryption.EncryptedElementTypeEncryptedKeyResolver;
import org.opensaml.xml.encryption.ChainingEncryptedKeyResolver;
import org.opensaml.xml.encryption.InlineEncryptedKeyResolver;
import org.opensaml.xml.encryption.SimpleRetrievalMethodEncryptedKeyResolver;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.keyinfo.KeyInfoCredentialResolver;
import org.opensaml.xml.security.keyinfo.StaticKeyInfoCredentialResolver;
import org.springframework.security.saml.context.SAMLContextProviderLB;
import org.springframework.security.saml.context.SAMLMessageContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SAMLContextProvider extends SAMLContextProviderLB {
    private List<String> certificates;
    public void setCertificates(List<String> certificates) { this.certificates = certificates; }

    private static final ChainingEncryptedKeyResolver encryptedKeyResolver = new ChainingEncryptedKeyResolver();
    static {
        encryptedKeyResolver.getResolverChain().add(new InlineEncryptedKeyResolver());
        encryptedKeyResolver.getResolverChain().add(new EncryptedElementTypeEncryptedKeyResolver());
        encryptedKeyResolver.getResolverChain().add(new SimpleRetrievalMethodEncryptedKeyResolver());
    }
    protected void populateDecrypter(SAMLMessageContext samlContext) {
        // Instead of single credentials we also add the secondary key for the StaticKeyInfoCredentialResolver
        // This is the only difference from SAMLContextProviderLB and SAMLContextProviderImpl
        List<Credential> encryptionCredentials = this.certificates.stream()
                .map(this.keyManager::getCredential)
                .collect(Collectors.toList());

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
}