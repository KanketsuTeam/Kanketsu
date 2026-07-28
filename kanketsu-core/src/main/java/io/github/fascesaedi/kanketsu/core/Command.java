package io.github.fascesaedi.kanketsu.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Command {
    private final String name;
    final Map<String, Command> children;
    private final Map<String, Option> options;
    private final Consumer<CommandContext> action;

    Command(String name, Map<String, Command> children,
            Map<String, Option> options, Consumer<CommandContext> action) {
        this.name = name;
        this.children = new HashMap<>(children);
        this.options = new LinkedHashMap<>(options);
        this.action = action;
    }

    public String getName() { return name; }
    public Map<String, Command> getChildren() { return Collections.unmodifiableMap(children); }
    public Map<String, Option> getOptions() { return Collections.unmodifiableMap(options); }
    public Consumer<CommandContext> getAction() { return action; }

    public void run(CommandContext ctx) {
        if (action != null) {
            action.accept(ctx);
        }
    }
}