# Better Sounds assets

ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.

The gameplay replacements below use the audio supplied for this update. All new
encodes are mono, 44.1 kHz Ogg Vorbis (quality 5), with short edge fades to avoid
clicks. No upstream ownership or license is changed by conversion; retain the
source providers' applicable licenses when distributing these sounds.

| Event | Supplied source | Edit |
| --- | --- | --- |
| Eating | `man-eating-pickle.mp3` | First audible bite, 0.236–1.036 seconds, 1.6× tempo; about half a second per event |
| Hits | `mixkit-strong-punches-to-the-body-2198.wav` | Three separate clips: 0–0.440, 1.333–1.663, 2.490–2.864 seconds; equal-probability Minecraft variants, never the entire three-hit sequence |
| Wind charges | `mixkit-air-woosh-1489.wav` | 0.100–1.400 seconds, 1.8× tempo |
| Mace hits | `mixkit-metallic-sword-strike-2160.wav` | Full 1.330-second strike, edge fades |
| Shield break | `Alternative Shield Sounds.zip` → `assets/minecraft/sounds/random/break.ogg` | Mono conversion, edge fades; only `minecraft:item.shield.break` is remapped |

Tempo edits retain source pitch. Runtime volume, pitch, location, source,
attenuation, relativity and delay still come from the original sound instance.
The raw AbstractSoundAccessor volume/pitch path remains intact because sound
events are not yet resolved at the playback hook. Replacement identifiers use
the `reconfig` namespace and are not mapped again. General item/block breaks and
shield blocking remain vanilla. Each category can be disabled separately.

Validation: `python -m unittest discover -s tests -p test_sound_replacement.py`
compiles the real BetterSounds implementation against a narrow unresolved-sound
fixture and probes all registered replacement audio with ffprobe. This verifies
the unresolved-level safety regression, shield-only mapping, toggles,
non-recursion, variant registration, codecs, channel count and short duration.
It does not replace a full Minecraft build or in-game listening test.
