# Thermal Vision contributor guide

## Project baseline

- Thermal Vision 4.0 for Minecraft 1.20.1, Forge 47.4.10, official Mojang mappings, Java 17.
- Gradle 8.8 through the checked-in wrapper; ForgeGradle is constrained to `[6.0,6.2)`.
- Mod id and asset namespace: `thermal_vision`.
- Main source set: `src/main/java`; resources: `src/main/resources`; generated data, when present: `src/generated/resources`.
- There are currently no automated test sources. A successful Gradle build does not compile or validate GLSL at runtime.

## Architecture

- `ThermalVision` is the mod entry point. It registers items/sounds and queues network registration.
- `registry/` owns Forge deferred registries; `item/` owns the goggles charge stored in item NBT and battery interaction.
- `common/ServerThermalVisionStates` is authoritative for activation, charge consumption, cooldown, lifecycle cleanup, and S2C synchronization. `ThermalVisionAccess` resolves the equipped goggles.
- `network/` contains the C2S toggle packet, S2C state packet, and the `SimpleChannel`.
- `client/ClientThermalVisionState` mirrors server state and drives fade/sound/reset behavior. `ThermalVisionClientEvents` is the central Forge client-event dispatcher. `ThermalVisionHudOverlay` and `ThermalVisionClientSounds` are presentation-only.
- `client/render/` contains entity-mask shader registration, renderer-layer registration, the custom mask `RenderType`, signature selection, mask/composite target management, and the client resource-reload listener.
- `ThermalVisionWorldRenderer` is independent of entity highlighting. It currently renders billboard/ground glows only for luminous blocks.

## Render pipeline

Keep the background effect and entity heat overlay as independent passes:

1. At `AFTER_CUTOUT_BLOCKS`, `ThermalVisionRenderPipeline.prepareFrame` creates/resizes a depth-enabled `TextureTarget`, clears it, and copies the main framebuffer depth.
2. Every supported `LivingEntityRenderer` has a `ThermalEntityLayer`. During normal entity rendering it reuses the renderer's real animated parent `EntityModel` and entity texture. It writes semantic data through `ThermalRenderTypes.entityMask` into the mask target: red is normalized heat, alpha is coverage/effect strength, and green/blue are reserved.
3. At `AFTER_ENTITIES`, `ThermalVisionWorldRenderer` may render luminous-block glows into the scene. Its `heat_aura.png` use is for blocks, not entities.
4. Immediately before GUI rendering, `ThermalVisionPostProcessor` applies the original luminance-based cold-world grading from the baseline implementation. The two-pass chain is built programmatically so shader uniforms are accessed through reobfuscation-safe `PostPass` references. `ThermalVisionRenderPipeline.composite` then overlays the independent semantic entity mask with the hot palette. With the GUI hidden, both passes run at `AFTER_LEVEL` because the first-person hand is also skipped.
5. The HUD is drawn after the hotbar overlay and is not part of either world post-process.

Framebuffer draw/read bindings are preserved around target creation, resize, clear, depth copy, post-processing, and destruction. Client resource reload invalidates both post chains and the mask target; they are recreated lazily with the reloaded shader resources.

Entity highlighting must continue to use real `EntityModel` geometry. Do not reintroduce entity AABB/billboard auras or make entity heat detection depend on the background shader.

## Commands

Run from the repository root on a Java 17 installation:

- Windows clean build: `.\gradlew.bat clean build --console=plain`
- Unix-like clean build: `./gradlew clean build --console=plain`
- Development client: `.\gradlew.bat runClient`
- Dedicated development server: `.\gradlew.bat runServer`
- GameTest server: `.\gradlew.bat runGameTestServer`
- Regenerate data: `.\gradlew.bat runData`
- IntelliJ run configurations: `.\gradlew.bat genIntellijRuns`

The reobfuscated release artifact is written to `build/libs/thermal_vision_v4.jar`.

## Change rules

- Inspect existing code and the working-tree diff before editing. Preserve unrelated or uncommitted user changes.
- Keep compatibility with Minecraft 1.20.1/Forge 47.x and Java 17. Do not replace compatible Forge APIs merely to silence deprecation/removal warnings.
- Avoid large architectural rewrites unless a confirmed defect requires one. Prefer small, reversible changes at existing boundaries.
- Preserve server authority for activation/charge and keep client-only classes behind client distribution boundaries.
- Keep render-thread/OpenGL work in render callbacks. Restore framebuffer and render state after custom passes, including failure paths.
- Avoid per-frame world scans, reflection, allocations, or entity-wide searches in new rendering code. Cache only with explicit invalidation on level changes, resize, disconnect, and resource reload.
- Shader JSON names, sampler names, vertex formats, and Java bindings form one interface; update and review them together.
- After any source/resource change, run the clean build. For render changes, also run `runClient` and test activation/deactivation fade, first/third person, resize/fullscreen, resource reload, dimension change, translucent blocks, invisible/spectator entities, and modded living renderers.
- When Java files are changed, include the complete Java diff in the handoff and explain each change briefly.

## Known limitations and current tasks

- `ThermalSignatureResolver.getHeat` currently returns `1.0` for every eligible living entity; meaningful type/state-based semantic heat signatures are not implemented yet.
- Entity types in `thermal_vision:no_thermal_signature` are excluded before mask rendering; the built-in tag currently contains armor stands.
- The entity thermal layer covers the parent living model, not every auxiliary renderer layer (armor, held items, feature layers), and non-living entities are excluded.
- The mask copies depth after cutout blocks, so translucent-world occlusion semantics still need explicit runtime verification.
- Luminous-block detection scans a large local volume periodically and performs line-of-sight checks for cached sources every rendered frame. This is the main known CPU hotspot.
- GLSL/program JSON failures are only observable in a running client. The Gradle build currently validates Java/resources packaging but not shader compilation or framebuffer behavior.
- `textures/effect/heat_outline.png` and `models/item/thermal_imager_accessory.json` appear unused. Confirm resource-pack/add-on compatibility before deleting them. `heat_aura.png` is still used by the separate luminous-block pass.

Recommended next work, in order: validate the new mask/composite pipeline in a client; implement an extensible, allocation-light heat-signature resolver; reduce luminous-block scan and per-frame occlusion cost; harden resize/resource-reload/failure recovery; then remove only confirmed obsolete entity-aura assets.
