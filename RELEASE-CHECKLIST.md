# Before publishing ReConfig

- [ ] Build with Java 21 using `gradlew.bat buildAndCollect` and retain the complete build log.
- [ ] Launch the resulting JAR on Fabric 1.21.11, first with only required dependencies, then the target modpack.
- [ ] Eat with Better Sounds enabled; verify all four sound switches, positions and volume.
- [ ] Enable Custom Crosshair, change shape/color/gap/opacity, test first/third person, F1, and restart.
- [ ] Create/edit/hide/delete waypoints; reconnect/restart; switch servers and dimensions; test one death marker per death.
- [ ] Publish complete corresponding source for the exact JAR, with wrapper/build scripts and all required notices. Do not point only to Polyfrost's upstream repository.
- [ ] Keep `LICENSE`, `LICENSE-RECONFIG.txt`, `ATTRIBUTIONS.md`, `THIRD_PARTY_NOTICES.md`, and `FILE_ATTRIBUTIONS.tsv` with the release.
- [ ] Verify rights for user-supplied media, Minecraft-named fonts and all bundled dependency assets. This repair does not certify those rights.
- [ ] Review GPL obligations and upstream terms with a qualified adviser if unsure, especially for commercial distribution.

The local repair environment cannot currently download Gradle. Passing helper tests does not certify a working Minecraft release. See `STATUS-v27.md`.
