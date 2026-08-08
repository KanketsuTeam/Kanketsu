package io.github.kanketsuteam.kanketsu.repl;

import org.jline.terminal.Terminal;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class TerminalsTest {

    @Test
    void shouldCreateDefaultTerminalSuccessfully() {
        Terminal terminal = Terminals.createDefault();
        assertThat(terminal).isNotNull();
        try {
            terminal.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}