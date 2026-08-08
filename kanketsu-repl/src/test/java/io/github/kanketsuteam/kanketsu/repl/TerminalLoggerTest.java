package io.github.kanketsuteam.kanketsu.repl;

import org.jline.terminal.Terminal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TerminalLoggerTest {

    @Mock
    private Terminal mockTerminal;

    @Mock
    private PrintWriter mockWriter;

    @Test
    void shouldLogMessageToTerminalWriterAndFlush() {
        when(mockTerminal.writer()).thenReturn(mockWriter);
        TerminalLogger logger = new TerminalLogger(mockTerminal);
        String message = "Hello, REPL!";

        logger.log(message);
    }

    @Test
    void shouldNotThrowWhenConstructedWithNullTerminal() {
        TerminalLogger logger = new TerminalLogger(null);
        assertThat(logger.getTerminal()).isNull();
    }

    @Test
    void shouldThrowNullPointerExceptionWhenLoggingWithNullTerminal() {
        TerminalLogger logger = new TerminalLogger(null);
        assertThatThrownBy(() -> logger.log("message"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldCreateDefaultTerminalWhenNoArgsConstructorUsed() {
        TerminalLogger logger = new TerminalLogger();
        assertThat(logger.getTerminal()).isNotNull();
    }
}