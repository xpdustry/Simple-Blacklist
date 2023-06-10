import java.util.regex.Pattern;

import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;

import mindustry.Vars;
import mindustry.game.EventType;


public class Main extends mindustry.mod.Plugin {
  private ObjectMap<String, Integer> blacklist = new ObjectMap<>(), // Blacklist, and number of times nickname as kicked player
                                     regexBlacklist = new ObjectMap<>();
  private ObjectMap<String, Pattern> compiledRegex = new ObjectMap<>(); // Store pattern of compiled regex
  private String message = "a part of your nickname is prohibited"; // Message for player
  private boolean mode = false, // just kick or ban
                  listenerPriority = true, // Priority of blacklist listener, true to put listener as first for this event
                  regexPriority = false,
                  enabled = true,
                  regexEnabled = true;

  @SuppressWarnings("unchecked")
  public Main() {
    if (Core.settings.has("simple-blacklist")) blacklist = Core.settings.getJson("simple-blacklist", ObjectMap.class, ObjectMap::new);
    if (Core.settings.has("simple-blacklist-regexlist")) regexBlacklist = Core.settings.getJson("simple-blacklist-regexlist", ObjectMap.class, ObjectMap::new);
    if (Core.settings.has("simple-blacklist-message")) message = Core.settings.getString("simple-blacklist-message");
    if (Core.settings.has("simple-blacklist-mode")) mode = Core.settings.getBool("simple-blacklist-mode");
    if (Core.settings.has("simple-blacklist-priority")) listenerPriority = Core.settings.getBool("simple-blacklist-priority");
    if (Core.settings.has("simple-blacklist-regex")) regexPriority = Core.settings.getBool("simple-blacklist-regex");
    if (Core.settings.has("simple-blacklist-enabled")) enabled = Core.settings.getBool("simple-blacklist-enabled");
    if (Core.settings.has("simple-blacklist-regex-enabled")) regexEnabled = Core.settings.getBool("simple-blacklist-regex-enabled");

    // Compile paterns
    regexBlacklist.each((r, t) -> compiledRegex.put(r, Pattern.compile(r)));
   
    // Blacklist listener
    Cons<EventType.ConnectPacketEvent> listener = e -> {
      if (e.packet.uuid == null || e.packet.usid == null) return;
      mindustry.net.Administration.PlayerInfo pInfo = Vars.netServer.admins.getInfoOptional(e.packet.uuid);
      
      if (pInfo != null && (pInfo.banned || arc.util.Time.millis() < Vars.netServer.admins.getKickTime(e.packet.uuid, e.connection.address))) return;
      else if (!validateName(e.packet.name)) {
        if (mode) Vars.netServer.admins.banPlayerID(e.packet.uuid);
        e.connection.kick(message);
      }
    };
     
    if (!listenerPriority) { // Listener set to default priority
      Events.on(EventType.ConnectPacketEvent.class, listener); 
      return;
    }
    
    // Try to move the listener at top for this event
    try {
      // Get events by changing variable as public instead of private
      final java.lang.reflect.Field field = Events.class.getDeclaredField("events");
      field.setAccessible(true);
      final Seq<Cons<?>> events = ((ObjectMap<Object, Seq<Cons<?>>>) field.get(null)).get(EventType.ConnectPacketEvent.class, () -> new Seq<>(Cons.class));
      
      // Now we got access to all event, remove the actual handle and place it a new at top of listeners
      events.insert(0, listener);
      
    } catch (final ReflectiveOperationException | SecurityException err) {
      Log.err("A security manager is present in this java version! Cannot put the blacklist listeners first in events list.");
      Log.err("Please remove the security manager if you want a first priority execution for backlist listener");

      // Falling back to default listener priority
      Events.on(EventType.ConnectPacketEvent.class, listener);
    }
  }
  
  public void saveSettings() {
    Core.settings.putJson("simple-blacklist", blacklist);
    Core.settings.putJson("simple-blacklist-regexlist", regexBlacklist);
    Core.settings.put("simple-blacklist-message", message);
    Core.settings.put("simple-blacklist-mode", mode);
    Core.settings.put("simple-blacklist-priority", listenerPriority);
    Core.settings.put("simple-blacklist-regex", regexPriority);
    Core.settings.put("simple-blacklist-enabled", enabled);
    Core.settings.put("simple-blacklist-regex-enabled", regexEnabled);
  }

  public boolean validateName(String name) {
    if (regexPriority) return !(checkInRegex(name) || checkInNames(name));
    return !(checkInNames(name) || checkInRegex(name));
  }

  public boolean checkInNames(String name) {
    if (!enabled) return false;

    for (ObjectMap.Entry<String, Integer> b : blacklist) {
      if (Strings.normalise(name).contains(b.key)) {
        blacklist.put(b.key, b.value+1);
        Core.settings.putJson("simple-blacklist", blacklist);
        
        return true;
      }
    }
    return false;
  }

  public boolean checkInRegex(String name) {
    if (!regexEnabled) return false;

    for (ObjectMap.Entry<String, Integer> b : regexBlacklist) {
      if (compiledRegex.get(b.key).matcher(Strings.normalise(name)).matches()) {
        regexBlacklist.put(b.key, b.value+1);
        Core.settings.putJson("simple-regexlist", regexBlacklist);
        
        return true;
      }
    }
    return false;
  }
  
  public void checkPlayer(mindustry.gen.Player p) {
    if (!validateName(p.name)) {
      if (mode) mindustry.Vars.netServer.admins.banPlayer(p.uuid());
       p.kick(message);
    }
  }
  
  @Override
  public void registerServerCommands(arc.util.CommandHandler handler) {
    handler.register("blacklist", "[help|arg0] [arg1...]", "Control the blacklist. (use 'blacklist help' for more infos)", args -> {
      if (args.length == 0) {
        if (blacklist.isEmpty() && regexBlacklist.isEmpty()) Log.info("Blacklist is empty");
        else {
          Seq<String> left = Strings.lJust(blacklist.keys().toSeq().map(s -> "| "+s), Strings.bestLength(blacklist.keys())+2), 
                      right = Strings.lJust(regexBlacklist.keys().toSeq().map(s -> "  | "+s), Strings.bestLength(regexBlacklist.keys())+3);
          
          left = Strings.mJust(left, blacklist.values().toSeq().map(t -> " (times used: "+t+")"), 0);
          right = Strings.mJust(right, regexBlacklist.values().toSeq().map(t -> " (times used: "+t+")"), 0);
          
          left.insert(0, "Blacklist of nicknames: [total: "+blacklist.size+", "+(enabled ? "enabled" : "disabled")+"]");
          right.insert(0, "  Blacklist of regex: [total: "+regexBlacklist.size+", "+(regexEnabled ? "enabled" : "disabled")+"]");

          left = Strings.lJust(left, Strings.bestLength(left));
          right = Strings.lJust(right, Strings.bestLength(right));
          
          Strings.mJust(left, right, 0).each(s -> Log.info(s));
        }
        return;
      }
      
      switch (args[0]) {
        case "help":
          Log.info("Usage:  blacklist");
          Log.info("   or:  blacklist help");
          Log.info("   or:  blacklist <add|remove>[-regex] <nickname|regex...>");
          Log.info("   or:  blacklist mode [ban|kick]");
          Log.info("   or:  blacklist priority [first|default|regex|names]");
          Log.info("   or:  blacklist message [text...]");
          Log.info("   or:  blacklist <enable|disable> <names|regex>");
          Log.info("");
          Log.info("Description:");
          Log.info("  Any player nickname containing an item of regex or nicknames blacklist");
          Log.info("  will be kicked or banned, depending on the setting.");
          Log.info("  The blacklist with prioority set to 'first' can be used like a 'first");
          Log.info("  security barrier' (or second if you use an proxy).");
          Log.info("");
          Log.info("  To create good regex, I recommend these websites:");
          Log.info("    - https://regex101.com/");
          Log.info("    - https://regex-generator.olafneumann.org/");
          Log.info("");
          Log.info("Note:");
          Log.info("  - If mode is setted to ban, this will also show him the kick message");
          Log.info("  - Colors and glyphs are removed before nickname verification");
          Log.info("");
          Log.info("Options:");
          Log.info("  <no options>        Print the status of lists and the blacklist with");
          Log.info("                      times a nickname or regex kicked a player");
          Log.info("  help                Show this help and exit");
          Log.info("  add nickname        Add a nickname to blacklist");
          Log.info("  remove nickname     Remove a nickname from blacklist");
          Log.info("  add-regex regex     Add a regex to blacklist");
          Log.info("  remove-regex regex  Remove a regex to blacklist");
          Log.info("  mode                Print the actual working mode");
          Log.info("  mode ban            Set the working mode to ban player if his");
          Log.info("                      nickname contains one of list");
          Log.info("  mode kick           Set the working mode to kick player if his");
          Log.info("                      nickname contains one of list");
          Log.info("  priority            Print priority of listener and blacklists");
          Log.info("  priority first      Set the blacklist listerner at top of list.");
          Log.info("                      This can be used like a first security barrier");
          Log.info("  priority default    Leave the blacklisted listener at priority");
          Log.info("                      where it was declared. This can change depending");
          Log.info("                      on order in which the server loads plugins.");
          Log.info("  priority regex      Set regex list has priority over nicknames list");
          Log.info("  priority names      Set nicknames list has priority over regex list");
          Log.info("  message             Print current message sent to kicked player");
          Log.info("  message text        Set the message to sent to kicked player");
          Log.info("  enable names        Enable the nicknames blacklist");
          Log.info("  disable names       Disable the nicknames blacklist");
          Log.info("  enable regex        Enable the regex blacklist");
          Log.info("  disable regex       Disable the regex blacklist");
          Log.info("");
          break;
          
        case "add":
          if (args.length == 2) {
            if (!blacklist.containsKey(args[1])) {
              blacklist.put(args[1], 0);
              saveSettings();
              Log.info("Nickname added to blacklist");
              mindustry.gen.Groups.player.each(p -> checkPlayer(p));
              
            } else Log.err("Nickname already blacklisted");
          } else Log.err("Invalid usage. try 'help' argument to see usage");
          break;
          
        case "remove":
          if (args.length == 2) {
            if (blacklist.containsKey(args[1])) {
              blacklist.remove(args[1]);
              saveSettings();
              Log.info("Nickname removed from blacklist");
            
            } else Log.err("Nickname not blacklisted");
          } else Log.err("Invalid usage. try 'help' argument to see usage");
          break;
          
          case "add-regex":
          if (args.length == 2) {
            if (!regexBlacklist.containsKey(args[1])) {
              // Check if regex is valid
              try {
                if (Pattern.compile(args[1]).matcher("test string") == null) {
                  Log.err("Bad formatted regex '@'", args[1]);
                  break;
                }
              } catch (java.util.regex.PatternSyntaxException e) {
                Log.err("Bad formatted regex '@'", args[1]);
                break;
              }
              
              compiledRegex.put(args[1], Pattern.compile(args[1]));
              regexBlacklist.put(args[1], 0);
              saveSettings();
              Log.info("Regex added to blacklist");
              mindustry.gen.Groups.player.each(p -> checkPlayer(p));
              
            } else Log.err("Regex already blacklisted");
          } else Log.err("Invalid usage. try 'help' argument to see usage");
          break;
          
        case "remove-regex":
          if (args.length == 2) {
            if (regexBlacklist.containsKey(args[1])) {
              regexBlacklist.remove(args[1]);
              compiledRegex.remove(args[1]);
              saveSettings();
              Log.info("Regex removed from blacklist");
            
            } else Log.err("Regex not blacklisted");
          } else Log.err("Invalid usage. try 'help' argument to see usage");
          break;

        case "mode":
          if (args.length == 2) {
            if (args[1].equals("ban")) {
              mode = true;
              saveSettings();
              Log.info("Working mode set to ban the player");
              
            } else if (args[1].equals("kick")) {
              mode = false;
              saveSettings();
              Log.info("Working mode set to just kick the player");
              
            } else Log.err("Working mode must be 'kick' or 'ban'");
          } else Log.info("Actual working mode is to @", mode ? "ban player" : "just kick it");
          break;
        
        case "priority":
          if (args.length == 2) {
            if (args[1].equals("first")) {
              listenerPriority = true;
              saveSettings();
              Log.info("Listener priority set to first position in events list");
              Log.info("&gNow restart the server for the changes to take effect");
              
            } else if (args[1].equals("default")) {
              listenerPriority = false;
              saveSettings();
              Log.info("Listener priority set to default position in events list");
              Log.info("&gNow restart the server for the changes to take effect");
              
            } else if (args[1].equals("names")) {
              regexPriority = false;
              saveSettings();
              Log.info("Regex list set to have priority over nicknames list");
              
            } else if (args[1].equals("regex")) {
              regexPriority = true;
              saveSettings();
              Log.info("Nicknames list set to have priority over regex list");
              
            } else Log.err("Priority argument must be 'first', 'default', 'names' or 'regex'");
          } else {
            Log.info("Actual priority of blacklist listener is @", listenerPriority ? "first of list" : "default");
            Log.info("@ has priority over @ list", regexPriority ? "Regex" : "Nicknames", regexPriority ? "nicknames" : "regex");
          }
          break;
          
        case "message":
          if (args.length == 2) {
            Log.info("Message modified");
            message = args[1];
            saveSettings();
            
          } else Log.info("Actual message is: @", message);
          break;
          
        case "enable":
          if (args.length == 2) {
            if (args[1].equals("names")) {
              enabled = true;
              saveSettings();
              Log.info("Enabled nicknames blacklist");
              mindustry.gen.Groups.player.each(p -> checkPlayer(p));
              
            } else if (args[1].equals("regex")) {
              regexEnabled = true;
              saveSettings();
              Log.info("Enabled regex blacklist");
              mindustry.gen.Groups.player.each(p -> checkPlayer(p));
              
            } else Log.err("List type must be 'regex' or 'names'");
          } else Log.err("Invalid usage. try 'help' argument to see usage");
          break;

        case "disable":
          if (args.length == 2) {
            if (args[1].equals("names")) {
              enabled = false;
              saveSettings();
              Log.info("Disabled nicknames blacklist");
              
            } else if (args[1].equals("regex")) {
              regexEnabled = false;
              saveSettings();
              Log.info("Disabled regex blacklist");
              
            } else Log.err("List type must be 'regex' or 'names'");
          } else Log.err("Invalid usage. try 'help' argument to see usage");
          break;    

        default: Log.err("Invalid argument. try 'help' argument to see usage");
      }
    });
  }
}