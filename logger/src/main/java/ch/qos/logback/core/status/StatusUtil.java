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
package ch.qos.logback.core.status;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.CoreConstants;

public class StatusUtil {

  StatusManager sm;

  public StatusUtil(StatusManager sm) {
    this.sm = sm;
  }

  public StatusUtil(Context context) {
    this.sm = context.getStatusManager();
  }

  /**
   * Returns true if the StatusManager associated with the context passed
   * as parameter has one or more StatusListener instances registered. Returns
   * false otherwise.
   *
   * @param context
   * @return true if one or more StatusListeners registered, false otherwise
   * @since 1.0.8
   */
  static public boolean contextHasStatusListener(Context context) {
    StatusManager sm = context.getStatusManager();
    if(sm == null)
      return false;
    List<StatusListener> listeners = sm.getCopyOfStatusListenerList();
    if(listeners == null || listeners.size() == 0)
      return false;
    else
      return true;
  }

  static public List<Status> filterStatusListByTimeThreshold(List<Status> rawList, long threshold) {
    List<Status> filteredList = new ArrayList<Status>();
    for (Status s : rawList) {
      if (s.getDate() >= threshold)
        filteredList.add(s);
    }
    return filteredList;
  }

  public void addStatus(Status status) {
    if (sm != null) {
      sm.add(status);
    }
  }

  public void addInfo(Object caller, String msg) {
    addStatus(new InfoStatus(msg, caller));
  }

  public void addWarn(Object caller, String msg) {
    addStatus(new WarnStatus(msg, caller));
  }

  public void addError(Object caller, String msg,
      Throwable t) {
    addStatus(new ErrorStatus(msg, caller, t));
  }

  public boolean hasXMLParsingErrors(long threshold) {
    return containsMatch(threshold, Status.ERROR, CoreConstants.XML_PARSING);
  }

  public boolean noXMLParsingErrorsOccurred(long threshold) {
    return !hasXMLParsingErrors(threshold);
  }

  public int getHighestLevel(long threshold) {
    List<Status> filteredList = filterStatusListByTimeThreshold(sm.getCopyOfStatusList(), threshold);
    int maxLevel = Status.INFO;
    for (Status s : filteredList) {
      if (s.getLevel() > maxLevel)
        maxLevel = s.getLevel();
    }
    return maxLevel;
  }

  public boolean isErrorFree(long threshold) {
    return Status.ERROR > getHighestLevel(threshold);
  }

  public boolean isWarningOrErrorFree(long threshold) {
    return Status.WARN > getHighestLevel(threshold);
  }

  public boolean containsMatch(long threshold, int level, String regex) {
    List<Status> filteredList = filterStatusListByTimeThreshold(sm.getCopyOfStatusList(), threshold);
    Pattern p = Pattern.compile(regex);

    for (Status status : filteredList) {
      if (level != status.getLevel()) {
        continue;
      }
      String msg = status.getMessage();
      Matcher matcher = p.matcher(msg);
      if (matcher.lookingAt()) {
        return true;
      }
    }
    return false;
  }

  public boolean containsMatch(int level, String regex) {
    return containsMatch(0, level, regex);
  }

  public boolean containsMatch(String regex) {
    Pattern p = Pattern.compile(regex);
    for (Status status : sm.getCopyOfStatusList()) {
      String msg = status.getMessage();
      Matcher matcher = p.matcher(msg);
      if (matcher.lookingAt()) {
        return true;
      }
    }
    return false;
  }

  public int matchCount(String regex) {
    int count = 0;
    Pattern p = Pattern.compile(regex);
    for (Status status : sm.getCopyOfStatusList()) {
      String msg = status.getMessage();
      Matcher matcher = p.matcher(msg);
      if (matcher.lookingAt()) {
        count++;
      }
    }
    return count;
  }

  public boolean containsException(Class<?> exceptionType) {
    Iterator<Status> stati = sm.getCopyOfStatusList().iterator();
    while (stati.hasNext()) {
      Status status = stati.next();
      Throwable t = status.getThrowable();
      while (t != null) {
        if (t.getClass().getName().equals(exceptionType.getName())) {
          return true;
        }
        t = t.getCause();
      }
    }
    return false;
  }

  /**
   * Return the time of last reset. -1 if last reset time could not be found
   *
   * @return time of last reset or -1
   */
  public long timeOfLastReset() {
    List<Status> statusList = sm.getCopyOfStatusList();
    if (statusList == null)
      return -1;

    int len = statusList.size();
    for (int i = len - 1; i >= 0; i--) {
      Status s = statusList.get(i);
      if (CoreConstants.RESET_MSG_PREFIX.equals(s.getMessage())) {
        return s.getDate();
      }
    }
    return -1;
  }

}
