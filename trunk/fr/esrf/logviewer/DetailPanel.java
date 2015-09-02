/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software
 * License version 1.1, a copy of which has been included with this
 * distribution in the LICENSE.txt file.  */
package fr.esrf.logviewer;

import java.awt.BorderLayout;
import java.text.MessageFormat;
import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * A panel for showing a stack trace.
 *
 * @author <a href="mailto:oliver@puppycrawl.com">Oliver Burn</a>
 */
public class DetailPanel
    extends JPanel
    implements ListSelectionListener {

    /** used to format the logging event **/
    private static final MessageFormat FORMATTER = new MessageFormat(
        "<b>Time:</b> <code>{0}</code>"               +
        "&nbsp;&nbsp;<b>Level:</b> <code>{1}</code>"  +
        "&nbsp;&nbsp;<b>Device:</b> <code>{3}</code>" +
        "&nbsp;&nbsp;<b>Thread:</b> <code>{2}</code>" +
        "<pre>{4}</pre>"
    );
    
    /** the model for the data to render **/
    private final MyTableModel mModel;
    /** pane for rendering detail **/
    private final JEditorPane mDetails;

    /**
     * Creates a new <code>DetailPanel</code> instance.
     *
     * @param aTable the table to listen for selections on
     * @param aModel the model backing the table
     */
    public DetailPanel(JTable aTable, final MyTableModel aModel) {
        mModel = aModel;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Log Details"));

        mDetails = new JEditorPane();
        mDetails.setEditable(false);
        mDetails.setContentType("text/html");
        add(new JScrollPane(mDetails), BorderLayout.CENTER);

        final ListSelectionModel rowSM = aTable.getSelectionModel();
        rowSM.addListSelectionListener(this);
    }

    /** @see ListSelectionListener **/
    public void valueChanged(ListSelectionEvent aEvent) {
        //Ignore extra messages.
        if (aEvent.getValueIsAdjusting()) {
            return;
        }

        final ListSelectionModel lsm = (ListSelectionModel) aEvent.getSource();
        if (lsm.isSelectionEmpty()) {
            mDetails.setText("Nothing selected");
        } else {
            final int selectedRow = lsm.getMinSelectionIndex();
            final EventDetails e = mModel.getEventDetails(selectedRow);
            final Object[] args =
            {
                e.getRenderedTimeStamp(),
                e.getLevel(),
                escape(e.getThreadName()),
                escape(e.getCategoryName()),
                //escape(e.getLocationDetails()),
                escape(e.getMessage())//,
                //escape(getThrowableStrRep(e))
            };
            mDetails.setText(FORMATTER.format(args));
            mDetails.setCaretPosition(0);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Private methods
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Returns a string representation of a throwable.
     *
     * @param aEvent contains the throwable information
     * @return a <code>String</code> value
     */
    @SuppressWarnings("unused")
    private static String getThrowableStrRep(EventDetails aEvent) {
        final String[] throwableList = aEvent.getThrowableStrRep();
        if (throwableList == null) {
            return null;
        }

        final StringBuilder sb = new StringBuilder();
        for (String str : throwableList) {
            sb.append(str).append("\n");
        }

        return sb.toString();
    }

    /**
     * Escape &lt;, &gt; &amp; and &quot; as their entities. It is very
     * dumb about &amp; handling.
     * @param aStr the String to escape.
     * @return the escaped String
     */
    private String escape(String aStr) {
        if (aStr == null) {
            return null;
        }

        final StringBuilder buf = new StringBuilder();
        for (int i = 0; i < aStr.length(); i++) {
            char c = aStr.charAt(i);
            switch (c) {
            case '<':
                buf.append("&lt;");
                break;
            case '>':
                buf.append("&gt;");
                break;
            case '\"':
                buf.append("&quot;");
                break;
            case '&':
                buf.append("&amp;");
                break;
            default:
                buf.append(c);
                break;
            }
        }
        return buf.toString();
    }
}
