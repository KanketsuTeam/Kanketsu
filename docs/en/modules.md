# 📦 Modules

Kanketsu is built as a collection of independent modules.

Instead of putting every feature into a single framework, Kanketsu keeps the Core focused and allows additional functionality to evolve independently.

This modular architecture keeps applications lightweight while making the ecosystem easy to extend.

---

# 🏛 Design Philosophy

The module system reflects one of Kanketsu's core principles:

> **Modularity over Monolith**

The Core should provide only the essential building blocks required to create command-line applications.

Everything else belongs in optional modules.

This philosophy keeps the Core:

* 🚀 Lightweight
* 🔍 Predictable
* 🧩 Easy to understand
* 🏗 Friendly to GraalVM Native Image

A feature is not excluded because it is unnecessary—it is excluded because it belongs somewhere else.

---

# 📦 Core

**Artifact**

```text
kanketsu-core
```

The Core module is the heart of Kanketsu.

It provides everything required to build command-line applications, including:

* 🌳 Command registration
* 🧭 Command routing
* ⚙️ Argument parsing
* 📝 Option parsing
* ▶️ Command execution
* 🆘 Built-in help generation

The Core has **zero transitive dependencies** and intentionally avoids:

* Reflection
* Runtime scanning
* Annotation processing

For most applications, **Core is the only required dependency**.

---

# 💻 REPL

**Artifact**

```text
kanketsu-repl
```

The REPL module provides an interactive command-line environment built on top of Kanketsu.

Typical features include:

* ⌨️ Interactive shell
* 📜 Command history
* ✨ Tab completion
* 📝 Line editing

The REPL module depends on JLine and is completely optional.

Applications that execute commands directly from the operating system usually do not need this module.

---

# 📄 JSON

**Artifact**

```text
kanketsu-json
```

The JSON module adds support for parsing JSON input directly from command-line options or positional arguments.

It provides a `TypeConverter` implementation that automatically detects whether the input is:

* A JSON string (starting with `{` or `[`)
* A file path (reads the file content and parses it as JSON)

This module integrates seamlessly with Kanketsu's SPI and allows commands to receive strongly-typed JSON structures (`JsonObject`, `JsonArray`) without manual parsing or error-prone reflection.

**Key features:**

* 🔄 Automatic detection of JSON string vs. file path
* 📍 Precise error reporting with character position for invalid JSON
* 🏗 Zero reflection – fully GraalVM Native Image compatible
* 📦 Tiny dependency footprint (uses `json-simple` with no additional transitive dependencies)

**Usage example:**

```java
CLI.builder()
    .converter(JsonTypeConverter.INSTANCE)
    .command("parse", cmd -> cmd
        .option("config", opt -> opt
            .hasArg(true)
            .converter(Converters.STRING)
            .required(true))
        .action(ctx -> {
            JsonObject config = ctx.getOptionValueAs("config", JsonObject.class);
            String host = (String) config.get("host");
            System.out.println("Host: " + host);
        })
    )
    .build();
```

**When to use:**

* Your CLI needs to accept structured configuration (e.g., deployment specs, service definitions)
* You want to support both inline JSON and file-based inputs
* You require precise error feedback for invalid JSON

**Maven dependency:**

```xml
<dependency>
    <groupId>io.github.kanketsuteam</groupId>
    <artifactId>kanketsu-json</artifactId>
    <version>2.1.0</version>
</dependency>
```

---

# 🔮 Future Modules

Kanketsu intentionally keeps many features outside the Core.

Potential ecosystem modules include:

* 📄 YAML configuration
* 🔌 Third-party integrations

These modules are not part of the Core because they solve specific integration problems rather than general CLI problems.

Separating them allows the ecosystem to grow without increasing the complexity of the framework itself.

---

# 🎯 Choosing Modules

Choose only what your application requires.

For a typical CLI application:

```text
kanketsu-core
```

For an interactive shell:

```text
kanketsu-core
+ kanketsu-repl
```

For JSON input support:

```text
kanketsu-core
+ kanketsu-json
```

Future modules can be added independently without affecting existing applications.

---

# 💡 Why Modular?

Keeping the Core small provides several advantages:

* 📦 Smaller dependency footprint
* ⚡ Faster startup
* 🧹 Simpler maintenance
* 🔄 Independent module evolution
* 🏗 Better AOT compatibility

Rather than becoming a monolithic framework, Kanketsu aims to provide a stable foundation for a growing ecosystem.

---

# 🔧 Shell Completion

**Artifact**

```text
kanketsu-completion-maven-plugin
```

The Shell Completion module is a **Maven plugin** that automatically generates shell completion scripts (Bash / Zsh / Fish) for your Kanketsu-based CLI application, directly from your command tree definition — **no manual maintenance required**.

## 📦 Why This Module?

Instead of manually maintaining completion scripts that quickly drift out of sync with your code, this module introspects your existing `CLI` instance at build time and produces accurate, up‑to‑date completion scripts.

Key benefits:

- ✅ **Zero manual work** — completions are generated from your command tree.
- ✅ **Multi‑shell support** — Bash, Zsh, and Fish (PowerShell support planned).
- ✅ **Context‑aware** — completes commands, subcommands, and options based on the current command path.
- ✅ **Native‑image friendly** — no runtime reflection; completions are generated during the Maven build.

## 📥 Installation

Add the plugin to the `<plugins>` section of your `../../pom.xml`:

```xml
<plugin>
    <groupId>io.github.kanketsuteam</groupId>
    <artifactId>kanketsu-completion-maven-plugin</artifactId>
    <version>2.1.0</version>
    <executions>
        <execution>
            <id>generate-completion</id>
            <phase>package</phase>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## ⚙️ Configuration

| Parameter    | Type     | Required | Default                                    | Description                                                           |
|:-------------|:---------|:---------|:-------------------------------------------|:----------------------------------------------------------------------|
| `cliClass`   | `String` | ✅ Yes   | —                                          | Fully qualified class name that holds the static `CLI` instance.       |
| `cliField`   | `String` | ❌ No    | `cli`                                      | Name of the static field that holds the `CLI` instance.               |
| `shell`      | `String` | ❌ No    | `bash`                                     | Target shell: `bash`, `zsh`, or `fish`.                               |
| `commandName`| `String` | ❌ No    | `artifactId`                               | The CLI command name (used in the completion script).                 |
| `outputFile` | `File`   | ❌ No    | `target/${artifactId}_completion.${shell}` | Output path for the generated script.                                 |

### Example Configuration

```xml
<plugin>
    <groupId>io.github.kanketsuteam</groupId>
    <artifactId>kanketsu-completion-maven-plugin</artifactId>
    <version>2.1.0</version>
    <executions>
        <execution>
            <id>generate-completion</id>
            <phase>package</phase>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <cliClass>com.example.yourapp.CLIHolder</cliClass>
                <cliField>cli</cliField>
                <shell>bash</shell>
                <commandName>yourapp</commandName>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## 📝 Requirements

1. **Your CLI instance must be defined in a `public static` field**, as described in the [Native Image Guide](native-image.md#the-cliholder-pattern). For example:
   ```java
   public class CLIHolder {
       public static CLI cli = CLI.builder()
           .command("hello", cmd -> cmd.action(...))
           // ...
           .build();
   }
   ```
2. Your project must be built with **Java 25+**.
3. The `kanketsu-core` JAR must be available on the classpath.

## 🚀 Usage

### Generating the Script

Run Maven as usual — the script is automatically generated during the `package` phase:

```bash
mvn clean package
```

The generated script is located at `target/yourapp_completion.bash` (or `.zsh` / `.fish`, depending on the `shell` setting).

### Loading the Script

- **Bash / Zsh**
  ```bash
  source /path/to/yourapp_completion.bash
  ```
  To make it permanent, add the `source` command to your `~/.bashrc` or `~/.zshrc`.

- **Fish**
  ```bash
  source /path/to/yourapp_completion.fish
  ```
  Or copy it to `~/.config/fish/completions/` for automatic loading.

- **Git Bash on Windows**
  The same command works in Git Bash. Use POSIX-style paths:
  ```bash
  source /d/CLI/yourapp/target/yourapp_completion.bash
  ```

### Testing

After sourcing the script, type your command name and press **Tab**:

```bash
$ yourapp <Tab><Tab>
api   auth   issue   pr   repo   version

$ yourapp issue <Tab><Tab>
list   view   create   close

$ yourapp issue list -<Tab>
--repo   -r    --state   -s    --limit   -l    --assignee   -a
```

## 🧩 How It Works

1. **Build‑time introspection** — The Maven plugin loads your `CLI` instance via reflection at build time.
2. **Command tree traversal** — It walks the entire command tree, collecting subcommands and options.
3. **Script generation** — It produces a pure Shell script with `case`‑based dispatch for context‑sensitive completions.
4. **Zero runtime overhead** — The generated script runs entirely in the Shell; your application is never invoked for completion.

## 🐚 Supported Shells

| Shell   | Status   | Notes                                      |
|:--------|:---------|:-------------------------------------------|
| Bash    | ✅ Full  | Includes context‑sensitive completions.    |
| Zsh     | ✅ Full  | Native `compadd` support.                  |
| Fish    | ✅ Full  | Native `complete` command support.         |
| PowerShell | ❌ Not yet | May be added in a future release.        |

## 💡 Pro Tips

### Multiple Shells

You can generate multiple scripts by adding separate executions:

```xml
<execution>
    <id>generate-bash</id>
    <configuration><shell>bash</shell></configuration>
</execution>
<execution>
    <id>generate-zsh</id>
    <configuration><shell>zsh</shell></configuration>
</execution>
<execution>
    <id>generate-fish</id>
    <configuration><shell>fish</shell></configuration>
</execution>
```

### Distributing Completions

When packaging your CLI tool, you can include the generated completion scripts in your distribution bundle or package manager (e.g., Homebrew, APT). This allows users to enable completions with a single command.

### Windows Users

On Windows, the generated Bash script works in:
- **Git Bash** (recommended)
- **WSL** (Windows Subsystem for Linux)
- **Cygwin**

For native PowerShell completions, consider using a wrapper or waiting for future support.

## 🔗 Related Topics

- [🏗 Building Native Images](native-image.md) — Learn about the `CLIHolder` pattern.
- [🧠 Concepts](concepts.md) — Understand the command tree structure.
- [📦 Core Module](modules.md#-core) — The foundation of your CLI application.

## 📄 Source Code

The plugin is part of the Kanketsu repository:

🔗 [https://github.com/KanketsuTeam/Kanketsu/tree/master/kanketsu-completion-maven-plugin](https://github.com/KanketsuTeam/Kanketsu/tree/master/kanketsu-completion-maven-plugin)


# 🚀 What's Next?

Continue exploring Kanketsu:

* ⚡ **Performance** — Learn why Kanketsu achieves excellent startup time and parsing performance.
* 🤝 **Contributing** — Learn how to build, test, and contribute to the project.