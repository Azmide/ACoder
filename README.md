# ACoder

Redeem code plugin for **Paper 26.2**. Requires Java 25.

Players type `/redeem <code>` (alias `/code`) and get rewards. Admins manage codes in-game with `/acoder`.

## Features

- Paper plugin (`paper-plugin.yml`) with Brigadier-registered commands and tab completion
- MiniMessage, legacy (`&a`) and hex (`&#RRGGBB`) colors in every message
- Per-code reward lists, usage limits and expiry dates (valid through the expiry day)
- Reward prefixes: `[player]`, `[message]`, `[broadcast]` (default is a console command)
- Case-insensitive codes, one redemption per player (UUID) and optionally per IP
- Lockout after too many wrong codes, success sound, non-blocking `data.yml` saves

## Commands

| Command | Description |
| --- | --- |
| `/redeem <code>` | Redeem a code (`acoder.redeem`, default: everyone) |
| `/acoder create <code>` | Create a code |
| `/acoder delete <code>` | Delete a code and its redemptions |
| `/acoder info <code>` | Show rewards, limit, expiry and status |
| `/acoder list` | List all codes |
| `/acoder setlimit <code> <amount>` | Usage limit, `0` = unlimited |
| `/acoder setexpiry <code> <DD-MM-YYYY\|never>` | Set or clear the expiry date |
| `/acoder addreward <code> <command...>` | Add a reward |
| `/acoder removereward <code> <number>` | Remove a reward (numbers from `info`) |
| `/acoder reset <code>` | Let everyone redeem the code again |
| `/acoder reload` | Reload `config.yml`, `lang.yml`, `data.yml` |

Admin commands need `acoder.admin` (default: op).

## Files

- `config.yml` – prefix, redeem settings and the codes
- `lang.yml` – all messages (placeholders use `{braces}`)
- `data.yml` – who redeemed what

Upgrading from 1.3: messages moved from `config.yml` to `lang.yml` (placeholders changed from `%code%` to `{code}`),
so the old `messages:` section is no longer read. Codes and `data.yml` keep working.

## Build

```bash
mvn clean package
```
