# Contributing to DotKit

We welcome contributions to DotKit! Please follow these guidelines to ensure a smooth collaboration process.

## Guidelines

- **Kotlin Coding Conventions**: Please adhere to the official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).
- **Testing**:
    - Ensure all existing tests pass.
    - Add new tests for any new features or bug fixes.
- **Documentation**:
    - Write KDoc for all public APIs.
    - Update `README.md` if necessary.
- **Platform Specifics**: Use the `expect/actual` pattern for platform-specific implementations.

## Development Setup

### Building the Project

```bash
./gradlew build
```

### Running Tests

```bash
./gradlew test
./gradlew :dotkit-core:desktopTest
./gradlew :dotkit-core:testDebugUnitTest
```

## Pull Request Process

1.  Fork the repository.
2.  Create a feature branch (`git checkout -b feature/amazing-feature`).
3.  Commit your changes (`git commit -m 'Add some amazing feature'`).
4.  Push to the branch (`git push origin feature/amazing-feature`).
5.  Open a Pull Request.

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
