# Git Hooks Setup

This project uses git hooks to validate commit messages against the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/#summary) specification.

## Install Git Hooks

Run the installation script to set up the commit message validation hook:

```bash
./scripts/install-git-hooks.sh
```

After installation, all commit messages will be validated. Example valid formats:
- `feat: add new feature`
- `fix(parser): resolve parsing bug`
- `docs: update README`
- `feat!: breaking change`

---

# Essential Maven Goals:

```bash
# Analyze dependencies
./mvnw dependency:tree
./mvnw dependency:analyze
./mvnw dependency:resolve

./mvnw clean validate -U
./mvnw buildplan:list-plugin
./mvnw buildplan:list-phase
./mvnw help:all-profiles
./mvnw help:active-profiles
./mvnw license:third-party-report

# Clean the project
./mvnw clean

# Clean and package in one command
./mvnw clean package

# Run integration tests
./mvnw clean verify

# Generate Swagger UI
./mvnw clean verify -pl cursor-cloud-agents-openapi -Pswagger-ui
jwebserver -p 8020 -d "$(pwd)/cursor-cloud-agents-openapi/target/swagger-ui/"

# Run tests with code coverage
# Note: Run 'site' phase after verify to generate aggregated report
./mvnw clean test verify
./mvnw clean test verify site -Pjacoco

# Generate Cyclomatic complexity report
./mvnw clean site -Pcyclomatic-complexity

# Generate project reports
./mvnw clean site
jwebserver -p 8000 -d "$(pwd)/target/site/"
jwebserver -p 8015 -d "$(pwd)/jacoco-report-aggregated/target/site/"
jwebserver -p 8020 -d "$(pwd)/docs"

# Check for dependency updates
./mvnw versions:display-property-updates
./mvnw versions:display-dependency-updates
./mvnw versions:display-plugin-updates

mvn versions:set -DnewVersion=0.1.0
mvn versions:commit

# Create jar
./mvnw clean package -DskipTests

# Run Churrera
java -jar churrera-cli/target/churrera-cli-0.1.0.jar
```
