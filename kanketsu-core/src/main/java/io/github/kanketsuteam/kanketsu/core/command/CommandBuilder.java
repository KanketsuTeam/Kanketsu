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
package io.github.kanketsuteam.kanketsu.core.command;

import io.github.kanketsuteam.kanketsu.core.exception.CommandBuildException;
import io.github.kanketsuteam.kanketsu.core.Option;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class CommandBuilder {
    private final String name;
    private final String description;
    private final Map<String, Command> children = new HashMap<>();
    private final Map<String, Option> options = new LinkedHashMap<>();
    private Consumer<CommandContext> action = null;

    public CommandBuilder(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public CommandBuilder command(String name, String description,Consumer<CommandBuilder> consumer) {
        if (children.containsKey(name)) {
            throw new CommandBuildException("Duplicate subcommand: " + name);
        }
        CommandBuilder subBuilder = new CommandBuilder(name, description);
        consumer.accept(subBuilder);
        children.put(name, subBuilder.build());
        return this;
    }

    public CommandBuilder command(String name, Consumer<CommandBuilder> consumer){
        return command(name, "No description", consumer);
    }

    public CommandBuilder option(Option option) {
        options.put(option.getLongOpt(), option);
        return this;
    }

    public CommandBuilder option(String longOpt, Consumer<Option.Builder> config) {
        Option.Builder builder = new Option.Builder(longOpt);
        config.accept(builder);
        return option(builder.build());
    }

    public CommandBuilder action(Consumer<CommandContext> action) {
        this.action = action;
        return this;
    }

    public Command build() {
        return new Command(name, description, children, options, action);
    }
}