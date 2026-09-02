# ReConfig UI State, HUD Background, and Accent Design

ReConfig remembers the most recently visited top-level sidebar destination for two hours. Detail routes are reduced to their owning top-level route; after expiry, Friends is used. The timestamp and route persist across game restarts.

Every ReConfig HUD exposes a persisted Show Background option. The option controls both the glass fill and outline and is available in the standard HUD settings interface. Zoom is not a HUD and receives no such option.

The Hitbox Categories detail editor has a visible close/back action and Escape returns to Modules without changing the module's enabled state.

The native ReConfig preferences screen exposes appearance configuration, including an accent color whose default is #406CAB. Theme accent consumers and the ReConfig logo use the selected color live, and the setting persists through the existing config system.

Tests cover route expiry, top-level route normalization, HUD background declarations, Hitbox editor dismissal, accent persistence wiring, and logo tinting.
