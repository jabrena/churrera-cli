# Maintenance

Some **User prompts** designed to help in the maintenance of this repository.

```bash
# Prompt to provide a release changelog
Can you update the current changelog for 0.2.0 comparing git commits in relation to 0.1.0 tag. Use  @https://keepachangelog.com/en/1.1.0/  rules
```

## Release process

```bash
./mvnw versions:set -DnewVersion=0.2.0
./mvnw versions:commit
./mvnw clean test verify
```

- [ ] Update CHANGELOG.md
- [ ] Remove SNAPSHOT from pom.xml
- [ ] Review if necessary to update example with a newest PML Schema
- [ ] Last review in docs (Manual)
- [ ] Review git changes for hidden issues (Manual) https://github.com/jabrena/churrera/compare/0.1.0...0.2.0
- [ ] Tag repository
- [ ] Create a Github release
- [ ] Update jbang-catalog.json https://github.com/jabrena/jbang-catalog
- [ ] Create article
- [ ] Communicate in social media

---

```bash
# Prompt to provide a release changelog
Can you update the current changelog for 0.2.0 comparing git commits in relation to 0.1.0 tag. Use  @https://keepachangelog.com/en/1.1.0/  rules

## Note: Refactor a bit more to include all pom.xml

## Tagging process
git tag --list
git tag 0.1.0
git push --tags

# Update Snapshot
git tag -f 0.2.0-SNAPSHOT
git push --force --tags
```
