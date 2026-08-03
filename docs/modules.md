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

# 🔮 Future Modules

Kanketsu intentionally keeps many features outside the Core.

Potential ecosystem modules include:

* 🌱 Spring integration
* 📄 YAML configuration
* 📦 JSON configuration
* 🏷 Annotation processing
* 🖥 Shell completion
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
