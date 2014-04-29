/*
 * Copyright (C) 2005-2014 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.json;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotSame;

import org.apache.http.entity.StringEntity;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @see JSONUtil
 * 
 * @author Derek Hulley
 * @since 2.0
 */
@RunWith(JUnit4.class)
public class JSONUtilTest
{
    @Test
    public void testSetMethodBody() throws Exception
    {
        JSONObject json = new JSONObject();
        StringEntity se = JSONUtil.setMessageBody(json);
        // Check that UTF-8 is the encoding used
        assertEquals(new StringEntity("{}", "UTF-8").getContentLength(), se.getContentLength());
        assertNotSame(new StringEntity("{}", "UTF-16").getContentLength(), se.getContentLength());
    }
}
