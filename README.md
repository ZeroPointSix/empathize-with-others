# Empathy AI Assistant

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Compose-2024.12.01-green.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-Apache%202.0-orange.svg)](LICENSE)

[中文文档](README_CN.md)

---

## ⚠️ IMPORTANT NOTICE

### Disclaimer

**This is a personal learning project.** The developer makes NO warranties or guarantees regarding this application. Use at your own risk.

- ❌ **No Warranty**: This software is provided "as is" without warranty of any kind
- ❌ **No Liability**: The developer assumes no responsibility for any issues, damages, or consequences arising from the use of this application
- ⚠️ **Potential Bugs**: This project may contain bugs, security vulnerabilities, or other defects
- 📚 **Educational Purpose**: This is a practice project for learning Android development

### Privacy & Data Security

**Important information about data handling:**

- 🏠 **Local-Only Architecture**: This app runs entirely on your device with NO backend server
- 💾 **Local Data Storage**: All data (chat history, contact information, tags, etc.) is stored locally on your device
- 🔐 **Your Responsibility**: You are responsible for securing your device and the data stored on it
- 🌐 **API Communication**: Data is only transmitted when you make requests to third-party AI APIs (OpenAI, DeepSeek, etc.)
- ⚠️ **Sensitive Information**: This app may process sensitive information including chat messages and contact details. Use with caution and ensure you have appropriate permissions to process such data

**By using this application, you acknowledge that:**
- You understand this is an experimental learning project
- You accept all risks associated with its use
- You are responsible for complying with applicable privacy laws and regulations
- You will not hold the developer liable for any issues or damages

---

A privacy-first Android social communication assistant that helps users better understand and respond to social scenarios through AI technology.

## ✨ Core Features

- 🔒 **Privacy First**: Zero-backend architecture with all data stored locally
- 🔑 **Bring Your Own Key (BYOK)**: Support for 7+ AI providers including OpenAI, DeepSeek, and more
- 🎯 **Local First**: High-frequency operations use local rules for zero-latency response
- 🏗️ **Clean Architecture**: Strict multi-module architecture with pure Kotlin domain layer
- 🎨 **Material Design 3**: Modern Jetpack Compose UI

## 🏛️ Architecture

The project adopts Clean Architecture + MVVM multi-module architecture:

```
:domain/        # Pure Kotlin - Business models, UseCases, Repository interfaces
:data/          # Android Library - Room, Retrofit, Repository implementations
:presentation/  # Android Library - Compose UI, ViewModels
:app/           # Application - Entry point, Android services
```

### Dependency Rules

- `app` depends on `data` and `presentation`
- `data` and `presentation` depend on `domain`
- `domain` layer is **pure Kotlin** with NO Android SDK dependencies

### Data Flow

```
UI → ViewModel → UseCase → Repository (Interface) → Repository (Impl) → Data Source
```

### Tech Stack

- **Language**: Kotlin 2.0.21 (K2 Compiler)
- **Build Tool**: Gradle 8.13 + Kotlin DSL
- **UI Framework**: Jetpack Compose (BOM 2024.12.01) + Material 3
- **Dependency Injection**: Hilt 2.52
- **Database**: Room 2.6.1 (Schema v16)
- **Networking**: Retrofit 2.11.0 + OkHttp 4.12.0
- **Async**: Kotlin Coroutines 1.9.0 + Flow

## 🚀 Quick Start

### Requirements

- Android Studio Ladybug | 2024.2.1 or higher
- JDK 17
- Android SDK 34
- Gradle 8.13

### Setup

1. **Clone the repository**
```bash
git clone https://github.com/yourusername/empathy-ai.git
cd empathy-ai
```

2. **Configure SDK path**
```bash
# Copy example configuration
cp local.properties.example local.properties

# Edit local.properties and set your Android SDK path
# Windows: sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk
# macOS/Linux: sdk.dir=/Users/YourUsername/Library/Android/sdk
```

3. **Build the project**
```bash
# Windows
.\gradlew assembleDebug

# macOS/Linux
./gradlew assembleDebug
```

4. **Run the app**
```bash
# Install to device
.\gradlew installDebug

# Or run directly in Android Studio
```

### Configure AI Provider

On first launch, you need to configure an AI provider:

1. Go to Settings → AI Configuration
2. Click "Add Provider"
3. Select provider type (OpenAI, DeepSeek, etc.)
4. Enter your API key
5. Set as default provider

## 📱 Main Features

- **AI Advisor**: Analyze chat context and provide communication suggestions
- **Contact Profiles**: Intelligently record and analyze contact information
- **Tag System**: Manage landmine tags and strategy tags
- **Floating Window**: System-level floating window for quick access
- **Privacy Protection**: Data desensitization engine to protect sensitive information
- **Prompt Management**: Customize AI prompt templates

## 🧪 Testing

```bash
# Run all unit tests
.\gradlew test

# Run specific module tests
.\gradlew :domain:test
.\gradlew :data:test
.\gradlew :presentation:test

# Run Android tests (requires connected device)
.\gradlew connectedAndroidTest
```

## 🤝 Contributing

We welcome contributions! Here's how you can help:

### Getting Started

1. **Fork the repository** and clone your fork
2. **Create a feature branch** from `main`
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **Read the documentation**
   - [CLAUDE.md](CLAUDE.md) - Project overview and development guidelines
   - [WORKSPACE.md](WORKSPACE.md) - Task coordination and conflict prevention

### Development Guidelines

1. **Code Style**
   - Follow Kotlin coding conventions
   - Use meaningful variable and function names
   - Keep functions small and focused
   - Run `.\gradlew ktlintCheck` before committing

2. **Architecture Rules**
   - Respect module boundaries (see architecture section)
   - Domain layer must remain pure Kotlin (no Android dependencies)
   - Use `StateFlow` for state management
   - Use `Result<T>` for error handling
   - Use `viewModelScope` in ViewModels, never `GlobalScope`

3. **Database Changes**
   - Room schema changes MUST include migration scripts
   - Add migration tests
   - Update schema version in `gradle.properties`

4. **Testing**
   - Write unit tests for business logic
   - Test coverage for critical paths
   - Run tests before submitting PR

5. **Commit Messages**
   - Use conventional commits format: `type(scope): description`
   - Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`
   - Example: `feat(domain): add contact profile use case`

### Submitting Changes

1. **Ensure all tests pass**
   ```bash
   .\gradlew test
   .\gradlew lint
   ```

2. **Commit your changes**
   ```bash
   git add .
   git commit -m "feat(scope): your feature description"
   ```

3. **Push to your fork**
   ```bash
   git push origin feature/your-feature-name
   ```

4. **Create a Pull Request**
   - Provide a clear description of the changes
   - Reference any related issues
   - Ensure CI checks pass

### Areas for Contribution

- 🐛 Bug fixes
- ✨ New features (check issues for feature requests)
- 📝 Documentation improvements
- 🌐 Translations
- 🧪 Test coverage improvements
- ♿ Accessibility enhancements

### Code Review Process

- All submissions require review
- Maintainers will provide feedback
- Address review comments
- Once approved, your PR will be merged

## 📖 Documentation

- [Project Documentation](文档/项目文档/README.md)
- [Development Documentation](文档/开发文档/)
- [Tech Stack Details](.kiro/steering/tech.md)
- [Architecture Design](.kiro/steering/structure.md)

## 🐛 Reporting Issues

Found a bug? Please create an issue with:
- Clear description of the problem
- Steps to reproduce
- Expected vs actual behavior
- Device/OS information
- Relevant logs or screenshots

## 📄 License

This project is licensed under the [Apache License 2.0](LICENSE).

## 🙏 Acknowledgments

Thanks to all open source contributors, especially:

- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Hilt](https://dagger.dev/hilt/)
- [Room](https://developer.android.com/training/data-storage/room)

---

**Note**: This project is for learning and research purposes only. Please comply with the terms of service of relevant AI providers.
