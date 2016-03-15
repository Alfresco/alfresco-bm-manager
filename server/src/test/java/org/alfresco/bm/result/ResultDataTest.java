package org.alfresco.bm.result;

import static org.junit.Assert.assertEquals;

import org.alfresco.bm.exception.BenchmarkResultException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * JUnit testing of result data objects
 * 
 * @author Frank Becker
 * @since 2.1.2
 */
@RunWith(JUnit4.class)
public class ResultDataTest
{
    @Before
    public void setUp() throws Exception
    {
    }

    @After
    public void tearDown() throws Exception
    {
    }

    @Test
    public void testObjectsPerSecond() throws BenchmarkResultException
    {
        ObjectsPerSecondResultData data1 = new ObjectsPerSecondResultData(1.0,
                ResultObjectType.Document, getJsonValue());
        ObjectsPerSecondResultData data2 = new ObjectsPerSecondResultData(2.0,
                ResultObjectType.Document, getJsonValue());
        ObjectsPerSecondResultData data3 = ObjectsPerSecondResultData
                .Combine(data1, data2);

        assertEquals(ResultObjectType.Document, data1.getObjectType());
        assertEquals(ResultObjectType.Document, data2.getObjectType());
        assertEquals(ResultObjectType.Document, data3.getObjectType());
        assertEquals(getJsonValue(), data1.getDescription());
        assertEquals(getJsonValue(), data2.getDescription());
        assertEquals(getJsonValue(), data3.getDescription());
        assertEquals(1.0, data1.getObjectsPerSecond(), 0.0);
        assertEquals(2.0, data2.getObjectsPerSecond(), 0.0);
        assertEquals(1.5, data3.getObjectsPerSecond(), 0.0);
    }

    @Test(expected = BenchmarkResultException.class)
    public void testBoundsObjectsPerSecond() throws BenchmarkResultException
    {
        new ObjectsPerSecondResultData(-1.0, null, getJsonValue());
    }

    @Test(expected = BenchmarkResultException.class)
    public void testBoundsObjects() throws BenchmarkResultException
    {
        new ObjectsResultData(-1, null, getJsonValue());
    }

    @Test(expected = BenchmarkResultException.class)
    public void testBoundsRuntime() throws BenchmarkResultException
    {
        new RuntimeResultData(-1, getJsonValue());
    }

    @Test(expected = BenchmarkResultException.class)
    public void testDescriptionRuntime() throws BenchmarkResultException
    {
        RuntimeResultData data1 = new RuntimeResultData(1, getJsonValue());
        RuntimeResultData data2 = new RuntimeResultData(1, "");
        RuntimeResultData.Combine(data1, data2);
    }

    @Test(expected = BenchmarkResultException.class)
    public void testDescriptionObjects1() throws BenchmarkResultException
    {
        ResultObjectType type = ResultObjectType.Folder;
        ObjectsResultData data1 = new ObjectsResultData(1, type,
                getJsonValue());
        ObjectsResultData data2 = new ObjectsResultData(1, type, "");
        ObjectsResultData.Combine(data1, data2);
    }

    @Test(expected = BenchmarkResultException.class)
    public void testDescriptionObjects2() throws BenchmarkResultException
    {
        ObjectsResultData data1 = new ObjectsResultData(1,
                ResultObjectType.Folder, getJsonValue());
        ObjectsResultData data2 = new ObjectsResultData(1,
                ResultObjectType.Document, getJsonValue());
        ObjectsResultData.Combine(data1, data2);
    }

    @Test
    public void testNumberObjects() throws BenchmarkResultException
    {
        ObjectsResultData data1 = new ObjectsResultData(1,
                ResultObjectType.Document, getJsonValue());
        ObjectsResultData data2 = new ObjectsResultData(2,
                ResultObjectType.Document, getJsonValue());
        ObjectsResultData data3 = ObjectsResultData.Combine(data1, data2);

        assertEquals(ResultObjectType.Document, data1.getObjectType());
        assertEquals(ResultObjectType.Document, data2.getObjectType());
        assertEquals(ResultObjectType.Document, data3.getObjectType());
        assertEquals(getJsonValue(), data1.getDescription());
        assertEquals(getJsonValue(), data2.getDescription());
        assertEquals(getJsonValue(), data3.getDescription());
        assertEquals(1, data1.getNumberOfObjects());
        assertEquals(2, data2.getNumberOfObjects());
        assertEquals(3, data3.getNumberOfObjects());
    }

    @Test
    public void testRuntime() throws BenchmarkResultException
    {
        RuntimeResultData data1 = new RuntimeResultData(1,getJsonValue());
        RuntimeResultData data2 = new RuntimeResultData(2, getJsonValue());
        RuntimeResultData data3 = RuntimeResultData.Combine(data1, data2);

        assertEquals(getJsonValue(), data1.getDescription());
        assertEquals(getJsonValue(), data2.getDescription());
        assertEquals(getJsonValue(), data3.getDescription());
        assertEquals(1, data1.getRuntimeTicks());
        assertEquals(2, data2.getRuntimeTicks());
        assertEquals(3, data3.getRuntimeTicks());
    }

    /**
     * @return a JSON value to set as description
     */
    private String getJsonValue()
    {
        return "TestObject [platform=m3.2xlarge]";
    }
}
