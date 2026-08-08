package io.github.kanketsuteam.kanketsu.repl;

import io.github.kanketsuteam.kanketsu.repl.exception.BuildTerminalFailedException;
import io.github.kanketsuteam.kanketsu.spi.Logger;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

/**
 * A {@link Logger} implementation that writes log messages directly to a
 * system terminal.
 * <p>
 * This logger can either create its own terminal using the default system
 * terminal (with the "ffm" provider) or accept an externally provided terminal.
 * Each log message is printed to the terminal's writer, followed by a newline
 * and a flush to ensure immediate display.
 * </p>
 * <p>
 * If terminal creation fails in the no‑argument constructor, a
 * {@link BuildTerminalFailedException} is thrown. The constructor that accepts
 * a {@code Terminal} assumes the terminal is already valid.
 * </p>
 */
public class TerminalLogger implements Logger {

    private final Terminal terminal;

    /**
     * Constructs a new terminal logger that creates its own default system terminal.
     *
     * @throws BuildTerminalFailedException if the default terminal cannot be built
     *         due to an I/O error
     * @see Terminals#createDefault()
     */
    public TerminalLogger() {
        this.terminal = Terminals.createDefault();
    }

    /**
     * Constructs a new terminal logger that uses the provided terminal.
     *
     * @param terminal the terminal to use for logging; must not be {@code null}
     * @throws NullPointerException if {@code terminal} is {@code null}
     */
    public TerminalLogger(Terminal terminal){
        this.terminal = terminal;
    }

    /**
     * Returns the underlying terminal instance.
     *
     * @return the terminal used by this logger
     */
    public Terminal getTerminal() {
        return terminal;
    }

    /**
     * Logs the given message to the terminal.
     * <p>
     * The message is written, a newline is appended, and the writer is flushed.
     * </p>
     *
     * @param message the message to log
     */
    @Override
    public void log(String message) {
        terminal.writer().println(message);
        terminal.writer().flush();
    }
}