# StrikeDialogues

**StrikeDialogues** is an open-source Minecraft plugin that allows you to show players animated, multi-line text dialogues. You can create highly configurable dialogue sequences featuring typing effects and sounds. It supports optional player movement restriction, PlaceholderAPI integration, and many more.

This plugin is the perfect tool to make your server's stories, quests, or informational texts more dynamic and immersive.

## 🌟 Features

  * **➢ | Highly Customizable Configuration:**
    Adjust nearly every setting – including sounds, timing, messages, behavior flags, and per-dialogue options – through the detailed `config.yml`.

  * **➢ | Animated Text Display:**
    Show players smooth, multi-line text dialogues with a character-by-character typing animation.

  * **➢ | Multi-Page Dialogues:**
    Easily break down longer conversations into sequential, easy-to-read pages.

  * **➢ | Customizable Appearance:**
    Control text color, style, and animation speed per dialogue or globally.

  * **➢ | Typing & Start Sounds:**
    Add configurable sounds that play as text appears or when a dialogue begins.

  * **➢ | Movement Restriction:**
    Optionally prevent players from moving while a dialogue is active.

  * **➢ | Perspective Locking:**
    Optionally lock the player's camera view during dialogues (requires ProtocolLib).

  * **➢ | Dialogue End Actions:**
    Trigger actions upon dialogue completion, including:

      * `[CONSOLE]` - Running commands as the console.
      * `[PLAYER]` - Forcing the player to run commands.
      * `[BROADCAST]` - Broadcast a message to everyone.
      * `[SOUND]` - Play sounds when the dialogue ends.
      * `[MESSAGE]` - Send a message to the player.

  * **➢ | Live Custom Text Command:**
    Display specified lines of text instantly using the `/sd custom` command.

  * **➢ | PlaceholderAPI Support:**
    Use placeholders in dialogue text and end actions for dynamic content.

  * **➢ | Trigger Predefined Dialogues:**
    Start configured dialogues using the `/startdialogue` command, optionally targeting specific players.

  * **➢ | Per-Dialogue Settings:**
    Override global settings like sounds, animation speed, and appearance for individual dialogues.

## 🛠️ Installation

1.  Download the latest `.jar` file of StrikeDialogues compatible with your server version.
2.  Place the downloaded `.jar` file into your server's `plugins` directory.
3.  Restart your server or use the command `/reload`.
4.  Once the plugin is loaded, a `config.yml` file will be generated in the `plugins/StrikeDialogues/` directory. You can edit this file to configure the plugin to your liking.

## 💻 Commands & Permissions

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/startdialogue <dialogue> [player]` | Starts a pre-configured dialogue. | `sd.command.startdialogue` |
| `/sd custom <text>` | Instantly displays the specified text as a dialogue. | `sd.command.custom` |
| `/sd reload` | Reloads the plugin's configuration file. | `sd.command.reload` |

## ⚙️ Configuration

All features of the plugin can be controlled via the `config.yml` file. In this file, you can configure:

  * **Global Settings:** Default sounds, animation speed, and text styles.
  * **Dialogue Definitions:** Create your own dialogues. For each dialogue, you can define the text, pages, sounds, appearance, and actions to be triggered on completion.
  * **Behavior Settings:** Enable or disable features like locking player movement or perspective.

**Example Dialogue Configuration:**

```yaml
dialogues:
  welcome:
    pages:
      - "&aWelcome to our server, &e{player_name}!"
      - "&bWe hope you have a great time."
    sound: "entity.experience_orb.pickup"
    speed: 2
    end_actions:
      - "[MESSAGE] &6Dialogue finished! Let the adventure begin!"
      - "[CONSOLE] give {player_name} diamond 1"
```

## 📜 Contribution & Editing Guidelines

This project is open-source and open to community contributions. If you wish to contribute, please follow the rules below.

### Reporting Issues

  * If you find a bug, please create a new issue on the project's GitHub "Issues" tab.
  * In your report, clearly describe the steps to reproduce the bug, what you expected to happen, and what actually happened.
  * Include your plugin version, server version (Spigot, Paper, etc.), and a list of any other relevant plugins.

### Pull Requests

1.  **Fork & Clone:** Fork the project to your own GitHub account and then clone it to your local machine.
2.  **Create a Branch:** For a new feature or bug fix, create a new branch with a descriptive name, such as `feature/new-cool-feature` or `fix/login-bug`.
3.  **Code:** Make your changes. Please follow the existing code style and standards of the project.
4.  **Test:** Thoroughly test your changes to ensure they work as expected and do not break any existing functionality.
5.  **Create a Pull Request:** When you are happy with your changes, create a Pull Request to the main project.
      * Ensure your PR title and description are clear and concise. Summarize the changes you have made.
      * If your PR resolves an existing issue, please reference it in the description with a phrase like `Closes #123`.

### General Rules

  * **Be Respectful:** Be respectful and constructive in all your communications with other community members.
  * **Documentation:** If you are adding a new feature, please don't forget to update the relevant documentation (README, wiki, etc.).
  * **Stay Focused:** It is preferred that each Pull Request focuses on a single purpose or feature. This makes the review and merging process much easier.

Thank you for your contributions\!
