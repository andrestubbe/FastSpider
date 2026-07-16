# Building FastSpider from Source

## Prerequisites

- **JDK 17+** â€” [Download](https://adoptium.net/)
- **Maven 3.9+** â€” [Download](https://maven.apache.org/download.cgi)
- **Visual Studio 2022** â€” Community/Professional/Enterprise/BuildTools

## Quick Build

```bash
# 1. Build native DLL first (Windows)
compile.bat

# 2. Build JAR
mvn clean package -DskipTests
```

## Build Commands

| Command | Purpose |
|---------|---------|
| `compile.bat` | Build native DLL (Windows) |
| `mvn clean compile` | Compile Java only |
| `mvn clean package` | Build FatJAR with DLL embedded |
| `mvn test` | Run unit tests |

## Native DLL Build

The `compile.bat` script:
- Auto-detects Visual Studio 2019/2022
- Auto-detects JAVA_HOME
- Uses `native\fastspider.def` for JNI exports
- Outputs to `build\fastspider.dll`

The Maven `pom.xml` will automatically pick up `build\fastspider.dll` and bundle it inside the JAR.

## JNI Exports (.def File)

When using JNI, you MUST export your native functions in the `native\fastspider.def` file:

```def
LIBRARY fastspider
EXPORTS
    Java_fastspider_FastSpider_doSomethingNative
```

**Important:** Function names must match Java's expected format:
- Pattern: `Java_packagename_Classname_methodname`

Without the `.def` file, JNI methods won't be exported and you'll get `UnsatisfiedLinkError`.

## Troubleshooting

**"Cannot find DLL"** â€” Run `compile.bat` first

**"UnsatisfiedLinkError"** â€” Common causes:
1. DLL built but not included in JAR (check `build/` folder).
2. JNI exports missing â€” Verify `.def` file.
3. Wrong function name â€” Must match `Java_package_Class_method` exactly.

**"Java version mismatch"** â€” Ensure JDK 17+ is installed and JAVA_HOME is set.

