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
package io.github.kanketsuteam.kanketsu.spi;

import io.github.kanketsuteam.kanketsu.core.Option;
import io.github.kanketsuteam.kanketsu.core.command.Command;
import io.github.kanketsuteam.kanketsu.core.exception.UnknownCommandException;

import java.io.PrintStream;
import java.util.Map;

/**
 * Service provider interface for generating and displaying help text.
 * <p>
 * Implementations control the formatting and output destination of command-line
 * help messages, including overviews and detailed command-specific help.
 * </p>
 * <p>
 * Default implementations use a simple console output with fixed column widths.
 * </p>
 *
 * @see #system()
 * @see #toPrintStream(PrintStream)
 */
public interface HelpGenerator {

    /**
     * Returns the width (in characters) reserved for the command name column
     * in help output.
     *
     * @return the command name column width, default 16
     */
    default int getCommandWidth() {
        return 16;
    }

    /**
     * Returns the width (in characters) reserved for the option display column
     * in help output.
     *
     * @return the option column width, default 32
     */
    default int getOptionWidth() {
        return 32;
    }

    /**
     * Outputs the given help text to the configured destination.
     *
     * @param helpText the help text to output (never {@code null})
     */
    void output(String helpText);

    /**
     * Generates and outputs detailed help for the command identified by the given path.
     * <p>
     * This is a convenience method that calls {@link #generateDetailedHelp(Map, String)}
     * and passes the result to {@link #output(String)}.
     * </p>
     *
     * @param roots the map of root commands
     * @param path  the space-separated command path (e.g., "git commit")
     */
    default void printDetailedHelp(Map<String, Command> roots, String path) {
        output(generateDetailedHelp(roots, path));
    }

    /**
     * Generates and outputs an overview of all root commands.
     * <p>
     * This is a convenience method that calls {@link #generateOverview(Map)}
     * and passes the result to {@link #output(String)}.
     * </p>
     *
     * @param roots the map of root commands
     */
    default void printOverview(Map<String, Command> roots) {
        output(generateOverview(roots));
    }

    /**
     * Generates detailed help text for a specific command path.
     * <p>
     * If the path is {@code null} or blank, an overview is returned.
     * Otherwise, the command is resolved from the roots and its detailed
     * help is generated via {@link #generateCommandHelp(Command, String)}.
     * </p>
     *
     * @param roots the map of root commands
     * @param path  the command path (e.g., "git commit")
     * @return the generated help text
     * @throws UnknownCommandException if the command path cannot be resolved
     */
    default String generateDetailedHelp(Map<String, Command> roots, String path) {
        if (path == null || path.isBlank()) {
            return generateOverview(roots);
        }
        String[] parts = path.trim().split("\\s+");
        Command current = null;
        for (int i = 0; i < parts.length; i++) {
            if (i == 0) {
                current = roots.get(parts[i]);
            } else {
                current = current.getChildren().get(parts[i]);
            }
            if (current == null) {
                throw new UnknownCommandException("Command '" + path + "' not found", i);
            }
        }
        return generateCommandHelp(current, path);
    }

    /**
     * Generates an overview of all root commands, listing each with its description.
     *
     * @param roots the map of root commands
     * @return the overview help text
     */
    default String generateOverview(Map<String, Command> roots) {
        StringBuilder sb = new StringBuilder();
        sb.append("Usage:\n");
        sb.append("  <command> [options] [arguments]\n\n");
        sb.append("Commands:\n");
        int cmdWidth = getCommandWidth();
        for (Command cmd : roots.values()) {
            sb.append("  ");
            sb.append(padRight(cmd.getName(), cmdWidth));
            String desc = cmd.getDescription();
            if (desc != null && !desc.isBlank()) {
                sb.append(desc);
            }
            sb.append("\n");
        }
        sb.append("\nRun '<command> --help' for more information about a command.");
        return sb.toString();
    }

    /**
     * Generates detailed help text for a single command.
     * <p>
     * The output includes usage, description, options (with their short/long names,
     * descriptions, required flag, and default values), and subcommands if any.
     * </p>
     *
     * @param cmd      the command to generate help for
     * @param fullPath the full command path (e.g., "git commit")
     * @return the detailed help text
     */
    default String generateCommandHelp(Command cmd, String fullPath) {
        int optWidth = getOptionWidth();
        int cmdWidth = getCommandWidth();

        StringBuilder sb = new StringBuilder();
        sb.append("Usage:\n");
        sb.append("  ").append(fullPath).append(" [options] [arguments]\n");

        if (cmd.getDescription() != null && !cmd.getDescription().isBlank()) {
            sb.append("\n").append(cmd.getDescription()).append("\n");
        }

        sb.append("\nOptions:\n");
        if (cmd.getOptions().isEmpty()) {
            sb.append("  None\n");
        } else {
            for (Option opt : cmd.getOptions().values()) {
                StringBuilder left = new StringBuilder();
                if (opt.getShortOpt() != null && !opt.getShortOpt().isBlank()) {
                    left.append("-").append(opt.getShortOpt());
                }
                if (opt.getLongOpt() != null && !opt.getLongOpt().isBlank()) {
                    if (!left.isEmpty()) left.append(", ");
                    left.append("--").append(opt.getLongOpt());
                }
                if (opt.hasArg()) {
                    left.append(" <value>");
                }
                sb.append("  ").append(padRight(left.toString(), optWidth));
                String desc = opt.getDescription() == null ? "" : opt.getDescription();
                if (opt.isRequired()) desc += " (required)";
                if (opt.getDefaultValue() != null && !opt.getDefaultValue().isBlank()) {
                    desc += " (default: " + opt.getDefaultValue() + ")";
                }
                sb.append(desc).append("\n");
            }
        }

        if (!cmd.getChildren().isEmpty()) {
            sb.append("\nSubcommands:\n");
            for (Command child : cmd.getChildren().values()) {
                sb.append("  ").append(padRight(child.getName(), cmdWidth));
                String desc = child.getDescription();
                if (desc != null && !desc.isBlank()) sb.append(desc);
                sb.append("\n");
            }
            sb.append("\nRun '").append(fullPath).append(" <subcommand> --help' for more information.");
        }
        return sb.toString();
    }

    /**
     * Returns a {@link HelpGenerator} that outputs to {@link System#out}.
     *
     * @return a system-out-based help generator
     */
    static HelpGenerator system() {
        return System.out::println;
    }

    /**
     * Returns a {@link HelpGenerator} that outputs to the given {@link PrintStream}.
     *
     * @param out the print stream to write to
     * @return a help generator using the specified stream
     */
    static HelpGenerator toPrintStream(PrintStream out) {
        return out::println;
    }

    /**
     * Pads the given text to the specified width with trailing spaces.
     *
     * @param text  the text to pad
     * @param width the desired total width
     * @return the padded text, or the text with a trailing space if longer than width
     */
    private static String padRight(String text, int width) {
        if (text.length() >= width) {
            return text + " ";
        }
        return String.format("%-" + width + "s", text);
    }
}