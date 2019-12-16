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

import java.io.Serializable;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.LoggingEventVO;
import ch.qos.logback.core.spi.PreSerializationTransformer;

public class LoggingEventPreSerializationTransformer implements
    PreSerializationTransformer<ILoggingEvent> {

  public Serializable transform(ILoggingEvent event) {
    if(event == null) {
      return null;
    }
    if (event instanceof LoggingEvent) {
      return LoggingEventVO.build(event);
    } else if (event instanceof LoggingEventVO) {
      return (LoggingEventVO)  event;
    } else {
      throw new IllegalArgumentException("Unsupported type "+event.getClass().getName());
    }
  }

}
