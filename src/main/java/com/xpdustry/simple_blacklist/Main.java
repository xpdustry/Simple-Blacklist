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

import java.util.regex.Pattern;

import com.xpdustry.simple_blacklist.util.Logger;
import com.xpdustry.simple_blacklist.util.Strings;
import com.xpdustry.simple_blacklist.util.Subnet;
import com.xpdustry.simple_blacklist.util.VersionChecker;


public class Main extends mindustry.mod.Plugin {
  private static final Logger logger = new Logger();
  
  @Override
  public void init() {
    // First, check for updates
    VersionChecker.checkAndPromptToUpgrade(mindustry.Vars.mods.getMod(getClass()).meta);
    // After, load settings
    Config.init(getConfig());
    Config.load();
    // Import old settings, in the server's config, if necessary
    if (Config.needSettingsMigration()) {
      logger.warn("Detected an old configuration, in the server settings. Migrating the config...");
      Config.migrateOldSettings();
    }
    // And register plugin listeners
    Manager.registerListeners();

    //Warn about Anti-VPN-Service if present and the subnet list isn't empty
    if (!Config.subnetList.get().isEmpty() && mindustry.Vars.mods.locateMod("anti-vpn-service") != null) {
      logger.none();
      logger.warn("Detected that &lcAnti-VPN-Service&fr plugin is present and enabled.");
      logger.warn("Please merge the subnet list and use this plugin instead, for advanced features.");
    }
  }


  @Override
  public void registerServerCommands(arc.util.CommandHandler handler) {
    // Command only for server console
    handler.register("blacklist", "[help|command] [args...]", "Control the blacklist. (use 'blacklist help' for usage)", args -> {
      if (args.length == 0) {
        // Print settings
        logger.info("Settings:");
        logger.info("| &lc" + Config.mode.name +"&fr: @", Config.mode.get().desc);
        logger.info("| | &fi" + Config.mode.desc);
        logger.info("|");
        logger.info("| &lc" + Config.nameMessage.name +"&fr: @", Config.nameMessage.get().isEmpty() ? "&fi(default)" : 
                                                                 Config.nameMessage.get());
        logger.info("| | &fi" + Config.nameMessage.desc);
        logger.info("|");
        logger.info("| &lc" + Config.ipMessage.name +"&fr: @", Config.ipMessage.get().isEmpty() ? "&fi(default)" : 
                                                               Config.ipMessage.get());
        logger.info("| | &fi" + Config.ipMessage.desc);
        logger.info("|");
        logger.info("| &lc" + Config.ignoreAdmins.name + "&fr: @", Config.ignoreAdmins.get() ? "yes" : "no");
        logger.info("| | &fi" + Config.ignoreAdmins.desc);
        logger.info("|");
        logger.info("| &lc" + Config.nameCaseSensitive.name + "&fr: @", Config.nameCaseSensitive.get() ? "yes" : "no");
        logger.info("| | &fi" + Config.nameCaseSensitive.desc);
        
        
        // Format name and regex lists
        arc.struct.Seq<String> 
          left = Strings.lJust(Config.namesList.get().keys().toArray().map(s -> "| "+s), 
                               Strings.best(Config.namesList.get(), e -> e.key.length()+2)),
          right = Strings.lJust(Config.regexList.get().keys().toArray().map(s -> "  | "+s), 
                                Strings.best(Config.regexList.get(), e -> e.key.pattern().length()+4));

        left = Strings.sJust(left, Config.namesList.get().entries().toArray().map(t -> " (uses: "+t.value+")"), 0);
        right = Strings.sJust(right, Config.regexList.get().entries().toArray().map(t -> " (uses: "+t.value+")"), 0);

        left.insert(0, Config.namesList.desc+": ["+
                       (Config.namesList.get().isEmpty() ? "empty" : "total: "+Config.namesList.get().size)+", "+
                       (Config.namesEnabled.get() ? "enabled" : "disabled")+"]");
        right.insert(0, "  "+Config.regexList.desc+": ["+
                       (Config.regexList.get().isEmpty() ? "empty" : "total: "+Config.regexList.get().size)+", "+
                       (Config.regexEnabled.get() ? "enabled" : "disabled")+"]");

        left = Strings.lJust(left, Strings.bestLength(left));
        right = Strings.lJust(right, Strings.bestLength(right));

        // Print name and regex lists
        logger.none();
        Strings.sJust(left, right, Strings.bestLength(left)+Strings.bestLength(right))
               .each(logger::info);
        
        // Format subnet list
        left = Strings.lJust(Config.subnetList.get().keys().toArray().map(s -> "| "+s), 
                             Strings.best(Config.subnetList.get(), e -> e.key.toString().length()+2));

        // Print subnet list
        logger.none();
        logger.info(Config.subnetList.desc+": ["+
                    (Config.subnetList.get().isEmpty() ? "empty" : "total: "+Config.subnetList.get().size)+", "+
                    (Config.subnetEnabled.get() ? "enabled" : "disabled")+"]");
        Strings.sJust(left, Config.subnetList.get().entries().toArray().map(t -> " (uses: "+t.value+")"), 0)
               .each(logger::info);
        
        return;
        
      }

      switch (args[0]) {
        default:
          logger.err("Invalid arguments. Use 'blacklist help' to see usage.");
          return;

        case "help":
          logger.info("Usage:  blacklist\n"
                    + "   or:  blacklist help\n"
                    + "   or:  blacklist reload\n"
                    + "   or:  blacklist <names|regex|subnets> <add|del> <value...>\n"
                    + "   or:  blacklist <names|regex|subnets|ignore-admin|case-sensitive> <on|off>\n"
                    + "   or:  blacklist mode <ban-ip|ban-uuid|kick>\n"
                    + "   or:  blacklist <name-message|ip-message> <text...>\n\n"
                    + "Description:\n"
                    + "  Allows to filter player nicknames, which contain specific text or matches a regex.\n"
                    + "  Can also prohibit specific IP addresses or entire address ranges,\n"
                    + "  but for advanced features, please use the Anti-VPN-Service plugin instead.\n\n"
                    + "  To create good regex, I recommend these websites:\n"
                    + "    - https://regex101.com/\n"
                    + "    - https://regex-generator.olafneumann.org/\n\n"
                    + "Notes:\n"
                    + "  - Colors and glyphs are removed before nickname verification.\n"
                    + "  - The \"\" (double quotes) value can be used to specify an empty value.\n"
                    + "  - The subnet list cannot ignore admin players and can only kick players, \n"
                    + "    because at his working point, it's just a connect request.\n");
          return;

        case "reload":
          Config.load();
          logger.info("Configuration reloaded.");
          return;
          
        case "names":
          if (args.length < 2) break;
          else if (args[1].startsWith("add")) {
            String arg = args[1].substring(3).trim();
            if (arg.isEmpty()) break;
            
            if (!Config.namesList.get().containsKey(arg)) {
              Config.namesList.getForChange().put(arg, 0);
              logger.info("Nickname added to the list.");
              Manager.checkOnlinePlayers();

            } else logger.err("Nickname already in the list.");

          } else if (args[1].startsWith("del")) {
            String arg = args[1].substring(3).trim();
            if (arg.isEmpty()) break;
            
            if (Config.namesList.get().containsKey(arg)) {
              Config.namesList.getForChange().remove(arg);
              logger.info("Nickname removed from the list");

            } else logger.err("Nickname not in the list");
            
          } else if (Strings.isTrue(args[1])) {
            Config.namesEnabled.set(true);
            logger.info("Enabled nickname list.");
            
          } else if (Strings.isFalse(args[1])) {
            Config.namesEnabled.set(false);
            logger.info("Disabled nickname list.");
            
          } else logger.err("Invalid argument. Must be 'add', 'del', 'on' or 'off'.");
          return;
            
        case "regex":
          if (args.length < 2) break;
          else if (args[1].startsWith("add")) {
            String arg = args[1].substring(3).trim();
            if (arg.isEmpty()) break;

            if (Structs_find(Config.regexList.get().keys(), p -> p.pattern().equals(arg)) == null) {
              Pattern pattern = null;
              // Check if regex is valid
              try {
                pattern = Pattern.compile(arg);
                if (pattern.matcher("test string") == null) pattern = null;
              } catch (java.util.regex.PatternSyntaxException e) {}
              
              if (pattern == null) {
                logger.err("Bad formatted regex '@'.", arg);
                return;
              }

              Config.regexList.getForChange().put(pattern, 0);
              logger.info("Regex added to the list.");
              Manager.checkOnlinePlayers();

            } else logger.err("Regex already in the list.");
            
          } else if (args[1].startsWith("del")) {
            String arg = args[1].substring(3).trim();
            if (arg.isEmpty()) break;
            
            Pattern pattern = Structs_find(Config.regexList.get().keys(), p -> p.pattern().equals(arg));
            if (pattern != null) {
              Config.regexList.getForChange().remove(pattern);
              logger.info("Regex removed from the list");

            } else logger.err("Regex not in the list");

          } else if (Strings.isTrue(args[1])) {
            Config.regexEnabled.set(true);
            logger.info("Enabled regex list.");
            
          } else if (Strings.isFalse(args[1])) {
            Config.regexEnabled.set(false);
            logger.info("Disabled regex list.");
            
          } else logger.err("Invalid argument. Must be 'add', 'del', 'on' or 'off'.");
          return;
          
        case "subnet":
          if (args.length < 2) break;
          else if (args[1].startsWith("add")) {
            String arg = args[1].substring(3).trim();
            if (arg.isEmpty()) break;
            
            if (Structs_find(Config.subnetList.get().keys(), s -> s.toString().equals(arg)) == null) {
              Subnet subnet;
              // Check if valid
              if (!com.xpdustry.simple_blacklist.util.InetAddressValidator.isValid(arg) ||
                  (subnet = Subnet.createInstance(arg)) == null) {
                logger.err("Bad formatted IP/Subnet '@'.", arg);
                return;
              }

              Config.subnetList.getForChange().put(subnet, 0);
              logger.info("IP/Subnet added to the list.");
              Manager.checkOnlinePlayers();

            } else logger.err("IP/Subnet already in the list.");
            
          } else if (args[1].startsWith("del")) {
            String arg = args[1].substring(3).trim();
            if (arg.isEmpty()) break;

            Subnet subnet = Structs_find(Config.subnetList.get().keys(), s -> s.toString().equals(arg));
            if (subnet != null) {
              Config.subnetList.getForChange().remove(subnet);
              logger.info("IP/Subnet removed from the list");

            } else logger.err("IP/Subnet not in the list");
            
          } else if (Strings.isTrue(args[1])) {
            Config.subnetEnabled.set(true);
            logger.info("Enabled subnet list.");
            
          } else if (Strings.isFalse(args[1])) {
            Config.subnetEnabled.set(false);
            logger.info("Disabled subnet list.");
            
          } else logger.err("Invalid argument. Must be 'add', 'del', 'on' or 'off'.");
          return;
          
        case "ignore-admin":
          if (args.length < 2) break;
          else if (Strings.isTrue(args[1])) {
            Config.regexEnabled.set(true);
            logger.info("Blacklists will ignore admin players.");
            
          } else if (Strings.isFalse(args[1])) {
            Config.regexEnabled.set(false);
            logger.info("Blacklists will check everyone.");
            
          } else logger.err("Invalid argument. Must be 'on' or 'off'.");
          return;
          
        case "case-sensitive":
          if (args.length < 2) break;
          else if (Strings.isTrue(args[1])) {
            Config.nameCaseSensitive.set(true);
            logger.info("Nickname list is now case sensitive.");
            
          } else if (Strings.isFalse(args[1])) {
            Config.nameCaseSensitive.set(false);
            logger.info("Nickname list will now ignore the case.");
            
          } else logger.err("Invalid argument. Must be 'on' or 'off'.");
          return;
          
        case "mode":
          if (args.length < 2) break;
          switch (args[1]) {
            case "ban-ip":
              Config.mode.set(Config.WorkingMode.banip);
              logger.info("Working mode sets to ban the player IP.");
              return;
              
            case "ban-uuid":
              Config.mode.set(Config.WorkingMode.banuuid);
              logger.info("Working mode sets to ban the player UUID.");
              return;
              
            case "kick":
              Config.mode.set(Config.WorkingMode.kick);
              logger.info("Working mode sets to kick the player.");
              return;
              
            default:
              logger.err("Invalid argument. Working mode must be 'ban-ip', 'ban-uuid' or 'kick'.");
              return;
          }
          
        case "name-message":
          if (args.length < 2) break;
          else if (args[1].equals("\"\"")) {
            Config.nameMessage.set("");
            logger.info("Kick message for blacklisted nickname sets to default.");
            
          } else {
            Config.nameMessage.set(args[1]);
            logger.info("Kick message for blacklisted nickname modified.");
          }
          return;
          
        case "ip-message":
          if (args.length < 2) break;
          else if (args[1].equals("\"\"")) {
            Config.ipMessage.set("");
            logger.info("Kick message for blacklisted IP sets to default.");
            
          } else {
            Config.ipMessage.set(args[1]);
            logger.info("Kick message for blacklisted IP modified.");
          }
          return;
      }
      
      logger.err("Missing argument(s). Use 'blacklist help' to see usage.");
    });
  }
  
  /** Idk why there is only {@link arc.util.Structs#find(Object[], Boolf)}, with object array, and not with iterable. */
  private static <T> T Structs_find(Iterable<T> array, arc.func.Boolf<T> value){
    for(T t : array) {
      if (value.get(t)) return t;
    }
    return null;
  }
}
