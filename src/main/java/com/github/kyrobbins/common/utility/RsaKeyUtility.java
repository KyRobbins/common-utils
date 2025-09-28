package com.github.kyrobbins.common.utility;

import com.github.kyrobbins.common.exception.ConfigurationException;
import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.Nonnull;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Utility for parsing Public / Private RSA keys */
public class RsaKeyUtility {

    private final KeyFactory keyFactory;

    private static final Pattern ALL_WHITE_SPACE = Pattern.compile("\\s+");
    private static final Pattern PEM_SECTION_DIVIDERS = Pattern.compile("-----[^-]*-----");

    public RsaKeyUtility() throws NoSuchAlgorithmException {
        keyFactory = KeyFactory.getInstance("RSA");
    }

    /**
     * Creates a {@link PrivateKey} using the given private key data string
     *
     * @param privateKeyString The private key data to create a {@link PrivateKey} from
     * @return The created {@link PrivateKey}
     */
    @Nonnull
    public PrivateKey createPrivateKey(@Nonnull String privateKeyString) {
        final String key = normalizeKeyString(privateKeyString);

        try {
            byte[] encoded = Base64.getDecoder().decode(key);

            final KeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            return keyFactory.generatePrivate(keySpec);
        } catch (InvalidKeySpecException | IllegalAccessError e) {
            throw new ConfigurationException("Failed to load private key", e);
        }
    }

    /**
     * Creates a {@link PublicKey} using the given public key data string
     *
     * @param publicKeyString The public key data to create a {@link PublicKey} from
     * @return The created {@link PublicKey}
     */
    @Nonnull
    public PublicKey createPublicKey(@Nonnull String publicKeyString) {
        try {
            final String key = normalizeKeyString(publicKeyString);
            final KeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(key));
            return keyFactory.generatePublic(keySpec);
        } catch (InvalidKeySpecException e) {
            throw new ConfigurationException("Failed to load public key", e);
        }
    }

    /**
     * Strips the extra formatting characters found in a PEM file format, if they are there
     *
     * @param keyString The raw value from the PEM file
     * @return The stripped down string containing just the normalized key
     */
    @Nonnull
    @VisibleForTesting
    String normalizeKeyString(@Nonnull String keyString) {
        return Optional.of(keyString)
                .map(ALL_WHITE_SPACE::matcher)
                .map(this::removeAll)
                .map(PEM_SECTION_DIVIDERS::matcher)
                .map(this::removeAll)
                .orElseThrow(() -> new ConfigurationException("Invalid key string"));
    }

    /**
     * Removes all the instances of the matcher group
     *
     * @param matcher The matcher group to remove
     * @return The normalized string
     */
    @Nonnull
    private String removeAll(@Nonnull Matcher matcher) {
        return matcher.replaceAll("");
    }
}
