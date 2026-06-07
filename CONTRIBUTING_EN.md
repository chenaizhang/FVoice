# Contributing to Clarivo

Thank you for your interest in contributing to Clarivo! We welcome pull requests, bug reports, and feature suggestions.

## Development Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/chenaizhang/Clarivo.git
   cd Clarivo
   ```

2. **Open in Android Studio**
   - Use Android Studio Koala or newer
   - JDK 17 is required

3. **Build the project**
   ```bash
   ./gradlew :app:assembleDebug
   ```

## Code Style

- Follow the existing Kotlin coding conventions
- Use meaningful variable and function names
- Add KDoc for public APIs
- Keep Composable functions focused and reusable

## Commit Messages

Please follow [Conventional Commits](https://www.conventionalcommits.org/):

- `feat:` — New feature
- `fix:` — Bug fix
- `docs:` — Documentation only changes
- `style:` — Code style changes (formatting, missing semi colons, etc)
- `refactor:` — Code change that neither fixes a bug nor adds a feature
- `perf:` — Performance improvement
- `test:` — Adding or correcting tests
- `chore:` — Changes to the build process or auxiliary tools

Example:
```
feat: add real-time recording waveform visualization
```

## Pull Request Process

1. Fork the repository and create your branch from `main`
2. Ensure your code compiles and passes basic checks
3. Update documentation if needed
4. Open a Pull Request with a clear description of the changes

## Native Engine Development

If you are working on whisper.cpp or DeepFilterNet integration:

1. Download the third-party source code into `app/src/main/cpp/`
2. Uncomment the `externalNativeBuild` block in `app/build.gradle.kts`
3. Ensure CMake 3.22.1+ and NDK are installed
4. See the native engine compilation section in [Build Guide](docs/BUILDING_EN.md)

## Reporting Issues

When reporting bugs, please include:
- Device model and Android version
- Steps to reproduce
- Expected vs actual behavior
- Relevant logs (if possible)
