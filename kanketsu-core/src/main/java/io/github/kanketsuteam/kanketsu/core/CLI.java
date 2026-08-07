/*
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
package io.github.kanketsuteam.kanketsu.core;

import io.github.kanketsuteam.kanketsu.core.command.Command;
import io.github.kanketsuteam.kanketsu.core.command.CommandBuilder;
import io.github.kanketsuteam.kanketsu.core.command.CommandContext;
import io.github.kanketsuteam.kanketsu.core.exception.*;
import io.github.kanketsuteam.kanketsu.core.parser.OptionParser;
import io.github.kanketsuteam.kanketsu.spi.HelpGenerator;
import io.github.kanketsuteam.kanketsu.spi.Logger;
import io.github.kanketsuteam.kanketsu.spi.TypeConverter;

import java.util.*;

/**
 * The core entry point of the Kanketsu command-line framework.
 * <p>
 * It parses user command-line arguments, dispatches to registered root commands,
 * handles global options, and generates help output.
 * </p>
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 * CLI cli = CLI.builder()
 *     .command("git", cmd -> {
 *         cmd.action(ctx -> System.out.println("Git command executed"));
 *         cmd.option("verbose", opt -> opt.shortOpt("v").description("Enable verbose output"));
 *     })
 *     .option(Option.builder("debug").global(true).shortOpt("d").build())
 *     .build();
 * int exitCode = cli.execute("git", "--verbose");
 * }</pre>
 *
 * @see Builder
 * @see Command
 * @see Option
 */
public class CLI {

    /**
     * The double dash constant, used to separate options from positional arguments.
     */
    public static final String DOUBLE_DASH = "--";

    private final Map<String, Command> roots;
    private final Logger logger;
    private final List<TypeConverter> converters;
    private final HelpGenerator helpGenerator;
    private final OptionParser op;
    private final boolean autoHelp;
    private final Map<String, Option> globalOptions;
    private final Map<String, String> globalShortToLong;

    private CLI(Builder builder) {
        this.roots = builder.roots;
        this.logger = builder.logger != null ? builder.logger : Logger.system();
        this.helpGenerator = builder.helpGenerator != null ? builder.helpGenerator : HelpGenerator.system();
        this.converters = Collections.unmodifiableList(builder.converters);
        this.autoHelp = Boolean.parseBoolean(System.getProperty("kanketsu.autoHelp", "true"));

        this.globalOptions = Collections.unmodifiableMap(builder.globalOptions);
        Map<String, String> shortMap = new LinkedHashMap<>();
        for (Option opt : globalOptions.values()) {
            if (opt.getShortOpt() != null) {
                shortMap.put(opt.getShortOpt(), opt.getLongOpt());
            }
        }
        this.globalShortToLong = Collections.unmodifiableMap(shortMap);
        this.op = new OptionParser(converters);
    }

    /**
     * Returns an unmodifiable map of all registered root commands.
     *
     * @return a map from command name to {@link Command} instance
     */
    public Map<String, Command> getRootCommands() {
        return Collections.unmodifiableMap(roots);
    }

    /**
     * Parses the command-line arguments and executes the matching command.
     * <p>
     * This method handles global option parsing, root command lookup, subcommand
     * resolution, and option validation. All errors are caught and converted
     * to appropriate exit codes.
     * </p>
     *
     * @param args the command-line arguments (typically from {@code main(String[])})
     * @return exit code: 0 for success, 1 for general error (unknown command, etc.),
     *         2 for option value or build errors
     */
    public int execute(String... args) {
        if (args == null) {
            try {
                throw new CommandException(1, "Arguments array cannot be null");
            } catch (CommandException e) {
                logger.error(e.getMessage(), e, e.getPosition());
                return 1;
            }
        }
        if (args.length == 0) {
            logger.info("No command provided. Use --help for usage.");
            return 1;
        }

        Map<Option, Object> globalParsed = new LinkedHashMap<>();
        int idx = 0;
        try {
            while (idx < args.length) {
                String token = args[idx];
                if (token.equals(DOUBLE_DASH)) {
                    break;
                }
                if (token.startsWith("-")) {
                    Option matchedGlobal = findGlobalOption(token);
                    if (matchedGlobal == null) {
                        break;
                    }
                    int newIdx = op.parseOptionToken(token, idx, args, globalOptions, globalShortToLong, globalParsed);
                    if (newIdx == idx) break;
                    idx = newIdx;
                } else {
                    break;
                }
            }
        } catch (CommandException e) {
            logger.error("Global option error: " + e.getMessage(), e, e.getPosition(), args);
            return 1;
        }

        String[] remaining = Arrays.copyOfRange(args, idx, args.length);

        if (remaining.length == 0) {
            logger.info(generateGlobalHelp());
            return 0;
        }
        if (autoHelp && remaining.length == 1 && ("--help".equals(remaining[0]) || "-h".equals(remaining[0]))) {
            logger.info(generateGlobalHelp());
            return 0;
        }

        return executeCommand(remaining, globalParsed);
    }

    private Option findGlobalOption(String token) {
        if (token.startsWith("--")) {
            String key = token.substring(2);
            int eq = key.indexOf('=');
            if (eq != -1) key = key.substring(0, eq);
            return globalOptions.get(key);
        } else if (token.startsWith("-") && token.length() > 1) {
            String optStr = token.substring(1);
            int eqIndex = optStr.indexOf('=');
            if (eqIndex != -1) {
                String firstChar = optStr.substring(0, 1);
                String longKey = globalShortToLong.get(firstChar);
                if (longKey != null) {
                    return globalOptions.get(longKey);
                }
                return null;
            }
            if (optStr.length() == 1) {
                String longKey = globalShortToLong.get(optStr);
                if (longKey != null) {
                    return globalOptions.get(longKey);
                }
                return null;
            }
            for (char ch : optStr.toCharArray()) {
                String key = globalShortToLong.get(String.valueOf(ch));
                if (key == null || !globalOptions.containsKey(key)) {
                    return null;
                }
            }
            String firstLongKey = globalShortToLong.get(optStr.substring(0, 1));
            return globalOptions.get(firstLongKey);
        }
        return null;
    }

    private int executeCommand(String[] args, Map<Option, Object> globalParsed) {
        Command current;
        List<String> path = new ArrayList<>();
        Map<Option, Object> preParsed = new LinkedHashMap<>();
        int i = 1;

        try {
            current = roots.get(args[0]);
            if (current == null) {
                throw new UnknownCommandException("Unknown command: " + args[0], 0);
            }
            path.add(args[0]);

            while (i < args.length) {
                String token = args[i];

                if (token.equals(DOUBLE_DASH)) {
                    break;
                }

                if (autoHelp && ("--help".equals(token) || "-h".equals(token))) {
                    String fullPath = String.join(" ", path);
                    logger.info(helpGenerator.generateDetailedHelp(roots, fullPath));
                    return 0;
                }

                if (token.startsWith("-")) {
                    i = op.parseOptionToken(token, i, args, current, preParsed);
                    continue;
                }

                Command next = current.getChildren().get(token);
                if (next != null) {
                    current = next;
                    path.add(token);
                    i++;
                } else {
                    if (!current.getChildren().isEmpty()) {
                        throw new UnknownSubcommandException("Unknown subcommand: " + token, i);
                    }
                    break;
                }
            }

            String[] remaining = Arrays.copyOfRange(args, i, args.length);

            if (autoHelp) {
                for (String arg : remaining) {
                    if ("--help".equals(arg) || "-h".equals(arg)) {
                        String fullPath = String.join(" ", path);
                        logger.info(helpGenerator.generateDetailedHelp(roots, fullPath));
                        return 0;
                    }
                }
            }

            if (current.getAction() == null) {
                String fullPath = String.join(" ", path);
                logger.info(helpGenerator.generateDetailedHelp(roots, fullPath));
                return 0;
            }

            Map<Option, Object> initial = new LinkedHashMap<>(globalParsed);
            initial.putAll(preParsed);

            CommandContext ctx = op.parseOptions(remaining, current, initial, globalOptions);

            for (Option opt : current.getOptions().values()) {
                if (opt.isRequired() && !ctx.hasOption(opt)) {
                    throw new MissingRequiredOptionException("Missing required option: --" + opt.getLongOpt());
                }
            }

            return current.run(ctx);

        } catch (CommandException e) {
            logger.error("Parameter error: " + e.getMessage(), e, e.getPosition(), args);
            String fullPath = path.isEmpty() ? "" : String.join(" ", path);
            logger.info(helpGenerator.generateDetailedHelp(roots, fullPath));
            return e.getCode() == 2 ? 2 : 1;
        } catch (Exception e) {
            logger.error("Unexpected error: " + e.getMessage(), e);
            e.printStackTrace();
            return 1;
        }
    }

    private String generateGlobalHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append("Usage: <command> [options] [arguments]\n");
        sb.append("Available commands:\n");
        int maxLen = 0;
        for (String name : roots.keySet()) {
            if (name.length() > maxLen) maxLen = name.length();
        }
        String format = "  %-" + maxLen + "s  %s\n";
        for (Map.Entry<String, Command> entry : roots.entrySet()) {
            String name = entry.getKey();
            String desc = entry.getValue().getDescription();
            if (desc == null) desc = "";
            sb.append(String.format(format, name, desc));
        }
        sb.append("\nUse '<command> --help' for more information on a specific command.");
        return sb.toString();
    }

    /**
     * Builder for {@link CLI}, allowing fluent registration of root commands,
     * global options, loggers, and help generators.
     */
    public static class Builder {
        private final Map<String, Command> roots = new LinkedHashMap<>();
        private final Map<String, Option> globalOptions = new LinkedHashMap<>();
        private Logger logger;
        private HelpGenerator helpGenerator;
        private final List<TypeConverter> converters = new ArrayList<>();

        /**
         * Sets a custom logger implementation.
         *
         * @param logger an implementation of {@link Logger}; if {@code null}, the default
         *               system logger is used
         * @return this builder instance (for chaining)
         */
        public Builder logger(Logger logger) {
            this.logger = logger;
            return this;
        }

        /**
         * Adds a custom type converter.
         *
         * @param converter an implementation of {@link TypeConverter}
         * @return this builder instance
         */
        public Builder converter(TypeConverter converter) {
            this.converters.add(converter);
            return this;
        }

        /**
         * Adds multiple custom type converters.
         *
         * @param converters a list of {@link TypeConverter} instances
         * @return this builder instance
         */
        public Builder converters(List<TypeConverter> converters) {
            this.converters.addAll(converters);
            return this;
        }

        /**
         * Sets a custom help generator.
         *
         * @param hg an implementation of {@link HelpGenerator}
         * @return this builder instance
         */
        public Builder helpGenerator(HelpGenerator hg) {
            this.helpGenerator = hg;
            return this;
        }

        /**
         * Registers a global option (affects all commands).
         * <p>
         * The provided {@link Option} must have {@code global(true)} set; otherwise,
         * a {@link CommandBuildException} is thrown.
         * </p>
         *
         * @param option the global option to register
         * @return this builder instance
         * @throws CommandBuildException if the option is not marked as global
         */
        public Builder option(Option option) {
            if (option.isGlobal()) {
                globalOptions.put(option.getLongOpt(), option);
            } else {
                throw new CommandBuildException(
                        "Option '" + option.getLongOpt() + "' is not global. " +
                                "Only global options can be registered at CLI level. " +
                                "Please set .global(true) in Option builder."
                );
            }
            return this;
        }

        /**
         * Registers a root command with a description.
         *
         * @param name        the command name
         * @param description a short description (used in help output)
         * @param consumer    a callback to configure the command (add options,
         *                    subcommands, and action)
         * @return this builder instance
         */
        public Builder command(String name, String description, java.util.function.Consumer<CommandBuilder> consumer) {
            CommandBuilder cb = new CommandBuilder(name, description);
            consumer.accept(cb);
            roots.put(name, cb.build());
            return this;
        }

        /**
         * Registers a root command with a default description ("No description").
         *
         * @param name     the command name
         * @param consumer a callback to configure the command
         * @return this builder instance
         * @see #command(String, String, java.util.function.Consumer)
         */
        public Builder command(String name, java.util.function.Consumer<CommandBuilder> consumer) {
            return command(name, "No description", consumer);
        }

        /**
         * Builds the final {@link CLI} instance.
         *
         * @return the immutable CLI object
         */
        public CLI build() {
            return new CLI(this);
        }
    }

    /**
     * Creates a new {@link Builder} instance for constructing a {@link CLI} object.
     *
     * @return a fresh builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}