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
package org.alfresco.bm.chart;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.alfresco.bm.event.ResultService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Factory class to generate charts from some {@link ResultService results}.
 * 
 * @author Derek Hulley
 * @since 1.3
 */
@SuppressWarnings("unused")
public class ResultChart
{
    private final ResultService resultService;
    private final Log logger = LogFactory.getLog(ResultChart.class);
    
    // Record event names encountered
    private Set<String> eventNames = new HashSet<String>();
    
    // Map from event names to series index, sorted by event name
    private Map<String, Integer> eventIndexes = new TreeMap<String, Integer>();
    
//    private XYItemRenderer renderer;
    
    /**
     * @param resultService         the service to iterate over the results
     */
    public ResultChart(ResultService resultService)
    {
        this.resultService = resultService;
    }
//    
//    /**
//     * Construct an X-Y chart
//     * 
//     * @param newResultWaitTime     how long to wait before polling for new results.
//     *                              A value of zero or less will terminate polling.
//     * @param startTime             the time of the first event to plot (<tt>null</tt> to ignore)
//     * @param endTime               the time of the last event to plot (<tt>null</tt> to ignore)
//     * @param windowSize            the data window size (values are grouped and averaged)
//     * @return                      a chart that will track the result data until disposed
//     */
//    public JFreeChart createXYChart(
//            long startTime,
//            long endTime,
//            int resultCount,
//            int smoothingCount)
//    {
//        // Adjust the time bounds
//        long startTimeResults = resultService.getMinStartTime();
//        long endTimeResults = resultService.getMaxStartTime();
//        final long startTimeChart = (startTime < startTimeResults) ? startTimeResults : startTime;
//        final long endTimeChart = (endTime > endTimeResults) ? endTimeResults : endTime;
//        
//        final long chartWindowSize = ResultChart.getWindowSize(startTimeChart, endTimeChart, resultCount);
//        
//        // Create a timeseries to track each event
//        final TimeSeriesCollection meanTimes = new TimeSeriesCollection();
//        final TimeSeriesCollection volumes = new TimeSeriesCollection();
//
//        final AtomicInteger index = new AtomicInteger(0);
//
//        // Handler that populates the time series
//        final ResultHandler handler = new ResultHandler()
//        {
//            @Override
//            public boolean processResult(
//                    long fromTime, long toTime,
//                    Map<String, DescriptiveStatistics> statsByEventName) throws Throwable
//            {
//                if (fromTime > endTimeChart)
//                {
//                    // We have passed the point of interest
//                    return true;
//                }
//                
//                // Keep track of events encountered in this window.
//                Set<String> eventNamesForWindow = new HashSet<String>(statsByEventName.size() + 7);
//                
//                RegularTimePeriod time = new Millisecond(new Date(toTime));
//                for (String eventName : statsByEventName.keySet())
//                {
//                    eventNames.add(eventName);
//                    eventNamesForWindow.add(eventName);
//                    
//                    DescriptiveStatistics stats = statsByEventName.get(eventName);
//                    double nPerSecond = (double) stats.getN() / (double) (chartWindowSize) * 1000.0;
//                    // Add the mean value
//                    TimeSeries meanTimesForEvent = meanTimes.getSeries(eventName);
//                    if (meanTimesForEvent == null)
//                    {
//                        // Add it
//                        meanTimesForEvent = new TimeSeries(eventName, Millisecond.class);
//                        eventIndexes.put(eventName, index.getAndIncrement());
//                        meanTimes.addSeries(meanTimesForEvent);
//                    }
//                    meanTimesForEvent.addOrUpdate(time, stats.getMean());
//                    // Add the data volume (N/s)
//                    TimeSeries volumesForEvent = volumes.getSeries(eventName);
//                    if (volumesForEvent == null)
//                    {
//                        volumesForEvent = new TimeSeries(eventName, Millisecond.class);
//                        volumes.addSeries(volumesForEvent);
//                    }
//                    volumesForEvent.addOrUpdate(time, nPerSecond);
//                }
//                
//                // Give zero/s for volume of events NOT seen in this window
//                for (String eventName : eventNames)
//                {
//                    if (!eventNamesForWindow.contains(eventName))
//                    {
//                        // The event was not seen
//                        TimeSeries volumesForEvent = volumes.getSeries(eventName);
//                        if (volumesForEvent == null)
//                        {
//                            volumesForEvent = new TimeSeries(eventName, Millisecond.class);
//                            volumes.addSeries(volumesForEvent);
//                        }
//                        volumesForEvent.addOrUpdate(time, 0.0);
//                    }
//                }
//                
//                return false;
//            }
//            
//            @Override
//            public long getWaitTime()
//            {
//                return -1L;
//            }
//        };
//        try
//        {
//            resultService.getResults(handler, startTimeChart, null, Boolean.TRUE, chartWindowSize, true);
//        }
//        catch (IllegalStateException e)
//        {
//            if (e.getMessage().toLowerCase().contains("mongo"))
//            {
//                // This is expected.
//            }
//            else
//            {
//                logger.error(e);
//            }
//        }
//        
//        // Now create moving averages
//        final TimeSeriesCollection meanTimesMoving = MovingAverage.createMovingAverage(
//                meanTimes,
//                "",
//                (int)(smoothingCount * chartWindowSize),
//                0);
//        final TimeSeriesCollection volumesMoving = MovingAverage.createMovingAverage(
//                volumes,
//                "",
//                (int)(smoothingCount * chartWindowSize),
//                0);
//
//        this.renderer = new APXYLineAndShapeRenderer(true, false);
//        
//        // tooltips
//        final Locale locale = Locale.ENGLISH;
//        for(final String eventName : eventIndexes.keySet())
//        {
//            XYToolTipGenerator ttGenerator = new XYToolTipGenerator()
//            {
//                public String generateToolTip(XYDataset dataset, int series, int item)
//                {
//                    StringBuffer sb = new StringBuffer();
//                    Number x = dataset.getX(series, item);
//                    Number y = dataset.getY(series, item);
//                    Date time = new Date((Long)x);
//                    sb.append("<html><p style='color:#ff0000;'>");
//                    sb.append(eventName);
//                    sb.append(":</p>");
//                    sb.append(time);
//                    sb.append(" <br />");
//                    sb.append(String.format(locale, "%.2f</html>", y.doubleValue()));
//                    return sb.toString();
//                }
//            };
//
//            Integer idx = eventIndexes.get(eventName);
//            this.renderer.setSeriesToolTipGenerator(idx, ttGenerator);
//        }
//        ToolTipManager.sharedInstance().setDismissDelay(1000 * 100);
//        UIManager.put("ToolTip.background", new Color(0.9f, 0.9f, 0.9f));
//        UIManager.put("ToolTip.font", null);
//
//        // Format chart domain (x-axis)
//        DateAxis timeAxis = new DateAxis("Time");
//        timeAxis.setAutoRange(true);
//        timeAxis.setLowerMargin(0.0);
//        timeAxis.setUpperMargin(0.0);
//        timeAxis.setTickLabelsVisible(true);
//        timeAxis.setAutoRange(true);
//        
//        // Format event mean time
//        NumberAxis meanRange = new NumberAxis("Event Time (ms)");
//        meanRange.setAutoRange(true);
//        meanRange.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
//        XYPlot meanPlot = new XYPlot(meanTimesMoving, timeAxis, meanRange, renderer);
//
//        // Format event volume (N/s) 
//        NumberAxis volumeRange = new NumberAxis("Volume (N/s)");
//        volumeRange.setAutoRange(true);
//        XYPlot volumePlot = new XYPlot(volumesMoving, timeAxis, volumeRange, renderer);
//
//        CombinedDomainXYPlot plot = new CombinedDomainXYPlot(timeAxis);
//        plot.add(meanPlot, 80);
//        plot.add(volumePlot, 20);
//        plot.setGap(plot.getGap()*10);
//        
//        JFreeChart chart = new JFreeChart(
//                resultService.getDataLocation(),
//                plot);
//        chart.setBackgroundPaint(Color.white);
//        
//        // Done
//        return chart;
//    }
//    
//    /**
//     * 
//     * @param resultCount               number of data points to plot
//     * @param smoothingCount            number of data points to keep for smoothing
//     * @param startPercent              the start time as a percentage of the overall time
//     * @param endPercent                the end time as a percentage of the overall time
//     */
//    public void showXYChart(int resultCount, int smoothingCount, int startPercent, int endPercent, String notes)
//    {
//        long minStartTime = resultService.getMinStartTime();
//        long maxStartTime = resultService.getMaxStartTime();
//        long delta = maxStartTime - minStartTime;
//        long chartStartTime = minStartTime;
//        long chartEndTime = maxStartTime;
//        if (delta > 0)
//        {
//            chartStartTime = minStartTime + (long)((double)startPercent/100.0*(double)delta);
//            chartEndTime = minStartTime + (long)((double)endPercent/100.0*(double)delta);
//        }
//        
//        JFreeChart chart = createXYChart(chartStartTime, chartEndTime, resultCount, smoothingCount);
//        
//        // Graph and toggles
//        JPanel resultsPanel = new JPanel(new BorderLayout());
//
//        ChartPanel chartPanel = new ChartPanel(chart);
//        chartPanel.setBorder(BorderFactory.createCompoundBorder(
//                BorderFactory.createEmptyBorder(4, 4, 4, 4),
//                BorderFactory.createLineBorder(Color.black)));
//        
//        // Show/hide individual series
//        JPanel togglePanel = new JPanel();
//        BoxLayout togglePanelLayout = new BoxLayout(togglePanel, BoxLayout.Y_AXIS);
//        togglePanel.setLayout(togglePanelLayout);
//        for(String eventName : eventIndexes.keySet())
//        {
//            JCheckBox box = new JCheckBox(eventName); 
//            box.setActionCommand(eventName); 
//            box.addActionListener(new ActionListener()
//            {
//                @Override
//                public void actionPerformed(ActionEvent e)
//                {
//                    handleToggle(e);
//                }
//            }); 
//            box.setSelected(true);
//            togglePanel.add(box);
//        }
//        JScrollPane togglePanelScroll = new JScrollPane(
//                togglePanel,
//                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
//                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
//        
//        resultsPanel.add(chartPanel, BorderLayout.CENTER);
//        resultsPanel.add(togglePanelScroll, BorderLayout.WEST);
//
//        final JFrame frame = new JFrame();
//        frame.setLayout(new BorderLayout());
//        frame.getContentPane().add(resultsPanel, BorderLayout.CENTER);
//
//        frame.setBounds(200, 120, 1024, 768);
//        frame.setVisible(true);
//        
//        final CountDownLatch closeLatch = new CountDownLatch(1);
//        // Wait for the window to be killed
//        frame.addWindowListener(new WindowAdapter()
//        {
//            @Override
//            public void windowClosing(WindowEvent e)
//            {
//                closeLatch.countDown();
//            }
//
//            @Override
//            public void windowClosed(WindowEvent e)
//            {
//                closeLatch.countDown();
//            }
//        });
//        
//        // Wait for the chart to close
//        try
//        {
//            closeLatch.await();
//        }
//        catch (InterruptedException e)
//        {
//        }
//        frame.setVisible(false);
//        frame.dispose();
//    }
//    
//    /** 
//     * Handler for series toggle checkboxes.
//     * 
//     * @param e the action event. 
//     */ 
//    public void handleToggle(ActionEvent e)
//    {
//        String eventName = e.getActionCommand();
//        toggleSeries(eventName);
//    }
//    
//    /**
//     * Toggle whether a series is shown or not.
//     * 
//     * @param eventName
//     */
//    private void toggleSeries(String eventName)
//    {
//        Integer index = eventIndexes.get(eventName);
//        if(index != null)
//        {
//            int series = index.intValue();
//            if(series >= 0)
//            {
//                boolean visible = this.renderer.getItemVisible(series, 0); 
//                this.renderer.setSeriesVisible(series, new Boolean(!visible));
//            }
//        }
//    }
//    
//    /**
//     * From http://www.jfree.org/phpBB2/viewtopic.php?f=3&t=37814
//     *
//     */
//    public class APXYLineAndShapeRenderer extends XYLineAndShapeRenderer
//    {
//        public APXYLineAndShapeRenderer()
//        {
//            super();
//        }
//
//        public APXYLineAndShapeRenderer(boolean lines, boolean shapes)
//        {
//            super(lines, shapes);
//        }
//
//        private static final long serialVersionUID = 1L; // <- eclipse insists on this and I hate warnings ^^
//
//        @Override
//        protected void addEntity(EntityCollection entities, Shape area, XYDataset dataset, int series, int item, double entityX, double entityY)
//        {
//            if(area != null && (area.getBounds().width < 20 || area.getBounds().height < 20))
//            {
//                super.addEntity(entities, null, dataset, series, item, entityX, entityY);
//            }
//            else
//            {
//                super.addEntity(entities, area, dataset, series, item, entityX, entityY);
//            }
//        }
//    }
}
