/*
 * #%L
 * Alfresco Benchmark Manager
 * %%
 * Copyright (C) 2005 - 2018 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.bm.common.util;

import org.alfresco.bm.common.util.cipher.AESCipher;
import org.alfresco.bm.common.util.cipher.CipherException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

@RunWith(JUnit4.class)
public class AESCipherTest
{
    /**
     * test name
     */
    public static final String RELEASE = "AESCipherTest";

    /**
     * logger
     */
    private Log logger = LogFactory.getLog(this.getClass());

    @Before
    public void setUp()
    {
    }

    @After
    public void tearDown()
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

    @Test(expected = CipherException.class)
    public void failTest() throws Exception
    {
        String value = "JUnit fail test";
        String salt = "salt";
        String encoded = AESCipher.encode(salt, value);
        // should throw 'CipherException'
        String decoded = AESCipher.decode(null, encoded);

        assertNotEquals("Expected not equal values because of different salt", value, decoded);
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
            while ((line = br.readLine()) != null)
            {
                text += line + "\r\n";
            }
            if (logger.isDebugEnabled())
            {
                logger.debug("Test text: ' " + text + "'");
            }
            if (text == null || text.isEmpty())
            {
                text =
                    "Հայերեն Shqip \u202Bالعربية\u202Bالعربية   Български Català 中文简体 Hrvatski Česky Dansk Nederlands English Eesti "
                        + "Filipino Suomi Français ქართული Deutsch Ελληνικά \u202Bעברית\u202Bעברית   हिन्दी Magyar Indonesia Italiano Latviski "
                        + "Lietuviškai македонски Melayu Norsk Polski Português Româna Pyccкий Српски Slovenčina Slovenščina Español Svenska "
                        + "ไทย Türkçe Українська Tiếng Việt\n" + "Lorem Ipsum\n"
                        + "\"Neque porro quisquam est qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit...\"\n"
                        + "\"There is no one who loves pain itself, who seeks after it and wants to have it, simply because it is pain...\"";
            }
            // encode and decode - text and salt the same 
            String encoded = AESCipher.encode(text, text);
            String decoded = AESCipher.decode(text, encoded);

            assertEquals("Expected same text ...", decoded, text);

            // encode and decode using a smaller salt
            String salt = "Test Salt";
            encoded = AESCipher.encode(salt, text);
            decoded = AESCipher.decode(salt, encoded);

            assertEquals("Expected same text ...", decoded, text);
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
