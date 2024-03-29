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
package ch.qos.logback.core.html;

import java.util.HashMap;
import java.util.Map;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.LayoutBase;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.pattern.ConverterUtil;
import ch.qos.logback.core.pattern.parser.Node;
import ch.qos.logback.core.pattern.parser.Parser;
import ch.qos.logback.core.spi.ScanException;

import static ch.qos.logback.core.CoreConstants.LINE_SEPARATOR;

/**
 * This class is a base class for HTMLLayout classes part of
 * other logback modules such as logback-classic and logback-access.
 *
 *
 * @author S&eacute;bastien Pennec
 */
public abstract class HTMLLayoutBase<E> extends LayoutBase<E> {

  protected String pattern;

  protected Converter<E> head;

  protected String title = "Logback Log Messages";

  //It is the responsibility of derived classes to set
  //this variable in their constructor to a default value.
  protected CssBuilder cssBuilder;

   // counter keeping track of the rows output
  protected long counter = 0;

  /**
   * Set the <b>ConversionPattern</b> option. This is the string which controls
   * formatting and consists of a mix of literal content and conversion
   * specifiers.
   * @param conversionPattern the desired conversion pattern
   */
  public void setPattern(String conversionPattern) {
    pattern = conversionPattern;
  }

  /**
   * Returns the value of the <b>ConversionPattern </b> option.
   * @return the conversion pattern
   */
  public String getPattern() {
    return pattern;
  }

  public CssBuilder getCssBuilder() {
    return cssBuilder;
  }

  public void setCssBuilder(CssBuilder cssBuilder) {
    this.cssBuilder = cssBuilder;
  }

  /**
   * Parses the pattern and creates the Converter linked list.
   */
  @Override
  public void start() {
    int errorCount = 0;

    try {
      Parser<E> p = new Parser<E>(pattern);
      p.setContext(getContext());
      Node t = p.parse();
      this.head = p.compile(t, getEffectiveConverterMap());
      ConverterUtil.startConverters(this.head);
    } catch (ScanException ex) {
      addError("Incorrect pattern found", ex);
      errorCount++;
    }

    if (errorCount == 0) {
      super.started = true;
    }
  }

  protected abstract Map<String, String> getDefaultConverterMap();


  /**
   * Returns a map where the default converter map is merged with the map
   * contained in the context.
   * @return the converter map
   */
  public Map<String, String> getEffectiveConverterMap() {
    Map<String, String> effectiveMap = new HashMap<String, String>();

    // add the least specific map fist
    Map<String, String> defaultMap = getDefaultConverterMap();
    if (defaultMap != null) {
      effectiveMap.putAll(defaultMap);
    }

    // contextMap is more specific than the default map
    Context context = getContext();
    if (context != null) {
      @SuppressWarnings("unchecked")
      Map<String, String> contextMap = (Map<String, String>) context
          .getObject(CoreConstants.PATTERN_RULE_REGISTRY);
      if (contextMap != null) {
        effectiveMap.putAll(contextMap);
      }
    }
    return effectiveMap;
  }

  /**
   * The <b>Title</b> option takes a String value. This option sets the
   * document title of the generated HTML document.
   *
   * <p>
   * Defaults to 'Logback Log Messages'.
   * @param title desired document title
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * Returns the current value of the <b>Title </b> option.
   * @return the page/document title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Returns the content type output by this layout, i.e "text/html".
   * @return the content type
   */
  @Override
  public String getContentType() {
    return "text/html";
  }

  /**
   * Returns appropriate HTML headers.
   * @return the headers
   */
  @Override
  public String getFileHeader() {
    StringBuilder sbuf = new StringBuilder();
    sbuf.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"");
    sbuf.append(" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
    sbuf.append(LINE_SEPARATOR);
    sbuf.append("<html>");
    sbuf.append(LINE_SEPARATOR);
    sbuf.append("  <head>");
    sbuf.append(LINE_SEPARATOR);
    sbuf.append("    <title>");
    sbuf.append(title);
    sbuf.append("</title>");
    sbuf.append(LINE_SEPARATOR);

    cssBuilder.addCss(sbuf);

    sbuf.append(LINE_SEPARATOR);
    sbuf.append("  </head>");
    sbuf.append(LINE_SEPARATOR);
    sbuf.append("<body>");
    sbuf.append(LINE_SEPARATOR);

    return sbuf.toString();
  }

  public String getPresentationHeader() {
    StringBuilder sbuf = new StringBuilder();
    sbuf.append("<hr/>");
    sbuf.append(LINE_SEPARATOR);
    sbuf.append("<p>Log session start time ");
    sbuf.append(new java.util.Date());
    sbuf.append("</p><p></p>");
    sbuf.append(LINE_SEPARATOR);
    sbuf.append(LINE_SEPARATOR);
    sbuf.append("<table cellspacing=\"0\">");
    sbuf.append(LINE_SEPARATOR);

    buildHeaderRowForTable(sbuf);

    return sbuf.toString();
  }


  private void buildHeaderRowForTable(StringBuilder sbuf) {
    Converter<E> c = head;
    String name;
    sbuf.append("<tr class=\"header\">");
    sbuf.append(LINE_SEPARATOR);
    while (c != null) {
      name = computeConverterName(c);
      if (name == null) {
        c = c.getNext();
        continue;
      }
      sbuf.append("<td class=\"");
      sbuf.append(computeConverterName(c));
      sbuf.append("\">");
      sbuf.append(computeConverterName(c));
      sbuf.append("</td>");
      sbuf.append(LINE_SEPARATOR);
      c = c.getNext();
    }
    sbuf.append("</tr>");
    sbuf.append(LINE_SEPARATOR);
  }

  public String getPresentationFooter() {
    StringBuilder sbuf = new StringBuilder();
    sbuf.append("</table>");
    return sbuf.toString();
  }

  /**
   * Returns the appropriate HTML footers.
   */
  @Override
  public String getFileFooter() {
    StringBuilder sbuf = new StringBuilder();
    sbuf.append(LINE_SEPARATOR);
    sbuf.append("</body></html>");
    return sbuf.toString();
  }

  protected void startNewTableIfLimitReached(StringBuilder sbuf) {
    if (this.counter >= CoreConstants.TABLE_ROW_LIMIT) {
      counter = 0;
      sbuf.append("</table>");
      sbuf.append(LINE_SEPARATOR);
      sbuf.append("<p></p>");
      sbuf.append("<table cellspacing=\"0\">");
      sbuf.append(LINE_SEPARATOR);
      buildHeaderRowForTable(sbuf);
    }
  }

  protected String computeConverterName(Converter<E> c) {
    String className = c.getClass().getSimpleName();
    int index = className.indexOf("Converter");
    if (index == -1) {
      return className;
    } else {
      return className.substring(0, index);
    }
  }

}
