/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software
 * License version 1.1, a copy of which has been included with this
 * distribution in the LICENSE.txt file.  */
package fr.esrf.logviewer;

import java.lang.Runtime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.table.AbstractTableModel;
import org.apache.log4j.Level;
import org.apache.log4j.Logger; 

/**
 * Represents a list of <code>EventDetails</code> objects that are sorted on
 * logging time. Methods are provided to filter the events that are visible.
 *
 * @author <a href="mailto:oliver@puppycrawl.com">Oliver Burn</a>
 */
public class MyTableModel extends AbstractTableModel {
    
    /** Free memory in % (free/total) */
    private static double freeMemoryThreshold = 10;
    
    /** Last amont of free memory */
    private static double lastFreeMemory = 0;

    /** # logs threshold */
    private static long  nEventsThreshold = 50000;
    
    /** events update time threshold in ms */
    private static final int filteredEventsUpdadetTimeThreshold = 100;

    /** pending events threshold */
    private static final int pendingEventsThreshold = 4096;
    
    /** remove 25% of the oldest events if saturated */
    private static final double eventsPercent = 0.50;
    
    /** used to log messages **/
    private static final Logger LOG = Logger.getLogger(MyTableModel.class);
    
    /** use the compare logging events **/
    private static final Comparator MY_COMP = new Comparator() {
        /** @see Comparator **/
        public int compare(Object aObj1, Object aObj2) {
            if ((aObj1 == null) && (aObj2 == null)) {
                return 0; // treat as equal
            } else if (aObj1 == null) {
                return -1; // null less than everything
            } else if (aObj2 == null) {
                return 1; // think about it. :->
            }
            
            // will assume only have LoggingEvent
            final EventDetails le1 = (EventDetails) aObj1;
            final EventDetails le2 = (EventDetails) aObj2;
            
            if (le1.getTimeStamp() < le2.getTimeStamp()) {
                return 1;
            }
            // assume not two events are logged at exactly the same time
            return -1;
        }
    };
    
    /**
     * Helper that actually processes incoming events.
     * @author <a href="mailto:oliver@puppycrawl.com">Oliver Burn</a>
     */
    private class Processor implements Runnable {
        
        /** time to update the filtered list **/
        private long lastUpdateTime = 0;
        
        /** loops getting the events **/
        public void run() {
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // ignore
                }
                synchronized (mLock) {
                    if (mPaused && mPendingEvents.size() < pendingEventsThreshold) {
                      continue;
                    }
                    
                    boolean needUpdate = false;    
                    
                    Runtime rt = Runtime.getRuntime();
                    
                    double freeMemory = 100.0 *
                                        (double)rt.freeMemory() /  
                                        (double)rt.totalMemory();
                    
                    LOG.debug("Last free memory    [%]: " + lastFreeMemory);
                    LOG.debug("Current free memory [%]: " + freeMemory);
                   
                    if (
                         ( 
                           (lastUpdateTime > filteredEventsUpdadetTimeThreshold) 
                            && 
                           (mAllEvents.size() > nEventsThreshold)
                         )
                          ||
                         ( 
                           ((freeMemory - lastFreeMemory) > 5)
                            && 
                           (freeMemory < freeMemoryThreshold)
                         )
                       )
                    {      
                        clearOldEvents();
                        mFilteredEvents = new EventDetails[0];
                        Runtime.getRuntime().gc();
                        lastFreeMemory = 100.0 *
                                        (double)rt.freeMemory() /  
                                        (double)rt.totalMemory();
                        needUpdate = true;
                    } 
                    else {
                        lastFreeMemory = freeMemory;
                    }
                    final Iterator it = mPendingEvents.iterator();
                    boolean toHead = true; // were events added to head
                    while (it.hasNext()) {
                        final EventDetails event = (EventDetails) it.next();
                        mAllEvents.add(event);
                        toHead = toHead && (event == mAllEvents.first());
                        needUpdate = needUpdate || matchFilter(event);
                    }
                    mPendingEvents.clear();
                    if (needUpdate) {
                        lastUpdateTime = updateFilteredEvents(toHead);
                    }
                }
            }
        }
    }
    
        /** names of the columns in the table **/
         private static final String[] COL_NAMES = {
            "Trace", "Time", "Level", "Source", "Message"
        };
        
        /** definition of an empty list **/
        private static final EventDetails[] EMPTY_LIST =  new EventDetails[] {};
     
        /** the lock to control access **/
        private final Object mLock = new Object();
        /** set of all logged events - not filtered **/
        private final SortedSet mAllEvents = new TreeSet(MY_COMP);
        /** events that are visible after filtering **/
        private EventDetails[] mFilteredEvents = EMPTY_LIST;
        /** list of events that are buffered for processing **/
        private final List mPendingEvents = new ArrayList();
        /** indicates whether event collection is paused to the UI **/
        private boolean mPaused = false;
        
        /** filter for the timestamp **/
        private String mTimeStampFilter = "";
        /** filter for the thread **/
        private String mThreadFilter = "";
        /** filter for the message **/
        private String mMessageFilter = "";
        /** filter for the category **/
        private String mCategoryFilter = "";
        /** filter for the level **/
        private Level mLevelFilter = Level.DEBUG;
        
        private final javax.swing.JCheckBox mTrue  = new javax.swing.JCheckBox("", true);
        private final javax.swing.JCheckBox mFalse = new javax.swing.JCheckBox("", false);
        
        /**
         * Creates a new <code>MyTableModel</code> instance.
         *
         */
        public MyTableModel() {
            final Thread t = new Thread(new Processor());
            t.setDaemon(true);
            t.start();
        }
        
        
        ////////////////////////////////////////////////////////////////////////////
        // Table Methods
        ////////////////////////////////////////////////////////////////////////////
        
        /** @see javax.swing.table.TableModel **/
        public int getRowCount() {
            synchronized (mLock) {
                return mFilteredEvents.length;
            }
        }
        
        /** @see javax.swing.table.TableModel **/
        public int getColumnCount() {
            // does not need to be synchronized
            return COL_NAMES.length;
        }
        
        /** @see javax.swing.table.TableModel **/
        public String getColumnName(int aCol) {
            // does not need to be synchronized
            return COL_NAMES[aCol];
        }
        
        /** @see javax.swing.table.TableModel **/
        public Class getColumnClass(int aCol) {
            // does not need to be synchronized
            //return (aCol == 0) ? Boolean.class : 
                return Object.class;
        }
        
        /** @see javax.swing.table.TableModel **/
        public Object getValueAt(int aRow, int aCol) {
            synchronized (mLock) {
                final EventDetails event = mFilteredEvents[aRow];
                if (aCol == 0) {
                    return (event.getThrowableStrRep() == null) ? Boolean.FALSE : Boolean.TRUE;
                } else if (aCol == 1) {
                    return event.getRenderedTimeStamp();
                } else if (aCol == 2) {
                    return event.getLevel();
                } else if (aCol == 3) {
                    return event.getCategoryName();
                } 
                return event.getMessage();
            }
        }
        
        ////////////////////////////////////////////////////////////////////////////
        // Public Methods
        ////////////////////////////////////////////////////////////////////////////
        
        /**
         * Sets the level to filter events on. Only events of equal or higher
         * property are now displayed.
         *
         * @param aLevel the level to filter on
         */
        public void setLevelFilter(Level aLevel) {
            synchronized (mLock) {
                mLevelFilter = aLevel;
                updateFilteredEvents(false);
            }
        }
        
        /**
         * Set the filter for the thread field.
         *
         * @param aStr the string to match
         */
        public void setThreadFilter(String aStr) {
            synchronized (mLock) {
                mThreadFilter = aStr.trim();
                updateFilteredEvents(false);
            }
        }
        
        /**
         * Set the filter for the message field.
         *
         * @param aStr the string to match
         */
        public void setMessageFilter(String aStr) {
            synchronized (mLock) {
                mMessageFilter = aStr.trim();
                updateFilteredEvents(false);
            }
        }

        /**
         * Set the filter for the time field.
         *
         * @param aStr the string to match
         */
        public void setTimeStampFilter(String aStr) {
            synchronized (mLock) {
                mTimeStampFilter = aStr.trim();
                updateFilteredEvents(false);
            }
        }

        /**
         * Set the filter for the category field.
         *
         * @param aStr the string to match
         */
        public void setCategoryFilter(String aStr) {
            synchronized (mLock) {
                mCategoryFilter = aStr.trim();
                updateFilteredEvents(false);
            }
        }
        
        /**
         * Add an event to the list.
         *
         * @param aEvent a <code>EventDetails</code> value
         */
        public void addEvent(EventDetails aEvent) {
            synchronized (mLock) {
               mPendingEvents.add(aEvent);
            }
        }
        
        /**
         * Clear the list of all events.
         */
        public void clear() {
            synchronized (mLock) {
                mAllEvents.clear();
                mFilteredEvents = new EventDetails[0];
                mPendingEvents.clear();
                fireTableDataChanged();
            }
        }
        
        /**
         * Clear olddest events.
         */
        public void clearOldEvents() {
            //-assume we hold mLock
            int n = mAllEvents.size();
            LOG.debug("In clearOldEvents: #before:" +  n);
            int n_to_clear = (int)(n * eventsPercent);
            if (n_to_clear <= 0) {
               return; 
            }
            int remaining = (int)((1.0 - eventsPercent) * (double)n);
            LOG.debug("In clearOldEvents: #to-clear:" +  n_to_clear);
            Iterator it = mAllEvents.iterator();
            int i;
            int n_skip = n - n_to_clear;
            for (i = 0; i < n_skip && it.hasNext(); i++) {
                it.next();
            }
            for (i = 0; i < n_to_clear && it.hasNext(); i++) {
                EventDetails ed = (EventDetails)it.next();
                if (!ed.getLevel().isGreaterOrEqual(Level.WARN)) {
                    it.remove();
                }
            }
            LOG.debug("In clearOldEvents: #after:" + mAllEvents.size());
        }
        
        /** Toggle whether collecting events **/
        public void toggle() {
            synchronized (mLock) {
                mPaused = !mPaused;
            }
        }
        
        /** @return whether currently paused collecting events **/
        public boolean isPaused() {
            synchronized (mLock) {
                return mPaused;
            }
        }
        
        /**
         * Get the throwable information at a specified row in the filtered events.
         *
         * @param aRow the row index of the event
         * @return the throwable information
         */
        public EventDetails getEventDetails(int aRow) {
            synchronized (mLock) {
                return mFilteredEvents[aRow];
            }
        }
        
        ////////////////////////////////////////////////////////////////////////////
        // Private methods
        ////////////////////////////////////////////////////////////////////////////
        
        /**
         * Update the filtered events data structure.
         * @param aInsertedToFront indicates whether events were added to front of
         *        the events. If true, then the current first event must still exist
         *        in the list after the filter is applied.
         */
        private long updateFilteredEvents(boolean aInsertedToFront) {
            final long start = System.currentTimeMillis();
            final List filtered = new ArrayList();
            final int size = mAllEvents.size();
            final Iterator it = mAllEvents.iterator();
            
            while (it.hasNext()) {
                final EventDetails event = (EventDetails) it.next();
                if (matchFilter(event)) {
                    filtered.add(event);
                }
            }
            
            final EventDetails lastFirst = (mFilteredEvents.length == 0)
                                           ? null
                                           : mFilteredEvents[0];
            mFilteredEvents = (EventDetails[]) filtered.toArray(EMPTY_LIST);
            
            if (aInsertedToFront && (lastFirst != null)) {
                final int index = filtered.indexOf(lastFirst);
                if (index < 1) {
                    LOG.warn("In strange state");
                    fireTableDataChanged();
                } else {
                    fireTableRowsInserted(0, index - 1);
                }
            } else {
                fireTableDataChanged();
            }
            
            final long end = System.currentTimeMillis();
            
            LOG.debug("# logs: " + size);
            LOG.debug("Update time [ms]: " + (end - start));
            
            return end - start;
        }
        
        /**
         * Returns whether an event matches the filters.
         *
         * @param aEvent the event to check for a match
         * @return whether the event matches
         */
        private boolean matchFilter(EventDetails aEvent) {
            if (
                  aEvent.getLevel().isGreaterOrEqual(mLevelFilter) 
                    &&
                  (aEvent.getCategoryName().indexOf(mCategoryFilter) >= 0) 
                    &&
                  (aEvent.getRenderedTimeStamp().indexOf(mTimeStampFilter) >= 0) 
                    &&
                  (aEvent.getThreadName().indexOf(mThreadFilter) >= 0) 
               ) 
            {
                final String rm = aEvent.getMessage();
                if (rm == null) {
                    // only match if we have not filtering in place
                    return (mMessageFilter.length() == 0);
                } else {
                    return (rm.indexOf(mMessageFilter) >= 0);
                }
            }
            return false; // by default not match
        }
}
