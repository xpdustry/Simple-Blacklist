import java.util.regex.Pattern;

import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;

import mindustry.Vars;
import mindustry.core.Version;
import mindustry.game.EventType;
import mindustry.net.Packets.KickReason;


public class Main extends mindustry.mod.Plugin {
  private ObjectMap<String, Integer> blacklist = new ObjectMap<>(), // Blacklist, and number of times nickname as kicked player
                                     regexBlacklist = new ObjectMap<>();
  private ObjectMap<String, Pattern> compiledRegex = new ObjectMap<>(); // Store pattern of compiled regex
  private String message = "a part of your nickname is prohibited"; // Message for player
  private boolean mode = false, // just kick or ban
                  listenerPriority = true, // Priority of blacklist listener, true to put listener as first for this event
                  regexPriority = false,
                  enabled = true,
                  regexEnabled = true,
                  ignoreAdmins = false;

  @SuppressWarnings("unchecked")
  public Main() {
    if (Core.settings.has("simple-blacklist")) blacklist = Core.settings.getJson("simple-blacklist", ObjectMap.class, ObjectMap::new);
    if (Core.settings.has("simple-blacklist-regexlist")) regexBlacklist = Core.settings.getJson("simple-blacklist-regexlist", ObjectMap.class, ObjectMap::new);
    if (Core.settings.has("simple-blacklist-message")) message = Core.settings.getString("simple-blacklist-message");
    if (Core.settings.has("simple-blacklist-settings")) {
      boolean[] settings = Strings.integer2binary(Core.settings.getInt("simple-blacklist-settings"));

      // Avoid errors when adding new settings
      try {
        mode = settings[1];
        listenerPriority = settings[2];
        regexPriority = settings[3];
        enabled = settings[4];
        regexEnabled = settings[5];
        ignoreAdmins = settings[6];
      } catch (IndexOutOfBoundsException e) { saveSettings(); }
    }

    // Compile paterns
    regexBlacklist.each((r, t) -> compiledRegex.put(r, Pattern.compile(r)));


    // Blacklist listener
    Cons<EventType.ConnectPacketEvent> listener = e -> {
      // Just for visual in console
      e.connection.uuid = e.packet.uuid;

      // Handle case of multiple connection of client
      if (e.connection.hasBegunConnecting) {
          e.connection.kick(KickReason.idInUse, 0);
          return;
      }

      // Avoid to continue verification by server if client is kicked
      e.connection.hasBegunConnecting = true;

      // Redo the verification of customers in a more logical way with a kick time of 0s to avoid creation of an empty account.
      // This avoids filling the backup with empty accounts if the server suffered a raid

      // First check if is an valid client
      if (e.packet.uuid == null || e.packet.usid == null) e.connection.kick(KickReason.idInUse, 0);

      // After check version of client
      else if (e.packet.versionType == null ||
              ((e.packet.version == -1 ||
                !e.packet.versionType.equals(Version.type)) &&
              Version.build != -1 && !Vars.netServer.admins.allowsCustomClients()))
        e.connection.kick(!Version.type.equals(e.packet.versionType) ? KickReason.typeMismatch : KickReason.customClient, 0);

      else if (e.packet.version != Version.build && Version.build != -1 && e.packet.version != -1)
        e.connection.kick(e.packet.version > Version.build ? KickReason.serverOutdated : KickReason.clientOutdated, 0);

      else {
        // Now check if the nickname is valid
        e.packet.name = Vars.netServer.fixName(e.packet.name);
        if (Vars.netServer.fixName(e.packet.name).trim().length() <= 0) {
          e.connection.kick(KickReason.nameEmpty, 0);
          return;
        }

        // And finish by checking if name is blacklisted
        mindustry.net.Administration.PlayerInfo pInfo = Vars.netServer.admins.getInfoOptional(e.packet.uuid);
        if (ignoreAdmins && pInfo != null && pInfo.admin) return;

        if (!validateName(e.packet.name)) {
          if (mode) {
            // The account will be banned,
            // so must be manually create an account and fill it with as much information as possible,
            // if it not already exist.
            // This also avoids to create empty accounts but not filling the server settings.

            if (pInfo == null) {
              Vars.netServer.admins.updatePlayerJoined(e.packet.uuid, e.connection.address, e.packet.name);
              pInfo = Vars.netServer.admins.getInfo(e.packet.uuid);
              pInfo.adminUsid = e.packet.usid;
              pInfo.timesJoined = 0; // the client never joined the server, this value can be used as a filter to know all invalid accounts
            }

            Vars.netServer.admins.banPlayerID(e.packet.uuid);
          }

          e.connection.kick(message, 0);
        
        } else e.connection.hasBegunConnecting = false; // client is valid so don't forgot to re-set this at default value
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
    Core.settings.put("simple-blacklist-settings",
      Strings.binary2integer(true, mode, listenerPriority, regexPriority, enabled, regexEnabled, ignoreAdmins));
  }                         // ^^ avoid losing data

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
        Core.settings.putJson("simple-blacklist-regexlist", regexBlacklist);

        return true;
      }
    }
    return false;
  }

  public void checkPlayer(mindustry.gen.Player p) {
    // Ignore admins if enabled
    if (ignoreAdmins && p.admin) return;

    if (!validateName(p.name)) {
      if (mode) mindustry.Vars.netServer.admins.banPlayerID(p.uuid());
       p.kick(message);
    }
  }

  @Override
  public void registerServerCommands(arc.util.CommandHandler handler) {
    // Command only for server console
    handler.register("blacklist", "[help|arg0] [arg1...]", "Control the blacklist. (use 'blacklist help' for more infos)", args -> {
      if (args.length == 0) {
        // Format list
        Seq<String> left = Strings.lJust(blacklist.keys().toSeq().map(s -> "| "+s), Strings.bestLength(blacklist.keys())+2),
                    right = Strings.lJust(regexBlacklist.keys().toSeq().map(s -> "  | "+s), Strings.bestLength(regexBlacklist.keys())+4);

        left = Strings.mJust(left, blacklist.values().toSeq().map(t -> " (times used: "+t+")"), 0);
        right = Strings.mJust(right, regexBlacklist.values().toSeq().map(t -> " (times used: "+t+")"), 0);

        left.insert(0, "Nicknames blacklist: ["+(blacklist.isEmpty() ? "empty" : "total: "+blacklist.size)+", "+(enabled ? "enabled" : "disabled")+"]");
        right.insert(0, "  Regex blacklist: ["+(regexBlacklist.isEmpty() ? "empty" : "total: "+regexBlacklist.size)+", "+(regexEnabled ? "enabled" : "disabled")+"]");

        left = Strings.lJust(left, Strings.bestLength(left));
        right = Strings.lJust(right, Strings.bestLength(right));

        // Print settings
        Log.info("Settings:");
        Log.info("| Working mode: @", mode ? "ban player" : "kick player");
        Log.info("| Listener priority: @", listenerPriority ? "first of list" : "default position");
        Log.info("| Kick message: @", message);
        Log.info("| List priority: nicknames @ regex", regexPriority ? "<==over==" : "==over==>");
        Log.info("| Ignore admin players: @", ignoreAdmins);

        // Print lists
        Log.info("");
        Strings.mJust(left, right, 0).each(s -> Log.info(s));
        return;
      }

      if (args[0].equals("help")) {
        Log.info("Usage:  blacklist");
        Log.info("   or:  blacklist help");
        Log.info("   or:  blacklist <add|remove>[-regex] <nickname|regex...>");
        Log.info("   or:  blacklist mode <ban|kick>");
        Log.info("   or:  blacklist priority <first|default|regex|names>");
        Log.info("   or:  blacklist message <text...>");
        Log.info("   or:  blacklist <enable|disable> <names|regex|ignore-admin>");
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
        Log.info("  <no options>          Print all settings about plugin");
        Log.info("  help                  Show this help and exit");
        Log.info("  add nickname          Add a nickname to blacklist");
        Log.info("  remove nickname       Remove a nickname from blacklist");
        Log.info("  add-regex regex       Add a regex to blacklist");
        Log.info("  remove-regex regex    Remove a regex to blacklist");
        Log.info("  mode ban              Set the working mode to ban player if his");
        Log.info("                        nickname contains one of list");
        Log.info("  mode kick             Set the working mode to kick player if his");
        Log.info("                        nickname contains one of list");
        Log.info("  priority first        Set the blacklist listerner at top of list.");
        Log.info("                        This can be used like a first security barrier");
        Log.info("  priority default      Leave the blacklisted listener at priority");
        Log.info("                        where it was declared. This can change depending");
        Log.info("                        on order in which the server loads plugins.");
        Log.info("  priority regex        Set regex list has priority over nicknames list");
        Log.info("  priority names        Set nicknames list has priority over regex list");
        Log.info("  message text          Set the message to sent to kicked player");
        Log.info("  enable names          Enable the nicknames blacklist");
        Log.info("  enable regex          Enable the regex blacklist");
        Log.info("  enable ignore-admin   Username of admin players will not be verified");
        Log.info("  disable names         Disable the nicknames blacklist");
        Log.info("  disable regex         Disable the regex blacklist");
        Log.info("  disable ignore-admin  All player's username will be verified");
        Log.info("");
        return;
      }


      if (args.length < 2) {
        Log.err("Invalid usage. try 'help' argument to see usage");
        return;
      }


      switch (args[0]) {
        case "add":
          if (!blacklist.containsKey(args[1])) {
            blacklist.put(args[1], 0);
            saveSettings();
            Log.info("Nickname added to blacklist");
            mindustry.gen.Groups.player.each(p -> checkPlayer(p));

          } else Log.err("Nickname already blacklisted");
          break;

        case "remove":
          if (blacklist.containsKey(args[1])) {
            blacklist.remove(args[1]);
            saveSettings();
            Log.info("Nickname removed from blacklist");

          } else Log.err("Nickname not blacklisted");
          break;

        case "add-regex":
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
          break;

        case "remove-regex":
          if (regexBlacklist.containsKey(args[1])) {
            regexBlacklist.remove(args[1]);
            compiledRegex.remove(args[1]);
            saveSettings();
            Log.info("Regex removed from blacklist");

          } else Log.err("Regex not blacklisted");
          break;

        case "mode":
          if (args[1].equals("ban")) {
            mode = true;
            saveSettings();
            Log.info("Working mode set to ban the player");

          } else if (args[1].equals("kick")) {
            mode = false;
            saveSettings();
            Log.info("Working mode set to just kick the player");

          } else Log.err("Working mode must be 'kick' or 'ban'");
          break;

        case "priority":
          switch (args[1]) {
            case "first":
              listenerPriority = true;
              Log.info("Listener priority set to first position in events list");
              Log.info("&gNow restart the server for the changes to take effect");
              break;

            case "default":
              listenerPriority = false;
              Log.info("Listener priority set to default position in events list");
              Log.info("&gNow restart the server for the changes to take effect");
              break;

            case "names":
              regexPriority = false;
              Log.info("Regex list set to have priority over nicknames list");
              break;

            case "regex":
              regexPriority = true;
              Log.info("Nicknames list set to have priority over regex list");
              break;

            default:
              Log.err("Priority argument must be 'first'/'default' or 'names'/'regex'");
              return;
          }
          saveSettings();
          break;

        case "message":
          Log.info("Message modified");
          message = args[1];
          saveSettings();
          break;

        case "enable":
          switch (args[1]) {
            case "names":
              enabled = true;
              Log.info("Enabled nicknames blacklist");
              mindustry.gen.Groups.player.each(p -> checkPlayer(p));
              break;

            case "regex":
              regexEnabled = true;
              Log.info("Enabled regex blacklist");
              mindustry.gen.Groups.player.each(p -> checkPlayer(p));
              break;

            case "ignore-admin":
              ignoreAdmins = true;
              Log.info("Username of admin players will not be verified");
              break;

            default:
              Log.err("Argument must be 'regex', 'names' or 'ignore-admin'");
              return;
          }
          saveSettings();
          break;

        case "disable":
          switch (args[1]) {
            case "names":
              enabled = false;
              Log.info("Disabled nicknames blacklist");
              break;

            case "regex":
              regexEnabled = false;
              Log.info("Disabled regex blacklist");
              break;

            case "ignore-admin":
              ignoreAdmins = false;
              Log.info("All player's username will be verified");
              break;

            default:
              Log.err("Argument must be 'regex', 'names' or 'ignore-admin'");
              return;
          }
          saveSettings();
          break;

        default:
          Log.err("Invalid argument. try 'help' argument to see usage");
      }
    });
  }
}
