/*
 * This file is part of Simple Blacklist.
 *
 * MIT License
 *
 * Copyright (c) 2023-2025 Xpdustry
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

package com.xpdustry.simple_blacklist;

import com.xpdustry.simple_blacklist.util.Logger;
import com.xpdustry.simple_blacklist.util.PlayerLogger;

import arc.util.CommandHandler;

import mindustry.gen.Player;


public class Main extends mindustry.mod.Plugin {
  private static Logger logger = new Logger();
  
  @Override
  public void init() {
    // Init logger
    Logger.init(mindustry.Vars.mods.getMod(getClass()).meta.displayName, getClass());
    
    // Load settings
    Config.init(getConfig());
    Config.load();
    
    // Import old settings, in the server's config, if necessary
    if (Config.needSettingsMigration()) {
      logger.warn("Detected an old configuration, in the server settings. Migrating the config...");
      Config.migrateOldSettings();
    }
    
    // Register plugin listeners
    Manager.registerListeners();
  }

  @Override
  public void registerServerCommands(arc.util.CommandHandler handler) {
    handler.register("blacklist", "[command] [args...]", "Control the blacklist. (use 'blacklist help' for usage)", args -> 
      BlacklistCommand.run(args, logger));
  }
  
  @Override
  public void registerClientCommands(CommandHandler handler){
    handler.<Player>register("blacklist", "[command] [args...]", "Control the blacklist. (use '/blacklist help' for usage)", (args, player) -> {
      PlayerLogger plogger = new PlayerLogger(player);
      
      if (player.admin) BlacklistCommand.run(args, plogger); 
      else plogger.err("You need admin permissions to use this command.");
    });
  }
}
