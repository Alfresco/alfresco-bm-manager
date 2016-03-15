package org.alfresco.bm.exception;

/**
 * Benchmark result exception
 * 
 * @author Frank Becker
 * @since 2.1.2
 */
public class BenchmarkResultException extends Exception
{
    /** Serialization ID */
    private static final long serialVersionUID = -877508813243951980L;

    /** Constructor */
    public BenchmarkResultException(String message)
    {
        super (message);
    }
    
    /** Constructor */
    public BenchmarkResultException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
