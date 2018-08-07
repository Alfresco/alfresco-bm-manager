package org.alfresco.bm.common.util.exception;

import org.alfresco.bm.common.TestRunState;

/**
 * Exception generated when an attempt is made to change the state of a test run
 * in a way that is not supported.
 *
 * @author Derek Hulley
 * @since 2.0
 */
public class RunStateException extends Exception
{
    private static final long serialVersionUID = 6589647065852440835L;
    private final String test;
    private final String run;
    private final TestRunState previousState;
    private final TestRunState newState;
    public RunStateException(String test, String run, TestRunState previousState, TestRunState newState)
    {
        super("The test run '" + test + "." + run + "' state cannot be change from " + previousState + " to " + newState);
        this.test = test;
        this.run = run;
        this.previousState = previousState;
        this.newState = newState;
    }
    public String getTest()
    {
        return test;
    }
    public String getRun()
    {
        return run;
    }
    public TestRunState getPreviousState()
    {
        return previousState;
    }
    public TestRunState getNewState()
    {
        return newState;
    }
}
