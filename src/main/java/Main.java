import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Strings;
import mindustry.Vars;

import mindustry.game.EventType;


public class Main extends mindustry.mod.Plugin {
  private ObjectMap<String, Integer> blacklist = new ObjectMap<String, Integer>(); // Blacklist, and number of times nickname as kicked player
  private String message = "a part of your nickname is prohibited"; // Message for player
  private boolean mode = false, // just kick or ban
                  listenerPriority = true; // Priority of blacklist listener, true to put listener as first for this event

  @SuppressWarnings("unchecked")
  public Main() {
    if (Core.settings.has("simple-blacklist")) blacklist = Core.settings.getJson("simple-blacklist", ObjectMap.class, ObjectMap::new);
    if (Core.settings.has("simple-blacklist-message")) message = Core.settings.getString("simple-blacklist-message");
    if (Core.settings.has("simple-blacklist-mode")) mode = Core.settings.getBool("simple-blacklist-mode");
    if (Core.settings.has("simple-blacklist-priority")) listenerPriority = Core.settings.getBool("simple-blacklist-priority");
    
   
    // Blacklist listener
    Cons<EventType.ConnectPacketEvent> listener = e -> {
      if (e.packet.uuid == null || e.packet.usid == null) e.connection.kick("not an mindustry client!");
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
      Log.err("Please remove the security manager if you want a first priority execution for backlist");
      
      // Falling back to default listener priority
      Events.on(EventType.ConnectPacketEvent.class, listener);
    }
  }
  
  public void saveSettings() {
    Core.settings.putJson("simple-blacklist", blacklist);
    Core.settings.put("simple-blacklist-message", message);
    Core.settings.put("simple-blacklist-mode", mode);
    Core.settings.put("simple-blacklist-priority", listenerPriority);
  }

  public boolean validateName(String name) {
    for (ObjectMap.Entry<String, Integer> b : blacklist) {
      if (Strings.stripColors(Strings.stripGlyphs(name)).contains(b.key)) {
        blacklist.put(b.key, b.value+1);
        Core.settings.putJson("simple-blacklist", blacklist);
        
        return false;
      }
    }
    return true;
  }
  
  public void checkPlayer(mindustry.gen.Player p) {
    if (!validateName(p.name)) {
      if (mode) mindustry.Vars.netServer.admins.banPlayer(p.uuid());
       p.kick(message);
    }
  }
  
  public static String lJust(String str, int newLenght) { return lJust(str, newLenght, " "); }
  public static String lJust(String str, int newLenght, String filler) {
      if (filler.length() >= str.length() + newLenght) return str;
      
      while (str.length() < newLenght) str += filler;
      return str;
  }
  
  @Override
  public void registerServerCommands(arc.util.CommandHandler handler) {
    handler.register("blacklist", "[help|arg0] [arg1...]", "Control the blacklist of nicknames (colors and glyphs will be cutted on check)", args -> {
      if (args.length == 0) {
        if (blacklist.isEmpty()) Log.info("Blacklist is empty");
        else {
          Log.info("Blacklist of nicknames: [Total: @]", blacklist.size);
          int best = 0;
          for (ObjectMap.Entry<String, Integer> b : blacklist) {
            if (b.key.length() > best) best = b.key.length();
          }
          final int bestSize = best;
          blacklist.each((n, t) -> Log.info("| @ (times used: @)", lJust(n, bestSize), t));
        }
        return;
      }
      
      switch (args[0]) {
        case "help":
          Log.info("Usage: blacklist");
          Log.info("       blacklist help");
          Log.info("       blacklist add|remove <nickname...>");
          Log.info("       blacklist mode [ban|kick]");
          Log.info("       blacklist priority [first|default]");
          Log.info("       blacklist message [text...]");
          Log.info("");
          Log.info("Options:");
          Log.info("  <no options>      Print the blacklist and times a nickname kicked");
          Log.info("                    a player");
          Log.info("  help              Show this help and exit");
          Log.info("  add nickname      Add a nickname to blacklist");
          Log.info("  remove nickname   Remove a nickname from blacklist");
          Log.info("  mode              Print the actual working mode");
          Log.info("  mode ban          Set the working mode to ban player if his");
          Log.info("                    nickname contains one of list");
          Log.info("  mode kick         Set the working mode to kick player if his");
          Log.info("                    nickname contains one of list");
          Log.info("  priority first    Set the blacklist listerner at top of list.");
          Log.info("                    This can be used like a first security barrier");
          Log.info("  priority default  Leave the blacklisted listener at priority");
          Log.info("                    where it was declared. This can change depending");
          Log.info("                    on order in which the server loads plugins.");
          Log.info("  message           Print current message sent to kicked player.");
          Log.info("                    &fiNote: on ban of player, it's also sent to player");
          Log.info("  message text      Set the message to sent to kicked player");
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
              
            } else Log.err("Listener priority must be 'first' or 'default'");
          } else Log.info("Actual priority of blacklist listener is @", listenerPriority ? "first of list" : "default");
          break;
          
        case "message":
          if (args.length == 2) {
            Log.info("Message modified");
            message = args[1];
            saveSettings();
            
          } else Log.info("Actual message is: @", message);
          break;
          
        default: Log.err("Invalid argument. try 'help' argument to see usage");
      }
    });
  }
}