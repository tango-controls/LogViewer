/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 * Copyright (C) The TANGO Team.
 *
 * This software is published under the terms of the Apache Software
 * License version 1.1, a copy of which has been included with this
 * distribution in the LICENSE.txt file.  */

package fr.esrf.logviewer;

// Java stuffs

import java.util.*;
import java.net.InetAddress;
import javax.swing.JOptionPane;

// TANGO stuffs
import fr.esrf.Tango.*;
import fr.esrf.TangoDs.*;
import fr.esrf.TangoApi.*;

// Log4j stuffs
import org.apache.log4j.Logger;

// LogConsumer stuffs
import LogConsumer.*;

// ATK stuffs
import fr.esrf.tangoatk.core.ATKException;

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
  final String mDYN_DEV_NAME_PREFIX = "tmp/log/";

  /**
   * the Server name *
   */
  private String mServerName;

  /**
   * the LogConsumer device name *
   */
  private String mDevName;

  /**
   * a map to store the logging sources *
   */
  private final SourceSet mSet = SourceSet.instance();

  /**
   * true if a device name is provided *
   */
  private boolean mRunningInStaticMode = false;

  /**
   * the logging source device property name *
   */
  private final String mLOG_SRC_PROPERTY = "logging_source";

  /**
   * the history area *
   */
  private HistoryArea mHistoryArea;

  /**
   * Tango device server stuff
   */
  private Util tg;

  //=========================================================================

  TangoLoggingReceiver(String[] aArgs, MyTableModel aModel, HistoryArea aHistoryArea) throws Exception {
    setDaemon(true);
    mHistoryArea = aHistoryArea;
    try {
      String device_name;
      String instance_name;
      String[] args = null;
      boolean instance_name_provided = true;
      final Random rand = new Random(System.currentTimeMillis());
      final String dev_name_suffix = "@" + rand.nextInt(128);
      // Does the user provide an instance name?
      if (aArgs.length == 0 || aArgs[0].indexOf("/") == -1) {
        LOG.debug("No instance name provided.");
        instance_name_provided = false;
        // Set both server and device name
        String hostname = InetAddress.getLocalHost().getHostName();
        String[] tokens = hostname.split(".");
        if (tokens.length != 0) {
          hostname = tokens[0];
        }
        instance_name = hostname + dev_name_suffix;
        device_name = mDYN_DEV_NAME_PREFIX + instance_name;
      } else {
        LOG.debug("Provided device name: " + aArgs[0]);
        device_name = aArgs[0];
        int pos = device_name.lastIndexOf('/');
        if (pos == -1) {
          LOG.fatal("Invalid device name specified. Quiting");
          JOptionPane.showMessageDialog(
                  null,
                  "Invalid device name specified. Quiting",
                  "Tango Log Viewer",
                  JOptionPane.ERROR_MESSAGE);
          System.exit(1);
        }
        instance_name = device_name.substring(pos + 1);
        mRunningInStaticMode = true;
      }
      mServerName = "logconsumer/" + instance_name;
      LOG.debug("Instance name is " + instance_name);
      LOG.debug("Server name is " + mServerName);
      LOG.debug("Device name is " + device_name);
      // Register the device into the database
      LOG.debug("Registering the LogConsumer into the database");
      DbDevInfo dinfo = new DbDevInfo(device_name, "LogConsumer", mServerName);
      ApiUtil.get_db_obj().add_device(dinfo);
      //- Instanciate args
      if (instance_name_provided) {
        args = new String[aArgs.length];
      } else {
        args = new String[aArgs.length + 1];
      }
      args[0] = instance_name;
      // Add remaining user arguments
      int i = (instance_name_provided) ? 1 : 0;
      for (int j = 1; i < aArgs.length; i++, j++) {
        args[j] = aArgs[i];
      }
      // - Normal TANGO device startup
      //---------------------------------------------------------------------
      LOG.debug("Initializing the util singleton");
      LOG.debug("Passing following args to Util.init:");
      for (i = 0; i < args.length; i++) {
        LOG.debug("\targs[" + String.valueOf(i) + "]: " + args[i]);
      }
      // Init TANGO Util
      tg = Util.init(args, "LogConsumer");
      LOG.debug("Initializing the server");
      tg.server_init();
      // Give the device a ref to our model
      Vector dev_list = tg.get_device_list_by_class("LogConsumer");
      ((LogConsumer) (dev_list.elementAt(0))).setModel(aModel);
      mDevName = ((LogConsumer) (dev_list.elementAt(0))).name();
      // - Restore sources
      //---------------------------------------------------------------------
      if (mRunningInStaticMode == true) {
        getLoggingSourceProperty();
      }
    } catch (Exception e) {
      displayException(e);
    }
  }

  //=========================================================================
  public void setTableModel(MyTableModel aModel) {

    try {
      Vector dev_list = tg.get_device_list_by_class("LogConsumer");
      ((LogConsumer) (dev_list.elementAt(0))).setModel(aModel);
    } catch(Exception e) {
      displayException(e);
    }

  }

  //=========================================================================

  public void setHistoryArea(HistoryArea aHistoryArea) {

    mHistoryArea = aHistoryArea;

  }

  //=========================================================================

  public String getDeviceName() {
    return mDevName;
  }
  //=========================================================================

  public boolean isRunningInStaticMode() {
    return mRunningInStaticMode;
  }
  //=========================================================================

  public void run() {
    LOG.debug("Device thread started");
    try {
      LOG.info("Ready to accept request");
      Util.instance().server_run();
      LOG.debug("Device thread exiting");
    } catch (Exception e) {
      displayException(e);
    }
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

  public int addLoggingSources(String pattern) {
    int result = 0;
    String[] dev_list = getDeviceList(pattern);
    for (int i = 0; i < dev_list.length; i++) {
      if (addLoggingSource(dev_list[i]) == -1) {
        result = -1;
      }
    }
    return result;
  }
  //=========================================================================

  public int addColocatedSources(String dev_name) {
    try {
      final DeviceProxy dp = new DeviceProxy(dev_name);
      final DeviceProxy dsp = new DeviceProxy(dp.adm_name());
      final String[] argin = {"*", "device::" + mDevName};
      DeviceData dd = new DeviceData();
      dd.insert(argin);
      dsp.command_inout("AddLoggingTarget", dd);
      String server_name = dp.adm_name().substring(dp.adm_name().indexOf('/') + 1);
      LOG.debug("TangoLoggingReceiver::addColocatedSources::server_name is " + server_name);
      String[] dev_list = ApiUtil.get_db_obj().get_device_class_list(server_name);
      for (int i = 0; i < dev_list.length; i += 2) {
        LOG.debug("TangoLoggingReceiver::addColocatedSources::mSet adding " + dev_list[i]);
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
      final DeviceProxy dp = new DeviceProxy(dev_name);
      final DeviceProxy dsp = new DeviceProxy(dp.adm_name());
      final String[] argin = {dev_name, "device::" + mDevName};
      DeviceData dd = new DeviceData();
      dd.insert(argin);
      dsp.command_inout("AddLoggingTarget", dd);
      mSet.add(dev_name);
      LOG.debug("Logging sources set contains " + mSet.size() + " entries");
    } catch (Exception e) {
      displayException(e);
      return -1;
    }
    return 0;
  }
  //=========================================================================

  public int addTangoCoreLogger(String dev_name) {
    try {
      final DeviceProxy dp = new DeviceProxy(dev_name);
      final DeviceProxy dsp = new DeviceProxy(dp.adm_name());
      LOG.debug("Adding " + dsp.get_name() + " to logging sources list");
      final String[] argin = {dsp.get_name(), "device::" + mDevName};
      DeviceData dd = new DeviceData();
      dd.insert(argin);
      dsp.command_inout("AddLoggingTarget", dd);
      mSet.add(dev_name);
      LOG.debug("Logging sources set contains " + mSet.size() + " entries");
    } catch (Exception e) {
      displayException(e);
      return -1;
    }
    return 0;
  }
  //=========================================================================

  public int removeLoggingSources(String pattern) {
    int result = 0;
    String[] dev_list = getDeviceList(pattern);
    for (int i = 0; i < dev_list.length; i++) {
      if (removeLoggingSource(dev_list[i]) == -1) {
        result = -1;
      }
    }
    return result;
  }
  //=========================================================================

  public int removeLoggingSource(String dev_name) {
    return removeLoggingSource(dev_name, true);
  }
  //=========================================================================

  private int removeLoggingSource(String dev_name, boolean removeFromSet) {
    try {
      LOG.debug("Removing " + dev_name + " from logging sources list");
      final DeviceProxy dp = new DeviceProxy(dev_name);
      final DeviceProxy dsp = new DeviceProxy(dp.adm_name());
      final String[] argin = {dev_name, "device::" + mDevName};
      DeviceData dd = new DeviceData();
      dd.insert(argin);
      dsp.command_inout("RemoveLoggingTarget", dd);
      if (removeFromSet == true) {
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

  public int removeTangoCoreLogger(String dev_name) {
    try {
      final DeviceProxy dp = new DeviceProxy(dev_name);
      final DeviceProxy dsp = new DeviceProxy(dp.adm_name());
      LOG.debug("Removing " + dsp.get_name() + " from logging sources list");
      final String[] argin = {dsp.get_name(), "device::" + mDevName};
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

  public int removeColocatedSources(String dev_name) {
    return removeColocatedSources(dev_name, true);
  }
  //=========================================================================

  public int removeColocatedSources(String dev_name, boolean removeFromSet) {
    try {
      final DeviceProxy dp = new DeviceProxy(dev_name);
      final DeviceProxy dsp = new DeviceProxy(dp.adm_name());
      final String[] argin = {"*", "device::" + mDevName};
      DeviceData dd = new DeviceData();
      dd.insert(argin);
      dsp.command_inout("RemoveLoggingTarget", dd);
      if (removeFromSet == true) {
        String server_name = dp.adm_name().substring(dp.adm_name().indexOf('/') + 1);
        LOG.debug("TangoLoggingReceiver::removeColocatedSources::server_name is " + server_name);
        String[] dev_list = ApiUtil.get_db_obj().get_device_class_list(server_name);
        for (int i = 0; i < dev_list.length; i += 2) {
          LOG.debug("TangoLoggingReceiver::removeColocatedSources::mSet removing " + dev_list[i]);
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

  public int removeAllSources() {
    LOG.debug("Removing all logging sources");
    String[] slist = mSet.content();
    int result = 0;
    for (int i = 0; i < slist.length; i++) {
      if (removeColocatedSources(slist[i], false) == -1) {
        result = -1;
      }
    }
    mSet.clear();
    return 0;
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
      if (dvlsa.lvalue.length > 0) {
        return dvlsa.lvalue[0];
      }
    } catch (Exception e) {
      displayException(e);
      return -1;
    }
    return 0;
  }
  //=========================================================================

  public int setDevicesLoggingLevel(String pattern, int level) {
    int result = 0;
    String[] dev_list = getDeviceList(pattern);
    for (int i = 0; i < dev_list.length; i++) {
      if (setDeviceLoggingLevel(dev_list[i], level, false) == -1) {
        result = -1;
      }
    }
    return result;
  }
  //=========================================================================

  public int setDeviceLoggingLevel(String dev_name, int level, boolean add_before) {
    try {
      final DeviceProxy dp = new DeviceProxy(dev_name);
      final DeviceProxy dsp = new DeviceProxy(dp.adm_name());
      if (add_before) {
        final String[] argin = {dev_name, "device::" + mDevName};
        DeviceData dd = new DeviceData();
        dd.insert(argin);
        dsp.command_inout("AddLoggingTarget", dd);
      }
      final DevVarLongStringArray dvlsa = new DevVarLongStringArray();
      dvlsa.lvalue = new int[1];
      dvlsa.lvalue[0] = level;
      dvlsa.svalue = new String[1];
      dvlsa.svalue[0] = dev_name;
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

  public int setColocatedDevicesLoggingLevel(String dev_name, int level) {
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

  public String[] getLoggingSources() {
    return mSet.content();
  }
  //=========================================================================

  protected void displayException(Exception e) {
    LOG.error(e);
    Exception ex = null;
    if (e instanceof DevFailed) {
      ex = new ATKException((DevFailed) e);
      //Except.print_exception(e);
    } else {
      ex = e;
    }
    mHistoryArea.write(ex);
  }
  //=========================================================================

  public int setLoggingSourceProperty() {
    LOG.debug("Saving current logging source list into the TANGO database");
    String[] list = new String[mSet.size()];
    int i = 0;
    Iterator it = mSet.iterator();
    while (it.hasNext()) {
      list[i++] = (String) it.next();
    }
    DbDatum[] dbd = new DbDatum[1];
    dbd[0] = new DbDatum(mLOG_SRC_PROPERTY, list);
    try {
      ApiUtil.get_db_obj().put_device_property(mDevName, dbd);
    } catch (Exception e) {
      displayException(e);
      return -1;
    }
    return 0;
  }
  //=========================================================================

  public int deleteLoggingSourceProperty() {
    try {
      ApiUtil.get_db_obj().delete_device_property(mDevName, mLOG_SRC_PROPERTY);
    } catch (Exception e) {
      displayException(e);
      return -1;
    }
    return 0;
  }
  //=========================================================================

  public int getLoggingSourceProperty() {
    try {
      DbDatum dbd = ApiUtil.get_db_obj().get_device_property(mDevName, mLOG_SRC_PROPERTY);
      if (dbd.is_empty() == true) {
        LOG.debug("Restore sources::nothing to restore");
        return 0;
      }
      String[] src_list = dbd.extractStringArray();
      LOG.debug("Restore sources::#source to add " + src_list.length);
      for (int i = 0; i < src_list.length; i++) {
        LOG.debug("Restore sources::adding " + src_list[i]);
        addLoggingSource(src_list[i]);
      }
    } catch (Exception e) {
      displayException(e);
      return -1;
    }
    return 0;
  }
  //=========================================================================

  public void cleanup() {
    if (mRunningInStaticMode == true && mSet.size() != 0) {
      int user_answer =
              JOptionPane.showConfirmDialog(null,
                      "Save current source list before quitting?",
                      "Tango LogViewer",
                      JOptionPane.YES_NO_OPTION);
      if (user_answer == JOptionPane.YES_OPTION) {
        setLoggingSourceProperty();
      }
    }
    removeAllSources();
    try {
      // Delete the device from the database
      if (mRunningInStaticMode == false) {
        LOG.debug("Deleting server from the TANGO database");
        ApiUtil.get_db_obj().delete_server(Util.instance().get_ds_name());
      }
    } catch (Exception e) {
      displayException(e);
    }
  }

}