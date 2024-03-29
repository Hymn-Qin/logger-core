/**
 * Copyright 2019 Anthony Trinh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.qos.logback.classic.net;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.UnknownHostException;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.pattern.SyslogStartConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.util.LevelToSyslogSeverity;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.net.SyslogAppenderBase;
import ch.qos.logback.core.net.SyslogOutputStream;

/**
 * This appender can be used to send messages to a remote syslog daemon. <p> For
 * more information about this appender, please refer to the online manual at
 * http://logback.qos.ch/manual/appenders.html#SyslogAppender
 *
 * @author Ceki G&uuml;lc&uuml;
 */
public class SyslogAppender extends SyslogAppenderBase<ILoggingEvent> {

  static final public String DEFAULT_SUFFIX_PATTERN = "[%thread] %logger %msg";
  static final public String DEFAULT_STACKTRACE_PATTERN = "" + CoreConstants.TAB;

  PatternLayout stackTraceLayout = new PatternLayout();
  String stackTracePattern = DEFAULT_STACKTRACE_PATTERN;

  boolean throwableExcluded = false;


  public void start() {
    super.start();
    setupStackTraceLayout();
  }

  String getPrefixPattern() {
    return "%syslogStart{" + getFacility() + "}%nopex{}";
  }

  @Override
  public SyslogOutputStream createOutputStream() throws SocketException, UnknownHostException {
    return new SyslogOutputStream(getSyslogHost(), getPort());
  }

  /**
   * Convert a level to equivalent syslog severity. Only levels for printing
   * methods i.e DEBUG, WARN, INFO and ERROR are converted.
   *
   * @see ch.qos.logback.core.net.SyslogAppenderBase#getSeverityForEvent(Object)
   */
  @Override
  public int getSeverityForEvent(Object eventObject) {
    ILoggingEvent event = (ILoggingEvent) eventObject;
    return LevelToSyslogSeverity.convert(event);
  }

  @Override
  protected void postProcess(Object eventObject, OutputStream sw) {
    if (throwableExcluded)
      return;

    ILoggingEvent event = (ILoggingEvent) eventObject;
    IThrowableProxy tp = event.getThrowableProxy();

    if (tp == null)
      return;

    String stackTracePrefix = stackTraceLayout.doLayout(event);
    boolean isRootException = true;
    while (tp != null) {
      StackTraceElementProxy[] stepArray = tp.getStackTraceElementProxyArray();
      try {
        handleThrowableFirstLine(sw, tp, stackTracePrefix, isRootException);
        isRootException = false;
        for (StackTraceElementProxy step : stepArray) {
          StringBuilder sb = new StringBuilder();
          sb.append(stackTracePrefix).append(step);
          sw.write(sb.toString().getBytes());
          sw.flush();
        }
      } catch (IOException e) {
        break;
      }
      tp = tp.getCause();
    }
  }

  // LOGBACK-411 and  LOGBACK-750
  private void handleThrowableFirstLine(OutputStream sw, IThrowableProxy tp, String stackTracePrefix, boolean isRootException) throws IOException {
    StringBuilder sb = new StringBuilder().append(stackTracePrefix);

    if (!isRootException) {
      sb.append(CoreConstants.CAUSED_BY);
    }
    sb.append(tp.getClassName()).append(": ").append(tp.getMessage());
    sw.write(sb.toString().getBytes());
    sw.flush();
  }

  boolean stackTraceHeaderLine(StringBuilder sb, boolean topException) {

    return false;
  }

  public Layout<ILoggingEvent> buildLayout() {
    PatternLayout layout = new PatternLayout();
    layout.getInstanceConverterMap().put("syslogStart",
            SyslogStartConverter.class.getName());
    if (suffixPattern == null) {
      suffixPattern = DEFAULT_SUFFIX_PATTERN;
    }
    layout.setPattern(getPrefixPattern() + suffixPattern);
    layout.setContext(getContext());
    layout.start();
    return layout;
  }

  private void setupStackTraceLayout() {
    stackTraceLayout.getInstanceConverterMap().put("syslogStart",
            SyslogStartConverter.class.getName());

    stackTraceLayout.setPattern(getPrefixPattern() + stackTracePattern);
    stackTraceLayout.setContext(getContext());
    stackTraceLayout.start();
  }

  public boolean isThrowableExcluded() {
    return throwableExcluded;
  }

  /**
   * Setting throwableExcluded to true causes no Throwable's stack trace data to be sent to
   * the syslog daemon. By default, stack trace data is sent to syslog daemon.
   *
   * @param throwableExcluded
   * @since 1.0.4
   */
  public void setThrowableExcluded(boolean throwableExcluded) {
    this.throwableExcluded = throwableExcluded;
  }

  /**
   * See {@link #setStackTracePattern(String)}.
   *
   * @return the stackTraceSuffixPattern
   * @since 1.0.4
   */
  public String getStackTracePattern() {
    return stackTracePattern;
  }

  /**
   * Stack trace lines are sent to the syslog server separately from the main message
   * For stack trace lines, the stackTracePattern is used instead of {@link #suffixPattern}.
   * The <b>stackTracePattern</b> option allows specification of a separately format for the
   * non-standardized part of stack trace lines.
   *
   * @param stackTracePattern
   * @since 1.0.4
   */
  public void setStackTracePattern(String stackTracePattern) {
    this.stackTracePattern = stackTracePattern;
  }
}