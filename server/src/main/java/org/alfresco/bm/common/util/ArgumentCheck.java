package org.alfresco.bm.common.util;

/**
 * Validation for passed arguments.
 * 
 * @author Frank Becker
 * @since 2.0.10
 */
public final class ArgumentCheck
{
    /**
     * Checks that the parameter with the given name has content i.e. it's not null
     * 
     * @param argumentName
     *            (String, required) Name of parameter to check
     * @param object
     *            Value of the parameter to check
     */
    public static final void checkMandatoryObject(final Object object, final String argumentName)
    {
        checkMandatoryString(argumentName, "argumentName");

        if (null == object)
        {
            throw new IllegalArgumentException("'" + argumentName + "' is a mandatory parameter");
        }
    }

    /**
     * Checks that the string parameter with the given name is not null or empty
     * 
     * @param argumentName
     *            (String, required) Name of parameter to validate
     * @param argumentValue
     *            (String) Value of the parameter to validate
     */
    public static final void checkMandatoryString(final String argumentValue, final String argumentName)
    {
        if (null == argumentName || argumentName.isEmpty())
        {
            throw new IllegalArgumentException("'argumentName' is a mandatory parameter");
        }

        if (null == argumentValue || argumentValue.isEmpty())
        {
            throw new IllegalArgumentException("'" + argumentName + "' is a mandatory parameter");
        }
    }

    /**
     * Checks that the number is positive.
     * 
     * @param number
     *            (long) value to validate
     * @param argumentName
     *            (String, required) name of argument to validate
     */
    public static void checkNumberPositive(final long number, String argumentName)
    {
        checkMandatoryString(argumentName, "argumentName");
        if (number < 0)
        {
            throw new IllegalArgumentException("'" + argumentName + "': positive value required");
        }
    }
}
