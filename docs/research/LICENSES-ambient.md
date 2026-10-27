# LICENSES — ambient sounds & textures (client.ambient)

**Last Updated**: 2026-10-27 — initial: all assets original, no third-party material.

| Asset | Origin | License |
|-------|--------|---------|
| `assets/hukhong/sounds/amb/*.ogg` (void_drone, void_whisper_1..3, war_drum, town_day, port_water, thunder_far, dust_hum, wild_night) | Synthesized from scratch (numpy + soundfile, FFT-filtered noise, sine/FM partials) by `tools/gen_ambient_sounds.py` — deterministic seed, re-runnable | Original work of the project (same terms as the mod); no samples used |
| `assets/hukhong/textures/ambient/mote.png`, `ash.png` | Procedurally generated radial/blob alpha masks (Python/Pillow) | Original work of the project |
| `assets/hukhong/shaders/**/void_grade.*` | Original GLSL; uses vanilla `blit` vertex shader by reference only | Original work of the project |

No CC0/CC-BY sources are used, so no attribution is required.
