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
package ch.qos.logback.core.joran.action;

import org.xml.sax.Attributes;

import ch.qos.logback.core.joran.spi.InterpretationContext;


/**
 * No operation (NOP) action that does strictly nothing. 
 * Setting a rule to this pattern is sometimes useful in order
 * to prevent implicit actions to kick in.
 *  
 * @author Ceki G&uuml;lc&uuml;
 */
public class NOPAction extends Action {
  
  public void begin(InterpretationContext ec, String name, Attributes attributes) {
  }


  public void end(InterpretationContext ec, String name) {
  }
}
