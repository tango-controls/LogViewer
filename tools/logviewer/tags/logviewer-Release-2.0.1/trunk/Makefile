#+======================================================================
# $Source: $
#
# Project:      Tango Device Server
#
# Description:  Makefile to generate the JAVA Tango classes package
#
# $Author: pascal_verdier $
#
# $Revision: 28176 $
#
#-======================================================================


MAJOR_REV   = 2
MIDDLE_REV  = 0
MINOR_REV   = 1

APPLI_VERS	=	$(MAJOR_REV).$(MIDDLE_REV).$(MINOR_REV)
SVN_TAG_REV =	Release-$(MAJOR_REV).$(MIDDLE_REV).$(MINOR_REV)

PACKAGE        = logviewer
JAR_NAME      = LogViewer
TANGO_HOME     = /segfs/tango
SVN_LOG_VIEWER_PATH = $(SVN_TCS)/tools/$(PACKAGE)
JAR_DIR    = $(TANGO_HOME)/release/java/appli
#JAR_DIR    = .

# -----------------------------------------------------------------
#
#		The compiler flags
#
#------------------------------------------------------------------

BIN_DIR   = ./bin
JAVAFLAGS = -Xlint:unchecked -deprecation -d $(BIN_DIR)
JAVAC = javac $(JAVAFLAGS)

#-------------------------------------------------------------------


#-----------------------------------------------------------------

all:	 trace server gui



trace:
	@echo $(CLASSPATH)

gui:
	$(JAVAC) fr/esrf/$(PACKAGE)/*.java

server:
	$(JAVAC) org/tango/logconsumer/*.java

clean:
	rm  -Rf $(BIN_DIR)/*


jar: server gui
	@make_jar  $(JAR_NAME)  $(APPLI_VERS) $(JAR_DIR)


OPERATION = /operation/dserver/java/appli
install_op:
	@segfs2operation $(JAR_DIR) $(JAR_NAME) $(APPLI_VERS) $(OPERATION)

tag:
	@echo "Tagging  $(PACKAGE)  for $(SVN_TAG_REV)"
	svn copy  $(SVN_LOG_VIEWER_PATH)/trunk \
	          $(SVN_LOG_VIEWER_PATH)/tags/$(PACKAGE)-$(SVN_TAG_REV)
