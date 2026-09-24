# TierSpoofer 1.21.11 — Build Instructions

## Requirements
- **Java 21** (must be exactly 21, not higher for best compatibility)
- **Git** (optional but recommended)
- Internet connection (Gradle downloads dependencies on first build)

## Project Structure

After extracting this ZIP, move files so your project looks like this:

```
tierspoofer/
├── build.gradle
├── gradle.properties
├── settings.gradle
├── gradlew          (Linux/Mac — make executable: chmod +x gradlew)
├── gradlew.bat      (Windows)
├── LICENSE
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar    ← YOU MUST DOWNLOAD THIS (see below)
│       └── gradle-wrapper.properties
└── src/
    └── client/
        ├── java/
        │   └── com/tierspoofer/
        │       ├── TierSpoofer.java
        │       ├── SkinCache.java
        │       ├── ColorCodeParser.java
        │       ├── config/
        │       │   ├── TierSpooferConfig.java
        │       │   ├── TierSpooferConfigScreen.java
        │       │   └── ModMenuIntegration.java
        │       ├── model/
        │       │   └── SpoofedPlayer.java
        │       └── mixin/
        │           ├── MixinPlayerEntity.java
        │           ├── MixinPlayerListHud.java
        │           ├── MixinPlayerListEntrySkin.java
        │           ├── MixinChatHud.java
        │           ├── MixinDeathScreen.java
        │           ├── MixinChatInputSuggestor.java
        │           └── MixinSuggestionWindow.java
        └── resources/
            ├── fabric.mod.json
            └── tierspoofer.mixins.json
```

## Step 1 — Download gradle-wrapper.jar

The `gradle-wrapper.jar` cannot be included in ZIPs (it is a binary).
Download it from the official Gradle GitHub releases:

```
https://github.com/gradle/gradle/raw/v8.10.0/gradle/wrapper/gradle-wrapper.jar
```

Save it to: `gradle/wrapper/gradle-wrapper.jar`

**Or** run this one-liner (requires curl):
```bash
curl -L https://github.com/gradle/gradle/raw/v8.10.0/gradle/wrapper/gradle-wrapper.jar \
     -o gradle/wrapper/gradle-wrapper.jar
```

**Or on Windows (PowerShell):**
```powershell
Invoke-WebRequest `
  -Uri "https://github.com/gradle/gradle/raw/v8.10.0/gradle/wrapper/gradle-wrapper.jar" `
  -OutFile "gradle\wrapper\gradle-wrapper.jar"
```

## Step 2 — Copy the source files

From the ZIP's `ported_1.21.11_source/` folder:
- Copy `com/` → `src/client/java/`
- Copy `resources/` → `src/client/resources/`

## Step 3 — Build

**Windows:**
```
gradlew.bat build
```

**Linux / macOS:**
```
chmod +x gradlew
./gradlew build
```

First build takes 5–10 minutes (downloads MC, mappings, dependencies).
Subsequent builds are fast.

## Step 4 — Find the output JAR

```
build/libs/tierspoofer-1.0.0+1.21.11.jar
```

Drop this into your `.minecraft/mods/` folder alongside:
- Fabric Loader 0.18.1+
- Fabric API 0.141.1+1.21.11
- ModMenu 17.0.0 (optional)

## Troubleshooting

**"Could not resolve net.fabricmc:yarn"**
→ Check your internet connection. The FabricMC Maven server may be temporarily down.

**"Unsupported class file major version"**
→ You're using the wrong Java version. Must be Java 21.

**"Invalid mixin config tierspoofer.mixins.json"**
→ The mixins.json is in the wrong folder. It must be at `src/client/resources/tierspoofer.mixins.json`.

**Compile error in TierSpooferConfigScreen.java on mouseClicked**
→ Check if `Click` has `x()` and `y()` methods in your IDE. If yes, replace
  `clickX(click)` / `clickY(click)` with `click.x()` / `click.y()` directly.
  This is the one spot that couldn't be 100% verified without the actual mapped JAR.
