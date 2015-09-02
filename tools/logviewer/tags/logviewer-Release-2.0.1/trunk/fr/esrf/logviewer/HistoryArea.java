package fr.esrf.logviewer;

// Swing stuffs
import javax.swing.JTextArea;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

public class HistoryArea extends JTextArea {
    JScrollPane mScrollPane;
    //========================================================================= 
    public HistoryArea (JScrollPane sp) {
        mScrollPane = sp;
        setEditable(false);
        setTabSize(2);
    }
    //========================================================================= 
    public void write (String txt) {
        scroll();
        append(txt + "\r\n");
    }
    //========================================================================= 
    public void write (Exception e) {
        scroll();
        append("=== ERROR ==========================================\r\n");
        append(e.toString() + "\r\n"); 
        append("====================================================\r\n");
    }
    //========================================================================= 
    private void scroll () {
        JScrollBar vsb = mScrollPane.getVerticalScrollBar();
        vsb.setValue(vsb.getMaximum());
    }
    //=========================================================================
    public void clear() {
      setText("");
    }

}
