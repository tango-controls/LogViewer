/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software
 * License version 1.1, a copy of which has been included with this
 * distribution in the LICENSE.txt file.  */
package fr.esrf.logviewer;

// Java stuffs

import java.util.*;
import java.text.DateFormat;

// AWT stuffs
import java.awt.Font;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.*;
import java.awt.Color;
import java.awt.Component;

// Swing stuffs
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.tree.*;

// Log4j stuffs
import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.ApiUtil;
import fr.esrf.TangoApi.Database;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

// ATK stuffs
import fr.esrf.tangoatk.widget.util.*;

/**
 * The main application.
 *
 * @author <a href="mailto:oliver@puppycrawl.com">Oliver Burn</a>
 */
public class Main extends JFrame {

  private final static String APP_RELEASE = "1.2.3";

  // use to log messages
  private static final Logger LOG = Logger.getLogger(Main.class);

  // the messages receiver
  private static TangoLoggingReceiver mTlr = null;

  // the device tree
  private JTree            devTree;
  private DefaultTreeModel treeModel;
  private JScrollPane      treeView;
  private TangoNode        root;

  // Popup menus
  private JPopupMenu  mMemberPopup;
  private ButtonGroup mMemberLevelGroup1;
  private ButtonGroup mMemberLevelGroup2;
  private JMenuItem   mMemberNameItem;

  // main window label
  private CustomLabel mLabel;

  // history area
  private HistoryArea mHistoryArea;

  // mPopupTrigger
  private boolean mPopupTrigger = false;

  // Handle to the database
  private Database db;

  static JFrame self = null;
  static boolean inited = false;

  //=========================================================================
  /**
   * Constructor added to close frame (and not exit) on exit button clicked
   * if called from another application..
   */
  //=========================================================================
  private boolean closeOnExit = false;

  public Main(JFrame parent) {
    this(new String[0], true);
  }

  //=========================================================================
  //=========================================================================

  public Main(String[] aArgs, boolean closeOnExit) {

    // Super class setup
    super("Tango LogViewer (exported as ...)");
    initLog4J();
    this.closeOnExit = closeOnExit;
    this.self = this;

    // Initialise the Tango database
    String tangoHost = null;
    try {
      tangoHost = ApiUtil.getTangoHost();
    } catch ( DevFailed e ) {
      System.out.println("TANGO_HOST no defined, exiting...");
      exitForm();
    }

    try {
      db = ApiUtil.get_db_obj();
    } catch (DevFailed e) {
      showTangoError(e);
      db = null;
      exitForm();
    }

    // Create startup screen
    Splash splash = new Splash();
    splash.setTitle("TANGO LogViewer");
    splash.setCopyright("(c) TANGO Team 2002-2010 / (c) Apache Project 2002");
    splash.setMaxProgress(10);
    splash.setMessage("Setting up UI...");
    int splashProgression = 1;
    // Animate progress bar
    splash.progress(splashProgression++);
    // Set main font
    Font font = new Font("terminal", 0, 12);
    UIManager.put("Label.font", font);
    UIManager.put("MenuBar.font", font);
    UIManager.put("Menu.font", font);
    UIManager.put("PopupMenu.font", font);
    UIManager.put("MenuItem.font", font);
    UIManager.put("ComboBox.font", font);
    UIManager.put("RadioButtonMenuItem.font", font);
    UIManager.put("Label.font", font);
    UIManager.put("TextField.font", font);
    UIManager.put("Button.font", font);
    UIManager.put("Table.font", font);
    UIManager.put("TableColumn.font", font);
    UIManager.put("TextArea.font", font);
    // Create the all important model
    final MyTableModel model = new MyTableModel();
    // Create the menu bar.
    final JMenuBar menuBar = new JMenuBar();
    setJMenuBar(menuBar);
    // Create the File menu
    JMenu menu = new JMenu("File");
    menuBar.add(menu);
    // Populate the File menu
    try {
      // Instance the XML file loader
      final LoadXMLAction lxa = new LoadXMLAction(this, model);
      // Create the Load File item
      final JMenuItem loadMenuItem = new JMenuItem("Load file...");
      // Add it the the File menu
      menu.add(loadMenuItem);
      // Link the LoadXMLAction with the Load File item
      loadMenuItem.addActionListener(lxa);
    } catch (NoClassDefFoundError e) {
      // Unable to locate the LoadXMLAction class
      LOG.info("Missing classes for XML parser", e);
      JOptionPane.showMessageDialog(
              this,
              "XML parser not in classpath - unable to load XML events.",
              "Tango LogViewer",
              JOptionPane.ERROR_MESSAGE);
    } catch (Exception e) {
      // Any other error
      LOG.info("Unable to create the action to load XML files", e);
      JOptionPane.showMessageDialog(
              this,
              "Unable to create a XML parser - unable to load XML events.",
              "Tango LogViewer",
              JOptionPane.ERROR_MESSAGE);
    }
    // Create the Exit item
    final JMenuItem exitMenuItem = new JMenuItem("Exit");
    // Add it the the File menu
    menu.add(exitMenuItem);
    // Link an action with the Exit item
    //exitMenuItem.addActionListener(ExitAction.INSTANCE);
    exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        exitBtnActionPerformed(evt);
      }
    });
    //-- FILE MENU
    JMenu actionMenu = new JMenu("Actions");
    menuBar.add(actionMenu);
    // Populate the Action menu
    JMenuItem anItem = new JMenuItem("Refresh Device Tree");
    anItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        mLabel.setText("Refreshing the device tree... ");
        refreshTree();
        mLabel.setText(" ");
      }
    });
    actionMenu.add(anItem);
    actionMenu.add(new JSeparator());
    anItem = new JMenuItem("Logging Source List");
    anItem.addActionListener(new SourceListActionListener());
    actionMenu.add(anItem);
    actionMenu.add(new JSeparator());
    anItem = new JMenuItem("Remove All Logging Source");
    anItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        mLabel.setText("Removing all logging source... ");
        if (mTlr.removeAllSources() != -1) {
          mHistoryArea.write("Removed all sources");
        }
        mLabel.setText(" ");
      }
    });
    actionMenu.add(anItem);
    actionMenu.add(new JSeparator());
    anItem = new JMenuItem("Clear history");
    anItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        mHistoryArea.clear();
      }
    });
    actionMenu.add(anItem);

    // Animate progress bar
    splash.progress(splashProgression++);
    // Add control panel
    final ControlPanel cp = new ControlPanel(model, this, closeOnExit);
    getContentPane().add(cp, BorderLayout.NORTH);
    // Animate progress bar
    splash.progress(splashProgression++);
    // Create the table
    final JTable table = new JTable(model);
    table.setRowSelectionAllowed(true);
    table.setColumnSelectionAllowed(true);
    table.setDragEnabled(true);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    final Enumeration cenum = table.getColumnModel().getColumns();
    LogTableRowRenderer dtcr = new LogTableRowRenderer();
    int i = 0;
    TableColumn tc;
    int col_width[] = {60, 140, 75, 155, 500};
    while (cenum.hasMoreElements()) {
      tc = (TableColumn) cenum.nextElement();
      tc.setCellRenderer(dtcr);
      tc.setPreferredWidth(col_width[i++]);
    }
    JScrollPane scrollPane = new JScrollPane(table);
    scrollPane.setBorder(BorderFactory.createTitledBorder("Logs"));
    scrollPane.setMinimumSize(new Dimension(150, 150));
    scrollPane.setPreferredSize(new Dimension(790, 450));
    // Animate progress bar
    splash.progress(splashProgression++);
    // Create the details
    final JPanel details = new DetailPanel(table, model);
    details.setMinimumSize(new Dimension(0, 0));
    details.setPreferredSize(new Dimension(790, 0));
    // Animate progress bar
    splash.progress(splashProgression++);
    // Add the table and stack trace into a splitter
    final JSplitPane jsp1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, details);
    jsp1.setOneTouchExpandable(true);
    jsp1.setDividerSize(9);

    // Create the device tree
    treeView = new JScrollPane();
    treeView.setPreferredSize(new Dimension(170,0));
    initTree();

    // Animate progress bar
    splash.progress(splashProgression++);
    //-- MEMBER POPUP MENU
    mMemberPopup = new JPopupMenu();
    mMemberNameItem = new JMenuItem("dev/name/goes.here");
    mMemberNameItem.setEnabled(false);
    mMemberPopup.add(mMemberNameItem);
    mMemberPopup.add(new JSeparator());
    anItem = new JMenuItem("Add");
    anItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        addLoggingSource();
      }
    });
    mMemberPopup.add(anItem);
    anItem = new JMenuItem("Add Colocated");
    anItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        addColocatedSources();
      }
    });
    /*
    mMemberPopup.add(anItem);
    anItem = new JMenuItem("Add Tango Core Logger");
    anItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addTangoCoreLogger();
            }
        });
    */
    mMemberPopup.add(anItem);
    mMemberLevelGroup1 = new ButtonGroup();
    mMemberPopup.add(new LoggingLevelMenu("Add/Set Logging Level", mMemberLevelGroup1, true));
    mMemberPopup.add(new JSeparator());
    anItem = new JMenuItem("Remove");
    anItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        removeLoggingSource();
      }
    });
    mMemberPopup.add(anItem);
    anItem = new JMenuItem("Remove Colocated");
    anItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        removeColocatedSources();
      }
    });
    mMemberPopup.add(anItem);
    /*
    anItem = new JMenuItem("Remove Tango Core Logger");
    anItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                removeTangoCoreLogger();
            }
        });
    */
    mMemberPopup.add(anItem);
    mMemberPopup.add(new JSeparator());
    mMemberLevelGroup2 = new ButtonGroup();
    mMemberPopup.add(new LoggingLevelMenu("Set Logging Level", mMemberLevelGroup2, false));
    mMemberPopup.add(new BasicLoggingLevelMenu("Set Logging Level (colocated)"));
    // Add the device tree into the splitter
    final JSplitPane jsp2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeView, jsp1);
    jsp2.setOneTouchExpandable(true);
    jsp2.setDividerSize(9);
    // Add a listener to the main window
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent aEvent) {
        //ExitAction.INSTANCE.actionPerformed(null);
        windowClosingPerformed(aEvent);
      }
    });
    // Animate progress bar
    splash.progress(splashProgression++);
    // Add the history text pane
    scrollPane = new JScrollPane();
    scrollPane.setBorder(BorderFactory.createTitledBorder("History"));
    mHistoryArea = new HistoryArea(scrollPane);
    mHistoryArea.setMinimumSize(new Dimension(0, 0));
    //mHistoryArea.setPreferredSize(new Dimension(900,150));
    Date today = new Date(System.currentTimeMillis());
    DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
    mHistoryArea.write("Welcome to the TANGO LogViewer (started on " + df.format(today) + ")");
    scrollPane.setViewportView(mHistoryArea);
    final JSplitPane jsp3 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, jsp2, scrollPane);
    jsp3.setOneTouchExpandable(true);
    jsp3.setDividerSize(9);
    getContentPane().add(jsp3, BorderLayout.CENTER);
    // Animate progress bar
    splash.progress(splashProgression++);
    // Add a label
    mLabel = new CustomLabel();
    mLabel.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
    getContentPane().add(mLabel, BorderLayout.SOUTH);
    // Create the TANGO log receiver (i.e. the LogConsumer device)
    splash.setMessage("Starting up the LogConsumer device...");
    setupReceiver(aArgs, model);
    // Animate progress bar
    splash.progress(splashProgression++);
    if (mTlr.isRunningInStaticMode() == true) {
      actionMenu.add(new JSeparator());
      menu = new JMenu("Logging Sources Property");
      anItem = new JMenuItem("Save");
      anItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          mLabel.setText("Saving logging source list into the TANGO database...");
          mTlr.setLoggingSourceProperty();
          mLabel.setText(" ");
        }
      });
      menu.add(anItem);
      anItem = new JMenuItem("Delete");
      anItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          mLabel.setText("Deleting logging source list from the TANGO database...");
          mTlr.deleteLoggingSourceProperty();
          mLabel.setText(" ");
        }
      });
      menu.add(anItem);
      anItem = new JMenuItem("Restore");
      anItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          mLabel.setText("Restoring logging sources from the TANGO database...");
          mTlr.removeAllSources();
          mTlr.getLoggingSourceProperty();
          mLabel.setText(" ");
        }
      });
      menu.add(anItem);
      actionMenu.add(menu);
    }
    String dev_name = mTlr.getDeviceName();
    setTitle("Tango Log Viewer " + APP_RELEASE + " [" + dev_name + "]");
    // Pack and make the main window visible
    pack();
    setLocationRelativeTo(splash);
    splash.setVisible(false);
    setVisible(true);
    splash.toFront();
    splash.setVisible(true);
    mLabel.setText(" ");
    // Animate progress bar
    splash.progress(splashProgression++);
    // Close the startup screen
    splash.setVisible(false);
    splash.dispose();
    devTree.setVisible(true);

    if (!inited) {
      Runtime.getRuntime().addShutdownHook(new Thread() {
        public void run() {
          Main.cleanup();
        }
      });
      inited = true;
    }

  }

  //===============================================================

  /**
   * Exit or close the Application
   */
  //===============================================================
  private void exitBtnActionPerformed(java.awt.event.ActionEvent evt) {
    exitForm();
  }
  //===============================================================

  /**
   * Exit or close the Application
   */
  //===============================================================
  private void windowClosingPerformed(java.awt.event.WindowEvent evt) {
    exitForm();
  }

  public void exitForm() {

    if (closeOnExit == true) {
      setVisible(false);
      if(mTlr!=null) mTlr.removeAllSources();
    } else {
      ExitAction.INSTANCE.actionPerformed(null);
    }

  }

  //=========================================================================

  private void setupReceiver(String[] aArgs, MyTableModel aModel) {
    try {
      if( mTlr==null ) {
        mTlr = new TangoLoggingReceiver(aArgs, aModel, mHistoryArea);
        mTlr.start();
      } else {
        mTlr.setTableModel(aModel);
        mTlr.setHistoryArea(mHistoryArea);
      }
    } catch (Exception e) {
      LOG.fatal("Unable to instanciate the TANGO log consumer device. Quiting", e);
      JOptionPane.showMessageDialog(
              this,
              "Unable to instanciate the TANGO log consumer device. Quiting",
              "Tango Log Viewer",
              JOptionPane.ERROR_MESSAGE);
      exitForm();
    }
  }
  //=========================================================================

  private void addLoggingSource() {
    Object n = devTree.getLastSelectedPathComponent();
    if (n == null) {
      return;
    }
    if (n instanceof DeviceNode) {
      String devname = ((DeviceNode) n).devName;
      mLabel.setText("Contacting " + devname + "...");
      if (mTlr.addLoggingSource(devname) != -1) {
        mHistoryArea.write("Added " + devname);
      }
    }
    mLabel.reset();
  }
  //=========================================================================

  private void addColocatedSources() {
    Object n = devTree.getLastSelectedPathComponent();
    if (n == null) {
      return;
    }
    if (n instanceof DeviceNode) {
      String devname = ((DeviceNode) n).devName;
      mLabel.setText("Adding  " + devname + " and colocated devices...");
      if (mTlr.addColocatedSources(devname) != -1) {
        mHistoryArea.write("Added  " + devname + " and colocated devices");
      }
    }
    mLabel.reset();
  }
  //=========================================================================

  private void removeLoggingSource() {
    Object n = devTree.getLastSelectedPathComponent();
    if (n == null) {
      return;
    }
    if (n instanceof DeviceNode) {
      String devname = ((DeviceNode) n).devName;
      mLabel.setText("Contacting " + devname + "...");
      if (mTlr.removeLoggingSource(devname) != -1) {
        mHistoryArea.write("Removed " + devname);
      }
    }
    mLabel.reset();
  }
  //=========================================================================

  private void removeColocatedSources() {
    Object n = devTree.getLastSelectedPathComponent();
    if (n == null) {
      return;
    }
    if (n instanceof DeviceNode) {
      String devname = ((DeviceNode) n).devName;
      mLabel.setText("Removing  " + devname + " and colocated devices...");
      if (mTlr.removeColocatedSources(devname) != -1) {
        mHistoryArea.write("Removed  " + devname + " and colocated devices");
      }
    }
    mLabel.reset();
  }
  //=========================================================================

  public void deviceTreeMousePressed(MouseEvent evt) {
    int selectedRow = devTree.getRowForLocation(evt.getX(), evt.getY());
    if (selectedRow != -1) {
      mPopupTrigger = evt.isPopupTrigger();
      devTree.setSelectionRow(selectedRow);
      Object n = devTree.getLastSelectedPathComponent();
      if (n == null) {
        return;
      }
      if (n instanceof DeviceNode) {

        mLabel.setText("Contacting " + ((DeviceNode) n).devName);
        int level = mTlr.getDeviceLoggingLevel(((DeviceNode) n).devName);
        mLabel.reset();
        if (level == -1) {
          return;
        }
        LoggingLevelMenuItem item;
        Enumeration enuma = mMemberLevelGroup1.getElements();
        while (enuma.hasMoreElements()) {
          item = (LoggingLevelMenuItem) enuma.nextElement();
          if (item.getLevel() == level) {
            item.setSelected(true);
            break;
          }
        }
        enuma = mMemberLevelGroup2.getElements();
        while (enuma.hasMoreElements()) {
          item = (LoggingLevelMenuItem) enuma.nextElement();
          if (item.getLevel() == level) {
            item.setSelected(true);
            break;
          }
        }

      }
    }
  }
  //=========================================================================

  public void deviceTreeMouseReleased(MouseEvent evt) {
    int selectedRow = devTree.getRowForLocation(evt.getX(), evt.getY());
    if (selectedRow != -1 && (evt.isPopupTrigger() || mPopupTrigger)) {
      devTree.setSelectionRow(selectedRow);
      Object n = devTree.getLastSelectedPathComponent();
      if (n == null) {
        return;
      }
      LOG.debug("n is a " + n.getClass().getName());
      if (n instanceof DeviceNode) {
        mMemberNameItem.setText(((DeviceNode) n).devName);
        mMemberPopup.show(evt.getComponent(), evt.getX(), evt.getY());
      }
    }
  }
  //=========================================================================

  private class SourceListActionListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      mHistoryArea.write("Current source list:");
      final String[] slist = mTlr.getLoggingSources();
      if (slist.length == 0) {
        mHistoryArea.write("\t- none");
      } else {
        for (int i = 0; i < slist.length; i++) {
          mHistoryArea.write("\t- " + slist[i]);
        }
      }
    }
  }
  //=========================================================================

  private class BasicLevelActionListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      String levelStr = ((JMenuItem) e.getSource()).getText();
      Object n = devTree.getLastSelectedPathComponent();
      if (n == null) {
        return;
      }
      if (n instanceof DeviceNode) {
        int level = ((BasicLoggingLevelMenuItem) e.getSource()).getLevel();
        String suffix = "logging level for colocated devices matching " + ((DeviceNode) n).devName + " to " + levelStr;
        mLabel.setText("Changing " + suffix);
        if (mTlr.setColocatedDevicesLoggingLevel(((DeviceNode) n).devName, level) != -1) {
          mHistoryArea.write("Changed " + suffix);
        }
      }
      mLabel.reset();
    }
  }
  //=========================================================================

  private class BasicLoggingLevelMenuItem extends JMenuItem {
    private int mLevel;

    public BasicLoggingLevelMenuItem(String iText, int iLevel) {
      super(iText);
      mLevel = iLevel;
    }

    public int getLevel() {
      return mLevel;
    }
  }

  //=========================================================================

  private class BasicLoggingLevelMenu extends JMenu {
    public BasicLoggingLevelMenu(String title) {
      super(title);
      JMenuItem anItem = new BasicLoggingLevelMenuItem("OFF", 0);
      anItem.addActionListener(new BasicLevelActionListener());
      add(anItem);
      anItem = new BasicLoggingLevelMenuItem("FATAL", 1);
      anItem.addActionListener(new BasicLevelActionListener());
      add(anItem);
      anItem = new BasicLoggingLevelMenuItem("ERROR", 2);
      anItem.addActionListener(new BasicLevelActionListener());
      add(anItem);
      anItem = new BasicLoggingLevelMenuItem("WARN", 3);
      anItem.addActionListener(new BasicLevelActionListener());
      add(anItem);
      anItem = new BasicLoggingLevelMenuItem("INFO", 4);
      anItem.addActionListener(new BasicLevelActionListener());
      add(anItem);
      anItem = new BasicLoggingLevelMenuItem("DEBUG", 5);
      anItem.addActionListener(new BasicLevelActionListener());
      add(anItem);
    }
  }
  //=========================================================================

  private class LevelActionListener implements ActionListener {
    private boolean mAddBefore = false;

    public LevelActionListener(boolean add_before) {
      mAddBefore = add_before;
    }

    public void actionPerformed(ActionEvent e) {
      String levelStr = ((JMenuItem) e.getSource()).getText();
      Object n = devTree.getLastSelectedPathComponent();
      if (n == null) {
        return;
      }
      int level;
      if (n instanceof DeviceNode) {
        level = ((LoggingLevelMenuItem) e.getSource()).getLevel();
        String devname = ((DeviceNode) n).devName;
        mLabel.setText("Contacting " + devname + "...");
        if (mTlr.setDeviceLoggingLevel(devname, level, mAddBefore) != -1) {
          if (mAddBefore) {
            mHistoryArea.write("Added " + devname);
          }
          mHistoryArea.write("Changed " + devname + " logging level to " + levelStr);
        }
      }
      mLabel.reset();
    }
  }
  //=========================================================================

  private class LoggingLevelMenuItem extends JRadioButtonMenuItem {
    private int mLevel;

    public LoggingLevelMenuItem(String iText, int iLevel) {
      super(iText);
      mLevel = iLevel;
    }

    public int getLevel() {
      return mLevel;
    }
  }
  //=========================================================================

  private class LoggingLevelMenu extends JMenu {
    public LoggingLevelMenu(String title, ButtonGroup _group, boolean add_before) {
      super(title);
      ButtonGroup group;
      if (_group != null) {
        group = _group;
      } else {
        group = new ButtonGroup();
      }
      JMenuItem lItem = new LoggingLevelMenuItem("OFF", 0);
      lItem.addActionListener(new LevelActionListener(add_before));
      lItem.setSelected(true);
      group.add(lItem);
      add(lItem);
      lItem = new LoggingLevelMenuItem("FATAL", 1);
      lItem.addActionListener(new LevelActionListener(add_before));
      group.add(lItem);
      add(lItem);
      lItem = new LoggingLevelMenuItem("ERROR", 2);
      lItem.addActionListener(new LevelActionListener(add_before));
      group.add(lItem);
      add(lItem);
      lItem = new LoggingLevelMenuItem("WARN", 3);
      lItem.addActionListener(new LevelActionListener(add_before));
      group.add(lItem);
      add(lItem);
      lItem = new LoggingLevelMenuItem("INFO", 4);
      lItem.addActionListener(new LevelActionListener(add_before));
      group.add(lItem);
      add(lItem);
      lItem = new LoggingLevelMenuItem("DEBUG", 5);
      lItem.addActionListener(new LevelActionListener(add_before));
      group.add(lItem);
      add(lItem);
    }
  }
  //=========================================================================

  private class CustomLabel extends JLabel {
    public CustomLabel() {
      super.setText(" ");
    }

    public void setText(String txt) {
      super.setText(txt);
      update(getGraphics());
    }

    public void reset() {
      setText(" ");
    }
  }
  //=========================================================================

  public class LogTableRowRenderer extends DefaultTableCellRenderer {

    private final Color _scolor = new Color(204, 204, 255);
    private final Color _color = new Color(230, 230, 230);
    private final JCheckBox _true = new JCheckBox("", true);
    private final JCheckBox _false = new JCheckBox("", false);

    LogTableRowRenderer() {
      setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
      _true.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
      _false.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    }

    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int col) {
      String col_header = (String) table.getColumnModel().getColumn(col).getHeaderValue();
      //-- Set back and fore colors
      if (isSelected) {
        setBackground(_scolor);
      } else if (col_header.equals("Level")) {
        Level level = (Level) value;
        if (level == Level.FATAL) {
          setBackground(Color.black);
          setForeground(Color.white);
        } else if (level == Level.ERROR) {
          setBackground(Color.red);
          setForeground(Color.black);
        } else if (level == Level.WARN) {
          setBackground(Color.orange);
          setForeground(Color.black);
        } else if (level == Level.INFO) {
          setBackground(Color.green);
          setForeground(Color.black);
        } else if (level == Level.DEBUG) {
          setBackground(Color.cyan);
          setForeground(Color.black);
        }
      } else {
        if ((row % 2) == 0) {
          setBackground(_color);
        } else {
          setBackground(Color.white);
        }
        setForeground(Color.black);
      }
      //-- Set cell content and height
      if (col_header.equals("Trace")) {
        JCheckBox cb = ((Boolean) value == Boolean.TRUE) ? _true : _false;
        return cb;
      }

      return super.getTableCellRendererComponent(table,
              value,
              isSelected,
              hasFocus,
              row,
              col);
    }
  }

  //=========================================================================
  private void initTree() {

    root = new RootNode(db);
    treeModel = new DefaultTreeModel(root);
    devTree = new JTree(treeModel);
    devTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    devTree.setMinimumSize(new Dimension(0, 0));
    devTree.setDragEnabled(true);
    devTree.setRootVisible(false);
    devTree.setShowsRootHandles(true);
    devTree.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent evt) {
        deviceTreeMousePressed(evt);
      }

      public void mouseReleased(MouseEvent evt) {
        deviceTreeMouseReleased(evt);
      }
    });

    treeView.getViewport().removeAll();
    treeView.getViewport().add(devTree);

  }

  //=========================================================================
  // Search the tree

  public TangoNode searchNode(TangoNode startNode, String value) {

    int numChild = treeModel.getChildCount(startNode);
    int i = 0;
    boolean found = false;
    TangoNode elem = null;

    while (i < numChild && !found) {
      elem = (TangoNode) treeModel.getChild(startNode, i);
      found = elem.toString().compareToIgnoreCase(value) == 0;
      if (!found) i++;
    }

    if (found) {
      return elem;
    } else {
      return null;
    }

  }

  //=========================================================================
  // Refresh the tree

  public void refreshTree() {

    treeView.getViewport().removeAll();

    TreePath oldPath = devTree.getSelectionPath();
    if (db != null) initTree();

    if (oldPath != null) {

      // Reselect old node
      TreePath newPath = new TreePath(root);
      TangoNode node = root;
      boolean found = true;
      int i = 1;
      while (found && i < oldPath.getPathCount()) {

        String item = oldPath.getPathComponent(i).toString();

        // Search for item
        node = searchNode(node, item);

        // Construct the new path
        if (node != null) {
          newPath = newPath.pathByAddingChild(node);
          i++;
        } else {
          found = false;
        }

      }

      devTree.setSelectionPath(newPath);
      devTree.expandPath(newPath);
      devTree.makeVisible(newPath);
      devTree.scrollPathToVisible(newPath);

    }

  }

  //=========================================================================
  public void selectDevice(String devName) {

    int bslash = devName.indexOf('/');
    int lslash = devName.lastIndexOf('/');

    String domain = devName.substring(0,bslash);
    String family = devName.substring(bslash+1,lslash);
    String member = devName.substring(lslash+1);

    // Search server
    TangoNode domainNode = searchNode(root,domain);
    if(domainNode==null) return;
    TangoNode familyNode = searchNode(domainNode,family);
    if(familyNode==null) return;
    TangoNode memberNode = searchNode(familyNode,member);
    if(memberNode==null) return;
    TreePath selPath = new TreePath(root);
    selPath = selPath.pathByAddingChild(domainNode);
    selPath = selPath.pathByAddingChild(familyNode);
    selPath = selPath.pathByAddingChild(memberNode);
    devTree.setSelectionPath(selPath);
    devTree.scrollPathToVisible(selPath);

  }

  //=========================================================================
  // STATIC METHODS
  //=========================================================================

  public static void showTangoError(DevFailed e) {

    String result = "";

    if (e != null) {

      for (int i = 0; i < e.errors.length; i++) {
        result += "Desc -> " + e.errors[i].desc + "\n";
        result += "Reason -> " + e.errors[i].reason + "\n";
        result += "Origin -> " + e.errors[i].origin + "\n";
      }

      if (result.length() > 0)
        JOptionPane.showMessageDialog(self, result, "Tango error", JOptionPane.ERROR_MESSAGE);

    }

  }

  private static void initLog4J() {
    final Properties props = new Properties();
    props.setProperty("log4j.rootCategory", "ERROR, A1");
    props.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
    props.setProperty("log4j.appender.A1.layout", "org.apache.log4j.TTCCLayout");
    PropertyConfigurator.configure(props);
  }
  //=========================================================================

  public static void cleanup() {
    if (mTlr != null) {
      mTlr.cleanup();
      mTlr = null;
    }
  }
  //=========================================================================

  public static void main(String[] aArgs) {
    Main m = new Main(aArgs, false);
  }

}
   
