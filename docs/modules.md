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
    <groupId>io.github.fascesaedi</groupId>
    <artifactId>kanketsu-json</artifactId>
    <version>2.0.0</version>
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

# 🚀 What's Next?

Continue exploring Kanketsu:

* ⚡ **Performance** — Learn why Kanketsu achieves excellent startup time and parsing performance.
* 🤝 **Contributing** — Learn how to build, test, and contribute to the project.