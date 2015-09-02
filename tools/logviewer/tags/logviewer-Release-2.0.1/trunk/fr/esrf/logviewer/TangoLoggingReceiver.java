/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 * Copyright (C) The TANGO Team.
 *
 * This software is published under the terms of the Apache Software
 * License version 1.1, a copy of which has been included with this
 * distribution in the LICENSE.txt file.  */

package fr.esrf.logviewer;

// Java stuffs

import fr.esrf.Tango.DevFailed;
import fr.esrf.Tango.DevVarLongStringArray;
import fr.esrf.TangoApi.*;
import fr.esrf.tangoatk.core.ATKException;
import org.apache.log4j.Logger;
import org.tango.logconsumer.LogConsumer;
import org.tango.logconsumer.SourceSet;
import org.tango.server.ServerManager;

import javax.swing.*;
import java.net.InetAddress;
import java.util.List;
import java.util.Random;

// TANGO stuffs
// Log4j stuffs
// LogConsumer stuffs
// ATK stuffs

/**
 * A daemon thread the processes connections from a
 * <code>org.apache.log4j.net.SocketAppender.html</code>.
 *
 * @author <a href="mailto:oliver@puppycrawl.com">Oliver Burn</a>
 */
class TangoLoggingReceiver extends Thread {

    /**
     * used to log messages *
     */
    private static final Logger LOG = Logger.getLogger(TangoLoggingReceiver.class);

    /**
     * the LogConsumer device name prefix *
     */
    private static final String DYN_DEV_NAME_PREFIX = "tmp/log/";

    /**
     * the LogConsumer device name *
     */
    private String deviceName;

    /**
     * a map to store the logging sources *
     */
    private static final SourceSet mSet = SourceSet.instance();

    /**
     * true if a device name is provided *
     */
    private boolean mRunningInStaticMode = false;

    /**
     * the logging source device property name *
     */
    private static final String LOG_SRC_PROPERTY = "logging_source";

    /**
     * the history area *
     */
    private HistoryArea mHistoryArea;

    private String instanceName;
    private static final String className = "LogConsumer";

    //=========================================================================

    TangoLoggingReceiver(String[] aArgs, MyTableModel aModel, HistoryArea aHistoryArea) throws Exception {
        setDaemon(true);
        mHistoryArea = aHistoryArea;
        try {
            String[] args;
            boolean instanceNameProvided = true;
            final Random rand = new Random(System.currentTimeMillis());
            final String dev_name_suffix = "@" + rand.nextInt(128);
            // Does the user provide an instance name?
            if (aArgs.length==0 || !aArgs[0].contains("/")) {
                LOG.debug("No instance name provided.");
                instanceNameProvided = false;
                // Set both server and device name
                String hostname = InetAddress.getLocalHost().getHostName();
                String[] tokens = hostname.split(".");
                if (tokens.length!=0) {
                    hostname = tokens[0];
                }
                instanceName = hostname + dev_name_suffix;
                deviceName = DYN_DEV_NAME_PREFIX + instanceName;
            } else {
                LOG.debug("Provided device name: " + aArgs[0]);
                deviceName = aArgs[0];
                int pos = deviceName.lastIndexOf('/');
                if (pos==-1) {
                    LOG.fatal("Invalid device name specified. Quiting");
                    JOptionPane.showMessageDialog(
                            null,
                            "Invalid device name specified. Quiting",
                            "Tango Log Viewer",
                            JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
                instanceName = deviceName.substring(pos + 1);
                mRunningInStaticMode = true;
            }
            String serverName = className + "/" + instanceName;
            LOG.debug("Device name is " + deviceName);
            // Register the device into the database
            LOG.debug("Registering the LogConsumer into the database");
            DbDevInfo dinfo = new DbDevInfo(deviceName, "LogConsumer", serverName);
            ApiUtil.get_db_obj().add_device(dinfo);


            //- Instantiate args
             if (instanceNameProvided) {
                args = new String[aArgs.length];
            } else {
                args = new String[aArgs.length + 1];
            }
            args[0] = instanceName;

            // Add remaining user arguments
            int i = (instanceNameProvided) ? 1 : 0;
            for (int j = 1 ; i<aArgs.length ; i++, j++) {
                args[j] = aArgs[i];
            }

            // - Normal TANGO device startup
            LOG.debug("Initializing the util singleton");
            LOG.debug("Passing following args to Util.init:");
            for (i = 0; i<args.length ; i++) {
                LOG.debug("\targs[" + String.valueOf(i) + "]: " + args[i]);
            }

            LOG.debug("Starting " + serverName);
            ServerManager.getInstance().start(args, LogConsumer.class);
            LogConsumer consumer = LogConsumer.getDeviceInstance(deviceName);
            consumer.setModel(aModel);

            // - Restore sources
            if (mRunningInStaticMode) {
                getLoggingSourceProperty();
            }
        } catch (Exception e) {
            displayException(e);
        }
    }

    //=========================================================================
    public void setTableModel(MyTableModel aModel) {
        LogConsumer.getDeviceInstance(deviceName).setModel(aModel);
    }

    //=========================================================================
    public void setHistoryArea(HistoryArea aHistoryArea) {
        mHistoryArea = aHistoryArea;
    }

    //=========================================================================
    public String getDeviceName() {
        return deviceName;
    }
    //=========================================================================
    public boolean isRunningInStaticMode() {
        return mRunningInStaticMode;
    }
    //=========================================================================
    public void run() {
         LOG.debug("Device server started");
    }
    //=========================================================================
    private String[] getDeviceList(String pattern) {
        String[] dev_list = new String[0];
        try {
            DeviceData argin = new DeviceData();
            argin.insert(pattern);
            DeviceData argout = ApiUtil.get_db_obj().command_inout("DbGetDeviceExportedList", argin);
            dev_list = argout.extractStringArray();
        } catch (Exception e) {
            displayException(e);
        }
        return dev_list;
    }
    //=========================================================================
    @SuppressWarnings("unused")
    public int addLoggingSources(String pattern) {
        int result = 0;
        String[] deviceList = getDeviceList(pattern);
        for (String device : deviceList) {
            if (addLoggingSource(device)==-1) {
                result = -1;
            }
        }
        return result;
    }
    //=========================================================================
    public int addCoLocatedSources(String dev_name) {
        try {
            final DeviceProxy deviceProxy = new DeviceProxy(dev_name);
            final DeviceProxy adminProxy = new DeviceProxy(deviceProxy.adm_name());
            final String[] argIn = {"*", "device::" + deviceName};
            DeviceData deviceData = new DeviceData();
            deviceData.insert(argIn);
            adminProxy.command_inout("AddLoggingTarget", deviceData);
            String server_name = deviceProxy.adm_name().substring(deviceProxy.adm_name().indexOf('/') + 1);
            LOG.debug("TangoLoggingReceiver::addCoLocatedSources::server_name is " + server_name);
            String[] dev_list = ApiUtil.get_db_obj().get_device_class_list(server_name);
            for (int i = 0 ; i<dev_list.length ; i += 2) {
                LOG.debug("TangoLoggingReceiver::addCoLocatedSources::mSet adding " + dev_list[i]);
                mSet.add(dev_list[i]);
            }
            LOG.debug("Logging sources set contains " + mSet.size() + " entries");
        } catch (Exception e) {
            displayException(e);
            return -1;
        }
        return 0;
    }
    //=========================================================================
    public int addLoggingSource(String dev_name) {
        try {
            LOG.debug("Adding " + dev_name + " to logging sources list");
            final DeviceProxy deviceProxy = new DeviceProxy(dev_name);
            final DeviceProxy adminProxy = new DeviceProxy(deviceProxy.adm_name());
            final String[] argin = {dev_name, "device::" + deviceName};
            DeviceData dd = new DeviceData();
            dd.insert(argin);
            adminProxy.command_inout("AddLoggingTarget", dd);
            mSet.add(dev_name);
            LOG.debug("Logging sources set contains " + mSet.size() + " entries");
        } catch (Exception e) {
            displayException(e);
            return -1;
        }
        return 0;
    }
    //=========================================================================
    @SuppressWarnings("unused")
    public int addTangoCoreLogger(String dev_name) {
        try {
            final DeviceProxy deviceProxy = new DeviceProxy(dev_name);
            final DeviceProxy adminProxy = new DeviceProxy(deviceProxy.adm_name());
            LOG.debug("Adding " + adminProxy.get_name() + " to logging sources list");
            final String[] argin = {adminProxy.get_name(), "device::" + deviceName};
            DeviceData dd = new DeviceData();
            dd.insert(argin);
            adminProxy.command_inout("AddLoggingTarget", dd);
            mSet.add(dev_name);
            LOG.debug("Logging sources set contains " + mSet.size() + " entries");
        } catch (Exception e) {
            displayException(e);
            return -1;
        }
        return 0;
    }
    //=========================================================================
    @SuppressWarnings("unused")
    public int removeLoggingSources(String pattern) {
        int result = 0;
        String[] deviceList = getDeviceList(pattern);
        for (String device : deviceList) {
            if (removeLoggingSource(device)==-1) {
                result = -1;
            }
        }
        return result;
    }
    //=========================================================================

    public int removeLoggingSource(String devName) {
        return removeLoggingSource(devName, true);
    }
    //=========================================================================

    private int removeLoggingSource(String dev_name, boolean removeFromSet) {
        try {
            LOG.debug("Removing " + dev_name + " from logging sources list");
            final DeviceProxy dp = new DeviceProxy(dev_name);
            final DeviceProxy dsp = new DeviceProxy(dp.adm_name());
            final String[] argin = {dev_name, "device::" + deviceName};
            DeviceData dd = new DeviceData();
            dd.insert(argin);
            dsp.command_inout("RemoveLoggingTarget", dd);
            if (removeFromSet) {
                mSet.remove(dev_name);
                LOG.debug("Logging sources set contains " + mSet.size() + " entries");
            }
        } catch (Exception e) {
            displayException(e);
            return -1;
        }
        return 0;
    }
    //=========================================================================
    @SuppressWarnings("unused")
    public int removeTangoCoreLogger(String dev_name) {
        try {
            final DeviceProxy dp = new DeviceProxy(dev_name);
            final DeviceProxy dsp = new DeviceProxy(dp.adm_name());
            LOG.debug("Removing " + dsp.get_name() + " from logging sources list");
            final String[] argin = {dsp.get_name(), "device::" + deviceName};
            DeviceData dd = new DeviceData();
            dd.insert(argin);
            dsp.command_inout("RemoveLoggingTarget", dd);
            mSet.remove(dsp.get_name());
            LOG.debug("Logging sources set contains " + mSet.size() + " entries");
        } catch (Exception e) {
            displayException(e);
            return -1;
        }
        return 0;
    }
    //=========================================================================

    public int removeCoLocatedSources(String dev_name) {
        return removeCoLocatedSources(dev_name, true);
    }
    //=========================================================================

    public int removeCoLocatedSources(String dev_name, boolean removeFromSet) {
        try {
            final DeviceProxy dp = new DeviceProxy(dev_name);
            final DeviceProxy dsp = new DeviceProxy(dp.adm_name());
            final String[] argin = {"*", "device::" + deviceName};
            DeviceData dd = new DeviceData();
            dd.insert(argin);
            dsp.command_inout("RemoveLoggingTarget", dd);
            if (removeFromSet) {
                String server_name = dp.adm_name().substring(dp.adm_name().indexOf('/') + 1);
                LOG.debug("TangoLoggingReceiver::removeCoLocatedSources::server_name is " + server_name);
                String[] dev_list = ApiUtil.get_db_obj().get_device_class_list(server_name);
                for (int i = 0 ; i<dev_list.length ; i += 2) {
                    LOG.debug("TangoLoggingReceiver::removeCoLocatedSources::mSet removing " + dev_list[i]);
                    mSet.remove(dev_list[i]);
                }
                LOG.debug("Logging sources set contains " + mSet.size() + " entries");
            }
        } catch (Exception e) {
            displayException(e);
            return -1;
        }
        return 0;
    }
    //=========================================================================
    public boolean removeAllSources() {
        LOG.debug("Removing all logging sources");
        List<String> contentList = mSet.content();
        boolean failed = false;
        for (String content : contentList) {
            if (removeCoLocatedSources(content, false)==-1) {
                failed = true;
            }
        }
        mSet.clear();
        return !failed;
    }
    //=========================================================================

    public int getDeviceLoggingLevel(String dev_name) {
        try {
            final DeviceProxy dp = new DeviceProxy(dev_name);
            final DeviceProxy dsp = new DeviceProxy(dp.adm_name());
            final String[] dvsa = {dev_name};
            DeviceData ddin = new DeviceData();
            ddin.insert(dvsa);
            DeviceData ddout = dsp.command_inout("GetLoggingLevel", ddin);
            DevVarLongStringArray dvlsa = ddout.extractLongStringArray();
            if (dvlsa.lvalue.length>0) {
                return dvlsa.lvalue[0];
            }
        } catch (Exception e) {
            displayException(e);
            return -1;
        }
        return 0;
    }
    //=========================================================================
    @SuppressWarnings("unused")
    public int setDevicesLoggingLevel(String pattern, int level) {
        int result = 0;
        String[] deviceList = getDeviceList(pattern);
        for (String device : deviceList) {
            if (setDeviceLoggingLevel(device, level, false)==-1) {
                result = -1;
            }
        }
        return result;
    }
    //=========================================================================
    public int setDeviceLoggingLevel(String devName, int level, boolean add_before) {
        try {
            final DeviceProxy deviceProxy = new DeviceProxy(devName);
            final DeviceProxy adminProxy = new DeviceProxy(deviceProxy.adm_name());
            if (add_before) {
                final String[] argIn = {devName, "device::" + deviceName};
                DeviceData deviceData = new DeviceData();
                deviceData.insert(argIn);
                adminProxy.command_inout("AddLoggingTarget", deviceData);
            }
            final DevVarLongStringArray longStringArray = new DevVarLongStringArray();
            longStringArray.lvalue = new int[1];
            longStringArray.lvalue[0] = level;
            longStringArray.svalue = new String[1];
            longStringArray.svalue[0] = devName;
            DeviceData argIn = new DeviceData();
            argIn.insert(longStringArray);
            adminProxy.command_inout("SetLoggingLevel", argIn);
        } catch (Exception e) {
            displayException(e);
            return -1;
        }
        return 0;
    }
    //=========================================================================
    public int setCoLocatedDevicesLoggingLevel(String dev_name, int level) {
        try {
            final DeviceProxy dp = new DeviceProxy(dev_name);
            final DeviceProxy dsp = new DeviceProxy(dp.adm_name());
            final DevVarLongStringArray dvlsa = new DevVarLongStringArray();
            dvlsa.lvalue = new int[1];
            dvlsa.lvalue[0] = level;
            dvlsa.svalue = new String[1];
            dvlsa.svalue[0] = "*";
            DeviceData argin = new DeviceData();
            argin.insert(dvlsa);
            dsp.command_inout("SetLoggingLevel", argin);
        } catch (Exception e) {
            displayException(e);
            return -1;
        }
        return 0;
    }
    //=========================================================================
    public List<String> getLoggingSources() {
        return mSet.content();
    }
    //=========================================================================
    protected void displayException(Exception e) {
        LOG.error(e);
        Exception exception;
        if (e instanceof DevFailed) {
            exception = new ATKException((DevFailed) e);
            //Except.print_exception(e);
        } else {
            exception = e;
        }
        mHistoryArea.write(exception);
    }
    //=========================================================================
    public int setLoggingSourceProperty() {
        LOG.debug("Saving current logging source list into the TANGO database");
        List<String> content = mSet.content();
        String[] list = new String[content.size()];
        for (int i=0 ; i<content.size() ; i++)
            list[i] = content.get(i);
        DbDatum[] dbd = new DbDatum[] { new DbDatum(LOG_SRC_PROPERTY, list) };
        try {
            ApiUtil.get_db_obj().put_device_property(deviceName, dbd);
        } catch (Exception e) {
            displayException(e);
            return -1;
        }
        return 0;
    }
    //=========================================================================
    public int deleteLoggingSourceProperty() {
        try {
            ApiUtil.get_db_obj().delete_device_property(deviceName, LOG_SRC_PROPERTY);
        } catch (Exception e) {
            displayException(e);
            return -1;
        }
        return 0;
    }
    //=========================================================================
    public int getLoggingSourceProperty() {
        try {
            DbDatum datum = ApiUtil.get_db_obj().get_device_property(deviceName, LOG_SRC_PROPERTY);
            if (datum.is_empty()) {
                LOG.debug("Restore sources::nothing to restore");
                return 0;
            }
            String[] sources = datum.extractStringArray();
            LOG.debug("Restore sources::#source to add " + sources.length);
            for (String source : sources) {
                LOG.debug("Restore sources::adding " + source);
                addLoggingSource(source);
            }
        } catch (Exception e) {
            displayException(e);
            return -1;
        }
        return 0;
    }
    //=========================================================================
    public void cleanup() {
        if (mRunningInStaticMode && mSet.size()!=0) {
            int user_answer =
                    JOptionPane.showConfirmDialog(null,
                            "Save current source list before quitting?",
                            "Tango LogViewer",
                            JOptionPane.YES_NO_OPTION);
            if (user_answer==JOptionPane.YES_OPTION) {
                setLoggingSourceProperty();
            }
        }
        removeAllSources();
        try {
            // Delete the device from the database
            if (!mRunningInStaticMode) {
                String serverName = className + "/" + instanceName;
                LOG.debug("Deleting" + serverName + " server from the TANGO database");
                ApiUtil.get_db_obj().delete_server(serverName);
            }
        } catch (Exception e) {
            displayException(e);
        }
    }

}