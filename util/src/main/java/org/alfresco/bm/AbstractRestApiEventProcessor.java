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
package org.alfresco.bm;

import org.alfresco.bm.driver.event.AbstractEventProcessor;
import org.alfresco.rest.core.RestWrapper;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public abstract class AbstractRestApiEventProcessor extends AbstractEventProcessor implements ApplicationContextAware
{

    protected ApplicationContext context;
    protected String baseUrl;

    private ThreadLocal<RestWrapper> restWrapperContainer;
    private ThreadLocal<DataUser> dataUserContainer;
    private ThreadLocal<DataContent> dataContentContainer;
    private ThreadLocal<DataSite> dataSiteContainer;

    /**
     * Never, ever, cache this object in your code.
     * Always use this method
     *
     * @return the RestWrapper to use for this thread
     */
    public synchronized RestWrapper getRestWrapper()
    {
        if (restWrapperContainer == null)
        {
            restWrapperContainer = new ThreadLocal<>();
        }
        if (restWrapperContainer.get() == null)
        {
            RestWrapper rw = (RestWrapper) this.context.getBean("restWrapper");
            rw.configureRequestSpec().setBaseUri(baseUrl);
            restWrapperContainer.set(rw);
        }
        return restWrapperContainer.get();
    }

    /**
     * Never, ever, cache this object in your code.
     * Always use this method
     *
     * @return the DataUser to use for this thread
     */
    public synchronized DataUser getDataUser()
    {
        if (dataUserContainer == null)
        {
            dataUserContainer = new ThreadLocal<>();
        }
        if (dataUserContainer.get() == null)
        {
            DataUser du = (DataUser) this.context.getBean("dataUser");
            dataUserContainer.set(du);
        }
        return dataUserContainer.get();
    }

    /**
     * Never, ever, cache this object in your code.
     * Always use this method
     *
     * @return the DataContent to use for this thread
     */
    public synchronized DataContent getDataContent()
    {
        if (dataContentContainer == null)
        {
            dataContentContainer = new ThreadLocal<>();
        }
        if (dataContentContainer.get() == null)
        {
            DataContent rw = (DataContent) this.context.getBean("dataContent");
            dataContentContainer.set(rw);
        }
        return dataContentContainer.get();
    }

    /**
     * Never, ever, cache this object in your code.
     * Always use this method
     *
     * @return the DataSite to use for this thread
     */
    public synchronized DataSite getDataSite()
    {
        if (dataSiteContainer == null)
        {
            dataSiteContainer = new ThreadLocal<>();
        }
        if (dataSiteContainer.get() == null)
        {
            DataSite ds = (DataSite) this.context.getBean("dataSite");
            dataSiteContainer.set(ds);
        }
        return dataSiteContainer.get();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.context = applicationContext;
    }

    public void setBaseUrl(String baseUrl)
    {
        this.baseUrl = baseUrl;
    }

}
