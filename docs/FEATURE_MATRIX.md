# RIMVISUALS requirement registry

**Обновлено:** ветка `integration/rimvisuals-complete` (основа — `security/installer-audit` 1cb2759). Пересмотрены только строки Fade, Combat (частицы, HUD) и Trails, написанные в PR №3; для них указан реальный модуль. **Все остальные строки — это аудит базового коммита 2142161 и устарели**: в частности блок Launcher (PR №2, №4–№8 добавили установку, аккаунты, профили, менеджер модов, Demo-запуск) требует отдельного пересмотра на этапе 2. Ничто не отмечено как проверенное в игровом мире, потому что такой проверки пока не было.

Baseline: 0.3.0-alpha.2, commit 2142161915d9e755dff396b5f12e450193492449. This registry describes the audit before rebuild fixes. Nothing is marked game-verified merely because a menu or build exists.

| Category | Requirement | Audit status | Evidence / acceptance gap |
|---|---|---|---|
| Fade & Explosions | Anchor Fade | Отсутствует | No implementation/render hook found in baseline. |
| Fade & Explosions | Crystal Fade | Требует проверки в Minecraft | Модуль crystal_fade: translucent-слой и tint alpha, непрозрачность 0.3–1, проявление при появлении. Fabric CI: сборка, JUnit и старт клиента с применёнными mixin прошли (коммит 327d604). Эффект в игровом мире НЕ проверен. Порядок отрисовки прозрачности рядом со стеклом не проверен. |
| Fade & Explosions | Anchor Scale | Отсутствует | No implementation/render hook found in baseline. |
| Fade & Explosions | Crystal Scale | Требует проверки в Minecraft | Модуль crystal_scale: масштаб матрицы рендера кристалла 0.4–1.6 и рост при появлении; хитбокс не меняется. Fabric CI: сборка, JUnit и старт клиента с применёнными mixin прошли (коммит 327d604). Эффект в игровом мире НЕ проверен. |
| Fade & Explosions | Totem Fade | Отсутствует | Floating item рисуется через ItemRenderState без параметра альфы; нужен отдельный путь отрисовки. Сжатие в Totem Scale — не fade. |
| Fade & Explosions | Totem Scale | Требует проверки в Minecraft | Модуль totem_scale: масштаб floating item 0.3–2 и сжатие в конце анимации. Fabric CI: сборка, JUnit и старт клиента с применёнными mixin прошли (коммит 327d604). Эффект в игровом мире НЕ проверен. |
| Fade & Explosions | Explosion Particles | Отсутствует | No implementation/render hook found in baseline. |
| Fade & Explosions | Explosion Color | Отсутствует | No implementation/render hook found in baseline. |
| Fade & Explosions | Explosion Glow | Отсутствует | No implementation/render hook found in baseline. |
| Fade & Explosions | Explosion Duration | Отсутствует | No implementation/render hook found in baseline. |
| Combat | Hit Color | Требует проверки в Minecraft | Модуль hit_color: перекраска строк hurt-вспышки в текстуре overlay (загрузка только при смене цвета). Влияет на все сущности, включая игрока. Fabric CI: сборка, JUnit и старт клиента с применёнными mixin прошли (коммит 327d604). Эффект в игровом мире НЕ проверен. |
| Combat | Hit Particles | Требует проверки в Minecraft | Модуль hit_particles: dust-частицы при ударе игрока по живой сущности (AttackEntityCallback), общий бюджет частиц, потолок Safe Mode 6. Fabric CI: сборка, JUnit и старт клиента с применёнными mixin прошли (коммит 327d604). Эффект в игровом мире НЕ проверен. |
| Combat | Critical Particles | Требует проверки в Minecraft | Модуль crit_particles: клиентская оценка условий крита (падение, не бег, кулдаун >0.9); настоящий крит определяет сервер. Fabric CI: сборка, JUnit и старт клиента с применёнными mixin прошли (коммит 327d604). Эффект в игровом мире НЕ проверен. |
| Combat | Sharpness Particles | Отсутствует | No implementation/render hook found in baseline. |
| Combat | Damage Particles | Требует проверки в Minecraft | Модуль damage_particles: частицы при падении синхронизированного здоровья сущности в радиусе; количество растёт с уроном. Fabric CI: сборка, JUnit и старт клиента с применёнными mixin прошли (коммит 327d604). Эффект в игровом мире НЕ проверен. |
| Combat | Damage Tint | Отсутствует | No implementation/render hook found in baseline. |
| Combat | Hurt Camera Control | Отсутствует | No implementation/render hook found in baseline. |
| Combat | Hit Marker | Отсутствует | No implementation/render hook found in baseline. |
| Combat | Kill Effects | Отсутствует | No implementation/render hook found in baseline. |
| Combat | Target Highlight | Отсутствует | No implementation/render hook found in baseline. |
| Combat | Target HUD | Требует проверки в Minecraft | Модуль target_hud: имя, полоса здоровья и дистанция цели под прицелом или последней атакованной; перетаскивается в HUD Editor. Fabric CI: сборка, JUnit и старт клиента с применёнными mixin прошли (коммит 327d604). Эффект в игровом мире НЕ проверен. |
| Combat | Armor HUD | Требует проверки в Minecraft | Модуль armor_hud: надетая броня, процент и полоса прочности, вертикальный/горизонтальный вариант. Fabric CI: сборка, JUnit и старт клиента с применёнными mixin прошли (коммит 327d604). Эффект в игровом мире НЕ проверен. |
| Combat | Armor Durability | Требует проверки в Minecraft | Входит в armor_hud (процент и полоса). Отдельного модуля нет. Fabric CI: сборка, JUnit и старт клиента с применёнными mixin прошли (коммит 327d604). Эффект в игровом мире НЕ проверен. |
| Combat | Enchantment Glint Control | Отсутствует | No implementation/render hook found in baseline. |
| Combat | Custom Crosshair | Отсутствует | No implementation/render hook found in baseline. |
| Combat | Attack Indicator | Отсутствует | No implementation/render hook found in baseline. |
| Trails | Sword Trails | Требует проверки в Minecraft | Модуль sword_trails: dust-частицы по приблизительной дуге кончика меча (точная позиция кончика клиенту неизвестна). Fabric CI: сборка, JUnit и старт клиента с применёнными mixin прошли (коммит 327d604). Эффект в игровом мире НЕ проверен. |
| Trails | Weapon Trails | Отсутствует | No implementation/render hook found in baseline. |
| Trails | Pearl Trails | Требует проверки в Minecraft | Модуль pearl_trails: частицы вдоль пути летящей жемчужины. Fabric CI: сборка, JUnit и старт клиента с применёнными mixin прошли (коммит 327d604). Эффект в игровом мире НЕ проверен. |
| Trails | Arrow Trails | Частично реализовано | Стрелы покрыты projectile_trails; отдельной настройки нет. |
| Trails | Potion Trails | Частично реализовано | Брошенные зелья — ProjectileEntity и покрыты projectile_trails; отдельной настройки нет; не проверено. |
| Trails | Projectile Trails | Требует проверки в Minecraft | Модуль projectile_trails: частицы за снарядами (стрелы, трезубцы, брошенные предметы). Fabric CI: сборка, JUnit и старт клиента с применёнными mixin прошли (коммит 327d604). Эффект в игровом мире НЕ проверен. |
| Trails | Player Tracers | Отсутствует | No implementation/render hook found in baseline. |
| Trails | Projectile Tracers | Отсутствует | No implementation/render hook found in baseline. |
| Trails | Trail Glow | Отсутствует | No implementation/render hook found in baseline. |
| Trails | Trail Length | Отсутствует | No implementation/render hook found in baseline. |
| Trails | Trail Width | Отсутствует | No implementation/render hook found in baseline. |
| Trails | Trail Color | Отсутствует | No implementation/render hook found in baseline. |
| Trails | Trail Fade | Отсутствует | No implementation/render hook found in baseline. |
| World | Custom Sky | Отсутствует | No implementation/render hook found in baseline. |
| World | Sky Fade | Отсутствует | No implementation/render hook found in baseline. |
| World | Custom Fog | Отсутствует | No implementation/render hook found in baseline. |
| World | Fog Color | Отсутствует | No implementation/render hook found in baseline. |
| World | Fog Density | Отсутствует | No implementation/render hook found in baseline. |
| World | Weather Color | Отсутствует | No implementation/render hook found in baseline. |
| World | Rain Opacity | Отсутствует | No implementation/render hook found in baseline. |
| World | Snow Opacity | Отсутствует | No implementation/render hook found in baseline. |
| World | Time Color Grading | Отсутствует | No implementation/render hook found in baseline. |
| World | Biome Colors | Отсутствует | No implementation/render hook found in baseline. |
| World | Grass Color | Отсутствует | No implementation/render hook found in baseline. |
| World | Foliage Color | Отсутствует | No implementation/render hook found in baseline. |
| World | Water Color | Отсутствует | No implementation/render hook found in baseline. |
| World | Water Transparency | Отсутствует | No implementation/render hook found in baseline. |
| World | Lava Color | Отсутствует | No implementation/render hook found in baseline. |
| World | Night Vision Fade | Отсутствует | No implementation/render hook found in baseline. |
| World | Fullbright | Отсутствует | No implementation/render hook found in baseline. |
| World | Block Highlight | Отсутствует | No implementation/render hook found in baseline. |
| World | Entity Outline | Отсутствует | No implementation/render hook found in baseline. |
| World | Custom Clouds | Отсутствует | No implementation/render hook found in baseline. |
| World | Particle Density | Отсутствует | No implementation/render hook found in baseline. |
| World | World Saturation | Отсутствует | No implementation/render hook found in baseline. |
| World | World Contrast | Отсутствует | No implementation/render hook found in baseline. |
| World | World Brightness | Отсутствует | No implementation/render hook found in baseline. |
| World | Enchanted Golden Apple Screen Glow | Отсутствует | No implementation/render hook found in baseline. |
| HUD | FPS | Частично реализовано | Existing source/UI; no recorded per-feature in-world acceptance or performance measurement. |
| HUD | Ping | Частично реализовано | Existing source/UI; no recorded per-feature in-world acceptance or performance measurement. |
| HUD | Coordinates | Частично реализовано | Existing source/UI; no recorded per-feature in-world acceptance or performance measurement. |
| HUD | CPS | Отсутствует | No implementation/render hook found in baseline. |
| HUD | Keystrokes | Отсутствует | No implementation/render hook found in baseline. |
| HUD | Armor Status | Отсутствует | No implementation/render hook found in baseline. |
| HUD | Potion HUD | Отсутствует | No implementation/render hook found in baseline. |
| HUD | Inventory HUD | Отсутствует | No implementation/render hook found in baseline. |
| HUD | BPS / Speed | Отсутствует | No implementation/render hook found in baseline. |
| HUD | Server Info | Отсутствует | No implementation/render hook found in baseline. |
| HUD | Session Info | Отсутствует | No implementation/render hook found in baseline. |
| HUD | Watermark | Частично реализовано | Existing source/UI; no recorded per-feature in-world acceptance or performance measurement. |
| HUD | Custom Hotbar | Отсутствует | No implementation/render hook found in baseline. |
| HUD | Scoreboard Fade | Отсутствует | No implementation/render hook found in baseline. |
| HUD | Chat Fade | Отсутствует | No implementation/render hook found in baseline. |
| HUD | Bossbar Customizer | Отсутствует | No implementation/render hook found in baseline. |
| HUD | Notifications | Отсутствует | No implementation/render hook found in baseline. |
| HUD | Drag & Drop HUD Editor | Частично реализовано | Existing source/UI; no recorded per-feature in-world acceptance or performance measurement. |
| Optimization | Performance Safe Mode | Требует проверки в Minecraft | Source exists; startup smoke is not in-world verification. |
| Optimization | Particle Limits | Требует проверки в Minecraft | Source exists; startup smoke is not in-world verification. |
| Optimization | Adaptive Particles | Требует проверки в Minecraft | Source exists; startup smoke is not in-world verification. |
| Optimization | Distance Culling | Отсутствует | No own 3D effect pipeline or resource-lifecycle implementation; 2D dot bounds check is not world/frustum culling. |
| Optimization | Frustum Culling | Отсутствует | No own 3D effect pipeline or resource-lifecycle implementation; 2D dot bounds check is not world/frustum culling. |
| Optimization | Render Batching | Отсутствует | No own 3D effect pipeline or resource-lifecycle implementation; 2D dot bounds check is not world/frustum culling. |
| Optimization | GPU Resource Cleanup | Отсутствует | No own 3D effect pipeline or resource-lifecycle implementation; 2D dot bounds check is not world/frustum culling. |
| Optimization | Texture Caching | Отсутствует | No own 3D effect pipeline or resource-lifecycle implementation; 2D dot bounds check is not world/frustum culling. |
| Optimization | Font Caching | Отсутствует | No own 3D effect pipeline or resource-lifecycle implementation; 2D dot bounds check is not world/frustum culling. |
| Optimization | Lazy Initialization | Отсутствует | No own 3D effect pipeline or resource-lifecycle implementation; 2D dot bounds check is not world/frustum culling. |
| Optimization | Reduced Motion | Требует проверки в Minecraft | Source exists; startup smoke is not in-world verification. |
| Optimization | Low-End Preset | Требует проверки в Minecraft | Source exists; startup smoke is not in-world verification. |
| Optimization | Balanced Preset | Требует проверки в Minecraft | Source exists; startup smoke is not in-world verification. |
| Optimization | Ultra Preset | Требует проверки в Minecraft | Source exists; startup smoke is not in-world verification. |
| Optimization | Performance Monitor | Требует проверки в Minecraft | Source exists; startup smoke is not in-world verification. |
| Optimization | Per-module render timings | Требует проверки в Minecraft | Source exists; startup smoke is not in-world verification. |
| Hands | Swing Animations | Реализовано, но не работает по отчёту пользователя | Existing HeldItemRenderer mixin; reproduce in-world, inspect submission matrices, then user retest. |
| Hands | Viewmodel | Реализовано, но не работает по отчёту пользователя | Existing HeldItemRenderer mixin; reproduce in-world, inspect submission matrices, then user retest. |
| Hands | Hand Lighting | Требует проверки в Minecraft | Source exists; startup smoke is not in-world verification. |
| Hands | Hand Glow | Отсутствует | No implementation/render hook found in baseline. |
| Hands | Hand Bloom | Отсутствует | No implementation/render hook found in baseline. |
| Hands | Hand Outline | Отсутствует | No implementation/render hook found in baseline. |
| Hands | Hand Color | Отсутствует | No implementation/render hook found in baseline. |
| Hands | Hand Gradient | Отсутствует | No implementation/render hook found in baseline. |
| Hands | Hand Transparency | Отсутствует | No implementation/render hook found in baseline. |
| Hands | Hand Rainbow | Отсутствует | No implementation/render hook found in baseline. |
| Hands | Hand Blur | Отсутствует | No implementation/render hook found in baseline. |
| Interface | RGB / HEX | Частично реализовано | Existing source/UI; no recorded per-feature in-world acceptance or performance measurement. |
| Interface | HSV | Отсутствует | No implementation/render hook found in baseline. |
| Interface | Gradient Themes | Отсутствует | No implementation/render hook found in baseline. |
| Interface | Panel Opacity | Отсутствует | No implementation/render hook found in baseline. |
| Interface | Background Editor | Отсутствует | No implementation/render hook found in baseline. |
| Interface | Blur | Отсутствует | No implementation/render hook found in baseline. |
| Interface | Animations | Частично реализовано | Existing source/UI; no recorded per-feature in-world acceptance or performance measurement. |
| Interface | Panel Size | Отсутствует | No implementation/render hook found in baseline. |
| Interface | Font Selection | Отсутствует | No implementation/render hook found in baseline. |
| Interface | Preview | Отсутствует | No implementation/render hook found in baseline. |
| Interface | Search | Частично реализовано | Existing source/UI; no recorded per-feature in-world acceptance or performance measurement. |
| Interface | Favourites | Отсутствует | No implementation/render hook found in baseline. |
| Interface | Module Reset | Отсутствует | No implementation/render hook found in baseline. |
| Launcher | Windows EXE | Реализовано, проверено в CI | NSIS EXE собирается и тихо устанавливается в изолированную папку CI, упакованное приложение открывает реальное окно; хеши JAR и app.asar проверены. Не подписан. Пользователь подтвердил установку и запуск на своём ПК. |
| Launcher | Independent Directory | Реализовано, проверено в CI | Игровой корень APPDATA/RIMVISUALS, профили в instances/. UI-смоук через реальный Electron IPC: корень профиля внутри instances, чужие saves сохраняются. Существующая .minecraft не используется. |
| Launcher | Minecraft Installation | Реализовано, проверено в CI | tests/install-smoke.js в CI (Actions 41308876501): реальная установка 1.21.11, 1.21.10, 1.21.4 в временную папку с проверкой хешей. Игра для 1.21.10/1.21.4 не запускалась. |
| Launcher | Fabric Installation | Реализовано, проверено в CI | Fabric Loader 0.19.3 ставится из метаданных Fabric с проверкой профиля; установка проверена в CI (Actions 41308876501) для 1.21.11. |
| Launcher | Library Installation | Реализовано, проверено в CI | Библиотеки/ассеты/natives с проверкой SHA-1 и лимитами потока; проверено в CI (Actions 41308876501). |
| Launcher | Microsoft OAuth | Частично реализовано | PKCE S256 через системный браузер, loopback, проверка state; проверено тестами с имитацией сервера. Реальный вход не проверен: нужен публичный Client ID владельца (в UI ошибка «Client ID» вместо входа). Чужие Client ID не используются. |
| Launcher | License Check | Требует проверки в Minecraft | Код проверки владения Minecraft есть, но без Client ID он не выполнялся с настоящим аккаунтом. Мультиплеер готовым не считается. |
| Launcher | Encrypted Token Storage | Реализовано, проверено в CI | Refresh-токен хранится через Electron safeStorage; нативный тест шифрования на Windows в CI (Actions 41308876501). |
| Launcher | Direct Java Launch | Частично реализовано | Прямой запуск Java подтверждён пользователем на его ПК (Demo World, 1.21.11 + Fabric + мод). В CI проверяется план запуска и аргументы, мир в CI не открывался. Лицензионный запуск не проверен. |
| Launcher | RAM / Java Choice | Частично реализовано | RAM 2–32 ГБ, выбор Java, разрешение и fullscreen хранятся в профиле и передаются в аргументы; тесты workspaces/launch-plan. Обработчик chooseJava (системный диалог) автотестом не покрыт. |
| Launcher | Bundled Visuals Updates | Реализовано, проверено в CI | Встроенный JAR устанавливается по манифесту с SHA-256; изменённые пользователем файлы не затираются, старые собственные копии сохраняются в backup. tests/components-smoke.js в CI (Actions 41308876501). |
| Launcher | Integrity Check | Частично реализовано | Диагностика (logs/diagnostics.json) отмечает компоненты как verified/modified/missing без токенов и путей. Автоматического «восстановить всё одной кнопкой» нет: повторная установка пересоздаёт отсутствующие файлы при PLAY/Prepare. |
| Launcher | Launch Logs | Реализовано, проверено в CI | Каждый запуск пишет logs/launch-*.log, токен маскируется ([REDACTED]). |
| Launcher | Bidirectional Sync | Отсутствует | Только лаунчер → мод: launcher.json содержит 4 переключателя (Pulse Glow, Particles, Watermark, Info HUD), акцент и масштаб HUD. Новые модули PR №3/№9 и обратная передача из мода в лаунчер не поддерживаются. |
| Launcher | Profile Import | Реализовано, проверено в CI | Импорт JSON-профиля как нового профиля с валидацией; без путей, токенов и аккаунтов. Проверено UI-смоуком через реальный IPC. |
| Launcher | Profile Export | Реализовано, проверено в CI | Экспорт профиля в JSON без accountId, Client ID и пути Java; проверено UI-смоуком. |
| Launcher | Backups | Частично реализовано | Резервируются только собственные компоненты (JAR) при обновлении. Резервных копий пользовательских настроек мода (config.properties/профили CFG) нет. |
| Launcher | Restore | Отсутствует | Восстановления настроек из резервной копии нет. Восстановление файлов — только переустановка недостающих компонентов. |
| Launcher | Version Selection | Частично реализовано | Minecraft 1.21.11 / 1.21.10 / 1.21.4 × Vanilla / Fabric в профиле; встроенные визуалы только для Fabric 1.21.11 (проверка совместимости). Создание vanilla 1.21.4 проверено UI-смоуком; установка файлов — install-smoke. |
| Launcher | Mod Manager | Частично реализовано | electron/mod-manager.cjs: список, включение/выключение (.disabled), импорт JAR; чужие моды не трогаются. Есть тест модуля; IPC-обработчики toggleMod/importMod отдельным UI-тестом не покрыты. |
| Launcher | Multiple Accounts | Реализовано, проверено в CI | Локальные записи и Microsoft-аккаунт, выбор/удаление; UI-смоук. Microsoft-вход не блокирует управление лаунчером и Demo. |
| Launcher | Local Demo Launch | Частично реализовано | Штатный Minecraft Demo без Microsoft-сессии, мультиплеер отключён. Пользователь запускал Demo World на своём ПК. Не лицензионный режим. |
| Launcher | Java Auto-install | Реализовано, проверено в CI | electron/java-runtime.cjs: Temurin JRE 21 x64, SHA-256, проверка redirect-хостов; tests/java-runtime-smoke.js скачивает и запускает Java в CI. |
| Helpers | FT Helper | Требует уточнения | User will provide exact functions; no empty menu entry. |
| Helpers | HV Helper | Требует уточнения | User will provide exact functions; no empty menu entry. |
