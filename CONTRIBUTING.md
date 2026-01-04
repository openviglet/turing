![viglet_logo.png](http://www.viglet.org/img/banner/viglet_turing.png)

# Contributing to Viglet Turing

Thank you for your interest in contributing to Viglet Turing! This guide will help you get started.

## Table of Contents

1. [Getting Started](#getting-started)
2. [How to Contribute](#how-to-contribute)
3. [Development Setup](#development-setup)
4. [Coding Standards](#coding-standards)
5. [Pull Request Process](#pull-request-process)
6. [Community](#community)

## Getting Started

### Quick Links

- 📖 [Architecture Documentation](ARCHITECTURE.md) - Understand the system design
- 🗺️ [Strategic Roadmap](ROADMAP.md) - See where we're heading
- 🎨 [Design Patterns Guide](DESIGN_PATTERNS.md) - Learn our coding patterns
- 📈 [Scalability Guide](SCALABILITY.md) - Performance and scaling strategies
- 👥 [Community Guide](COMMUNITY.md) - Join our community

### Find Your First Issue

If you want to contribute but you're not sure where to start, take a look at the
[issues with the "good first issue" label](https://github.com/openviglet/turing/labels/good%20first%20issue).
These are issues that we believe are particularly well suited for outside
contributions, often because we probably won't get to them right now.

If you decide to start on an issue, leave a comment so that other people know that
you're working on it. If you want to help out, but not alone, use the issue
comment thread to coordinate.

## How to Contribute

### Types of Contributions

We welcome all types of contributions:

1. **Code Contributions**
   - Bug fixes
   - New features
   - Performance improvements
   - Test coverage improvements
   - Refactoring

2. **Documentation**
   - API documentation
   - User guides
   - Tutorial videos
   - Blog posts
   - Translations

3. **Testing**
   - Write test cases
   - Report bugs
   - Perform security testing
   - Load testing

4. **Community**
   - Answer questions
   - Review pull requests
   - Help with triaging issues
   - Organize events

## Development Setup

### Prerequisites

- Java 21+
- Maven 3.6+
- Docker & Docker Compose (recommended)
- Node.js 24+ (for UI development)

### Quick Setup

```bash
# Clone your fork
git clone https://github.com/YOUR_USERNAME/turing.git
cd turing

# Start dependencies with Docker
docker-compose up -d

# Build the project
./mvnw clean install

# Run the application
./mvnw spring-boot:run -pl turing-app

# Access at http://localhost:2700
```

### Running Tests

```bash
# Run all tests
./mvnw test

# Run specific module tests
./mvnw test -pl turing-app

# Run with coverage
./mvnw test jacoco:report
```

## Coding Standards

### Architecture Guidelines

Before writing code, please review:
- [ARCHITECTURE.md](ARCHITECTURE.md) - Understand the overall architecture
- [DESIGN_PATTERNS.md](DESIGN_PATTERNS.md) - Follow established patterns

### Java Code Style

- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- Use meaningful variable and method names
- Write Javadoc for public APIs
- Keep methods focused and short (< 50 lines preferred)
- Use Lombok annotations to reduce boilerplate

### Design Principles

1. **SOLID Principles**
   - Single Responsibility Principle
   - Open-Closed Principle
   - Liskov Substitution Principle
   - Interface Segregation Principle
   - Dependency Inversion Principle

2. **DRY (Don't Repeat Yourself)**
   - Extract common functionality
   - Use utility classes
   - Apply design patterns appropriately

3. **KISS (Keep It Simple, Stupid)**
   - Prefer simple solutions
   - Avoid over-engineering
   - Clear is better than clever

### Code Quality Requirements

- All tests must pass
- Code coverage should not decrease
- SonarCloud quality gate must pass
- No merge conflicts
- Follow existing code patterns

## Pull Request Process

### Before Submitting

1. **Create an Issue** (for major changes)
   - Describe the problem you're solving
   - Discuss the approach
   - Get feedback from maintainers

2. **Create a Branch**
   ```bash
   git checkout -b feature/your-feature-name
   # or
   git checkout -b fix/your-bug-fix
   ```

3. **Make Your Changes**
   - Write clean, well-documented code
   - Add or update tests
   - Update documentation
   - Follow coding standards

4. **Test Your Changes**
   ```bash
   ./mvnw test
   ./mvnw clean install
   ```

5. **Commit Your Changes**
   ```bash
   git add .
   git commit -m "feat: add new search feature"
   ```
   
   Use [Conventional Commits](https://www.conventionalcommits.org/):
   - `feat:` - New feature
   - `fix:` - Bug fix
   - `docs:` - Documentation changes
   - `test:` - Test additions or changes
   - `refactor:` - Code refactoring
   - `chore:` - Maintenance tasks

### Submitting PR

1. **Push to Your Fork**
   ```bash
   git push origin feature/your-feature-name
   ```

2. **Create Pull Request**
   - Use clear, descriptive title
   - Reference related issues
   - Describe what changed and why
   - Include screenshots for UI changes
   - List any breaking changes

3. **PR Template**
   ```markdown
   ## Description
   Brief description of changes
   
   ## Related Issues
   Fixes #123
   
   ## Type of Change
   - [ ] Bug fix
   - [ ] New feature
   - [ ] Breaking change
   - [ ] Documentation update
   
   ## Testing
   How were these changes tested?
   
   ## Checklist
   - [ ] Tests added/updated
   - [ ] Documentation updated
   - [ ] Code follows style guidelines
   - [ ] All tests pass
   ```

### Code Review Process

1. **Automated Checks**
   - Build must succeed
   - Tests must pass
   - Code quality checks must pass

2. **Human Review**
   - At least 1 maintainer must approve
   - Address all feedback
   - Keep discussion constructive

3. **Merge**
   - Maintainer will merge approved PR
   - Squash and merge for clean history

## Community

### Communication Channels

- **GitHub Issues**: Bug reports and feature requests
- **GitHub Discussions**: Questions and general discussion
- **Discord** (coming soon): Real-time chat
- **Email**: opensource@viglet.com

### Code of Conduct

We are committed to providing a welcoming and inclusive environment. Please read our [Code of Conduct](CODE_OF_CONDUCT.md).

### Getting Help

- Check existing documentation
- Search closed issues
- Ask in GitHub Discussions
- Join community calls (see [COMMUNITY.md](COMMUNITY.md))

## Recognition

We value all contributions! Contributors will be:
- Listed in release notes
- Featured on our website
- Eligible for swag and rewards
- Invited to community events

See [COMMUNITY.md](COMMUNITY.md) for details on our recognition program.

## Additional Resources

- [README.adoc](README.adoc) - Project overview
- [ARCHITECTURE.md](ARCHITECTURE.md) - Architecture documentation
- [ROADMAP.md](ROADMAP.md) - Strategic roadmap
- [DESIGN_PATTERNS.md](DESIGN_PATTERNS.md) - Design patterns guide
- [SCALABILITY.md](SCALABILITY.md) - Scalability guide
- [COMMUNITY.md](COMMUNITY.md) - Community building guide

---

**Thank you for contributing to Viglet Turing!** 🚀