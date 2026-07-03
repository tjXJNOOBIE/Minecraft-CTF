# Utility Management Pattern

This document captures the utility style used in `Minecraft-CTF` so future systems can manage helpers without growing one cursed `Utils` class that becomes the source-code equivalent of a junk drawer.

## Intent

Utilities should make call sites smaller, safer, and more consistent. They should not become hidden service layers, global mutation traps, or places where domain logic goes to avoid accountability.

The pattern is:

- Keep utilities **purpose-scoped**.
- Put domain-specific utilities **near the domain they support**.
- Put broadly reusable platform helpers under a shared `util` package.
- Use package-private helpers when the helper only exists to clean up one feature area.
- Prefer explicit method names, guard clauses, readable locals, and stable return behavior.

## Package Placement

### Shared platform utilities

Use `dev.tjxjnoobie.ctf.util` or a focused child package when the helper is reusable across several domains.

Examples:

- `dev.tjxjnoobie.ctf.util.BukkitTaskOrchestrator`
- `dev.tjxjnoobie.ctf.util.CTFKeys`
- `dev.tjxjnoobie.ctf.util.bukkit.message.BukkitMessageUtils`
- `dev.tjxjnoobie.ctf.util.bukkit.runnable.BukkitRunnableUtil`

Use this for helpers that wrap Bukkit/Paper APIs, centralize shared key catalogs, handle task lifecycle conventions, or provide low-level reusable glue.

### Domain utilities

Keep utilities inside the owning domain package when the behavior is only meaningful there.

Examples:

- `dev.tjxjnoobie.ctf.team.TeamDomainUtil`
- `dev.tjxjnoobie.ctf.commands.util.TeamTabCompleteUtil`
- `dev.tjxjnoobie.ctf.game.player.managers.BuildToggleUtil`

This avoids polluting the global utility namespace and keeps business meaning close to the system that owns it.

### Package-private helpers

Use a package-private `*Helper` when extracting noisy behavior from handlers inside the same feature area.

Example:

- `dev.tjxjnoobie.ctf.game.player.handlers.FlagCarrierMovementHelper`

This is for local orchestration cleanup, not a public reusable API. If the helper only exists because one handler got chunky, keep it package-private and boring. Boring code pays rent.

### Builders are not utilities

Use `*Builder` for fluent construction patterns instead of pretending they are utilities.

Example:

- `dev.tjxjnoobie.ctf.items.builder.ItemBuilder`

Builders may hold temporary configuration state while constructing an object. Static utilities should not.

## Naming Rules

Use these names intentionally:

| Pattern | Use For | Example |
| --- | --- | --- |
| `*Util` | Stateless or tightly scoped reusable helper methods | `TeamDomainUtil`, `TeamTabCompleteUtil` |
| `*Utils` | Broad platform helpers where plural reads naturally | `BukkitMessageUtils` |
| `*Helper` | Package-private local behavior extraction | `FlagCarrierMovementHelper` |
| `*Orchestrator` | Thin lifecycle coordination wrapper | `BukkitTaskOrchestrator` |
| `*Builder` | Fluent object construction | `ItemBuilder` |
| `*Keys` | Stable key catalog and namespace holder | `CTFKeys` |

Do not create vague names like `CommonUtil`, `PluginUtil`, `GameUtil`, or `MiscUtil`. That is how a codebase gets a basement.

## Class Shape

Static utility holders should be `final` with a private constructor.

```java
public final class ExampleDomainUtil {

    // == Lifecycle ==
    private ExampleDomainUtil() {
    }

    // == Utilities ==
    public static String normalizeKey(String input) {
        if (input == null) {
            return null;
        }

        String normalizedInput = input.trim().toLowerCase(Locale.ROOT);
        return isKnownKey(normalizedInput) ? normalizedInput : null;
    }

    // == Predicates ==
    public static boolean isKnownKey(String key) {
        return "example".equals(key);
    }
}
```

Instance-backed utility managers are allowed only when the state is small, scoped, and obvious.

Example: `BuildToggleUtil` owns a small `Set<UUID>` for build bypass state. That works because the state is the utility's entire purpose, not a random stash of global runtime junk.

## Section Comments

Use compact section headers to make utility classes scan fast:

- `// == Runtime state ==`
- `// == Configuration ==`
- `// == Lifecycle ==`
- `// == Utilities ==`
- `// == Predicates ==`
- `// == Getters ==`

Do not turn these into decorative wallpaper. Use them when they separate meaningful groups.

## Guard Clause Style

Utilities should fail safely and early.

Preferred behavior:

- Return `List.of()` for unavailable suggestions or empty result sets.
- Return `false` for failed predicate-style operations.
- Return `null` only when the caller expects an optional object result and the current codebase already uses that convention.
- Clamp unsafe primitive inputs when possible, such as non-negative delays or minimum tick periods.
- Avoid throwing for ordinary invalid input unless the utility is a constructor/factory that cannot safely continue.

Example style:

```java
if (player == null) {
    return false;
}

UUID playerUUID = player.getUniqueId();
boolean containsPlayer = bypassPlayers.contains(playerUUID);
```

## Readable Local Extraction

Prefer readable locals before branching, filtering, or calling deeper APIs.

Good:

```java
String rawInput = args[index];
String normalizedInput = rawInput == null ? "" : rawInput.toLowerCase(Locale.ROOT);
return values.stream()
    .filter(value -> value.startsWith(normalizedInput))
    .toList();
```

Avoid:

```java
return values.stream()
    .filter(value -> value.startsWith(args[index].toLowerCase(Locale.ROOT)))
    .toList();
```

The second version technically works until it doesn't, which is Java's favorite form of comedy.

## Key Catalog Pattern

Use a dedicated `*Keys` class for stable string identifiers used across multiple systems.

The pattern is:

- Public static accessor methods.
- Private nested classes grouped by domain.
- Private constants inside those nested classes.
- No scattered string literals in gameplay code.

Example shape:

```java
public final class ProjectKeys {

    private ProjectKeys() {
    }

    public static String permissionAdmin() {
        return Permission.ADMIN;
    }

    private static final class Permission {
        private static final String ADMIN = "project.admin";

        private Permission() {
        }
    }
}
```

This keeps namespaces stable while allowing internal organization to evolve.

## Task Utility Pattern

Task helpers should centralize scheduling and cancellation behavior.

The CTF pattern uses two layers:

- `BukkitRunnableUtil` wraps runnable creation and scheduler calls.
- `BukkitTaskOrchestrator` provides lifecycle helpers that avoid duplicate timers and unregister cancelled tasks.

Future utilities should preserve that split:

- Low-level wrappers adapt external APIs.
- Orchestrators coordinate lifecycle rules.
- Registry tracking belongs near task scheduling/cancellation, not sprinkled through gameplay classes like confetti after a bad sprint demo.

## Domain Mapping Pattern

Domain mapping utilities should convert stable keys into visuals, materials, display components, and user-facing labels.

Example responsibilities:

- Normalize a domain key.
- Resolve a `Material`.
- Resolve a display name.
- Resolve a styled `Component`.
- Resolve a chat prefix or UI prefix.

This belongs beside the domain, not in global `util`, because the mappings are business meaning, not generic platform glue.

## Tab Completion Pattern

Command tab helpers should be tiny and pure.

Expected behavior:

- Accept raw command args.
- Validate the target argument index.
- Normalize current input safely.
- Return filtered suggestions.
- Return `List.of()` when the utility cannot safely produce suggestions.

This keeps command classes focused on command behavior instead of growing tab-complete sludge. Humanity suffered enough already.

## When To Create a Utility

Create a utility when at least one of these is true:

- The same helper logic appears in multiple classes.
- A handler or manager is cluttered by low-level API glue.
- A domain needs one stable place for mapping keys to visuals/components/materials.
- A lifecycle operation needs consistent safety rules across systems.
- A builder-style object needs fluent construction without repeating meta handling.

Do not create a utility when:

- The logic belongs to a real domain service.
- The method mutates cross-domain state.
- The name would need to be vague.
- The class would collect unrelated methods.
- The helper hides important gameplay rules from the owning system.

## Future Project Standard

Use this standard for future utility management:

1. Start with the owning domain package.
2. Promote to shared `util` only after at least two domains genuinely need it.
3. Keep utility APIs narrow and boring.
4. Use final classes and private constructors for static holders.
5. Use package-private helpers for local extraction.
6. Keep state out of utilities unless the state is tiny, scoped, and the class name directly describes it.
7. Keep Java call sites readable by extracting meaningful locals before nested calls.
8. Prefer guard clauses and safe neutral returns.
9. Avoid god utilities like they owe you money.

## Example Decision Tree

```text
Is this helper only for one handler/package?
  -> Use package-private *Helper beside that handler.

Is this helper only for one domain?
  -> Use DomainNameUtil in that domain package.

Is this helper shared Bukkit/Paper/API glue?
  -> Use util.bukkit.<surface> or util.<surface>.

Is this a catalog of stable string identifiers?
  -> Use ProjectKeys with nested private domain groups.

Is this fluent construction with temporary state?
  -> Use *Builder, not *Util.

Is the proposed name CommonUtil, MiscUtil, or GameUtil?
  -> Delete the idea and rethink the ownership.
```
