# SuperRun IntelliJ Plugin

SuperRun is an IntelliJ IDEA plugin that allows users to efficiently manage and execute multiple run configurations with optional delays. It provides an intuitive UI to select, reorder, and launch configurations in either **Run** or **Debug** mode.

## Features
- Select multiple run configurations to execute sequentially.
- Set a delay between launching different configurations.
- Easily reorder configurations using **Up** and **Down** buttons.
- Double-click on a configuration to quickly edit it.
- Saves the order of configurations for future use.

## Installation
### From Source
1. Clone the repository:
   ```sh
   git clone https://github.com/yourusername/superrun.git
   ```
2. Open the project in IntelliJ IDEA.
3. Ensure the required dependencies are resolved.
4. Build and package the plugin using:
   ```sh
   ./gradlew buildPlugin
   ```
5. Install the plugin in IntelliJ:
    - Go to **File** > **Settings** > **Plugins**.
    - Click on the gear icon and select **Install Plugin from Disk**.
    - Select the generated `.jar` file from `build/distributions/`.

## Usage
### Running SuperRun
1. Open IntelliJ IDEA.
2. Navigate to **Run** > **SuperRun** or use the assigned shortcut.
3. Select the configurations you want to execute by checking **Run** or **Debug**.
4. Set an optional delay between launches.
5. Use the **Up** and **Down** buttons to reorder the execution sequence.
6. Click **OK** to start execution.

### Editing Run Configurations
- Double-click on any configuration in the selection dialog to edit it directly.

## Development
### Prerequisites
- IntelliJ IDEA installed
- Gradle and Java 11+ configured

### Building the Plugin
Run the following command to build the plugin:
```sh
./gradlew buildPlugin
```

### Running the Plugin in a Sandbox Environment
To test the plugin in a sandboxed IntelliJ environment, use:
```sh
./gradlew runIde
```

## Contributing
Contributions are welcome! Please submit a pull request with a clear description of your changes.

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contact
For questions or suggestions, feel free to reach out to **Joe** at [your-email@example.com](mailto:your-email@example.com).

