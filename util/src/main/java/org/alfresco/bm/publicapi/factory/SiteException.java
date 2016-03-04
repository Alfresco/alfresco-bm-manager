package org.alfresco.bm.publicapi.factory;

/**
 * Site exception
 * 
 * @author Frank Becker
 * 
 * @since 2.1.2
 */
public class SiteException extends Exception
{
    /** Serialization ID */
    private static final long serialVersionUID = 5858602498644137295L;

    /**
     * Constructor
     * 
     * @param cause
     *        cause of exception
     */
    public SiteException(Throwable cause)
    {
        super(cause);
    }

    /**
     * Constructor
     * 
     * @param message
     *        Exception message
     */
    public SiteException(String message)
    {
        super(message);
    }

    /**
     * Constructor
     * 
     * @param msg
     *        Exception message
     * @param cause
     *        exception cause
     */
    public SiteException(String msg, Throwable cause)
    {
        super(msg, cause);
    }
}
