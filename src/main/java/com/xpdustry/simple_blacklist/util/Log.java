/*
 * This file is part of Simple Blacklist.
 *
 * MIT License
 *
 * Copyright (c) 2025 Xpdustry
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.xpdustry.simple_blacklist.util;


public class Log extends arc.util.Log {
  /** Will use slf4j when slf4md plugin is present */
  private static boolean slf4mdPresentAndEnabled = mindustry.Vars.mods.locateMod("slf4md") != null;
  private static Object slf4jLogger;
  
  protected static final Object[] empty = {};
  protected static String[] colorTags = {"&lc&fb", "&lb&fb", "&ly&fb", "&lr&fb", ""};
  protected static String mainTopic;
  protected static Class<?> mainClass;
  
  public static void init(String mainTopic, Class<? extends mindustry.mod.Plugin> mainClass) {
    Log.mainTopic = '[' + mainTopic + ']';
    Log.mainClass = mainClass;
  }
  
  public static void log(LogLevel level, String text, Object... args) {
    if (Log.level.ordinal() > level.ordinal()) return;

    text = format(text, args);
    
    if (slf4mdPresentAndEnabled) {
      if (slf4jLogger == null) {
        if (mainClass == null) slf4mdPresentAndEnabled = false;
        else slf4jLogger = org.slf4j.LoggerFactory.getLogger(mainClass);
      }
      org.slf4j.Logger l = (org.slf4j.Logger) slf4jLogger;
      arc.func.Cons<String> printer;
      
      switch (level) {
        case debug: printer = l::debug; break;
        case info: printer = l::info; break;
        case warn: printer = l::warn; break;
        case err: printer = l::error; break;
        default: return;
      }
      
      synchronized (slf4jLogger) {
        for (String line : text.split("\n")) printer.get(line);    
      }
      
    } else {
      synchronized (Log.class) {
        if (mainTopic == null) {
          for (String line : text.split("\n")) logger.log(level, line);
        } else {
          String topic = format(colorTags[level.ordinal()] + mainTopic + "&fr ");
          for (String line : text.split("\n")) logger.log(level, topic + line);
        }
      }
    }
  }
  
  public static void log(LogLevel level, String text) { log(level, text, empty); }

  public static void debug(String text, Object... args) { log(LogLevel.debug, text, args); }
  public static void debug(Object object) { debug(String.valueOf(object), empty); }

  public static void info(String text, Object... args) { log(LogLevel.info, text, args); }
  public static void info(Object object) { info(String.valueOf(object), empty); }

  public static void warn(String text, Object... args) { log(LogLevel.warn, text, args); }
  public static void warn(Object object) { warn(String.valueOf(object), empty); }
  
  public static void err(String text, Object... args) { log(LogLevel.err, text, args); }
  public static void err(Object object) { err(String.valueOf(object), empty); }
  public static void err(String text, Throwable th) { err(text + ": " + Strings.getStackTrace(th)); }
  public static void err(Throwable th){ err(Strings.getStackTrace(th)); } 
  
  /** Log an empty info line */
  public static void none() { log(LogLevel.info, ""); }
}
