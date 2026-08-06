package io.github.kanketsuteam.kanketsu.spi;

import io.github.kanketsuteam.kanketsu.core.Option;
import io.github.kanketsuteam.kanketsu.core.command.Command;
import io.github.kanketsuteam.kanketsu.core.exception.UnknownCommandException;

import java.io.PrintStream;
import java.util.Map;

public interface HelpGenerator {
    int OPTION_WIDTH = 32;
    int COMMAND_WIDTH = 16;

    void output(String helpText);

    default void printDetailedHelp(Map<String, Command> roots, String path) {
        String help = buildDetailedHelp(roots, path);
        output(help);
    }

    default void printOverview(Map<String, Command> roots) {
        String overview = buildOverview(roots);
        output(overview);
    }

    default String generateDetailedHelp(Map<String, Command> roots, String path) {
        return buildDetailedHelp(roots, path);
    }

    default String generateOverview(Map<String, Command> roots) {
        return buildOverview(roots);
    }

    static HelpGenerator system() {
        return System.out::println;
    }

    static HelpGenerator toPrintStream(PrintStream out) {
        return out::println;
    }

    private String buildDetailedHelp(Map<String, Command> roots, String path) {
        if (path == null || path.isBlank()) {
            return buildOverview(roots);
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
                throw new UnknownCommandException("Command '" + path + "' not found");
            }
        }
        return generateCommandHelp(current, path); // 仍可复用你原有的静态私有方法
    }

    private String buildOverview(Map<String, Command> roots) {
        // 直接复制你原来的 generateOverview 实现
        StringBuilder sb = new StringBuilder();
        sb.append("Usage:\n");
        sb.append("  <command> [options] [arguments]\n\n");
        sb.append("Commands:\n");
        for (Command cmd : roots.values()) {
            sb.append("  ");
            sb.append(padRight(cmd.getName(), COMMAND_WIDTH));
            String desc = cmd.getDescription();
            if (desc != null && !desc.isBlank()) {
                sb.append(desc);
            }
            sb.append("\n");
        }
        sb.append("\n");
        sb.append("Run '<command> --help' for more information about a command.");
        return sb.toString();
    }

    private static String generateCommandHelp(Command cmd, String fullPath) {

        StringBuilder sb = new StringBuilder();

        sb.append("Usage:\n");
        sb.append("  ").append(fullPath).append(" [options] [arguments]\n");

        if (cmd.getDescription() != null && !cmd.getDescription().isBlank()) {
            sb.append("\n");
            sb.append(cmd.getDescription()).append("\n");
        }

        sb.append("\n");

        sb.append("Options:\n");

        if (cmd.getOptions().isEmpty()) {
            sb.append("  None\n");
        } else {
            for (Option opt : cmd.getOptions().values()) {

                StringBuilder left = new StringBuilder();

                if (opt.getShortOpt() != null && !opt.getShortOpt().isBlank()) {
                    left.append("-").append(opt.getShortOpt());
                }

                if (opt.getLongOpt() != null && !opt.getLongOpt().isBlank()) {

                    if (!left.isEmpty()) {
                        left.append(", ");
                    }

                    left.append("--").append(opt.getLongOpt());
                }

                if (opt.hasArg()) {
                    left.append(" <value>");
                }

                sb.append("  ");
                sb.append(padRight(left.toString(), OPTION_WIDTH));

                String desc = opt.getDescription() == null ? "" : opt.getDescription();

                if (opt.isRequired()) {
                    desc += " (required)";
                }

                if (opt.getDefaultValue() != null && !opt.getDefaultValue().isBlank()) {
                    desc += " (default: " + opt.getDefaultValue() + ")";
                }

                sb.append(desc).append("\n");
            }
        }

        if (!cmd.getChildren().isEmpty()) {

            sb.append("\n");
            sb.append("Subcommands:\n");

            for (Command child : cmd.getChildren().values()) {

                sb.append("  ");
                sb.append(padRight(child.getName(), COMMAND_WIDTH));

                String desc = child.getDescription();

                if (desc != null && !desc.isBlank()) {
                    sb.append(desc);
                }

                sb.append("\n");
            }

            sb.append("\n");
            sb.append("Run '").append(fullPath).append(" <subcommand> --help' for more information.");
        }

        return sb.toString();
    }

    private static String padRight(String text, int width) {
        if (text.length() >= width) {
            return text + " ";
        }

        return String.format("%-" + width + "s", text);
    }
}
