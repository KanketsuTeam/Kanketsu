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
    private final Map<Option, String> options;
    private final List<String> positionalArgs;
    private final Map<String, Option> optionDefs;

    public CommandContext(Map<Option, String> options, List<String> positionalArgs, Map<String, Option> optionDefs) {
        this.options = new LinkedHashMap<>(options);
        this.positionalArgs = new ArrayList<>(positionalArgs);
        this.optionDefs = new LinkedHashMap<>(optionDefs);
    }

    public Map<Option, String> getOptions() { return options; }
    public List<String> getPositionalArgs() { return positionalArgs; }

    public String getOption(Option key) {
        return options.get(key);
    }

    public boolean hasOption(Option key) {
        return options.containsKey(key);
    }

    public Object getOption(String longOpt) {
        Option opt = optionDefs.get(longOpt);
        if (opt == null) {
            return null;
        }

        String rawValue = options.get(opt);
        if (rawValue == null) {
            return null;
        }

        Category category = opt.getCategory();
        switch (category) {
            case STRING:
                return rawValue;

            case BOOLEAN:
                return Boolean.parseBoolean(rawValue);

            case INT:
                try {
                    return Integer.parseInt(rawValue);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            String.format("Option --%s requires an integer, but '%s' was entered", longOpt, rawValue), e);
                }

            case LONG:
                try {
                    return Long.parseLong(rawValue);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            String.format("Option --%s requires a long integer, but '%s' was entered", longOpt, rawValue), e);
                }

            default:
                return rawValue;
        }
    }

    public boolean hasOption(String longOpt) {
        Option opt = optionDefs.get(longOpt);
        return opt != null && hasOption(opt);
    }

}
