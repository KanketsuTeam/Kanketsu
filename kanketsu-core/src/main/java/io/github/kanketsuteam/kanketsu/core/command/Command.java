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

import io.github.kanketsuteam.kanketsu.core.Option;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Represents a command that can be executed by the CLI framework.
 * <p>
 * A command has a name, an optional description, a map of subcommands,
 * a map of options, and an action (a {@link Consumer} of {@link CommandContext})
 * that is invoked when the command is run.
 * </p>
 * <p>
 * Instances of this class are immutable and are typically created through
 * a {@link CommandBuilder}.
 * </p>
 *
 * @see CommandBuilder
 * @see CommandContext
 */
public class Command {
    private final String name;
    private final String description;
    final Map<String, Command> children;
    private final Map<String, Option> options;
    private final Consumer<CommandContext> action;
    private final Map<String, String> shortToLongMap;

    /**
     * Package-private constructor used by {@link CommandBuilder}.
     *
     * @param name        the command name
     * @param description the command description
     * @param children    map of subcommand names to {@code Command} instances
     * @param options     map of option long names to {@link Option} instances
     * @param action      the action to execute when this command is run
     */
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

    /**
     * Returns the command name.
     *
     * @return the command name
     */
    public String getName() { return name; }

    /**
     * Returns the command description.
     *
     * @return the description, may be null
     */
    public String getDescription(){return description;}

    /**
     * Returns an unmodifiable map of subcommands.
     *
     * @return a map from subcommand name to {@code Command} instance
     */
    public Map<String, Command> getChildren() { return Collections.unmodifiableMap(children); }

    /**
     * Returns an unmodifiable map of options defined for this command.
     *
     * @return a map from option long name to {@link Option} instance
     */
    public Map<String, Option> getOptions() { return Collections.unmodifiableMap(options); }

    /**
     * Returns the action consumer for this command.
     *
     * @return the action, or {@code null} if no action is defined
     */
    public Consumer<CommandContext> getAction() { return action; }

    /**
     * Returns an unmodifiable map mapping short option names to their long names.
     *
     * @return a map from short option string to long option name
     */
    public Map<String, String> getShortToLongMap() {return shortToLongMap;}

    /**
     * Executes this command's action with the given context.
     * <p>
     * If an action is present, it is invoked and {@code 0} is returned.
     * Otherwise, {@code 1} is returned.
     * </p>
     *
     * @param ctx the command context containing parsed options and positional arguments
     * @return {@code 0} if the action executed successfully, {@code 1} if no action exists
     */
    public int run(CommandContext ctx) {
        if (action != null) {
            action.accept(ctx);
            return 0;
        } else {
            return 1;
        }
    }
}