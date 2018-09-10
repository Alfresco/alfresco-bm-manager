package org.alfresco.bm.integration.test.model;

import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * POJO representing a test definition
 * 
 * @author Andrei Forascu
 * @since 3.0
 */

public class TestDefinition
{
    Object _id;
    String release;
    String schema;

    public Object get_id()
    {
        return _id;
    }

    public void set_id(Object _id)
    {
        this._id = _id;
    }

    public String getRelease()
    {
        return release;
    }

    public void setRelease(String release)
    {
        this.release = release;
    }

    public String getSchema()
    {
        return schema;
    }

    public void setSchema(String schema)
    {
        this.schema = schema;
    }

}
