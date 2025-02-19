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

import com.xpdustry.simple_blacklist.util.Log;
import com.xpdustry.simple_blacklist.util.Strings;

import arc.struct.Seq;


public class Main extends mindustry.mod.Plugin {
  @Override
  public void init() {
    // Init logger
    Log.init(mindustry.Vars.mods.getMod(getClass()).meta.displayName, getClass());
    
    // Load settings
    Config.init(getConfig());
    Config.load();
    
    // Import old settings, in the server's config, if necessary
    if (Config.needSettingsMigration()) {
      Log.warn("Detected an old configuration, in the server settings. Migrating the config...");
      Config.migrateOldSettings();
    }
    
    // Register plugin listeners
    Manager.registerListeners();
  }

  @Override
  public void registerServerCommands(arc.util.CommandHandler handler) {
    // Command only for server console
    handler.register("blacklist", "[command] [args...]", "Control the blacklist. (use 'blacklist help' for usage)", args -> {
      if (args.length == 0) {
        // Print settings
        Log.info("Settings:");
        Log.info("&lk|&fr " + Config.mode.desc +": @", Config.mode.get().desc);
        Log.info("&lk|&fr " + Config.message.desc +": @", Config.message.get().isEmpty() ? "&fi(default)" : 
                                                                 Config.message.get());
        Log.info("&lk|&fr " + Config.ignoreAdmins.desc + ": @", Config.ignoreAdmins.get() ? "yes" : "no");
        Log.info("&lk|&fr " + Config.nameCaseSensitive.desc + ": @", Config.nameCaseSensitive.get() ? "yes" : "no");

        // Format the lists
        Seq<String> left = Strings.lJust(Config.namesList.get().keys().toArray().map(s -> "&lk|&fr "+s), 
                                         Strings.best(Config.namesList.get(), e -> e.key.length()+8)),
                    right = Strings.lJust(Config.regexList.get().keys().toArray().map(s -> "  &lk|&fr "+s), 
                                          Strings.best(Config.regexList.get(), e -> e.key.pattern().length()+10));

        left = Strings.sJust(left, Config.namesList.get().entries().toArray().map(t -> " &fi(uses: &lb"+t.value+"&lw)&fr"), 0);
        right = Strings.sJust(right, Config.regexList.get().entries().toArray().map(t -> " &fi(uses: &lb"+t.value+"&lw)&fr"), 0);

        left.insert(0, Config.namesList.desc+": ["+
                       (Config.namesList.get().isEmpty() ? "&lb&fbempty&fr" : "total: &lb&fb"+
                         Config.namesList.get().size)+"&fr, "+
                       (Config.namesEnabled.get() ? "&lgenabled&fr" : "&lrdisabled&fr")+"]");
        right.insert(0, "  "+Config.regexList.desc+": ["+
                       (Config.regexList.get().isEmpty() ? "&lb&fbempty" : "total: &lb&fb"+
                         Config.regexList.get().size)+"&fr, "+
                       (Config.regexEnabled.get() ? "&lgenabled&fr" : "&lrdisabled&fr")+"]");

        Strings_lJust(left);
        Strings_lJust(right);

        // Print the lists
        Log.none();
        Strings_sJust(left, right).each(Log::info);
        return;
        
      }

      switch (args[0]) {
        default:
          Log.err("Invalid arguments. Use 'blacklist help' to see usage.");
          return;

        case "help":
          Log.info("Usage:  blacklist\n"
                 + "   or:  blacklist help\n"
                 + "   or:  blacklist reload\n"
                 + "   or:  blacklist <names|regex> <add|del> <value...>\n"
                 + "   or:  blacklist <names|regex|ignore-admin|case-sensitive> <on|off>\n"
                 + "   or:  blacklist mode <ban-ip|ban-uuid|kick>\n"
                 + "   or:  blacklist message <text...>\n\n"
                 + "Description:\n"
                 + "  Allows to filter player nicknames, which contain specific text or matches a regex.\n\n"
                 + "  To create good regex, I recommend these websites:\n"
                 + "    - https://regex101.com/\n"
                 + "    - https://regex-generator.olafneumann.org/\n\n"
                 + "Notes:\n"
                 + "  - Colors and glyphs are removed before nickname verification.\n"
                 + "  - The \"\" (double quotes) value can be used to specify an empty value.\n ");
          return;

        case "reload":
          Config.load();
          Log.info("Configuration reloaded.");
          return;
          
        case "names":
          if (args.length < 2) break;
          else if (args[1].startsWith("add")) {
            String arg = args[1].substring(3).trim();
            if (arg.isEmpty()) break;
            
            if (!Config.namesList.get().containsKey(arg)) {
              Config.namesList.getForChange().put(arg, 0);
              Log.info("Nickname added to the list.");
              Manager.checkOnlinePlayers();

            } else Log.err("Nickname already in the list.");

          } else if (args[1].startsWith("del")) {
            String arg = args[1].substring(3).trim();
            if (arg.isEmpty()) break;
            
            if (Config.namesList.get().containsKey(arg)) {
              Config.namesList.getForChange().remove(arg);
              Log.info("Nickname removed from the list");

            } else Log.err("Nickname not in the list");
            
          } else if (Strings.isTrue(args[1])) {
            Config.namesEnabled.set(true);
            Log.info("Enabled nickname list.");
            
          } else if (Strings.isFalse(args[1])) {
            Config.namesEnabled.set(false);
            Log.info("Disabled nickname list.");
            
          } else Log.err("Invalid argument. Must be 'add', 'del', 'on' or 'off'.");
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
                Log.err("Bad formatted regex '@'.", arg);
                return;
              }

              Config.regexList.getForChange().put(pattern, 0);
              Log.info("Regex added to the list.");
              Manager.checkOnlinePlayers();

            } else Log.err("Regex already in the list.");
            
          } else if (args[1].startsWith("del")) {
            String arg = args[1].substring(3).trim();
            if (arg.isEmpty()) break;
            
            Pattern pattern = Structs_find(Config.regexList.get().keys(), p -> p.pattern().equals(arg));
            if (pattern != null) {
              Config.regexList.getForChange().remove(pattern);
              Log.info("Regex removed from the list");

            } else Log.err("Regex not in the list");

          } else if (Strings.isTrue(args[1])) {
            Config.regexEnabled.set(true);
            Log.info("Enabled regex list.");
            
          } else if (Strings.isFalse(args[1])) {
            Config.regexEnabled.set(false);
            Log.info("Disabled regex list.");
            
          } else Log.err("Invalid argument. Must be 'add', 'del', 'on' or 'off'.");
          return;
          
        case "ignore-admin":
          if (args.length < 2) break;
          else if (Strings.isTrue(args[1])) {
            Config.regexEnabled.set(true);
            Log.info("Blacklists will ignore admin players.");
            
          } else if (Strings.isFalse(args[1])) {
            Config.regexEnabled.set(false);
            Log.info("Blacklists will check everyone.");
            
          } else Log.err("Invalid argument. Must be 'on' or 'off'.");
          return;
          
        case "case-sensitive":
          if (args.length < 2) break;
          else if (Strings.isTrue(args[1])) {
            Config.nameCaseSensitive.set(true);
            Log.info("Nickname list is now case sensitive.");
            
          } else if (Strings.isFalse(args[1])) {
            Config.nameCaseSensitive.set(false);
            Log.info("Nickname list will now ignore the case.");
            
          } else Log.err("Invalid argument. Must be 'on' or 'off'.");
          return;
          
        case "mode":
          if (args.length < 2) break;
          switch (args[1]) {
            case "ban-ip":
              Config.mode.set(Config.WorkingMode.banip);
              Log.info("Working mode sets to ban the player IP.");
              return;
              
            case "ban-uuid":
              Config.mode.set(Config.WorkingMode.banuuid);
              Log.info("Working mode sets to ban the player UUID.");
              return;
              
            case "kick":
              Config.mode.set(Config.WorkingMode.kick);
              Log.info("Working mode sets to kick the player.");
              return;
              
            default:
              Log.err("Invalid argument. Working mode must be 'ban-ip', 'ban-uuid' or 'kick'.");
              return;
          }
          
        case "message":
          if (args.length < 2) break;
          else if (args[1].equals("\"\"")) {
            Config.message.set("");
            Log.info("Kick message for blacklisted nickname sets to default.");
            
          } else {
            Config.message.set(args[1]);
            Log.info("Kick message for blacklisted nickname modified.");
          }
          return;
      }
      
      Log.err("Missing argument(s). Use 'blacklist help' to see usage.");
    });
  }
  
  /** Idk why there is only {@link arc.util.Structs#find(Object[], Boolf)}, with object array, and not with iterable. */
  private static <T> T Structs_find(Iterable<T> array, arc.func.Boolf<T> value){
    for(T t : array) {
      if (value.get(t)) return t;
    }
    return null;
  }
  
  /** Optimized {@link Strings#lJust(Seq, int)} that ignore logging colors, automatically calculate max length. */
  private static void Strings_lJust(Seq<String> list) {
    Seq<Integer> sizes = list.map(l -> Log.removeColors(l).length());
    int length = Strings.best(sizes, e -> e);
    for (int i=0; i<list.size; i++) {
      if (sizes.get(i) < length) list.set(i, list.get(i)+Strings.repeat(" ", length-sizes.get(i)));
    }
  }
  
  /** Optimized {@link Strings#sJust(Seq, Seq, int)} that ignore logging colors, automatically calculate max length. */
  private static Seq<String> Strings_sJust(Seq<String> left, Seq<String> right) {
    Seq<Integer> sleft = left.map(l -> Log.removeColors(l).length()),
                 sright = right.map(l -> Log.removeColors(l).length());
    int length = Strings.best(sleft, e -> e)+Strings.best(sright, e -> e);
    Seq<String> arr = new Seq<>(Integer.max(left.size, right.size));
    int i = 0;

    for (; i<Integer.min(left.size, right.size); i++) {
      if (sleft.get(i)+sright.get(i) >= length) arr.add(left.get(i)+right.get(i));
      else arr.add(left.get(i)+Strings.repeat(" ", length-sleft.get(i)-sright.get(i))+right.get(i));
    }
    // Fill the rest
    for (; i<left.size; i++) {
      if (sleft.get(i) >= length) arr.add(left.get(i));
      else arr.add(left.get(i)+Strings.repeat(" ", length-sleft.get(i)));
    }
    for (; i<right.size; i++) {
      if (sright.get(i) >= length) arr.add(right.get(i));
      else arr.add(Strings.repeat(" ", length-sright.get(i))+right.get(i));
    }
    
    return arr;
  }
}
