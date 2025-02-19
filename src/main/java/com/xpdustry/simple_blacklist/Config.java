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

import java.util.regex.Pattern;

import com.xpdustry.simple_blacklist.util.JsonSettings;
import com.xpdustry.simple_blacklist.util.Strings;

import arc.Core;
import arc.struct.ObjectIntMap;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.serialization.Json;
import arc.util.serialization.JsonValue;


@SuppressWarnings({ "unchecked", "rawtypes" })
public class Config {
  public static final Seq<Field<?>> all = new Seq<>();
  protected static JsonSettings settings;
  
  public static void init(arc.files.Fi file) {
    settings = new JsonSettings(file);
    
    // Add serializers
    settings.getJson().setSerializer(Pattern.class, new Json.Serializer<Pattern>() {
      public void write(Json json, Pattern object, Class knownType) { json.writeValue(object.toString()); }
      public Pattern read(Json json, JsonValue jsonData, Class type) { return Pattern.compile(jsonData.asString()); }    
    });

    // Add an autosave task for every minutes
    arc.util.Timer.schedule(() -> {if (all.contains(Field::modified)) save();}, 60, 60);
  }
  
  public static synchronized void load() {
    if (settings == null) throw new IllegalStateException("#init() must be called before.");
    
    settings.load();
    all.each(f -> f.load());
  }
  
  public static synchronized void save() {
    all.each(f -> f.save());
    settings.save();
  }
  
  public static boolean needSettingsMigration() {
    return Core.settings.has("simple-blacklist") || Core.settings.has("simple-blacklist-regexlist") ||
           Core.settings.has("simple-blacklist-message") || Core.settings.has("simple-blacklist-settings");
  }

  public static void migrateOldSettings() {
    ObjectMap<String, Integer> map;
    if (Core.settings.has("simple-blacklist")) {
      map = Core.settings.getJson("simple-blacklist", ObjectMap.class, ObjectMap::new);
      ObjectIntMap<String> converted = new ObjectIntMap();
      map.each((k, v) -> converted.put(k, v));
      namesList.set(converted);
    }
    if (Core.settings.has("simple-blacklist-regexlist")) {
      map = Core.settings.getJson("simple-blacklist-regexlist", ObjectMap.class, ObjectMap::new);
      ObjectIntMap<Pattern> converted = new ObjectIntMap();
      map.each((k, v) -> converted.put(Pattern.compile(k), v));
      regexList.set(converted);
    }
    if (Core.settings.has("simple-blacklist-message")) 
      message.set(Core.settings.getString("simple-blacklist-message"));
    if (Core.settings.has("simple-blacklist-settings")) {
      boolean[] settings = Strings.int2bits(Core.settings.getInt("simple-blacklist-settings"));

      // Avoid errors when adding new settings
      try {
        mode.set(settings[1] ? WorkingMode.banuuid : WorkingMode.kick);
        //listenerPriority.set(settings[2]); //now it's falling back automatically
        //regexPriority.set(settings[3]); // useless
        namesEnabled.set(settings[4]);
        regexEnabled.set(settings[5]);
        ignoreAdmins.set(settings[6]);
        if (settings[1] && settings[7]) mode.set(WorkingMode.banip);
      } catch (IndexOutOfBoundsException ignored) {}
    }

    // Save settings to the new system and delete old settings
    save();
    Core.settings.remove("simple-blacklist");
    Core.settings.remove("simple-blacklist-regexlist");
    Core.settings.remove("simple-blacklist-message");
    Core.settings.remove("simple-blacklist-settings");
  }
  
  
  public static enum WorkingMode {
    kick("kick player"), banuuid("ban player UUID"), banip("ban player IP");
    
    public final String desc;
    WorkingMode(String desc) { this.desc = desc; }
  }
  
  
  public static class Field<T> {
    public final Class<?> elementType;
    public final T defaultValue;
    public final String name, desc;
    protected boolean modified, loaded;
    protected T value;
    
    public Field(String name, String desc, T def) { this(name, desc, null, def); }
    public Field(String name, String desc, Class<?> elementType, T def) {
      this.name = name;
      this.desc = desc;
      this.defaultValue = def;
      this.elementType = elementType;

      all.add(this);
    }
    
    public boolean modified() {
      return modified;
    }
    
    public T get() {
      if (!loaded) {
        load();
        loaded = true;
      }
      return value;
    }
    
    /** 
     * Same as {@link #get()} but assume that the object's content will be modified directly. 
     * So this will sets the {@link #modified()} flag.
     */
    public T getForChange() {
      T v = get();
      modified = true;
      return v;
    }
    
    public void set(T value) {
      this.value = value;
      modified = true;
      loaded = true;
    }

    public void load() {
      value = (T)settings.getOrPut(name, (Class<T>)defaultValue.getClass(), elementType, defaultValue);
      modified = false;
    }
    
    public void save() {
      if (modified) forcesave();
    }
    
    public void forcesave() {
      settings.put(name, elementType, loaded ? value : defaultValue);
      modified = false;
    }
    
    @Override
    public String toString() {
      return String.valueOf(get());
    }
  }
  
  // Settings
  public static final Field<Boolean> 
    namesEnabled = new Field<>("names-enabled", "", true),
    regexEnabled = new Field<>("regex-enabled", "", true);
  
  public static final Field<ObjectIntMap<String>> 
    namesList = new Field("names", "Nickname list", String.class, new ObjectIntMap<>());
  public static final Field<ObjectIntMap<Pattern>> 
    regexList = new Field("regex", "Regex list", Pattern.class, new ObjectIntMap<>());

  public static final Field<String>
    message = new Field<>("message", "Kick message &fi(can be empty)&fr", "A part of your nickname is prohibited.");
  public static final Field<WorkingMode>
    mode = new Field<>("mode", "Working mode", WorkingMode.kick);
  public static final Field<Boolean>
    ignoreAdmins = new Field<>("ignore-admins", "Ignore admin players", false),
    nameCaseSensitive = new Field<>("case-sensitive", "Nickname list case sensitive", false);
}
