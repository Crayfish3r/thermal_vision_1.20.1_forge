# Forge 1.21.1 Java changes

The complete source-level comparison against the stable 1.20.1 tree is in [JAVA_PORT_DIFF.patch](JAVA_PORT_DIFF.patch). `ThermalVisionState` and `ThermalVisionConfig` are unchanged and now compiled from `../common/src/main/java`.

| Java file | Port change |
| --- | --- |
| `ThermalVision.java` | Registers the new armor-material registry before items. |
| `client/ThermalVisionClientEvents.java` | Removes obsolete GUI events; keeps tick, lifecycle and world-stage dispatch. |
| `client/ThermalVisionGuiLayers.java` | Adds world-effect and post-hotbar HUD layers using Forge's 1.21.1 layered GUI. |
| `client/ThermalVisionHudOverlay.java` | Accepts `GuiGraphics` directly and honors hidden GUI. |
| `client/ThermalVisionPostProcessor.java` | Uses factory-created resource IDs and the 1.21.1 `PostChain.addPass` filter argument. |
| `client/ThermalVisionWorldRenderer.java` | Adapts pose and vertex-buffer APIs for the separate luminous-block pass. |
| `client/render/ThermalEntityLayer.java` | Encodes heat/strength as the packed model vertex color expected by 1.21.1. |
| `client/render/ThermalEntityLayerRegistration.java` | Uses the new player-skin model key and renderer lookup methods. |
| `client/render/ThermalPostRenderState.java` | Reconstructs the shader blend cache from captured GL state because its old field is private. |
| `client/render/ThermalSignatureResolver.java` | Uses the current resource-ID factory. |
| `client/render/ThermalVisionRenderPipeline.java` | Adapts resource IDs and post-pass creation; retains isolated mask/composite targets. |
| `client/render/ThermalVisionShaders.java` | Adapts shader registration resource IDs. |
| `item/ThermalBatteryItem.java` | Uses the 1.21.1 tooltip context signature. |
| `item/ThermalImagerAccessoryItem.java` | Stores charge in `CUSTOM_DATA` and accepts an armor-material holder and new tooltip context. |
| `network/ModNetworking.java` | Replaces legacy `SimpleChannel` registration and packet distributors with 1.21.1's builder API. |
| `network/SetThermalVisionS2CPacket.java` | Uses the current payload context and main-thread consumer. |
| `network/ToggleThermalVisionC2SPacket.java` | Uses the current payload context and main-thread consumer. |
| `registry/ModArmorMaterials.java` | Replaces the removed armor-material interface with a registered material record. |
| `registry/ModItems.java` | Passes the material holder and explicit 55-point helmet durability. |
| `registry/ModSounds.java` | Uses the current resource-ID factory. |

The other copied Java classes keep their 1.20.1 behavior unchanged. No changes were made to the stable root Java source.
