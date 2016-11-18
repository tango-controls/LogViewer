/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software
 * License version 1.1, a copy of which has been included with this
 * distribution in the LICENSE.txt file.  */
package fr.esrf.logviewer;

import java.util.*;

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

    /** used to format dates
    private static final DateFormat mDateFormatter =
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
    **/
    //===============================================================
    public static String formatDate(long ms) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(ms);
        int day    = calendar.get(Calendar.DAY_OF_MONTH);
        int month  = calendar.get(Calendar.MONTH)+1;
        int year   = calendar.get(Calendar.YEAR)-2000;
        int hour   = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes= calendar.get(Calendar.MINUTE);
        int seconds= calendar.get(Calendar.SECOND);
        int millis = calendar.get(Calendar.MILLISECOND);
        return String.format("%02d/%02d/%02d  %02d:%02d:%02d.%03d",
                day, month, year, hour, minutes, seconds, millis);
    }
    //===============================================================

    /**
     * Creates a new <code>EventDetails</code> instance.
     * @param aTimeStamp a <code>long</code> value
     * @param aLevel a <code>Priority</code> value
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
        /****
        Timestamp tms = new Timestamp(aTimeStamp);
        mTimeStampStr = mDateFormatter.format(new Date(aTimeStamp))
                + "." + String.format("%03d", (tms.getNanos() / 1000000));
        ****/
        mTimeStampStr = formatDate(aTimeStamp);
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
     * @param anEvent a <code>LoggingEvent</code> value
     */
    @SuppressWarnings("unused")
    EventDetails(LoggingEvent anEvent) {
        this(anEvent.timeStamp,
                anEvent.getLevel(),
                anEvent.getLoggerName(),
                anEvent.getNDC(),
                anEvent.getThreadName(),
                anEvent.getRenderedMessage(),
                anEvent.getThrowableStrRep(),
                (anEvent.getLocationInformation()==null)
                        ? null : anEvent.getLocationInformation().fullInfo);
    }

    /** @see #mTimeStamp **/
    long getTimeStamp() {
        return mTimeStamp;
    }
    
    String getRenderedTimeStamp() {
        return mTimeStampStr;
    }
    
    /** @see #mLevel **/
    Level getLevel() {
        return mLevel;
    }

    /** @see #mCategoryName **/
    String getCategoryName() {
        return mCategoryName;
    }

    /** @see #mNDC **/
    @SuppressWarnings("unused")
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
    @SuppressWarnings("unused")
    String getLocationDetails(){
        return mLocationDetails;
    }

    /** @see #mThrowableStrRep **/
    String[] getThrowableStrRep() {
        return mThrowableStrRep;
    }
}
