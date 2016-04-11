package org.alfresco.bm.exception;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Cipher Exception class
 * 
 * @author Frank Becker
 * @since 2.1.2
 */
public class CipherException extends Exception
{
    /** serial ID */
    private static final long serialVersionUID = -578031689334162353L;

    /**
     * Constructor
     * 
     * @param message
     *        Exception message
     * @param cause
     *        the cause
     */
    public CipherException(String message, Throwable cause)
    {
        super(message, cause);
        Log logger = LogFactory.getLog(this.getClass());
        logger.error(message, cause);
    }

    /**
     * Constructor
     * 
     * @param cause
     *        the cause
     */
    public CipherException(Throwable cause)
    {
        this("Genric cipher exception", cause);
        Log logger = LogFactory.getLog(this.getClass());
        logger.error("Cipher exception", cause);
    }

    /**
     * Constructor
     * 
     * @param message
     *        (String) exception message
     */
    public CipherException(String message)
    {
        super(message);
        Log logger = LogFactory.getLog(this.getClass());
        logger.error(message);
    }
}
