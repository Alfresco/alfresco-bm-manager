/*
 * #%L
 * Alfresco Benchmark Manager
 * %%
 * Copyright (C) 2005 - 2018 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
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
 * #L%
 */
package org.alfresco.bm.publicapi.factory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

/**
 * 
 * @author jottley
 * 
 */
public class CodeUrl
{

    public static final String CODE  = "code";
    public static final String SCOPE = "scope";
    public static final String STATE = "state";


    private URL                url;


    public CodeUrl(String authUrl)
        throws MalformedURLException
    {
        this.url = new URL(authUrl);
    }


    public String getQuery()
    {
        return url.getQuery();
    }


    private String[] parseQuery()
    {
        return this.getQuery().split("&");
    }


    public HashMap<String, String> getQueryMap()
    {
        HashMap<String, String> queryMap = new HashMap<String, String>();

        String[] query = this.parseQuery();
        for (int i = 0; i < query.length; i++)
        {
            String[] kv = query[i].split("=");

            queryMap.put(kv[0], kv[1]);
        }

        return queryMap;
    }


    @Override
    public String toString()
    {
        return url.toString();
    }
}
