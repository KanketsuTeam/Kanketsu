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

import java.util.Map;
import java.util.stream.Collectors;

public class HelpGenerator {
    public static String generateOverview(Map<String, Command> roots) {
        StringBuilder sb = new StringBuilder();
        sb.append("Usage: <command> [options]\n\n");
        sb.append("Available commands:\n");
        for (Command cmd : roots.values()) {
            sb.append("  ").append(cmd.getName());
            if (!cmd.getChildren().isEmpty()) {
                String childNames = cmd.getChildren().keySet().stream()
                        .limit(3)
                        .collect(Collectors.joining(", "));
                if (cmd.getChildren().size() > 3) childNames += ", ...";
                sb.append(" (").append(childNames).append(")");
            }
            sb.append("\n");
        }
        sb.append("\nUse '<command> --help' for more details on a specific command.");
        return sb.toString();
    }

    public static String generateDetailedHelp(Map<String, Command> roots, String path) {
        if (path == null || path.trim().isEmpty()) {
            return generateOverview(roots);
        }

        String[] parts = path.trim().split("\\s+");
        Command current = null;
        for (int i = 0; i < parts.length; i++) {
            String name = parts[i];
            if (i == 0) {
                current = roots.get(name);
            } else {
                if (current != null) {
                    current = current.getChildren().get(name);
                }
            }
            if (current == null) {
                return "Error: Command '" + path + "' not found.";
            }
        }

        return generateCommandHelp(current, path);
    }

    private static String generateCommandHelp(Command cmd, String fullPath) {
        StringBuilder sb = new StringBuilder();
        sb.append("Usage: ").append(fullPath).append(" [options]\n\n");
        if (!cmd.getOptions().isEmpty()) {
            sb.append("Options:\n");
            for (Option opt : cmd.getOptions().values()) {
                sb.append("  ");
                if (opt.getShortOpt() != null && !opt.getShortOpt().isEmpty()) {
                    sb.append("-").append(opt.getShortOpt());
                }
                if (opt.getLongOpt() != null && !opt.getLongOpt().isEmpty()) {
                    if (opt.getShortOpt() != null && !opt.getShortOpt().isEmpty()) {
                        sb.append(", ");
                    }
                    sb.append("--").append(opt.getLongOpt());
                }
                if (opt.hasArg()) {
                    sb.append(" <value>");
                }
                if (opt.isRequired()) {
                    sb.append(" (required)");
                }
                if (opt.getDefaultValue() != null && !opt.getDefaultValue().isEmpty()) {
                    sb.append(" [default: ").append(opt.getDefaultValue()).append("]");
                }
                sb.append("\n    ").append(opt.getDescription()).append("\n");
            }
        } else {
            sb.append("No options available.\n");
        }

        if (!cmd.getChildren().isEmpty()) {
            sb.append("\nSubcommands:\n");
            for (Command child : cmd.getChildren().values()) {
                sb.append("  ").append(child.getName()).append("\n");
            }
            sb.append("\nUse '<command> --help' for subcommand details.");
        }

        return sb.toString();
    }
}
