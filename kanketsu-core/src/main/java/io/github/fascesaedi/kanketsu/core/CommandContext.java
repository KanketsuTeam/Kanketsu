/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.fascesaedi.kanketsu.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CommandContext {
    private final Map<String, String> options;
    private final List<String> positionalArgs;

    public CommandContext(Map<String, String> options, List<String> positionalArgs) {
        this.options = new LinkedHashMap<>(options);
        this.positionalArgs = new ArrayList<>(positionalArgs);
    }

    public Map<String, String> getOptions() { return options; }
    public List<String> getPositionalArgs() { return positionalArgs; }

    public String getString(String key) {
        return options.get(key);
    }

    public int getInt(String key) {
        String val = options.get(key);
        if (val == null) {
            throw new IllegalArgumentException("Option '" + key + "' is not present");
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Option '" + key + "' value '" + val + "' is not a valid integer");
        }
    }

    public int getInt(String key, int defaultValue) {
        String val = options.get(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Option '" + key + "' value '" + val + "' is not a valid integer");
        }
    }

    public long getLong(String key) {
        String val = options.get(key);
        if (val == null) {
            throw new IllegalArgumentException("Option '" + key + "' is not present");
        }
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Option '" + key + "' value '" + val + "' is not a valid long");
        }
    }

    public long getLong(String key, long defaultValue) {
        String val = options.get(key);
        if (val == null) return defaultValue;
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Option '" + key + "' value '" + val + "' is not a valid long");
        }
    }

    public boolean getBoolean(String key) {
        String val = options.get(key);
        if (val == null) return false;
        return "true".equalsIgnoreCase(val) || Boolean.parseBoolean(val);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String val = options.get(key);
        if (val == null) return defaultValue;
        return "true".equalsIgnoreCase(val) || Boolean.parseBoolean(val);
    }

    public boolean hasOption(String key) {
        return options.containsKey(key);
    }
}
