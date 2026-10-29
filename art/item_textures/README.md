# Item textures: White-hot and Night vision

Source: `src/main/resources/assets/thermal_vision/textures/item/thermal_goggles.png` (32×32 RGBA).
Both sprites use the same canvas and occupied bounds as the source: x=1..30, y=3..28.
The final PNGs are in the same item texture directory, named `thermal_goggles_white_hot.png`
and `thermal_goggles_night_vision.png`. Corresponding item model JSONs use
`minecraft:item/generated` and their own `layer0` textures, matching the original item.
Equipped goggles still use the separate entity geometry and texture atlases.

Generated using the built-in imagegen tool with the source enlarged using nearest-neighbor sampling.
Outputs were sampled back to 32×32 using nearest-neighbor sampling and their alpha quantized
to 0/255 to avoid translucent edge artifacts in generated item geometry.
`item_textures_preview.png` shows the original and both final textures enlarged 8×.

## Verification

- Both item model references resolve to 32×32 RGBA PNGs with binary transparency.
- Original `thermal_goggles.png` is byte-for-byte unchanged.
- Clean build passed with the existing local Curios Maven cache:
  `.\gradlew.bat -I docs/integration/goggles/cache-maven.init.gradle clean build --console=plain`.
  The ordinary build failed because the official Curios Maven request timed out.
- Both new JSONs and PNGs were verified byte-for-byte inside `build/libs/thermal_vision_v5.jar`.
- Existing isolated vanilla client audit passed:
  `.\gradlew.bat -I docs/integration/goggles/cache-maven.init.gradle -PgogglesAudit runClient --console=plain`.
  It reached `GOGGLES CLIENT AUDIT COMPLETE`, including resource reload, resizing,
  fullscreen, activation/deactivation, first/third person, dimension transfer,
  invisible/spectator entities and both new vision modes. No missing new item
  textures/models were reported. Curios runtime and additional modded renderers were not tested.
- No Java source was changed; the release JAR contains no audit classes.

## White-hot prompt

Edit target: the attached upscaled Minecraft thermal goggles inventory icon. Create the WHITE-HOT variant of this exact sprite. Strictly preserve its composition, size, angle, pixel silhouette, head strap, screws and dark tactical black/charcoal frame, with the same simple restrained pixel shading. Replace ONLY the two orange/red glowing lenses with pale gray and noisy white lenses, a few gray/off-white speckles. Keep two separate lenses. Keep the tiny status indicator. This must remain a 32x32 native pixel-art item icon, displayed as a 1024x1024 nearest-neighbor enlargement with exactly 32x32 logical square pixels (32px per logical pixel). No fine detail smaller than one logical pixel, no antialiasing, no blur, no shadows outside the item, no extra objects, no text. Same framing and transparent padding as reference. Genuine transparent background; black parts of item opaque.

## Night vision prompt

Edit target: the attached upscaled Minecraft thermal goggles inventory icon. Create a matching NIGHT VISION inventory item sprite in the exact same pixel-art style, frontal slightly top-down angle, framing and size. Dark black/charcoal tactical headgear, preserve recognizable upper arched strap and metal hardware. Replace the lower binocular eyepieces and nose bridge with ONE continuous wide luminous green visor, spanning both eyes, no central divider and no two circular lenses. Angular dark frame around visor, subtly stepped corners. Green glass in restrained 3-tone pixel shading with a few pale green highlights, black tactical helmet-like top. This must be a 32x32 native Minecraft pixel-art item icon, displayed as a 1024x1024 nearest-neighbor enlargement with exactly 32x32 logical square pixels (32px per logical pixel). No fine detail smaller than one logical pixel, no antialiasing, no blur, no shadows outside the item, no extra objects, no text. Icon must fit same 30x26 logical-pixel bounding box centered inside canvas, same scale as reference. Genuine transparent background; black parts of item opaque.
