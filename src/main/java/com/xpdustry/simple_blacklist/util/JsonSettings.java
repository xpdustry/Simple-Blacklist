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

package com.xpdustry.simple_blacklist.util;

import java.io.IOException;

import arc.files.Fi;
import arc.struct.ArrayMap;
import arc.struct.ObjectMap;
import arc.util.io.Streams;
import arc.util.serialization.*;


public class JsonSettings {
  protected final Fi file;
  protected ArrayMap<String, JsonValue> values = new ArrayMap<>();
  protected boolean modified;
  protected final JsonReader reader = new JsonReader();
  protected Json json;

  public JsonSettings(Fi file) {
    this.file = file;
    setJson(new Json());
  }

  public void setJson(Json json) {
    this.json = json;
    this.json.setOutputType(JsonWriter.OutputType.json);
  }
  
  public Json getJson() {
    return json;
  }

  public boolean modified() {
    return modified;
  }

  /** Loads all values. */
  public synchronized void load() {
    if (!fileExists()) {
      save();
      return;
    }
  
    try { loadValues(getFile()); } 
    catch (Throwable error) { throw new RuntimeException(error); }
    modified = false;
  }

  /** Saves all values. */
  public synchronized void save() {
    if (!modified) return;
    
    try { saveValues(getFile()); }
    catch (Throwable error) { throw new RuntimeException(error); }
    modified = false;
  }

  public synchronized void loadValues(Fi file) throws IOException {
    java.io.Reader r = null;
    try { 
      JsonValue content = reader.parse(r = file.reader(8192));
      
      if (content != null) {
        for (JsonValue child=content.child, last=null; child!=null; last=child, child=child.next) {
          // for security
          child.parent = child.prev = null;
          if (last != null) last.next = null;
          
          values.put(child.name, child);    
        }
      }
    } 
    catch (Throwable e) { throw new IOException("Error reading file: " + file, e); } 
    finally { Streams.close(r); }
  }

  public synchronized void saveValues(Fi file) throws IOException {
    java.io.Writer w = null;
    try {
      JsonWriterBuilder builder = new JsonWriterBuilder();

      builder.object();
      for (ObjectMap.Entry<String, JsonValue> e : values) builder.set(e.key, e.value);
      builder.close();

      Strings.jsonPrettyPrint(builder.getJson(), w = new java.io.BufferedWriter(file.writer(false), 8192), 
                              JsonWriter.OutputType.json);
    }
    catch (Throwable e) { throw new IOException("Error writing file: " + file, e); } 
    finally { Streams.close(w); }
  }

  /** Return whether the file exists or not. */
  public boolean fileExists() {
    return file.exists();
  }
  
  /** Returns the file used for writing settings to. */
  public Fi getFile() {
    return file;
  }

  /** Clears all preference values. */
  public synchronized void clear(){
      values.clear();
      modified = true;
  }

  public synchronized boolean has(String name) {
    return values.containsKey(name);
  }
  
  public void put(String name, Object value) {
    put(name, null, value);
  }
  
  public synchronized <E> void put(String name, Class<E> elementType, Object value) {
    // Value is already a json object, no need to serialize it
    if (value instanceof JsonValue) {
      values.put(name, (JsonValue)value);
      modified = true;
      return;
    }
    
    try {
      JsonWriterBuilder builder = new JsonWriterBuilder();

      json.setWriter(builder);
      json.writeValue(value, value == null ? null : value.getClass(), elementType);

      values.put(name, builder.getJson());
      modified = true;  
        
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  } 
  
  public <T> T get(String name, Class<T> type, T def) {
    return get(name, type, null, def);
  }

  public synchronized <T, E> T get(String name, Class<T> type, Class<E> elementType, T def) {
    if (!has(name)) return def;
    
    try {
      T decoded = json.readValue(type, elementType, values.get(name));
      // if null, then the json was not decoded correctly 
      if (decoded == null) throw new SerializationException("failed to decode json");
      return decoded;
      
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }  
  
  public <T> T getOrPut(String name, Class<T> type, T def) {
    return getOrPut(name, type, null, def);
  }
  
  /** Put and return {@code def} if the {@code name} key is not found */
  public synchronized <T, E> T getOrPut(String name, Class<T> type, Class<E> elementType, T def) {
    if (!has(name)) {
      put(name, elementType, def);
      return def;
    }
    return get(name, type, elementType, def);
  }

  public float getFloat(String name, float def) {
    return getOrPut(name, Float.class, def);
  }
  
  public double getDouble(String name, double def) {
    return getOrPut(name, Double.class, def);
  }

  public int getInt(String name, int def) {
    return getOrPut(name, Integer.class, def);
  }
  
  public long getLong(String name, long def) {
    return getOrPut(name, Long.class, def);
  }

  public boolean getBool(String name, boolean def) {
    return getOrPut(name, Boolean.class, def);
  }

  public String getString(String name, String def) {
    return getOrPut(name, String.class, def);
  }

  public float getFloat(String name) {
    return get(name, Float.class, 0f);
  }
  
  public double getDouble(String name) {
    return get(name, Double.class, 0d);
  }

  public int getInt(String name) {
    return get(name, Integer.class, 0);
  }
  
  public long getLong(String name) {
    return get(name, Long.class, 0l);
  }
 
  public boolean getBool(String name) {
    return get(name, Boolean.class, false);
  }

  /** Runs the specified code once, and never again. */
  public void getBoolOnce(String name, Runnable run) {
    if (!getBool(name)) {
      run.run();
      put(name, true);
    }
  }

  /** Returns {@code false} once, and never again. */
  public boolean getBoolOnce(String name) {
    boolean val = getBool(name);
    if (!val) put(name, true);
    return val;
  }

  /** @return {@code null} if not found */
  public String getString(String name) {
    return get(name, String.class, null);
  }
  
  public synchronized void remove(String name) {
    values.removeKey(name);
    modified = true;
  }

  public synchronized Iterable<String> keys() {
    return values.keys();
  }

  public synchronized int size() {
    return values.size;
  }
}
