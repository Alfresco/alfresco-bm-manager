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
package org.alfresco.bm.driver.event;

import org.alfresco.bm.common.EventResult;
import org.alfresco.bm.common.ResultService;
import org.alfresco.bm.common.session.SessionService;
import org.apache.commons.lang3.time.StopWatch;

/**
 * Basic interface that test runner code needs to implement in order to be processed
 * by the Benchmark Server framework.
 * <p/>
 * <h1>Notes</h1>
 * <p/>
 * Implementations must be stateless.
 * <p/>
 * The {@link EventResult result} of {@link #processEvent(EventService, ResultService, Event) event processing}
 * does not have to contain any further events.  However, it is better to publish a 'finished' event
 * as it allows more flexibility in wiring test scenarios together.
 * <pre>
 *    start -> doA -> doB -> doB.done
 * </pre>
 * It is possible to remap <b>doB.done</b> above back into <b>doA</b> and thereby create an
 * test that never ends, for example.
 * <p/>
 * <h1>Javadoc Good Practice</h1>
 * Document the input (what type of {@link Event#getData() data} the input event must contain),
 * likely actions and the possible output e.g.<br/>
 * <b>INPUT:   </b>An HTTP session (Apache HttpClient} that has been
 *                 authenticated against the live system and displaying the user's dashboard.<br/>
 * <b>ACTIONS: </b>A random action next action will be chosen from the available screen.<br/>
 * <b>OUTPUT:  </b>The next user event will be sheduled e.g. 'click on advanced search box' using
 *                 event 'share.advancedSearch' ... etc.
 * 
 * @see SessionService
 *
 * @author Derek Hulley
 * @since 1.0
 */
public interface EventProcessor
{
    /**
     * @return                  the common name (usually a bean name) to reference instances by
     */
    String getName();
    
    /**
     * Provide a hint for the processing framework on how much time should elapse before
     * the <b>warnings</b> need to be issued over the delay
     * 
     * @return                  the maximum event processing time before warnings get issued
     */
    long getWarnDelay();
    
    /**
     * Hint whether the result should be included in charts
     * 
     * @return                  <tt>true</tt> to include results generated in output charts
     */
    boolean isChart();

    /**
     * Process an event.
     * <p/>
     * <b>NOTE:</b> All errors are treated as <b>terminal</b> i.e. there will be no
     * follow-up events published.  The event and result services are provided to allow
     * event processing to be aware of previous and future work.  If the service is used
     * to modify queues, the results could be unexpected e.g. clearing the queue will not
     * always mean that no more events will be processed.
     * <p/>
     * Timing: Event processing can {@link StopWatch#start()} and {@link StopWatch#stop()}
     *         the timer to better reflect actual event processing.
     * 
     * @param event             the event (along with associated data)
     * @param stopWatch         the timer that will be used or <tt>null</tt> to have one
     *                          created and attached to the process automatically
     * @return                  the result of the process (includes errors and next events)
     */
    EventResult processEvent(Event event, StopWatch stopWatch) throws Exception;
    
    /**
     * Must the framework automatically carry event sessions from event to event.
     * This is only supported where an event produces exactly one 'next' event i.e.
     * sessions should be associated with a linear sequence of events.
     * 
     * @return                  <tt>true</tt> to allow the framework to carry session IDs
     */
    boolean isAutoPropagateSessionId();
    
    /**
     * Must the framework auto-close any event sessions that cannot be propagated?  This
     * will be called if there are no further events to publish, either through an exception
     * coming out of the processing or because an event has no further events in the chain.
     * 
     * @return                  <tt>true</tt> to allow the framework to automatically manage session closure
     */
    boolean isAutoCloseSessionId();
}
