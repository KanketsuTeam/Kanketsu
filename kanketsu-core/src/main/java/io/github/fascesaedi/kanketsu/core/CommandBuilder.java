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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class CommandBuilder {
    private final String name;
    private final Map<String, Command> children = new HashMap<>();
    private final Map<String, Option> options = new LinkedHashMap<>();
    private Consumer<CommandContext> action = args -> {};

    CommandBuilder(String name) {
        this.name = name;
    }

    public CommandBuilder command(String name, Consumer<CommandBuilder> consumer) {
        if (children.containsKey(name)) {
            throw new CommandException(1, "Build failed", "Duplicate subcommand: " + name);
        }
        CommandBuilder subBuilder = new CommandBuilder(name);
        consumer.accept(subBuilder);
        children.put(name, subBuilder.build());
        return this;
    }

    public CommandBuilder option(Option option) {
        options.put(option.getLongOpt(), option);
        return this;
    }

    public CommandBuilder option(String longOpt, String description) {
        return option(new Option.Builder(longOpt)
                .description(description)
                .hasArg(false)
                .build());
    }

    public CommandBuilder option(String longOpt, String description, boolean hasArg) {
        return option(new Option.Builder(longOpt)
                .description(description)
                .hasArg(hasArg)
                .build());
    }

    public CommandBuilder option(String longOpt, String shortOpt, String description, boolean hasArg) {
        return option(new Option.Builder(longOpt)
                .shortOpt(shortOpt)
                .description(description)
                .hasArg(hasArg)
                .build());
    }

    public CommandBuilder option(String longOpt, String shortOpt, String description, boolean hasArg, String defaultValue) {
        return option(new Option.Builder(longOpt)
                .shortOpt(shortOpt)
                .description(description)
                .hasArg(hasArg)
                .defaultValue(defaultValue)
                .build());
    }

    public CommandBuilder option(String longOpt, String shortOpt, String description) {
        return option(new Option.Builder(longOpt)
                .shortOpt(shortOpt)
                .description(description)
                .build());
    }

    public CommandBuilder action(Consumer<CommandContext> action) {
        this.action = action;
        return this;
    }

    Command build() {
        return new Command(name, children, options, action);
    }
}