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
package io.github.fascesaedi.kanketsu.core.command;

import io.github.fascesaedi.kanketsu.core.Option;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Command {
    private final String name;
    private final String description;
    final Map<String, Command> children;
    private final Map<String, Option> options;
    private final Consumer<CommandContext> action;
    private final Map<String, String> shortToLongMap;

    Command(String name, String description, Map<String, Command> children,
            Map<String, Option> options, Consumer<CommandContext> action) {
        this.name = name;
        this.description = description;
        this.children = new HashMap<>(children);
        this.options = new LinkedHashMap<>(options);
        this.action = action;
        this.shortToLongMap = new HashMap<>();
        for (Option opt : options.values()) {
            String shortOpt = opt.getShortOpt();
            if (shortOpt != null && !shortOpt.isEmpty()) {
                shortToLongMap.put(shortOpt, opt.getLongOpt());
            }
        }
    }

    public String getName() { return name; }
    public String getDescription(){return description;}
    public Map<String, Command> getChildren() { return Collections.unmodifiableMap(children); }
    public Map<String, Option> getOptions() { return Collections.unmodifiableMap(options); }
    public Consumer<CommandContext> getAction() { return action; }
    public Map<String, String> getShortToLongMap() {return shortToLongMap;}

    public int run(CommandContext ctx) {
        if (action != null) {
            action.accept(ctx);
            return 0;
        } else {
            return 1;
        }
    }
}