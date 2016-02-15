package org.alfresco.bm.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import org.alfresco.bm.api.AESCipher;
import org.alfresco.bm.exception.CipherException;

@RunWith(JUnit4.class)
public class AESCipherTest
{
    /** test name */
    public static final String RELEASE = "AESCipherTest";

    /** logger */
    private Log logger = LogFactory.getLog(this.getClass());
    
    @Before
    public void setUp() throws Exception
    {
    }

    @After
    public void tearDown() throws Exception
    {
    }
    
    @Test
    public void EncodeDecodeWithSalt() throws Exception
    {
        String salt = "Property Name";
        String value = "JUnit value for a test";
        String encoded = AESCipher.encode(salt, value);
        String decoded = AESCipher.decode(salt, encoded);
        
        assertEquals("Expected value and decoded the same", value, decoded);
    }

    @Test
    public void EncodeDecodeWithoutSalt() throws Exception
    {
        String value = "JUnit value for a test";
        String encoded = AESCipher.encode(null, value);
        String decoded = AESCipher.decode("", encoded);
        
        assertEquals("Expected value and decoded the same", value, decoded);
    }
    
    
    @Test(expected=CipherException.class)
    public void failTest() throws Exception
    {
        String value = "JUnit fail test";
        String salt = "salt";
        String encoded = AESCipher.encode(salt, value);
        // should throw 'CipherException'
        String decoded = AESCipher.decode(null, encoded);
        
        assertNotEquals("Expected not equal values because of different salt",  value, decoded);
    }
    
    @Test
    public void extremeTextTest()
    {
        boolean failed = false;
        
        try
        {
            // get a text 
            String text = "";
            String line;
            URL url = new URL("http://www.lipsum.com/");
            InputStream is = url.openStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            br.readLine();
            while ((line = br.readLine()) != null) {
                text += line + "\r\n";
            }
            if (logger.isDebugEnabled())
            {
                logger.debug("Test text: ' " + text + "'");
            }
            
            // encode and decode - text and salt the same 
            String encoded = AESCipher.encode(text, text);
            String decoded = AESCipher.decode(text, encoded);
            
            assertEquals("Expected same text ...",  decoded, text);
            
            // encode and decode using a smaller salt
            String salt = "Test Salt";
            encoded = AESCipher.encode(salt, text);
            decoded = AESCipher.decode(salt, encoded);
            
            assertEquals("Expected same text ...",  decoded, text);
        }
        catch (CipherException ce)
        {
            failed = true;
            logger.error("Expected no cipher exception.", ce);
        }
        catch (Exception e)
        {
            failed = true;
            logger.error("Test text failed because of exception when retrieving test content.", e);
        }
        assertFalse("Expected no exception!", failed);
    }
}
