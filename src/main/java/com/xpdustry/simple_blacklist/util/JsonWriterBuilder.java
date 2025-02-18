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

import java.io.IOException;

import arc.util.serialization.BaseJsonWriter;
import arc.util.serialization.JsonValue;
import arc.util.serialization.JsonValue.ValueType;


/** Build a JsonValue object */
public class JsonWriterBuilder implements BaseJsonWriter {
  private JsonValue root, current, last;
  private String name;

  public JsonValue getJson() {
    //if (current != null && base != current) //Useless
    //  throw new IllegalStateException("Builder must be closed before getting the result");
    return root;
  }
  
  @Override
  /** No-op */
  public void setOutputType(arc.util.serialization.JsonWriter.OutputType outputType) {}

  @Override
  /** No-op */
  public void setQuoteLongValues(boolean quoteLongValues) {}

  @Override
  public BaseJsonWriter name(String name) throws IOException {
    if (current == null || current.isArray())
      throw new IllegalStateException("Current item must be an object.");
    if (name == null) throw new NullPointerException("name cannot be null");
      
    this.name = name;
    return this;
  }

  @Override
  public BaseJsonWriter value(Object object) throws IOException {
    requireName();
    if (current != null && !current.isArray() && !current.isObject()) 
      throw new IllegalStateException("Current item must be an object or an array.");
    
    JsonValue jval;
    
    if (object == null) jval = new JsonValue(ValueType.nullValue);
    else if (object instanceof Number) {
        Number number = (Number)object;
        if (object instanceof Byte) jval = new JsonValue(number.byteValue());
        else if (object instanceof Short) jval = new JsonValue(number.shortValue());
        else if (object instanceof Integer) jval = new JsonValue(number.intValue());
        else if (object instanceof Long) jval = new JsonValue(number.longValue());
        else if (object instanceof Float) jval = new JsonValue(number.floatValue());
        else if (object instanceof Double) jval = new JsonValue(number.doubleValue());
        else throw new IOException("Unknown number object type.");
    } else if (object instanceof CharSequence || 
               object instanceof Character) jval = new JsonValue(object.toString());
    else if (object instanceof Boolean) jval = new JsonValue((boolean)object);
    else if (object instanceof JsonValue) {
      JsonValue json = (JsonValue) object;
      if (json.isBoolean()) jval = new JsonValue(json.asBoolean());
      else if (json.isLong()) jval = new JsonValue(json.asLong());
      else if (json.isDouble()) jval = new JsonValue(json.asDouble());
      else if (json.isString()) jval = new JsonValue(json.asString());
      else if (json.isNull()) jval = new JsonValue(JsonValue.ValueType.nullValue);
      else {
        jval = new JsonValue(json.type());
        jval.child = json.child;
      }
    } else throw new IOException("Unknown object type.");
    
    if (root == null) root = current = jval;
    else addValue(jval);
    return this;
  }
  
  @Override
  public BaseJsonWriter object() throws IOException {
    requireName();
    newChild(false);
    return this;
  }

  @Override
  public BaseJsonWriter array() throws IOException {
    requireName();
    newChild(true);
    return this;
  }
  
  private void addValue(JsonValue value) {
    if (current.child == null || last == null) {
      current.addChild(name, value);
      last = current.child;
      while (last.next != null) last = last.next;
    
    } else {
      if (name != null) value.name = new String(name); // idk how this works
      value.parent = current;
      last.next = value;
      last = last.next;
    }
    
    name = null;
  }
  
  private void newChild(boolean array) {
    JsonValue newValue = new JsonValue(array ? ValueType.array : ValueType.object);
    JsonValue old = current;
    
    if (current == null) current = newValue;
    if (root == null) root = current;
    if (old != null) {
      addValue(newValue);
      current = newValue;
    }
    last = null;
  }
  
  
  private void requireName() {
    if (current != null && !current.isArray() && name == null)
      throw new IllegalStateException("Name must be set.");
  }
  
  @Override
  public BaseJsonWriter object(String name) throws IOException {
      return name(name).object();
  }

  @Override
  public BaseJsonWriter array(String name) throws IOException {
      return name(name).array();
  }

  @Override
  public BaseJsonWriter set(String name, Object value) throws IOException {
      return name(name).value(value);
  }

  @Override
  public BaseJsonWriter pop() throws IOException {
    if (name != null) 
      throw new IllegalStateException("Expected an object, array or value, since a name was set.");
    last = current;
    if (last != null && (last.isArray() || last.isObject())) {
      while (last.next != null) last = last.next;
      current = current.parent;      
    }
    return this;
  }

  @Override
  public void close() throws IOException {
    while (current != null && root != current) 
      pop();
  }

  public void clear() {
    root = current = last = null;
    name = null;
  }
}
