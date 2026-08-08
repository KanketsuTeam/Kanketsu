package io.github.kanketsuteam.kanketsu.repl;

import io.github.kanketsuteam.kanketsu.core.CLI;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

class REPLTest {

    private CLI cli;
    private Terminal dumbTerminal;

    @BeforeEach
    void setUp() throws IOException {
        cli = CLI.builder().build();
        dumbTerminal = TerminalBuilder.builder().dumb(true).build();
    }

    @Test
    void shouldConstructWithProvidedTerminalWithoutException() {
        REPL repl = new REPL(cli, dumbTerminal);
        assertThat(repl).isNotNull();
        assertThat(repl.getTerminal()).isSameAs(dumbTerminal);
    }

    @Test
    void shouldCloseTerminalWithoutError() {
        REPL repl = new REPL(cli, dumbTerminal);
        repl.close();
    }

    @Test
    void shouldSwallowIOExceptionDuringCloseAndLogIt() throws IOException {
        Terminal spyTerminal = spy(dumbTerminal);
        doThrow(new IOException("simulated close failure")).when(spyTerminal).close();
        REPL repl = new REPL(cli, spyTerminal);
        repl.close();
    }

    @Test
    void shouldThrowNullPointerExceptionWhenCliIsNull() {
        assertThatThrownBy(() -> new REPL(null, dumbTerminal))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldCreateREPLWithDefaultTerminal() {
        REPL repl = new REPL(cli);
        assertThat(repl).isNotNull();
        assertThat(repl.getTerminal()).isNotNull();
        repl.close();
    }
}