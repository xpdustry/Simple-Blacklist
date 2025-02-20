/*
 * This file is part of Anti-VPN-Service (AVS). The plugin securing your server against VPNs.
 *
 * MIT License
 *
 * Copyright (c) 2024 Xpdustry
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

import arc.struct.ObjectMap;


public class PlayerColorCodes /*extends arc.util.ColorCodes*/ {
  public static final String
    prefix = "&",
  
    // Some modifiers are empty because no color code (E.g. [blue]) are corresponding 
    flush = "[clear]", // just doesn't render the text until next color
    reset = "[]", // "[white]", // force white color for the rest
    bold = "",
    italic = "",
    underline = "",
    
    black = "[darkgray]",
    red = "[brick]",
    green = "[lime]",
    yellow = "[gold]",
    blue = "[blue]",
    purple = "[purple]",
    cyan = "[#40E0D0]", //turquoise
    white = "[white]",
    
    lightBlack = "[lightgray]",
    lightRed = "[scarlet]",
    lightGreen = "[green]",
    lightYellow = "[yellow]",
    lightBlue = "[#1E90FF]", //dodger
    lightMagenta = "[magenta]",
    lightCyan = "[cyan]",
    lightWhite = "[white]",
  
    backDefault = "",
    backRed = "",
    backGreen = "",
    backYellow = "",
    backBlue = "";

  public static final String[] codes, values;

  static{
    ObjectMap<String, String> map = ObjectMap.of(
      "ff", flush,
      "fr", reset,
      "fb", bold,
      "fi", italic,
      "fu", underline,
      "k", black,
      "lk", lightBlack,
      "lw", lightWhite,
      "r", red,
      "g", green,
      "y", yellow,
      "b", blue,
      "p", purple,
      "c", cyan,
      "lr", lightRed,
      "lg", lightGreen,
      "ly", lightYellow,
      "lm", lightMagenta,
      "lb", lightBlue,
      "lc", lightCyan,
      "w", white,

      "bd", backDefault,
      "br", backRed,
      "bg", backGreen,
      "by", backYellow,
      "bb", backBlue
    );

    codes = map.keys().toSeq().toArray(String.class);
    values = map.values().toSeq().toArray(String.class);
  }
 
  public static String apply(String text, boolean useColors) {
    if (useColors) {
      for (int i=0; i<codes.length; i++)
        text = text.replace(prefix + codes[i], values[i]);
    } else {
      for (String c : codes)
        text = text.replace(prefix + c, "");
    }
    
    return text;
  }
}
