package io.github.kanketsuteam.kanketsu.repl.exception;

/**
 * Thrown to indicate that the construction of a terminal instance has failed.
 * <p>
 * This is a runtime exception that wraps low‑level I/O errors that may occur
 * when creating a JLine {@link org.jline.terminal.Terminal} via
 * {@link org.jline.terminal.TerminalBuilder} or the {@code Terminals} factory.
 * </p>
 * <p>
 * It provides both a message‑only constructor and a constructor that
 * captures the underlying cause.
 * </p>
 */
public class BuildTerminalFailedException extends RuntimeException {

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message explaining the cause of the failure
     */
    public BuildTerminalFailedException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail message explaining the cause of the failure
     * @param e       the original exception that caused the failure
     */
    public BuildTerminalFailedException(String message, Exception e) {
        super(message, e);
    }
}