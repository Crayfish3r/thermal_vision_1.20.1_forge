# Thermal Vision ports

The repository root remains the independent, stable Minecraft 1.20.1 / Forge 47.4.10 / Java 17 release. Port work does not alter its source or build configuration.

| Directory | Minecraft | Forge | Java | Gradle | Current status |
| --- | --- | --- | --- | --- | --- |
| Repository root | 1.20.1 | 47.4.10 | 17 | 8.8 | Stable release |
| `forge-1.21.1` | 1.21.1 | 52.1.2–52.x (built with 52.1.16) | 21 | 9.3.1 | Preview JAR builds; manually checked on 52.1.16 |
| `forge-26.3` | 26.3 | 66.0.6 | 25 | 9.7.1 | Buildable workspace only; no distributable JAR |

The two ports are standalone Forge MDK builds. Both compile shared, version-independent state and configuration from `common/src/main/java`. Their Minecraft/Forge integrations stay in version-specific source sets; the root 1.20.1 build does not consume the shared directory.

## Forge 1.21.1

The port includes server-authoritative activation, charge/cooldown, battery and goggles, data-component charge storage, networking, client fade/sounds/HUD, cold-world post-processing, animated-model entity mask/composite, luminous-block glow, resources and data. It retains the original separation between background grading and entity heat. Its output is `forge-1.21.1/build/libs/thermal_vision_forge_1_21_1-4.0.0-1.21.1-preview1.jar`.

Build with `cd forge-1.21.1` and `.\gradlew.bat clean build --offline --console=plain` (or `./gradlew clean build --offline --console=plain` on Unix). The offline flag is optional when Maven/Minecraft services are reachable.

Compilation, resource packaging and JSON syntax have been checked. The Java sources also compile against the minimum declared Forge version, 52.1.2. Forge 52.1.0 and 52.1.1 are not supported because the HUD uses `ForgeLayeredDraw` and `AddGuiOverlayLayersEvent`, added in 52.1.2. The user manually checked the earlier preview JAR on Forge 52.1.16 without finding runtime issues; the broadened-version JAR and its minimum version have not yet been runtime-tested. This JAR remains a preview.

## Forge 26.3

The independent Java 25 / ForgeGradle 7 workspace is ready. Its JAR task remains disabled until it contains the complete mod. A direct copy of the 1.21.1 integration was tried and intentionally removed because it does not compile against the 26.3 APIs; do not distribute the current workspace as a Thermal Vision release.

The 26.3 integration needs version-specific replacements at these boundaries:

1. Gameplay: `ResourceLocation` is now `Identifier`; `ArmorItem` is gone, so goggles need the current equippable/item-component APIs. Adapt lifecycle event registration and payload networking without changing the server-authoritative state rules.
2. Presentation: update GUI extraction/layers and sound/keybind events, preserving the activation fade and HUD.
3. Rendering: use `AddFramePassEvent` and the current frame graph/RenderPipeline/PostChain APIs. Do not carry over the 1.20.1/1.21.1 raw OpenGL framebuffer calls: the 26.3 renderer must work on both OpenGL and Vulkan. Preserve two independent passes and real animated living-entity model geometry.
4. Data/assets: migrate recipes, item models, equipment assets and shader assets to the 26.3 resource formats.
5. Only after full compilation and static package checks, enable `jar`. Runtime testing remains for the user.

## Runtime acceptance checklist

For each finished JAR: activation/deactivation and fade; charge, depletion, battery, cooldown and networking; first/third person; HUD and sounds; cold-world grading; entity-model heat; luminous-block glows; reload, resize/fullscreen and dimension change; translucent blocks, invisibility/spectators and modded living renderers; dedicated-server loading. For 26.3, test both OpenGL and Vulkan.
