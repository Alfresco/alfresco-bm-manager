package org.alfresco.bm.common.util.exception;

/**
 * Exception generated when a test or test run is not found
 *
 * @author Derek Hulley
 * @since 2.0
 */
public class NotFoundException extends Exception
{
    private static final long serialVersionUID = 5931751540252570038L;

    public NotFoundException(String test, String run)
    {
        this(test, run, null);
    }

    public NotFoundException(String test, String run, Throwable cause)
    {
        super(run == null ?
                "Test not found: " + test :
                "Test run not found: " + test + "." + run
                , cause);
    }
}
