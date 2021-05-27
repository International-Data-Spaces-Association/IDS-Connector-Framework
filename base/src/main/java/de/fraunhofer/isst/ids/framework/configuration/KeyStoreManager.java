package de.fraunhofer.isst.ids.framework.configuration;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.IntStream;

import de.fraunhofer.iais.eis.ConfigurationModel;
import de.fraunhofer.isst.ids.framework.util.IDSUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

/**
 * The KeyStoreManager loads the IDSKeyStore and IDSTrustStore, provides the TrustManager
 * (for building OkHttpClients in {@link IDSUtils}) and the PrivateKey and Certificate of the Connector
 * (used in {@link de.fraunhofer.isst.ids.framework.daps.TokenManagerService}).
 *
 * The IDSKeyStore contains the Connectors PrivateKey and Certificate which are used to identify the Connector
 * in the IDS Context (e.g when requesting a DAT Token, see {@link de.fraunhofer.isst.ids.framework.daps.TokenManagerService})
 *
 * The IDSTrustStore contains the trusted certificates, which are used when creating an OkHttpClient using the {@link IDSUtils}
 */
@Slf4j
@Getter
public class KeyStoreManager {

    private ConfigurationModel configurationModel;
    private KeyStore keyStore;
    private char[] keyStorePw;
    private String keyAlias;
    private KeyStore trustStore;
    private char[] trustStorePw;
    private PrivateKey privateKey;
    private Certificate cert;
    private X509TrustManager trustManager;

    /**
     * Build the KeyStoreManager from the given configuration.
     *
     * @param configurationModel a ConfigurationModel
     * @param keystorePw the password for the IDSKeyStore
     * @param trustStorePw the password for the IDSTrustStore
     * @param keyAlias the alias of the IDS PrivateKey
     * @throws KeyStoreManagerInitializationException when the KeyStoreManager cannot be initialized
     */
    public KeyStoreManager(final ConfigurationModel configurationModel,
                           final char[] keystorePw,
                           final char[] trustStorePw,
                           final String keyAlias) throws KeyStoreManagerInitializationException {
        if (log.isDebugEnabled()) {
            log.debug("Initializing KeyStoreManager");
        }

        try {
            this.configurationModel = configurationModel;
            this.keyStorePw = keystorePw;
            this.trustStorePw = trustStorePw;
            this.keyAlias = keyAlias;
            //create the KeyStore (used for holding the PrivateKey for the DAPS)
            keyStore = loadKeyStore(keystorePw, configurationModel.getKeyStore());
            //create the TrustStore (used as TrustManager for building an OkHTTPClient)
            trustStore = loadKeyStore(trustStorePw, configurationModel.getTrustStore());
            final var myManager = loadTrustManager(trustStorePw);
            trustManager = configureTrustStore(myManager);
            getPrivateKeyFromKeyStore(keyAlias);
        } catch (IOException e) {
            if (log.isErrorEnabled()) {
                log.error("Key- or Truststore could not be loaded!");
            }
            throw new KeyStoreManagerInitializationException(e.getMessage(), e.getCause());
        } catch (CertificateException e) {
            if (log.isErrorEnabled()) {
                log.error("Error while loading a Certificate!");
                log.error(e.getMessage(), e);
            }
            throw new KeyStoreManagerInitializationException(e.getMessage(), e.getCause());
        } catch (UnrecoverableKeyException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not initialize Key/Truststore: password is incorrect!");
            }
            throw new KeyStoreManagerInitializationException(e.getMessage(), e.getCause());
        } catch (NoSuchAlgorithmException e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
            throw new KeyStoreManagerInitializationException(e.getMessage(), e.getCause());
        } catch (KeyStoreException e) {
            if (log.isErrorEnabled()) {
                log.error("Initialization of Key- or Truststore failed!");
                log.error(e.getMessage(), e);
            }
            throw new KeyStoreManagerInitializationException(e.getMessage(), e.getCause());
        }
    }

    /**
     * Load a KeyStore from the given location and open it with the given password.
     * Try to find it inside the jar first, if nothing is found there, try the path at system scope
     *
     * @param pw password of the keystore
     * @param location path of the keystore
     * @return the IdsKeyStore as java keystore instance
     * @throws CertificateException if any of the certificates in the keystore could not be loaded
     * @throws NoSuchAlgorithmException if the algorithm used to check the integrity of the keystore cannot be found
     * @throws IOException when the Key-/Truststore File cannot be found
     */
    private KeyStore loadKeyStore(final char[] pw, final URI location)
            throws CertificateException, NoSuchAlgorithmException, IOException {
        if (log.isInfoEnabled()) {
            log.info(String.format("Searching for keystore file %s", location.toString()));
        }
        KeyStore store;
        try {
            store = KeyStore.getInstance(KeyStore.getDefaultType());
        } catch (KeyStoreException e) {
            if (log.isErrorEnabled()) {
                log.error("Could not create a KeyStore with default type!");
                log.error(e.getMessage(), e);
            }
            return null;
        }
        final var path = Paths.get(location);
        var pathString = path.toString();
        //remove leading /, \ and . from path
        pathString = pathString.chars().dropWhile(value -> IntStream.of('\\', '/', '.').anyMatch(v -> v == value))
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        if (log.isInfoEnabled()) {
            log.info("Path: " + pathString);
        }

        final var keyStoreOnClassPath = new ClassPathResource(pathString).exists();

        if (!keyStoreOnClassPath) {
            if (log.isWarnEnabled()) {
                log.warn("Could not load keystore from classpath, trying to find it at system scope!");
            }
            try {
                if (log.isInfoEnabled()) {
                    log.info(path.toString());
                }
                final var fis = new FileInputStream(path.toString());
                store.load(fis, pw);
                fis.close();
            } catch (IOException e) {
                if (log.isWarnEnabled()) {
                    log.warn("Could not find keystore at system scope, aborting!");
                }
                throw e;
            }
        } else {
            if (log.isInfoEnabled()) {
                log.info("Loading KeyStore from ClassPath...");
            }

            final var is = new ClassPathResource(pathString).getInputStream();

            try {
                store.load(is, pw);
                is.close();
            } catch (IOException e) {
                if (log.isWarnEnabled()) {
                    log.warn("Could not find keystore, aborting!");
                }
                throw e;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Keystore loaded");
        }
        return store;
    }

    /**
     * Getter for the expiration date of the Cert in the KeyStore.
     *
     * @return expiration of currently used IDS Certificate
     */
    public Date getCertExpiration() {
        return ((X509Certificate) cert).getNotAfter();
    }

    /**
     * Load the TrustManager from the truststore.
     *
     * @param password password of the truststore
     * @return the X509TrustManager for the certificates inside the Truststore
     * @throws NoSuchAlgorithmException if no Provider supports a TrustManagerFactorySpi implementation for the specified algorithm
     * @throws UnrecoverableKeyException if the key cannot be recovered (e.g. the given password is wrong)
     * @throws KeyStoreException if initialization of the trustmanager fails
     */
    private X509TrustManager loadTrustManager(final char[] password)
            throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
        if (log.isDebugEnabled()) {
            log.debug("Loading trustmanager");
        }

        final var keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(this.trustStore, password);

        final var trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(this.trustStore);

        final var trustManagers = trustManagerFactory.getTrustManagers();

        if (log.isInfoEnabled()) {
            log.info("Trustmanager loaded");
        }
        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
            throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
        }
        return (X509TrustManager) trustManagers[0];
    }

    /**
     * Get the PrivateKey from the KeyStore (use the Key with the given alias).
     *
     * @param keyAlias the alias of the PrivateKey to be loaded
     * @throws UnrecoverableKeyException if the Key cannot be retrieved from the keystore (e.g. the given password is wrong)
     * @throws NoSuchAlgorithmException if the algorithm for recovering the key cannot be found
     * @throws KeyStoreException if KeyStore was not initialized
     */
    private void getPrivateKeyFromKeyStore(final String keyAlias)
            throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Getting private key %s from keystore", keyAlias));
        }

        final var key = keyStore.getKey(keyAlias, keyStorePw);
        if (key instanceof PrivateKey) {
            if (log.isDebugEnabled()) {
                log.debug("Setting private key and connector certificate");
            }

            this.privateKey = (PrivateKey) key;
            this.cert = keyStore.getCertificate(keyAlias);
        }
    }

    /**
     * Create a merged trustmanager (trust anchors are TrustStore + java Truststore combined).
     *
     * @param myTrustManager the IDS truststore
     * @return a new truststore merging the IDS and Java Truststores
     * @throws NoSuchAlgorithmException if default Truststore cannot be loaded
     * @throws KeyStoreException if default Truststore cannot be loaded
     */
    public X509TrustManager configureTrustStore(final X509TrustManager myTrustManager)
            throws NoSuchAlgorithmException, KeyStoreException {
        final var jreTrustManager = findDefaultTrustManager();
        return createMergedTrustManager(jreTrustManager, myTrustManager);
    }

    /**
     * Find the default system trustmanager.
     *
     * @return the default java truststore
     * @throws NoSuchAlgorithmException if default Truststore cannot be loaded
     * @throws KeyStoreException if default Truststore cannot be loaded
     */
    private X509TrustManager findDefaultTrustManager() throws NoSuchAlgorithmException, KeyStoreException {
        final var tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        final KeyStore blank = null;
        tmf.init(blank); // If keyStore is null, tmf will be initialized with the default jvm trust store
        for (final var tm : tmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager) {
                return (X509TrustManager) tm;
            }
        }
        return null;
    }

    /**
     * Create a merged trustmanager from 2 given trustmanagers.
     *
     * @param jreTrustManager the jre truststore
     * @param customTrustManager the custom ids truststore
     * @return a new truststore which will check the IDS Truststore and the default java truststore for certificates
     */
    private X509TrustManager createMergedTrustManager(final X509TrustManager jreTrustManager,
                                                      final X509TrustManager customTrustManager) {
        return new X509TrustManager() {

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                // If you're planning to use client-cert auth,
                // merge results from "defaultTm" and "myTm".
                return jreTrustManager.getAcceptedIssuers();
            }

            @Override
            public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
                //if custom trustmanager does not work, just use jre trustmanager
                try {
                    customTrustManager.checkServerTrusted(chain, authType);
                } catch (CertificateException e) {
                    // This will throw another CertificateException if this fails too.
                    jreTrustManager.checkServerTrusted(chain, authType);
                }
            }

            @Override
            public void checkClientTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
                // If you're planning to use client-cert auth,
                // do the same as checking the server.
                jreTrustManager.checkClientTrusted(chain, authType);
            }
        };
    }
}
