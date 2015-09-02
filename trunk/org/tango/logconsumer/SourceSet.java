//+======================================================================
// $Source$
//
// project :     Tango Device Server
//
// Description:	a set singleton to store the logging sources
//
// $Author: nleclercq $
//
// copyleft :   European Synchrotron Radiation Facility
//              BP 220, Grenoble 38043
//              FRANCE
//-======================================================================

package org.tango.logconsumer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class SourceSet extends HashSet<String>
{
   private final static SourceSet instance = new SourceSet();
   
   public static SourceSet instance () {
      return instance;
   }
   
   public boolean add(String s) {
     synchronized(instance) {
        return super.add(s);
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
   
    public List<String> content() {
     ArrayList<String> contentList = new ArrayList<>();
     synchronized(instance) {
        Iterator it = iterator();
        int i = 0;
        while (it.hasNext()) {
            contentList.add((String) it.next());
        }
     }
     return contentList;
   }
}

/* end of $Source$ */