# RaindanceMC/pets

- **`pets-module`** — Rain's Flexible Modules — Pets: pets that fight beside you. Vanilla animals and monsters — up to a
  mythic wither, warden and ender dragon — shrunk to a third or half a player, in four roles (fighter, guard, ranged,
  healer), with levels, a personality their owner bends a little, knock-outs instead of death, riding (flying ones
  fly), names painted like their owner's own. Earned by playing, bought (common ones only), granted by a rank or an
  admin, or kept for supporters; rarer species stay secret until somebody has seen one.
- **`pets-standalone`** — Rain's Pets: the pets module and the standard wrapper, shaded together.

The pets in the world are RainsCore's companions (`world.companion`, Core 2.42.0+): Core makes, drives and protects
the mob; this module only says what a pet is and hears what happens to it.

Part of [RaindanceMC](https://github.com/RaindanceMC); built on [RainsCore](https://github.com/RaindanceMC/RainsCore)
and the shared parent in [framework](https://github.com/RaindanceMC/framework).

## Build

```
mvn install
```

Needs Java 25 and read access to `packages.tstieh.de`.

## Plugins

- `pets-standalone/target/RainsPets-0.1.0.jar` — also published as `de.raindancer.modules:pets-standalone:0.1.0`

## Commands

`/pets` (alias `/pet`) — the collection; `call|home|stay|follow|mode|rename|colours <pet>`, `species`;
staff: `/pets admin give <player> <species> [name]`, `take <player> <pet>`, `personality <player> <pet> <personality>`,
`list <player>`. Species and how each is had: `plugins/RainsPets/pets.yml`.
