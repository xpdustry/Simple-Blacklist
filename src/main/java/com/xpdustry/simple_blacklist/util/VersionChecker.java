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

import java.net.HttpURLConnection;

import arc.util.Http;


/** Class to simply check for updates of a mod. */
public class VersionChecker {
  private UpdateState state = UpdateState.error;
  /** Will be used as a value holder */
  private VersionChecker() {}
  
  private static final Logger logger = new Logger("Updater");
  
  public static String keyToFind = "tag_name";
  public static String repoLinkFormat = "https://github.com/@/releases/latest";
  public static String repoApiLinkFormat = mindustry.Vars.ghApi + "/repos/@/releases/latest";
  
  /** 
   * Check for update using the "version" and "repo" properties 
   * in the mod/plugin definition (plugin.json, mod.json). <br><br>
   * The github repo must be formatted like that "{@code <username>/<repo-name>}".<br>
   * The version can be formatted like that "{@code 146.2}" and can starts with "{@code v}", 
   * but must not contains letters, like "{@code beta}" or "{@code -dev}".
   * 
   * @return the update state
   */
  public static UpdateState checkAndPromptToUpgrade(mindustry.mod.Mods.ModMeta mod) {
    logger.info("Checking for updates...");
    
    if (mod.repo == null || mod.repo.isEmpty()) {
      logger.warn("No repo found, in mod meta, for update checking.");
      return UpdateState.missing;
    } else if (mod.version == null || mod.version.isEmpty()) {
      logger.warn("No version found, in mod meta, for update checking.");
      return UpdateState.missing;
    }
    
    VersionChecker holder = new VersionChecker();
    syncHttpGet(Strings.format(repoApiLinkFormat, mod.repo), success -> {
      if (success.getStatus() != Http.HttpStatus.OK) 
        throw new Http.HttpStatusException("not OK", success.getStatus(), success);
      
      try {
        String tag = new arc.util.serialization.JsonReader().parse(success.getResultAsStream())
                                                            .getString(keyToFind);
        
        // Compare the version
        if (Strings.isVersionAtLeast(mod.version, tag)) {
            logger.info("New version found: @. Current version: @", tag, mod.version);
            logger.info("Check out this link to upgrade: @", Strings.format(repoLinkFormat, mod.repo));
            holder.state = UpdateState.outdated;
        } else {
          logger.info("Already up-to-date, no need to upgrade.");
          holder.state = UpdateState.uptodate;
        }
        
      } catch (Exception e) {
        logger.err("Unable to parse json or find the key 'tag_name'.");
        logger.err("Error: @", e.getMessage());
        holder.state = UpdateState.error;
      }
      
    }, failure -> {
      logger.err("Failed to check for updates!");
      
      if (failure instanceof Http.HttpStatusException) {
        Http.HttpStatusException error = (Http.HttpStatusException) failure;
        Http.HttpStatus status = error.response.getStatus();
        int code;
        String message;
        
        // Why make this private Anuke???
        try {
          HttpURLConnection con = arc.util.Reflect.get(error.response, "connection");
          code = con.getResponseCode();
          
          message = error.response.getResultAsString().trim();
          if (message.isEmpty() || message.length() > 512) {
            String m = null;
            try { m = con.getResponseMessage(); } 
            catch (java.io.IOException ignored) {}
            
            message = m != null && !m.isEmpty() ? m : Strings.capitalize(status.toString().toLowerCase());
          }
          
        } catch (Exception e) { 
          code = status.code;
          message = Strings.capitalize(status.toString().toLowerCase());
        }
        
        logger.err("Status: @ '@'", code, message);
        
      } else logger.err("Error: @", failure.toString());

      holder.state = UpdateState.error;
    });
    
    return holder.state;
  }
  
  private static void syncHttpGet(String url, arc.func.ConsT<Http.HttpResponse, Exception> callback, 
                                  arc.func.Cons<Throwable> error) {
    Http.get(url).error(error).block(callback);
  }
  
  
  public static enum UpdateState {
    /** "version" or/and "repo" properties are missing in the mod/plugin definition. */
    missing,
    /** Error while checking for updates. */
    error, 
    /** No new updates found, it's the latest version. */
    uptodate,
    /** An update was found, the mod/plugin needs to be upgraded. */
    outdated
  }
}
