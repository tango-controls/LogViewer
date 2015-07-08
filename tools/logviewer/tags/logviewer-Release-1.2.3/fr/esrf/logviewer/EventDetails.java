/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software
 * License version 1.1, a copy of which has been included with this
 * distribution in the LICENSE.txt file.  */
package fr.esrf.logviewer;

import java.text.DateFormat;
import java.util.Date;
import java.sql.Timestamp;

import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Represents the details of a logging event. It is intended to overcome the
 * problem that a LoggingEvent cannot be constructed with purely fake data.
 *
 * @author <a href="mailto:oliver@puppycrawl.com">Oliver Burn</a>
 * @version 1.0
 */
public class EventDetails {
    /** the time of the event **/
    private final long mTimeStamp;
    /** the time of the event (as string) **/
    private final String mTimeStampStr;
    /** the priority of the event **/
    private final Level mLevel;
    /** the category of the event **/
    private final String mCategoryName;
    /** the NDC for the event **/
    private final String mNDC;
    /** the thread for the event **/
    private final String mThreadName;
    /** the msg for the event **/
    private final String mMessage;
    /** the throwable details the event **/
    private final String[] mThrowableStrRep;
    /** the location details for the event **/
    private final String mLocationDetails;

    /** used to format dates **/
    private static final DateFormat mDateFormatter =
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
    
    /**
     * Creates a new <code>EventDetails</code> instance.
     * @param aTimeStamp a <code>long</code> value
     * @param aPriority a <code>Priority</code> value
     * @param aCategoryName a <code>String</code> value
     * @param aNDC a <code>String</code> value
     * @param aThreadName a <code>String</code> value
     * @param aMessage a <code>String</code> value
     * @param aThrowableStrRep a <code>String[]</code> value
     * @param aLocationDetails a <code>String</code> value
     */
    public EventDetails( long aTimeStamp,
                         Level aLevel,
                         String aCategoryName,
                         String aNDC,
                         String aThreadName,
                         String aMessage,
                         String[] aThrowableStrRep,
                         String aLocationDetails)
    {
        mTimeStamp = aTimeStamp;
        Timestamp tms = new Timestamp(mTimeStamp);
        mTimeStampStr = mDateFormatter.format(new Date(mTimeStamp)) + "." + (tms.getNanos() / 1000000);    
        mLevel = aLevel;
        mCategoryName = aCategoryName;
        mNDC = aNDC;
        mThreadName = aThreadName;
        mMessage = aMessage;
        mThrowableStrRep = aThrowableStrRep;
        mLocationDetails = aLocationDetails;
    } 

    /**
     * Creates a new <code>EventDetails</code> instance.
     *
     * @param aEvent a <code>LoggingEvent</code> value
     */
    EventDetails(LoggingEvent anEvent) {
        this(anEvent.timeStamp,
             anEvent.getLevel(),
             anEvent.getLoggerName(),
             anEvent.getNDC(),
             anEvent.getThreadName(),
             anEvent.getRenderedMessage(),
             anEvent.getThrowableStrRep(),
             (anEvent.getLocationInformation() == null)
             ? null : anEvent.getLocationInformation().fullInfo);
    }

    /** @see #mTimeStamp **/
    long getTimeStamp() {
        return mTimeStamp;
    }
    
    String getRenderedTimeStamp() {
        return mTimeStampStr;
    }
    
    /** @see #mPriority **/
    Level getLevel() {
        return mLevel;
    }

    /** @see #mCategoryName **/
    String getCategoryName() {
        return mCategoryName;
    }

    /** @see #mNDC **/
    String getNDC() {
        return mNDC;
    }

    /** @see #mThreadName **/
    String getThreadName() {
        return mThreadName;
    }

    /** @see #mMessage **/
    String getMessage() {
        return mMessage;
    }

    /** @see #mLocationDetails **/
    String getLocationDetails(){
        return mLocationDetails;
    }

    /** @see #mThrowableStrRep **/
    String[] getThrowableStrRep() {
        return mThrowableStrRep;
    }
}
