package org.alfresco.bm.common.util.cipher;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

/**
 * Encoding and decoding of strings (passwords for JSON export)
 * 
 * @author Frank Becker
 *
 * @since 2.1.2
 */
public class AESCipher
{
    /**
     * Returns an encoded BASE64 string of the value.
     * 
     * @param salt
     *        the salt to use for encoding
     * @param value
     *        value to encode
     * 
     * @return encoded BASE64 string of the value or null if no value given
     * 
     * @throws CipherException
     */
    public static String encode(String salt, String value)
            throws CipherException
    {
        // verify value
        if (null == value || value.isEmpty())
        {
            return null;
        }

        // create cipher object
        SecretKeySpec keySpec = createAESCipher(salt);

        Cipher cipher;
        try
        {
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] enc = cipher.doFinal(value.getBytes());

            Encoder encoder = Base64.getEncoder();
            return encoder.encodeToString(enc);
        }
        catch (Exception e)
        {
            throw new CipherException("Error during encryption.", e);
        }
    }

    /**
     * Decodes a string with a given salt
     * 
     * @param salt
     *        salt to use for decoding
     * @param value
     *        BASE64 value to decode
     * 
     * @return decoded value as String or null if no value
     * 
     * @throws CipherException
     */
    public static String decode(String salt, String value)
            throws CipherException
    {
        // verify value
        if (null == value || value.isEmpty())
        {
            return null;
        }

        // create cipher object
        SecretKeySpec keySpec = createAESCipher(salt);

        try
        {
            // get BASE64 decoder and decode from BASE64
            Decoder decoder = Base64.getDecoder();
            byte[] crypted = decoder.decode(value);
            // get cipher and decode
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            try
            {
                return new String(cipher.doFinal(crypted));
            }
            catch (IllegalBlockSizeException | BadPaddingException e)
            {
                throw new CipherException("Error decrypting '" + value + "' - bad 'salt'?", e);
            }
        }
       catch (Exception e)
        {
            throw new CipherException("Error decrypting '" + value + "'.", e);
        }
    }

    /**
     * Creates and returns the cipher object
     */
    private static SecretKeySpec createAESCipher(String salt)
            throws CipherException
    {
        try
        {
            // make sure we have a salt
            String keyStr = (salt == null || salt.isEmpty()) ? "(AlfrescoBMServerDefaultSalt)" : salt;
            byte[] key = (keyStr).getBytes("UTF-8");
            MessageDigest sha = MessageDigest.getInstance("MD5");
            key = sha.digest(key);
            return new SecretKeySpec(Arrays.copyOf(key, 16), "AES");
        }
        catch (Exception e)
        {
            throw new CipherException(
                    "Error creating 'AESCipher cipher object'.", e);
        }
    }
}
