# ReConfig 3.1.4 Repair Design

ReConfig 3.1.4 repairs interaction and rendering regressions without exposing unrelated OneConfig content.

## UI and social

- ReConfig search owns its corpus and returns only ReConfig modules, pages, and settings.
- Text fields consume Right Shift while focused, so underscore entry cannot close the screen.
- Shared animated controls provide eased hover, press, dialog, navigation, friend removal, and reply transitions.
- Emoji messages store stable ReConfig asset tokens and render the supplied image assets on every client.
- Settings reuse the mature control primitives already bundled in the standalone client while remaining ReConfig-branded.

## Gameplay rendering

- Custom Crosshair cancels vanilla only while active and draws from Minecraft's crosshair render pass at Minecraft's centered GUI coordinates.
- Freelook stores camera yaw/pitch independently from player yaw/pitch, preserves WASD movement, and restores perspective on exit.
- ToggleSprint uses explicit persisted state rather than altering the option supplier.
- Module HUDs render only in normal gameplay or the HUD editor. All panels use the same anti-aliased rounded/glass surface; Keystrokes is not a special pixel-drawn exception.

## Release

- All public version metadata is 3.1.4.
- Regression tests cover each repaired boundary, followed by the full 1.21.11 buildAndCollect build.
