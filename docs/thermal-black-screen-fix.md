# Исправление чёрного экрана Thermal Vision

> Этот отчёт относится к первой попытке исправления. После неё пользователь подтвердил сохранение чёрного экрана. Причина, найденная при повторном статическом аудите, и новое исправление описаны в [thermal-blend-cache-audit.md](thermal-blend-cache-audit.md). Прежние синтетические проверки не проверяли последующий vanilla GUI.

Дата: 2026-09-16. Minecraft 1.20.1, Forge 47.4.10, Java 17.

## Root cause / Why black screen happened

В исходной рабочей копии обнаружены два независимых дефекта:

1. `thermal_vision.fsh` передавал `source.a` в результат. Vanilla `blit.json` использует `srcalpha / 1-srcalpha`, а Minecraft 1.20.1 `PostPass.process()` сначала очищает выходной target. При alpha=0 правильный RGB thermal-кадра не записывается поверх очищенного framebuffer. Внутри entity-цепочки также использовался vanilla `blit`.
2. `thermal_entity_composite.vsh` вычисляет `texCoord = Position.xy / OutSize`, но `OutSize` отсутствовал в program JSON. `PostPass.safeGetUniform()` в этом случае ничего не обновляет; GLSL-uniform остаётся нулевым, и координаты выборки становятся неопределёнными. Composite мог испортить даже корректно обработанный фон.

Дополнительные проблемы: composite писал прямо в main после его очистки; обработчик исключения восстанавливал bindings, но не изображение. Размер background-chain обновлялся на тиках, а не непосредственно перед использованием. Composite-chain вызывал `resize()` каждый кадр; в этой версии Minecraft это пересоздаёт временные framebuffer.

## Changed files / What was changed

| Файл | Изменение |
| --- | --- |
| `src/main/java/com/yourname/thermalvision/client/ThermalVisionPostProcessor.java` | Создание и resize в render callback; собственный copy-pass; отдельный output target; запись в main только после успеха; проверка targets/uniforms/sampler/GL errors; восстановление состояния в finally. |
| `src/main/java/com/yourname/thermalvision/client/render/ThermalVisionRenderPipeline.java` | Замена vanilla blit; отдельный результат composite; безопасное копирование в main; проверка OutSize и samplers; прекращение resize на каждом кадре; восстановление состояния после подготовки, composite и удаления targets. Маска сохраняет настоящую геометрию EntityModel. |
| `src/main/java/com/yourname/thermalvision/client/render/ThermalPostRenderState.java` | Общая сохраняемая структура состояния без новых буферов на каждом кадре; проверки ресурсов; копирование только color через framebuffer blit. |
| `src/main/resources/assets/thermal_vision/shaders/program/thermal_copy.json` | Новый copy-pass с ONE/ZERO; использует существующий `thermal_vision.vsh`, объявляет ProjMat и OutSize. |
| `src/main/resources/assets/thermal_vision/shaders/program/thermal_copy.fsh` | Копирует RGB и выдаёт alpha=1. |
| `src/main/resources/assets/thermal_vision/shaders/program/thermal_vision.fsh` | Итоговая alpha=1; исходная палитра, шум, яркость и fade сохранены. |
| `src/main/resources/assets/thermal_vision/shaders/program/thermal_entity_composite.json` | Добавлен обязательный OutSize. |
| `src/main/resources/assets/thermal_vision/shaders/program/thermal_entity_composite.fsh` | Итоговая alpha=1; семантическая маска и палитра сохранены. |

Полный Java-diff относительно рабочей копии **до этого исправления**, включая новый класс: `docs/thermal-black-screen-java.patch`. Проверен через `git apply --reverse --check`. Существовавшие незакоммиченные изменения сохранены.

## How the new framebuffer flow works

```text
main (готовый игровой мир)
  -> thermal_vision -> background_swap
  -> thermal_copy -> background_output
  -> color-only framebuffer copy -> main

main (thermal-фон)
  -> thermal_copy -> scene_copy
  -> thermal_entity_composite (+ entity mask) -> composite_output
  -> color-only framebuffer copy -> main

main -> обычный GUI и thermal HUD
```

Каждый PostPass очищает только собственный временный выход. Main никогда не используется как выход shader-pass. При ошибке до финального копирования текущий main сохраняется: обычная сцена при ошибке background либо thermal-фон при ошибке overlay. Финальное копирование не зависит от alpha blending, не очищает main и не меняет его depth.

Ошибка помечает соответствующую цепочку как failed, поэтому одинаковая ошибка не логируется каждый кадр. Повторная загрузка ресурсов/сброс сбрасывает ошибку; background также повторяет попытку при следующей активации. Диагностика включает этап, bindings и размеры.

Восстанавливаются READ/DRAW bindings, viewport, blend factors/equations и enable, depth test/function/mask, color mask, culling, scissor enable, clear color/depth, OpenGL program, active texture и затронутые texture bindings. Логический shader в RenderSystem не меняется. Структуры хранения состояния создаются один раз.

Пустые post JSON сохранены. Реализация Minecraft 1.20.1 поддерживает `addTempTarget()` и `addPass()`. После добавления проходов необходим `resize()`, который устанавливает их projection matrix. Это проверено на локальном mapped JAR 1.20.1/47.4.10 и реальном PostChain внутри клиента.

`RenderGuiEvent.Pre` сохранён: Forge публикует его перед обходом GUI overlays, что соответствует обработке сцены до HUD. Источник: [ForgeGui для 1.20.1](https://github.com/MinecraftForge/MinecraftForge/blob/1.20.1/src/main/java/net/minecraftforge/client/gui/overlay/ForgeGui.java). Существующая ветка AFTER_LEVEL при скрытом GUI также сохранена. Управление, HUD, серверная логика и сетевой протокол не изменены.

## Verification

- `gradlew.bat clean build --console=plain`: успешная сборка, включая reobfJar. Предупреждения deprecation существующих Forge API не заменялись несовместимыми API.
- Все 6 shader/post JSON прошли синтаксическую проверку. Проверены namespace, пути vertex/fragment, соответствие GLSL uniforms/samplers и program JSON, ONE/ZERO у трёх post-программ.
- Все три post-программы скомпилированы и слинкованы в реальном OpenGL 3.2 на NVIDIA GeForce GTX 1650 Ti. Проверены вход alpha=0, fade 0/0.5/1, видимость холодного фона, пустая и горячая маска, color-copy и сохранность depth. Проверка повторена на размерах 64x48 и 97x65 и после повторной загрузки программ.
- Запущен `runClient` с Forge 47.4.10. Временный проверочный callback внутри клиента использовал настоящий Minecraft PostChain, загруженные ресурсы и новый ThermalPostRenderState. Проверены zero-alpha input, фон, empty/hot mask, отклонение отсутствующего required uniform без изменения main, сохранность depth и восстановление GL-состояния. Результат: `THERMAL_RUNTIME_VERIFICATION PASSED` на 64x48, 97x65, 64x48.
- Временный callback и standalone-проверки удалены перед финальной чистой сборкой. В release JAR они не входят.

## Potential remaining compatibility risks

Проверка внутри клиента использовала искусственные framebuffer, а не полноценную игровую сцену. Не подтверждены ручным прогоном: активация/выключение в мире, первая/третья камера, F1, fullscreen/resize окна, F3+T в мире, переход между измерениями, прозрачные блоки, invisible/spectator и сторонние LivingEntityRenderer. Проверка разных размеров и повторного создания цепочек не заменяет эти игровые сценарии.

Oculus/Embeddium/OptiFine/Iris не требуются и не тестировались. Сторонние MSAA/нестандартные attachment-форматы или stencil-состояния требуют отдельной проверки. Дополнительные два полноразмерных targets увеличивают расход видеопамяти; они обеспечивают сохранность main при ошибке shader-pass.

Готовый артефакт: `build/libs/thermal_vision_v4.jar`.
