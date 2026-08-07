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

/**
 * Builder for creating {@link Command} instances.
 * <p>
 * This builder allows you to define a command's name, description, subcommands,
 * options, and action in a fluent manner.
 * </p>
 *
 * @see Command
 */
public class CommandBuilder {
    private final String name;
    private final String description;
    private final Map<String, Command> children = new HashMap<>();
    private final Map<String, Option> options = new LinkedHashMap<>();
    private Consumer<CommandContext> action = null;

    /**
     * Creates a new builder for a command with the given name and description.
     *
     * @param name        the command name (must be unique among siblings)
     * @param description the command description (used in help output)
     */
    public CommandBuilder(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * Adds a subcommand to this command.
     * <p>
     * The provided {@code consumer} receives a new {@code CommandBuilder} for the
     * subcommand, allowing configuration of its options, subcommands, and action.
     * </p>
     *
     * @param name        the subcommand name
     * @param description the subcommand description
     * @param consumer    a callback to configure the subcommand
     * @return this builder (for chaining)
     * @throws CommandBuildException if a subcommand with the same name already exists
     */
    public CommandBuilder command(String name, String description, Consumer<CommandBuilder> consumer) {
        if (children.containsKey(name)) {
            throw new CommandBuildException("Duplicate subcommand: " + name);
        }
        CommandBuilder subBuilder = new CommandBuilder(name, description);
        consumer.accept(subBuilder);
        children.put(name, subBuilder.build());
        return this;
    }

    /**
     * Adds a subcommand with a default description ("No description").
     *
     * @param name     the subcommand name
     * @param consumer a callback to configure the subcommand
     * @return this builder (for chaining)
     * @see #command(String, String, Consumer)
     */
    public CommandBuilder command(String name, Consumer<CommandBuilder> consumer){
        return command(name, "No description", consumer);
    }

    /**
     * Adds an option directly.
     *
     * @param option the {@link Option} instance to add
     * @return this builder (for chaining)
     */
    public CommandBuilder option(Option option) {
        options.put(option.getLongOpt(), option);
        return this;
    }

    /**
     * Adds an option using a lambda to configure its builder.
     * <p>
     * Example:
     * <pre>{@code
     * builder.option("verbose", opt -> opt.shortOpt("v").description("Verbose mode"));
     * }</pre>
     *
     * @param longOpt the long name of the option
     * @param config  a consumer that configures the {@link Option.Builder}
     * @return this builder (for chaining)
     */
    public CommandBuilder option(String longOpt, Consumer<Option.Builder> config) {
        Option.Builder builder = new Option.Builder(longOpt);
        config.accept(builder);
        return option(builder.build());
    }

    /**
     * Sets the action that will be executed when the command is run.
     * <p>
     * The action receives a {@link CommandContext} containing the parsed options
     * and positional arguments.
     * </p>
     *
     * @param action a {@link Consumer} that processes the command context
     * @return this builder (for chaining)
     */
    public CommandBuilder action(Consumer<CommandContext> action) {
        this.action = action;
        return this;
    }

    /**
     * Builds the {@link Command} instance with the configured properties.
     *
     * @return an immutable {@code Command} object
     */
    public Command build() {
        return new Command(name, description, children, options, action);
    }
}