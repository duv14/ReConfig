# Social, UI, and Module Reset Design

ReConfig keeps its standalone dark glass shell and Bricolage Grotesque typography. Module cards use new ReConfig-owned monochrome SVG icons. Left-click opens details only; enable/disable lives inside details. Card hover is a subtle eased scale, enabled cards use a muted blue title tint and status dot, and disabled cards retain the quiet border.

All module runtime hooks are removed for now. Every module retains its card, persisted enabled state, controls-style keybind, and harmless placeholder switches, sliders, and choices. General settings remain connected to the real UI renderer and use the inherited slider and switch controls.

The social Worker receives a new idempotent schema-repair migration because an already-recorded migration filename may not have applied later schema edits. Error responses expose safe database details during setup. Friend refresh, message refresh, and invitation refresh fail independently. Reciprocal requests form a friendship automatically.

The Add Friend overlay uses full-shell dimming with eased fade and scale. Chat bubbles animate on insertion. Polling tracks message IDs and sends an existing bottom-right ReConfig notification for each newly received message when the main UI is closed, without notifying for historical messages loaded at startup.

Verification covers source invariants, Worker behavior, JSON validity, ZIP integrity, and the Windows `gradlew.bat clean buildAndCollect` handoff.
