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

import mindustry.gen.Player;


/** Class to redirect logging messages to a player, with the same usage as {@link Logger} */
public class PlayerLogger extends Logger {
  public final Player player;

  public PlayerLogger(Player player) {
    this.player = player;
  }
  
  /** Send a message to the player */
  public synchronized void send(String text, Object... args) {
    text = Strings.format(text.replace("@", "&lb@&fr"), args);
    player.sendMessage(PlayerColorCodes.apply(text, Log.useColors));
  }  
  
  @Override
  public void log(LogLevel level, String text, Object... args) {
    if (Log.level.ordinal() > level.ordinal()) return;
    String prefix = level == LogLevel.debug ? "&lc" :
                    level == LogLevel.info ? "" :
                    level == LogLevel.warn ? "&ly" :
                    level == LogLevel.err ? "&lr":
                    "";
    send(prefix + text, args);
  }
  
  @Override
  /** There are already a gap between new messages/lines */
  public void none() {}
}
