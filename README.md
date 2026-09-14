# NoPork

A Paper plugin that punishes players for eating pork — raw or cooked.

Every pork chop eaten inflicts **Nausea for 20 seconds** and **Weakness for 20
seconds**. On top of that, each pork chop carries a **1-in-20 chance** of
teleporting the player to the nether roof at the coordinates matching where they
were standing.

Entirely server-side. Players connect with a vanilla client and need to install
nothing.

## Requirements

- Paper 26.2 (`api-version: 26.2`)
- Java 25 — the same runtime Paper 26.2 itself requires

Also runs on Folia: all state changes go through the offending player's own
region scheduler, and teleports use `teleportAsync`.

## Build

```sh
mvn package
```

The jar lands at `target/NoPork-1.0.0.jar`. Drop it into `plugins/` and restart.

## How the teleport picks its destination

"The corresponding coordinates" means the same mapping a nether portal uses:

| From | X / Z | Y |
| --- | --- | --- |
| Overworld | divided by 8 | roof |
| Nether | unchanged | roof |
| The End / custom | divided by 8 (no vanilla mapping exists) | roof |

`roof` is the world's **logical height** when the world has a ceiling — y=128 in
a vanilla nether, which is the first free block above the solid bedrock at y=127.
This is deliberately not `getMaxHeight()`: the nether's world height is 256, so
that would drop the player 128 blocks above the roof and into open air.

The target world is chosen in this order: the player's own world if it is already
a nether, then `<world>_nether`, then the first nether world the server has. If
the server has no nether at all, the teleport is skipped and the `no-nether`
message is sent instead.

Destinations are clamped inside the target world's border.

## Configuration

`plugins/NoPork/config.yml` — reload it in place with `/nopork reload`.

| Key | Default | Meaning |
| --- | --- | --- |
| `foods` | porkchop, cooked_porkchop | Items that count as pork. Any item id works. |
| `effects` | nausea 20s, weakness 20s | Any effects from the `mob_effect` registry, with duration and amplifier. |
| `override-existing-effects` | `true` | Restart effects at full duration instead of letting a longer existing effect win. |
| `teleport.enabled` | `true` | Master switch for the teleport. |
| `teleport.chance-denominator` | `20` | 1-in-N chance per pork item. |
| `teleport.y-offset` | `0` | Blocks added to the roof height. |
| `teleport.scale-coordinates` | `true` | Apply the 8:1 portal ratio. `false` keeps raw X/Z. |
| `worlds.mode` | `all` | `all`, `whitelist`, or `blacklist` over `worlds.list`. |
| `sound.*` | pig ambient | Sound played to the offender. Blank `id` disables it. |
| `messages.*` | see file | MiniMessage strings. Blank disables an individual message. |

Message placeholders: `<player>`, `<item>`, `<x>`, `<y>`, `<z>`, `<world>`.

Anything the server cannot resolve — an unknown item, effect, or sound — is
logged as a warning and skipped, so one typo never stops the plugin loading.

## Permissions

| Node | Default | Effect |
| --- | --- | --- |
| `nopork.exempt` | nobody | Holder is never punished. |
| `nopork.admin` | op | Allows `/nopork reload`. |

## Notes

The punishment runs on the tick after `PlayerItemConsumeEvent`, at `MONITOR`
priority with `ignoreCancelled = true`. That way another plugin cancelling the
meal also cancels the punishment, vanilla finishes eating before the effects
land, and a teleport never fires mid-consume.
