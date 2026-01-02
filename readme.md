# Chaos Pillars

A fun and chaotic Minecraft minigame plugin based on the Cubecraft game "Pillars of Fortune". Battle it out with up to 10 players on shrinking pillars with random events and powerups!

## Features

- 🎯 **Multiplayer Support** - Up to 29 players can compete simultaneously
- 🏗️ **Dual rings** - Uses two rings when there are more than 10 players
- ⚙️ **Fully Configurable** - All timings and settings can be adjusted
- 🧹 **Auto Reset** - Automatically resets after every game
- 🎲 **Random Events** - Powerups and events keep games exciting
- 📊 **Statistics System** - Track wins, losses, streaks, and more

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/chaos start` | Starts a new Chaos Pillars game | OP |
| `/chaos stop` | Stops and resets the current game | OP |
| `/chaos reload` | Reloads the plugin configuration | OP |
| `/chaos stats` | Shows your personal statistics | Everyone |

## Installation

1. Download the latest release from the releases page
2. Place the `.jar` file in your Paper or Spigot server's `plugins` folder
3. Restart your server
4. Configure the plugin (if standard settings need changing)
5. Start playing with `/chaos start`!

## Configuration

The configuration file is automatically generated in the plugin's folder (`plugins/ChaosPillars/config.yml`) when the plugin first loads.

### Customizable Options:
- **Pillar block types** - Change what blocks the pillars are made of
- **Floor block types** - Customize the arena floor material
- **Event timing** - Adjust how often random events occur
- **Game duration** - Control how long games last

After making changes, use `/chaos reload` to apply them instantly without restarting the server.

## How to Play

1. Players join the game world
2. An operator starts the game with `/chaos start`
3. Players spawn on individual pillars
4. Last player standing wins!

## License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for details.

## Contact

- **Discord**: BoesBoess102
- **Issues**: Please report bugs and feature requests on the GitHub issues page