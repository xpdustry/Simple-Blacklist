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

package com.xpdustry.simple_blacklist;

import com.xpdustry.simple_blacklist.util.Log;
import com.xpdustry.simple_blacklist.util.Strings;

import arc.Events;
import arc.func.Cons;
import arc.struct.ObjectIntMap;
import arc.struct.Seq;

import mindustry.net.Packets.KickReason;

import static com.xpdustry.simple_blacklist.Events.*;
import static mindustry.game.EventType.*;
import static mindustry.Vars.netServer;


public class Manager {
  public static void registerListeners() {
    // Name blacklist listener
    Cons<ConnectPacketEvent> listener = e -> {
      e.connection.uuid = e.packet.uuid; // For console visual 

      // Handle case of multiple connection of client
      if (e.connection.hasBegunConnecting) {
        e.connection.kick(KickReason.idInUse, 0);
        return;
          
      // Check client validity
      } else if (e.packet.uuid == null || e.packet.usid == null) {
        e.connection.kick(KickReason.idInUse, 0);
        return;
        
      // Check if the nickname is valid
      } else if (e.packet.name == null || 
                 (e.packet.name = netServer.fixName(e.packet.name)).trim().length() <= 0) {
        e.connection.kick(KickReason.nameEmpty, 0);
        return;
      }
      
      Events.fire(new CheckingNicknameEvent(e.packet.name, e.packet.uuid, e.connection, e.packet));
      
      // Ignore if it's an admin and the 'ignore-admins' option is enabled
      mindustry.net.Administration.PlayerInfo pInfo = netServer.admins.getInfoOptional(e.packet.uuid);
      if (Config.ignoreAdmins.get() && pInfo != null && 
          pInfo.admin && e.packet.usid.equals(pInfo.adminUsid)) 
        return;

      // Check if the nickname is blacklisted
      if (!isValidName(e.packet.name)) {
        if (Config.mode.get() == Config.WorkingMode.banuuid) {
          /* The player UUID will be banned.
           * So we need to manually create an account
           * and filling it with as much informations as possible, if not already.
           * 
           * This avoids to create empty accounts BUT not filling the server settings.
           */
          if (pInfo == null) {
            netServer.admins.updatePlayerJoined(e.packet.uuid, e.connection.address, e.packet.name);
            pInfo = netServer.admins.getInfo(e.packet.uuid);
            pInfo.adminUsid = e.packet.usid;
            // the client never joined the server, this value can be used as a filter, to know all invalid accounts
            pInfo.timesJoined = 0; 
          }
          
          netServer.admins.banPlayerID(e.packet.uuid);
          
        } else if (Config.mode.get() == Config.WorkingMode.banip)
          netServer.admins.banPlayerIP(e.connection.address);

        Log.info("Kicking client '@' [@] for a blacklisted nickname.", e.connection.address, e.packet.uuid);
        if (Config.message.get().isEmpty()) 
          e.connection.kick(Config.mode.get() == Config.WorkingMode.kick ? KickReason.kick : KickReason.banned, 
                            pInfo != null ? 30*1000 : 0);
        else e.connection.kick(Config.message.get(), pInfo != null ? 30*1000 : 0);
        Events.fire(new BlacklistedNicknameEvent(e.packet.name, e.packet.uuid, e.connection, e.packet));
      }
    };

    
    // Try to move listeners at top of lists
    try {
      arc.struct.ObjectMap<Object, Seq<Cons<?>>> events = arc.util.Reflect.get(Events.class, "events");
      
      events.get(ConnectPacketEvent.class, () -> new Seq<>(Cons.class)).insert(0, listener);

    } catch (RuntimeException err) {
      Log.warn("Unable to get access of Events.class, because of a security manager!");
      Log.warn("Falling back to a normal event...");

      Events.on(ConnectPacketEvent.class, listener);
    }
    
    // Add a listener when exiting the server
    arc.Core.app.addListener(new arc.ApplicationListener() {
      public void dispose() { Config.save(); }
    });  
  }

  /** 
   * @return {@code true} if the {@code name} is valid. If it's not in the name list and doesn't match with any regex.
   * @apiNote this will returns {@code true} if lists are both disabled.
   */
  public static boolean isValidName(String name) {
    name = Strings.normalise(name);
    
    if (Config.namesEnabled.get()) {
      String name0 = Config.nameCaseSensitive.get() ? name : name.toLowerCase();
      for (ObjectIntMap.Entry<String> e : Config.namesList.get()) {
        if (Config.nameCaseSensitive.get() ? name.contains(e.key) : name0.contains(e.key.toLowerCase())) {
          int old = Config.namesList.getForChange().increment(e.key);
          arc.Events.fire(new NicknameListUpdatedEvent(e.key, old+1));
          return false;
        }
      }
    }
    
    if (Config.regexEnabled.get()) {
      for (ObjectIntMap.Entry<java.util.regex.Pattern> e : Config.regexList.get()) {
        if (e.key.matcher(name).matches()) {
          int old = Config.regexList.getForChange().increment(e.key);
          arc.Events.fire(new RegexListUpdatedEvent(e.key, old+1));
          return false;
        }
      }
    }
    
    return true;
  }

  public static void checkOnlinePlayers() {
    mindustry.gen.Groups.player.each(p -> {
      // Ignore admins if enabled
      if (Config.ignoreAdmins.get() && p.admin) return;

      Events.fire(new CheckingNicknameEvent(p.name, p.uuid(), p.con, null));
      if (!isValidName(p.name)) {
        Log.info("Kicking player '@' [@] for a blacklisted nickname.", Strings.normalise(p.name), p.uuid());
        if (Config.mode.get() == Config.WorkingMode.banip) netServer.admins.banPlayerIP(p.con.address);
        else if (Config.mode.get() == Config.WorkingMode.banuuid) netServer.admins.banPlayerID(p.uuid());
        if (Config.message.get().isEmpty()) 
             p.kick(Config.mode.get() == Config.WorkingMode.kick ? KickReason.kick : KickReason.banned);
        else p.kick(Config.message.get());
        Events.fire(new BlacklistedNicknameEvent(p.name, p.uuid(), p.con, null));
      }      
    });
  }
}
