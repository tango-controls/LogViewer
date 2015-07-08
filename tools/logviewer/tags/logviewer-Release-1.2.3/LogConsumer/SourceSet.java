//+======================================================================
// $Source$
//
// project :     Tango Device Server
//
// Description:	a set singleton to store the logging sources
//
// $Author$
//
// copyleft :   European Synchrotron Radiation Facility
//              BP 220, Grenoble 38043
//              FRANCE
//-======================================================================

package LogConsumer;

import java.util.HashSet;
import java.util.Iterator;

public class SourceSet extends HashSet
{
   private static SourceSet instance = new SourceSet();
   
   public static SourceSet instance () {
      return instance;
   }
   
   public void add(String s) {
     synchronized(instance) {
        super.add(s);
     }
   }
   
   public void remove(String s) {
     synchronized(instance) {
        super.remove(s);
     }
   }
   
   public void clear() {
     synchronized(instance) {
        super.clear();
     }
   }
   
   public int size() {
     synchronized(instance) {
        return super.size();
     }
   }
   
    public String[] content() {
     String[] cur_content;
     synchronized(instance) {
        cur_content = new String[super.size()];
        Iterator it = iterator();
        int i = 0;
        while (it.hasNext()) {
            cur_content[i++] = (String)it.next();
        }
     }
     return cur_content;
   }
}

/* end of $Source$ */