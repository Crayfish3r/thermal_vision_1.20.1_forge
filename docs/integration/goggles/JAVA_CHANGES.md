# Пояснение полного Java diff

`JAVA_DIFF.patch` содержит весь Java delta этой задачи относительно снимка checkout
на момент начала интеграции, включая новые файлы и dev-аудит. Уже существовавшие
пользовательские Java-изменения присутствуют в исходном снимке и не отменяются патчем.
Патч — часть передачи результата, не инструкция автоматически применять его поверх
уже изменённого checkout. Код и ресурсы уже изменены в рабочей папке.

| Java-файл | Изменение и причина |
|---|---|
| `ThermalVision.java` | После common setup регистрирует опциональную интеграцию вместе с сетью. |
| `common/ThermalVisionAccess.java` | Единственный поиск функциональной живой пары; старый метод getThermalHelmet оставлен совместимым делегатом. |
| `common/ServerThermalVisionStates.java` | Привязывает активность к реальному ItemStack, выполняет миграцию до проверки/расхода, отправляет публичную позу наблюдателям и StartTracking, очищает серверные кэши. Второго расхода нет. |
| `item/ThermalBatteryItem.java` | Батарейка использует единый поиск, сохраняя прежнюю серверную запись NBT и синхронизацию владельца. |
| `item/ThermalImagerAccessoryItem.java` | Переключает разрешённые способы экипировки без смены предмета, блокирует бонусы брони с Curios, подключает клиентскую ModelPart-модель и текстуры. |
| `compat/ThermalEquipment.java` | Common facade без Curios-сигнатур; проверяет mod presence прежде чем вызывать compat-классы. |
| `compat/CuriosEquipment.java` | ICurioItem через API registerCurio, поиск функционального head, выбор видимой пары, безопасная миграция, запрет броневого раздатчика и отсутствие атрибутов аксессуара. |
| `compat/client/CuriosGogglesRenderer.java` | Отдельный клиентский ICurioRenderer; копирует готовую голову HumanoidModel, соблюдает видимость/косметику и использует общую модель. |
| `network/ModNetworking.java` | Протокол 2, регистрация визуального S2C-пакета, отправка наблюдателям и конкретному игроку. |
| `network/GogglesVisualS2CPacket.java` | Только публичное состояние очков, UUID/entity ID и выбранная видимая пара. Клиентский обработчик изолирован DistExecutor. |
| `client/model/ThermalGogglesModel.java` | Прямой адаптированный экспорт 88 исходных кубов; неподвижное крепление и вращаемый hinge, пустая humanoid-обвязка без геометрии тела. |
| `client/model/ThermalGogglesMotion.java` | Время в секундах, исходный easing и непрерывный разворот без сброса текущей позиции/скорости. |
| `client/model/ClientGogglesVisuals.java` | Отдельное состояние на UUID/entity ID; серверные подтверждения, начальный снимок, очистка при удалении сущности, выходе и смене мира. |
| `client/model/ThermalGogglesClient.java` | Регистрация layer, ленивое запекание, выбор on/off, Forge armor extension и guarded регистрация Curios renderer. |
| `client/render/ThermalVisionResourceReloadListener.java` | Дополнительно сбрасывает кэш запечённой модели; существующие обработчики сохранены. |
| `docs/integration/goggles/MotionChecks.java` | Численная проверка длительностей, smoothstep, C1-разворота, разных FPS, быстрых переключений и паузы. |
| `.../gogglesaudit/GogglesChecks.java` | Dev-сценарии экипировки, серверного заряда/батарейки, снятия/замены, жизненного цикла, пакета и реального раздатчика. |
| `.../gogglesaudit/CuriosChecks.java` | Dev-проверки slot data, косметики, миграции/NBT, видимости, реального Curios menu shift-click и ручной вставки. Reflection только в тесте обнаружения меню. |
| `.../gogglesaudit/GogglesClientAudit.java` | Запускает изолированный настоящий клиент/мир, сетевые переключения, позы, reload/resize/fullscreen, измерение, смерть, reconnect, серверный тестовый RemotePlayer, стекло/невидимость/spectator и захват кадров. |
| `.../gogglesaudit/GogglesServerAudit.java` | Запускает проверки на выделенном сервере с FakePlayer и завершает изолированный процесс; в релиз не входит. |

Не-Java изменения: compileOnly API и opt-in runtime Curios в Gradle, необязательная
зависимость mods.toml, head slot/entity/tag JSON, неизменённые исходные PNG в ресурсах,
документированный motion manifest, две строки подсказки en/ru и документация/проверки.
В пользовательском `-PauditRuntime` изменена только привязка Gradle callback:
`tasks.named` → ленивый `tasks.matching.configureEach`, поскольку Forge создаёт
runClient позже и исходный callback завершался ошибкой до запуска. Логика самого
пользовательского аудита и его Java-файл не изменены.
