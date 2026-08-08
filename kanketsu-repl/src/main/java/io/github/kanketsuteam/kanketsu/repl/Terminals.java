package io.github.kanketsuteam.kanketsu.repl;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import io.github.kanketsuteam.kanketsu.repl.exception.BuildTerminalFailedException;
import java.io.IOException;

/**
 * Factory for creating pre‑configured system terminals.
 * <p>
 * This utility class provides a single method to obtain a terminal with
 * a consistent configuration (system terminal, "ffm" provider). All methods
 * are static and the class cannot be instantiated.
 * </p>
 */
public final class Terminals {

    private Terminals() {}

    /**
     * Creates a default system terminal with the "ffm" provider.
     * <p>
     * The terminal is built using {@link TerminalBuilder#builder()} with
     * {@code system(true)} and provider {@code "ffm"}. If the underlying
     * JLine terminal builder throws an {@link IOException}, the exception
     * is wrapped in a {@link BuildTerminalFailedException} and rethrown.
     * </p>
     *
     * @return a configured Terminal instance
     * @throws BuildTerminalFailedException if terminal creation fails due to an I/O error
     */
    public static Terminal createDefault() {
        try {
            return TerminalBuilder.builder()
                    .system(true)
                    .provider("ffm")
                    .build();
        } catch (IOException e) {
            throw new BuildTerminalFailedException("Failed to create system terminal: " + e.getMessage(), e);
        }
    }
}