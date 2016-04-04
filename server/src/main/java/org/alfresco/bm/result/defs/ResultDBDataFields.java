package org.alfresco.bm.result.defs;

/**
 * Field definitions for the BM "results" collection.
 * 
 * @author Frank Becker
 * @since 2.1.2
 */
public interface ResultDBDataFields
{
    public final static String FIELD_BM_ID = "bmId";
    public final static String FIELD_TEST_NAME = "test";
    public final static String FIELD_TEST_RUN_NAME = "run";
    public final static String FIELD_DRIVER_ID = "drId";
    public final static String FIELD_TIMESTAMP = "tm";

    public final static String FIELD_RESULT_DATA = "data";

    public final static String FIELD_RESULT_OP = "resultOp";
    public final static String FIELD_DESCRIPTION = "desc";
    public final static String FIELD_DATA_TYPE = "dataType";

    public final static String FIELD_OBJECTS_PER_SECOND = "objPerSec";
    public final static String FIELD_OBJECT_TYPE = "objType";

    public static final String FIELD_NUMBER_OF_OBJECTS = "numObj";

    public static final String FIELD_RUN_TICKS = "runTck";
}
