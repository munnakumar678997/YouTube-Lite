#!/usr/bin/env bash

##############################################################################
##
##  Gradle wrapper
##
##############################################################################

# Determine the Java command to use to start the JVM.
if [ -n ""JAVA_HOME"" ] ; then
    if [ -x ""$JAVA_HOME/jre/sh/java"" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD=""$JAVA_HOME/jre/sh/java""
    else
        JAVACMD=""$JAVA_HOME/bin/java""
    fi
    if [ ! -x ""$JAVACMD"" ] ; then
        die ""ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

""Please set the JAVA_HOME variable in your environment to match the
""location of your Java installation.""
    fi
else
    JAVACMD=""java""
    which java >/dev/null || die ""ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

""Please set the JAVA_HOME variable in your environment to match the
""location of your Java installation or add 'java' to your PATH.""
fi

# Determine the script directory.
SCRIPT_DIR=""$(cd ""$(dirname ""$0"")"" && pwd)"

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS=""-Xmx64m -Xms64m""

APP_HOME=""$SCRIPT_DIR""
APP_NAME=""Gradle"
APP_BASE_NAME=""gradle"

# Add default Gradle options here.
DEFAULT_GRADLE_OPTS="""

# Collect all arguments for the Java command
CLASSPATH=""$APP_HOME/gradle/wrapper/gradle-wrapper.jar""

exec ""$JAVACMD"" ""$DEFAULT_JVM_OPTS"" ""$JAVA_OPTS"" ""$GRADLE_OPTS"" -classpath ""$CLASSPATH"" org.gradle.wrapper.GradleWrapperMain ""$DEFAULT_GRADLE_OPTS"" ""$@""
