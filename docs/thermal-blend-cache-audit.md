# Повторный аудит чёрного экрана

## Root cause / Why black screen happened

Обнаружена утечка **внутреннего shader blend cache** Minecraft 1.20.1. Предыдущее исправление восстанавливало реальные OpenGL blend factors и enable state, но оставляло `BlendMode.lastApplied` от последнего thermal-шейдера. Этот статический кэш общий для `EffectInstance` (post-processing) и `ShaderInstance` (обычные шейдеры Minecraft).

Последовательность подтверждена статическим разбором Minecraft 1.20.1, ресурсов vanilla и Forge GUI:

1. Thermal-программы используют ONE/ZERO. `parseBlendNode()` превращает такую конфигурацию в opaque `BlendMode`; `apply()` записывает его в `lastApplied`.
2. После возврата из `RenderGuiEvent.Pre` Forge первым рисует `VanillaGuiOverlay.VIGNETTE` при Fancy graphics. В логе/настройках проблемного клиента используется этот режим.
3. `Gui.renderVignette()` задаёт RGB blending `ZERO / ONE_MINUS_SRC_COLOR`. Поэтому чёрный центр текстуры должен сохранять изображение мира: `0 * source + 1 * destination`.
4. `GuiGraphics.blit()` применяет `position_tex`. Его shader JSON задаёт `SRC_ALPHA / ONE_MINUS_SRC_ALPHA`. Из-за оставшегося opaque-кэша `ShaderInstance.apply()` считает blend mode изменившимся и перезаписывает только что заданные vignette factors.
5. У vanilla `textures/misc/vignette.png` центр RGBA `(0,0,0,255)`, а alpha в проверенном углу также 255. С alpha blending чёрный центр заменяет мир: `1 * black + 0 * destination`. Модуляция яркости дополнительно затемняет края. HUD рисуется позже, поэтому может оставаться видимым.

Это корректные для OpenGL операции: shader loading и `glGetError()` не сообщают ошибку. Проверка PostChain на искусственных текстурах завершается раньше vanilla GUI и не обнаруживает дефект. Поэтому предыдущие успешные проверки не подтверждали исправление пользовательского сценария.

Лог проблемной сессии: Forge 47.4.23, Minecraft 1.20.1, только Minecraft/Forge/Thermal Vision; shader errors отсутствуют. Установленный JAR соответствовал предыдущему артефакту по SHA-256. Разбор API выполнен по локальному mapped Forge 47.4.10 и vanilla SRG Minecraft 1.20.1. Патченный клиентский JAR Forge 47.4.23 отдельно не исследован: автоматическая проверка разрешений отклонила его копирование из-за лимита сервиса.

## Changed files / What was changed

| Файл | Изменение |
| --- | --- |
| `ThermalPostRenderState.java` | Сохраняет точное значение `BlendMode.lastApplied` перед thermal-операциями и возвращает его вместе с OpenGL state. Уже существующие `finally` обеспечивают восстановление после background/composite и при их ошибках. Сохраняется и `null`, если шейдер ещё не применялся. |
| `ThermalVisionRenderPipeline.java` | Сохраняет тот же кэш в существующем стеке привязок entity mask, восстанавливает после mask draw и при восстановлении утёкших привязок. Opaque mask shader больше не оставляет свой blend cache следующему vanilla shader. |
| `META-INF/accesstransformer.cfg` | Открывает единственное поле `BlendMode.f_85499_` (`lastApplied` в official mappings 1.20.1). Это Forge Access Transformer; reflection в render loop не используется. |
| `build.gradle` | Подключает Access Transformer при компиляции mapped API. В release JAR конфигурация находится по стандартному пути Forge. |

Полный Java-diff этого повторного исправления относительно начала аудита: [thermal-blend-cache-java.patch](thermal-blend-cache-java.patch). Предыдущие изменения пользователя и предыдущего исправления сохранены. Ресурсы шейдеров, палитра, яркость, HUD, управление и сеть в повторном исправлении не менялись.

## How the framebuffer flow works

```text
main (мир) -> thermal_vision -> background_swap
           -> thermal_copy -> background_output -> color-only blit -> main
main (thermal-фон) -> thermal_copy -> scene_copy
                   -> entity composite + mask -> composite_output -> color-only blit -> main
restore OpenGL state + BlendMode cache
main -> vanilla GUI (включая корректную виньетку) -> thermal HUD
```

Пустые post JSON и программное создание цепочек корректны для 1.20.1. `RenderGuiEvent.Pre` остаётся подходящей границей перед GUI. Каждый shader pass пишет только во временный framebuffer. Существующие проверки targets/uniforms/samplers и fallback сохраняют main при исключении до финального копирования; ошибка overlay не очищает готовый thermal background. Текущий фикс устраняет повреждение изображения **после** этих проходов при рисовании GUI.

## Verification

- `gradlew.bat clean build --console=plain --offline`: **BUILD SUCCESSFUL**, включая компиляцию с Access Transformer и `reobfJar`. Только существующие предупреждения deprecated Forge API; тестовых исходников нет.
- Проверены шесть shader/post JSON: синтаксис, пути ресурсов и соответствие uniform/sampler declarations между GLSL и JSON. GLSL не изменялся; GPU-компиляция в этом аудите не запускалась.
- Статически проверен release bytecode: чтение и запись кэша переименованы в `BlendMode.f_85499_` в обоих классах. Проверено наличие `META-INF/accesstransformer.cfg` в release JAR.
- Полный Java patch проходит `git apply --reverse --check`; `git diff --check` проходит.
- **Minecraft, runClient, GL smoke tests и любые runtime-проверки в этом аудите не запускались по просьбе пользователя.** Устранение выявленного дефекта подтверждено на уровне кода; отображение игрового кадра после исправления не проверено запуском.

Артефакт: `build/libs/thermal_vision_v4.jar`.

SHA-256: `EF7EE2D2AADC5D873E038562A5496D8830326CB8EBED694934502F3380476136`.

## Potential remaining compatibility risks

Access Transformer привязан к полю Minecraft 1.20.1; при переносе на другую версию нужно проверить mapping и реализацию `BlendMode`. Восстанавливается прежний кэш, а не принудительный vanilla preset, поэтому состояние стороннего renderer не подменяется. Oculus/Embeddium/OptiFine/Iris и игровой сценарий после изменения не проверялись. Исходные ограничения семантической маски и translucent-depth occlusion этим небольшим исправлением не меняются.
