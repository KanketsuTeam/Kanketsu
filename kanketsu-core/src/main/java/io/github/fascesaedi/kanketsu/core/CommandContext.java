package io.github.fascesaedi.kanketsu.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CommandContext {
    private final Map<String, String> options;
    private final List<String> positionalArgs;

    public CommandContext(Map<String, String> options, List<String> positionalArgs) {
        this.options = new LinkedHashMap<>(options);
        this.positionalArgs = new ArrayList<>(positionalArgs);
    }

    public Map<String, String> getOptions() { return options; }
    public List<String> getPositionalArgs() { return positionalArgs; }

    public String getOption(String key) {
        return options.get(key);
    }

    public boolean hasOption(String key) {
        return options.containsKey(key);
    }
}
