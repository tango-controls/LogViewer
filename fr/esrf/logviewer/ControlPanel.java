/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software
 * License version 1.1, a copy of which has been included with this
 * distribution in the LICENSE.txt file.  */
package fr.esrf.logviewer;

import java.awt.GridBagConstraints;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

/**
 * Represents the controls for filtering, pausing, exiting, etc.
 *
 * @author <a href="mailto:oliver@puppycrawl.com">Oliver Burn</a>
 */
class ControlPanel extends JPanel {
  /**
   * use the log messages *
   */
  private boolean closeOnExit;
  private Main parent;

  /**
   * Creates a new <code>ControlPanel</code> instance.
   *
   * @param aModel the model to control
   */
  ControlPanel(final MyTableModel aModel, Main parent, boolean closeOnExit) {

    setLayout(new java.awt.BorderLayout());
    setBorder(BorderFactory.createTitledBorder("Controls"));

    this.parent = parent;
    this.closeOnExit = closeOnExit;
    final JPanel jp = new JPanel();
    jp.setLayout(new java.awt.GridBagLayout());

    JLabel label = new JLabel("Level Filter");
    GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
    jp.add(label, gridBagConstraints);

    int gridy = 1;

    label = new JLabel("Time Filter");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = gridy++;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
    jp.add(label, gridBagConstraints);

    label = new JLabel("Thread Filter");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = gridy++;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
    jp.add(label, gridBagConstraints);

    label = new JLabel("Source Filter");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = gridy++;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
    jp.add(label, gridBagConstraints);

    label = new JLabel("Message Filter");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = gridy++;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
    jp.add(label, gridBagConstraints);

    final Level[] levels = {Level.FATAL,
            Level.ERROR,
            Level.WARN,
            Level.INFO,
            Level.DEBUG};
    final JComboBox priorities = new JComboBox(levels);
    //priorities.setFont(new java.awt.Font("Dialog", 0, 11));
    final Level lowest = levels[levels.length - 1];
    priorities.setSelectedItem(lowest);
    aModel.setLevelFilter(lowest);
    priorities.setEditable(false);
    priorities.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent aEvent) {
        aModel.setLevelFilter(
                (Level) priorities.getSelectedItem());
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    jp.add(priorities, gridBagConstraints);

    gridy = 1;

    final JTextField timeField = new JTextField("");
    //timeField.setFont(new java.awt.Font("Dialog", 0, 11));
    timeField.getDocument().addDocumentListener(new DocumentListener() {
      public void insertUpdate(DocumentEvent aEvent) {
        aModel.setTimeStampFilter(timeField.getText());
      }

      public void removeUpdate(DocumentEvent aEvent) {
        aModel.setTimeStampFilter(timeField.getText());
      }

      public void changedUpdate(DocumentEvent aEvent) {
        aModel.setTimeStampFilter(timeField.getText());
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = gridy++;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.weightx = 1.0;
    jp.add(timeField, gridBagConstraints);

    final JTextField threadField = new JTextField("");
    //threadField.setFont(new java.awt.Font("Dialog", 0, 11));
    threadField.getDocument().addDocumentListener(new DocumentListener() {
      public void insertUpdate(DocumentEvent aEvent) {
        aModel.setThreadFilter(threadField.getText());
      }

      public void removeUpdate(DocumentEvent aEvente) {
        aModel.setThreadFilter(threadField.getText());
      }

      public void changedUpdate(DocumentEvent aEvent) {
        aModel.setThreadFilter(threadField.getText());
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = gridy++;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.weightx = 1.0;
    jp.add(threadField, gridBagConstraints);

    final JTextField catField = new JTextField("");
    //catField.setFont(new java.awt.Font("Dialog", 0, 11));
    catField.getDocument().addDocumentListener(new DocumentListener() {
      public void insertUpdate(DocumentEvent aEvent) {
        aModel.setCategoryFilter(catField.getText());
      }

      public void removeUpdate(DocumentEvent aEvent) {
        aModel.setCategoryFilter(catField.getText());
      }

      public void changedUpdate(DocumentEvent aEvent) {
        aModel.setCategoryFilter(catField.getText());
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = gridy++;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.weightx = 1.0;
    jp.add(catField, gridBagConstraints);

    final JTextField msgField = new JTextField("");
    //msgField.setFont(new java.awt.Font("Dialog", 0, 11));
    msgField.getDocument().addDocumentListener(new DocumentListener() {
      public void insertUpdate(DocumentEvent aEvent) {
        aModel.setMessageFilter(msgField.getText());
      }

      public void removeUpdate(DocumentEvent aEvent) {
        aModel.setMessageFilter(msgField.getText());
      }

      public void changedUpdate(DocumentEvent aEvent) {
        aModel.setMessageFilter(msgField.getText());
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = gridy++;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    gridBagConstraints.weightx = 1.0;
    jp.add(msgField, gridBagConstraints);

    gridy = 1;

    final JButton exitButton = new JButton("Exit");
    exitButton.setMnemonic('x');
    //exitButton.addActionListener(ExitAction.INSTANCE);
    exitButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        exitBtnActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = gridy++;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
    jp.add(exitButton, gridBagConstraints);

    JButton dummyButton = new JButton(" ");
    dummyButton.setEnabled(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = gridy++;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
    jp.add(dummyButton, gridBagConstraints);

    final JButton clearButton = new JButton("Clear");
    clearButton.setMnemonic('c');
    clearButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent aEvent) {
        aModel.clear();
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = gridy++;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
    jp.add(clearButton, gridBagConstraints);

    final JButton toggleButton = new JButton("Pause");
    toggleButton.setMnemonic('p');
    toggleButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent aEvent) {
        aModel.toggle();
        toggleButton.setText(
                aModel.isPaused() ? "Resume" : "Pause");
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = gridy++;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
    jp.add(toggleButton, gridBagConstraints);

    add(jp, BorderLayout.CENTER);
  }
  //===============================================================

  /**
   * Exit or close the Application
   */
  //===============================================================
  private void exitBtnActionPerformed(java.awt.event.ActionEvent evt) {
    parent.exitForm();
  }

}
