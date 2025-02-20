/*
 /*
 * This file is part of Anti-VPN-Service (AVS). The plugin securing your server against VPNs.
 *
 * MIT License
 *
 * Copyright (c) 2024-2025 Xpdustry
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

import arc.util.Log;
import arc.util.Log.LogLevel;


public class Logger {
  /** Will use slf4j when slf4md plugin is present */
  private static boolean slf4mdPresentAndEnabled = mindustry.Vars.mods.locateMod("slf4md") != null;
  private static Object slf4jLogger;
  
  protected static final Object[] empty = {};
  protected static String[] colorTags = {"&lc&fb", "&lb&fb", "&ly&fb", "&lr&fb", ""};
  protected static String mainTopic;

  public static void init(String mainTopic, Class<? extends mindustry.mod.Plugin> mainClass) {
    if (mainTopic != null) {
      mainTopic = mainTopic.trim();
      if (!mainTopic.isEmpty()) Logger.mainTopic = '[' + mainTopic + ']';
    }

    if (slf4mdPresentAndEnabled) 
      slf4jLogger = org.slf4j.LoggerFactory.getLogger(mainClass);
  }
  
  public void log(LogLevel level, String text, Object... args) {
    if (Log.level.ordinal() > level.ordinal()) return;

    text = Log.format(text, args);
    
    if (slf4mdPresentAndEnabled && slf4jLogger != null) {
      synchronized (slf4jLogger) {
        org.slf4j.Logger l = (org.slf4j.Logger)slf4jLogger;
        arc.func.Cons<String> printer;
        
        switch (level) {
          case debug: printer = l::debug; break;
          case info: printer = l::info; break;
          case warn: printer = l::warn; break;
          case err: printer = l::error; break;
          default: return;
        }

        for (String line : text.split("\n")) printer.get(line);    
      }
      
    } else {
      synchronized (Log.logger) {
        if (mainTopic == null) {
          for (String line : text.split("\n")) Log.logger.log(level, line);
        } else {
          String topic = Log.format(colorTags[level.ordinal()] + mainTopic + "&fr ");
          for (String line : text.split("\n")) Log.logger.log(level, topic + line);
        }
      }
    }
  }
  
  public void log(LogLevel level, String text) { log(level, text, empty); }

  public void debug(String text, Object... args) { log(LogLevel.debug, text, args); }
  public void debug(Object object) { debug(String.valueOf(object), empty); }

  public void info(String text, Object... args) { log(LogLevel.info, text, args); }
  public void info(Object object) { info(String.valueOf(object), empty); }

  public void warn(String text, Object... args) { log(LogLevel.warn, text, args); }
  public void warn(Object object) { warn(String.valueOf(object), empty); }
  
  public void err(String text, Object... args) { log(LogLevel.err, text, args); }
  public void err(Object object) { err(String.valueOf(object), empty); }
  public void err(String text, Throwable th) { err(text + ": " + Strings.getStackTrace(th)); }
  public void err(Throwable th){ err(Strings.getStackTrace(th)); } 
  
  /** Log an empty info line */
  public void none() { log(LogLevel.info, ""); }
}
