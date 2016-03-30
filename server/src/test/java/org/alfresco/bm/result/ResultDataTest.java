package org.alfresco.bm.result;

import static org.junit.Assert.assertEquals;

import org.alfresco.bm.exception.BenchmarkResultException;
import org.alfresco.bm.result.data.ObjectsPerSecondResultData;
import org.alfresco.bm.result.data.ObjectsResultData;
import org.alfresco.bm.result.data.RuntimeResultData;
import org.alfresco.bm.result.defs.ResultObjectType;
import org.alfresco.bm.result.defs.ResultOperation;
import org.apache.poi.ss.formula.ptg.OperationPtg;
import org.bson.Document;
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
                ResultObjectType.Document, getJsonValue(), ResultOperation.None);
        ObjectsPerSecondResultData data2 = new ObjectsPerSecondResultData(2.0,
                ResultObjectType.Document, getJsonValue(), ResultOperation.None);
        ObjectsPerSecondResultData data3 = ObjectsPerSecondResultData.combine(
                data1, data2);

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
        new ObjectsPerSecondResultData(-1.0, null, getJsonValue(), ResultOperation.None);
    }

    @Test(expected = BenchmarkResultException.class)
    public void testBoundsObjects() throws BenchmarkResultException
    {
        new ObjectsResultData(-1, null, getJsonValue(), ResultOperation.None);
    }

    @Test(expected = BenchmarkResultException.class)
    public void testBoundsRuntime() throws BenchmarkResultException
    {
        new RuntimeResultData(-1, getJsonValue(), ResultOperation.None);
    }

    @Test(expected = BenchmarkResultException.class)
    public void testDescriptionRuntime() throws BenchmarkResultException
    {
        RuntimeResultData data1 = new RuntimeResultData(1, getJsonValue(), ResultOperation.None);
        RuntimeResultData data2 = new RuntimeResultData(1, null, ResultOperation.None);
        RuntimeResultData.combine(data1, data2);
    }

    @Test(expected = BenchmarkResultException.class)
    public void testDescriptionObjects1() throws BenchmarkResultException
    {
        ResultObjectType type = ResultObjectType.Folder;
        ObjectsResultData data1 = new ObjectsResultData(1, type, getJsonValue(), ResultOperation.None);
        ObjectsResultData data2 = new ObjectsResultData(1, type, null, ResultOperation.None);
        ObjectsResultData.combine(data1, data2);
    }

    @Test(expected = BenchmarkResultException.class)
    public void testDescriptionObjects2() throws BenchmarkResultException
    {
        ObjectsResultData data1 = new ObjectsResultData(1,
                ResultObjectType.Folder, getJsonValue(), ResultOperation.None);
        ObjectsResultData data2 = new ObjectsResultData(1,
                ResultObjectType.Document, getJsonValue(), ResultOperation.None);
        ObjectsResultData.combine(data1, data2);
    }

    @Test
    public void testNumberObjects() throws BenchmarkResultException
    {
        ObjectsResultData data1 = new ObjectsResultData(1,
                ResultObjectType.Document, getJsonValue(), ResultOperation.None);
        ObjectsResultData data2 = new ObjectsResultData(2,
                ResultObjectType.Document, getJsonValue(), ResultOperation.None);
        ObjectsResultData data3 = ObjectsResultData.combine(data1, data2);

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
        RuntimeResultData data1 = new RuntimeResultData(1, getJsonValue(), ResultOperation.None);
        RuntimeResultData data2 = new RuntimeResultData(2, getJsonValue(), ResultOperation.None);
        RuntimeResultData data3 = RuntimeResultData.combine(data1, data2);

        assertEquals(getJsonValue(), data1.getDescription());
        assertEquals(getJsonValue(), data2.getDescription());
        assertEquals(getJsonValue(), data3.getDescription());
        assertEquals(1, data1.getRuntimeTicks());
        assertEquals(2, data2.getRuntimeTicks());
        assertEquals(3, data3.getRuntimeTicks());
    }

    @Test(expected = BenchmarkResultException.class)
    public void testResultOperation1() throws BenchmarkResultException
    {
        ResultObjectType type = ResultObjectType.Folder;
        ObjectsResultData data1 = new ObjectsResultData(1, type, getJsonValue(), ResultOperation.None);
        ObjectsResultData data2 = new ObjectsResultData(1, type, getJsonValue(), ResultOperation.Deleted);
        ObjectsResultData.combine(data1, data2);
    }

    @Test(expected = BenchmarkResultException.class)
    public void testResultOperation2() throws BenchmarkResultException
    {
        RuntimeResultData data1 = new RuntimeResultData(1, getJsonValue(), ResultOperation.Updated);
        RuntimeResultData data2 = new RuntimeResultData(1, getJsonValue(), ResultOperation.Created);
        RuntimeResultData.combine(data1, data2);
    }

    @Test(expected = BenchmarkResultException.class)
    public void testResultOperation3() throws BenchmarkResultException
    {
        ResultObjectType type = ResultObjectType.Folder;
        ObjectsPerSecondResultData data1 = new ObjectsPerSecondResultData(1, type, getJsonValue(),
                ResultOperation.Unchanged);
        ObjectsPerSecondResultData data2 = new ObjectsPerSecondResultData(1, type, getJsonValue(),
                ResultOperation.Created);
        ObjectsPerSecondResultData.combine(data1, data2);
    }

    /**
     * @return a BSON document to set as description
     */
    private Document getJsonValue()
    {
        return new Document("TestObject", "platform=m3.2xlarge");
    }
}
