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
| Combat | Sharpness Particles | Требует проверки в Minecraft | Модуль sharpness_particles: частицы при ударе предметом с Sharpness, количество растёт с уровнем (уровень читается из предмета в руке; при недоступном реестре эффект пропускается). Fabric CI на ветке feature/rimvisuals-mod-0.4: Gradle, JUnit и старт клиента прошли (запуск 41331926524). В игровом мире НЕ проверено. |
| Combat | Damage Particles | Требует проверки в Minecraft | Модуль damage_particles: частицы при падении синхронизированного здоровья сущности в радиусе; количество растёт с уроном. Fabric CI: сборка, JUnit и старт клиента с применёнными mixin прошли (коммит 327d604). Эффект в игровом мире НЕ проверен. |
| Combat | Damage Tint | Требует проверки в Minecraft | Модуль damage_tint: красная виньетка по краям, пока идёт vanilla-таймер hurtTime. Fabric CI: сборка, JUnit, старт клиента с применёнными mixin прошли (коммиты 62db4ea, 857a419). Эффект в игровом мире НЕ проверен. |
| Combat | Hurt Camera Control | Требует проверки в Minecraft | Модуль hurt_camera: ModifyConstant 14.0 в GameRenderer.tiltViewWhenHurt (0–150%); анимация смерти не меняется. Mixin сверен с исходниками 1.21.11. Fabric CI: сборка, JUnit, старт клиента с применёнными mixin прошли (коммиты 62db4ea, 857a419). Эффект в игровом мире НЕ проверен. |
| Combat | Hit Marker | Требует проверки в Minecraft | Модуль hit_marker: 4 диагональные метки у прицела, затухание, длительность/длина/зазор. 2D-оверлей. Fabric CI: сборка, JUnit, старт клиента с применёнными mixin прошли (коммиты 62db4ea, 857a419). Эффект в игровом мире НЕ проверен. |
| Combat | Kill Effects | Требует проверки в Minecraft | Модуль kill_effects: всплеск частиц, когда цель, по которой вы ударили (≤6 с назад), умирает (клиентская оценка по здоровью). Fabric CI: сборка, JUnit, старт клиента с применёнными mixin прошли (коммиты 62db4ea, 857a419). Эффект в игровом мире НЕ проверен. |
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
| World | Custom Sky | Требует проверки в Minecraft | Модуль custom_sky: подмена skyColor в SkyRendering.updateRenderState для Overworld-подобного купола; End/Nether, солнце, луна и звёзды не меняются. Fabric CI: сборка, JUnit, старт клиента с применёнными mixin прошли (коммиты 62db4ea, 857a419). Эффект в игровом мире НЕ проверен. |
| World | Sky Fade | Требует проверки в Minecraft | Параметр «Sky fade (blend)» в custom_sky: смешивание ванильного и своего цвета неба. Fabric CI: сборка, JUnit, старт клиента с применёнными mixin прошли (коммиты 62db4ea, 857a419). Эффект в игровом мире НЕ проверен. |
| World | Custom Fog | Требует проверки в Minecraft | Модуль custom_fog: цвет и дистанции тумана через ModifyArgs в FogRenderer.applyFog. Fabric CI: сборка, JUnit, старт клиента с применёнными mixin прошли (коммиты 62db4ea, 857a419). Эффект в игровом мире НЕ проверен. |
| World | Fog Color | Требует проверки в Minecraft | Цвет и сила в custom_fog; тот же объект цвета используется для очистки кадра. Fabric CI: сборка, JUnit, старт клиента с применёнными mixin прошли (коммиты 62db4ea, 857a419). Эффект в игровом мире НЕ проверен. |
| World | Fog Density | Требует проверки в Minecraft | Параметр «Fog distance (%)» 20–200 в custom_fog (масштаб дистанций тумана). Это дистанция, а не плотность в шейдере. Fabric CI: сборка, JUnit, старт клиента с применёнными mixin прошли (коммиты 62db4ea, 857a419). Эффект в игровом мире НЕ проверен. |
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
| HUD | CPS | Требует проверки в Minecraft | Модуль cps_hud: нажатия attack/use, считаемые по кадрам; при очень быстрых кликах выше FPS недосчитывает. Счётчик покрыт JUnit. Fabric CI: сборка, JUnit, старт клиента с применёнными mixin прошли (коммиты 62db4ea, 857a419). Эффект в игровом мире НЕ проверен. |
| HUD | Keystrokes | Требует проверки в Minecraft | Модуль keystrokes_hud: WASD, прыжок, ЛКМ/ПКМ по вашим привязкам клавиш. Fabric CI: сборка, JUnit, старт клиента с применёнными mixin прошли (коммиты 62db4ea, 857a419). Эффект в игровом мире НЕ проверен. |
| HUD | Armor Status | Отсутствует | No implementation/render hook found in baseline. |
| HUD | Potion HUD | Требует проверки в Minecraft | Модуль potion_hud: эффекты игрока с уровнем и временем, подсветка последних 10 с. Fabric CI: сборка, JUnit, старт клиента с применёнными mixin прошли (коммиты 62db4ea, 857a419). Эффект в игровом мире НЕ проверен. |
| HUD | Inventory HUD | Требует проверки в Minecraft | Модуль inventory_hud: сетка основного инвентаря 9×1–3, количество в стаке. Читает только ваш инвентарь. Fabric CI на ветке feature/rimvisuals-mod-0.4: Gradle, JUnit и старт клиента прошли (запуск 41331926524). В игровом мире НЕ проверено. |
| HUD | BPS / Speed | Требует проверки в Minecraft | Модуль speed_hud: горизонтальная скорость по изменению позиции (блоков/с); формула покрыта JUnit. Fabric CI: сборка, JUnit, старт клиента с применёнными mixin прошли (коммиты 62db4ea, 857a419). Эффект в игровом мире НЕ проверен. |
| HUD | Server Info | Отсутствует | No implementation/render hook found in baseline. |
| HUD | Session Info | Требует проверки в Minecraft | Модуль session_hud: время в мире и адрес сервера. Fabric CI: сборка, JUnit, старт клиента с применёнными mixin прошли (коммиты 62db4ea, 857a419). Эффект в игровом мире НЕ проверен. |
| HUD | Watermark | Частично реализовано | Existing source/UI; no recorded per-feature in-world acceptance or performance measurement. |
| HUD | Custom Hotbar | Отсутствует | No implementation/render hook found in baseline. |
| HUD | Scoreboard Fade | Отсутствует | No implementation/render hook found in baseline. |
| HUD | Chat Fade | Отсутствует | No implementation/render hook found in baseline. |
| HUD | Bossbar Customizer | Отсутствует | No implementation/render hook found in baseline. |
| HUD | Notifications | Отсутствует | No implementation/render hook found in baseline. |
| HUD | Drag & Drop HUD Editor | Требует проверки в Minecraft | Right Shift → Edit HUD: перетаскивание, масштаб каждого элемента колесом мыши (0.5–2), правый клик сбрасывает элемент, привязка к сетке (настройка «HUD grid snap», сетка рисуется), общий Reset. Позиции и масштаб сохраняются в config. Математика snap/scale покрыта JUnit. Fabric CI на ветке feature/rimvisuals-mod-0.4: Gradle, JUnit и старт клиента прошли (запуск 41331926524). В игровом мире НЕ проверено. |
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
| Optimization | Performance Monitor | Требует проверки в Minecraft | Теперь выключен по умолчанию (для уже существующих config остаётся прежнее значение) и показывает 5 самых тяжёлых модулей вместо полного списка. Замеряет CPU-время отправки, не GPU и не FPS. |
| Optimization | Per-module render timings | Требует проверки в Minecraft | Source exists; startup smoke is not in-world verification. |
| Hands | Swing Animations | Реализовано, но не работает по отчёту пользователя | Existing HeldItemRenderer mixin; reproduce in-world, inspect submission matrices, then user retest. |
| Hands | Viewmodel | Реализовано, но не работает по отчёту пользователя | Existing HeldItemRenderer mixin; reproduce in-world, inspect submission matrices, then user retest. |
| Hands | Hand Lighting | Требует проверки в Minecraft | Source exists; startup smoke is not in-world verification. |
| Hands | Hand Glow | Отсутствует | No implementation/render hook found in baseline. |
| Hands | Hand Bloom | Отсутствует | No implementation/render hook found in baseline. |
| Hands | Hand Outline | Отсутствует | No implementation/render hook found in baseline. |
| Hands | Hand Color | Требует проверки в Minecraft | Модуль hand_color: тонирование ТОЛЬКО модели руки (голая рука и рука с предметом) через перехват submitModelPart, раздельно для основной и второй руки. Предметы не окрашиваются. Fabric CI: сборка, JUnit, старт клиента с применёнными mixin прошли (коммиты 62db4ea, 857a419). Эффект в игровом мире НЕ проверен. |
| Hands | Hand Gradient | Требует проверки в Minecraft | Режим Gradient в hand_color (анимированный переход между двумя цветами). Математика покрыта JUnit. Fabric CI: сборка, JUnit, старт клиента с применёнными mixin прошли (коммиты 62db4ea, 857a419). Эффект в игровом мире НЕ проверен. |
| Hands | Hand Transparency | Требует проверки в Minecraft | Прозрачность руки (отдельно для каждой руки) в hand_color через альфу tint в translucent-слое. Реальный вид прозрачности не проверен. Fabric CI: сборка, JUnit, старт клиента с применёнными mixin прошли (коммиты 62db4ea, 857a419). Эффект в игровом мире НЕ проверен. |
| Hands | Hand Rainbow | Требует проверки в Minecraft | Режим Rainbow в hand_color. Fabric CI: сборка, JUnit, старт клиента с применёнными mixin прошли (коммиты 62db4ea, 857a419). Эффект в игровом мире НЕ проверен. |
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

## Update 0.4.0 (commit after 862f33d): HUD Editor, CFG Manager, World Tweaks, Hand Glow

CI evidence: Gradle build + JUnit + Minecraft startup smoke test green (mixin targets resolve against 1.21.11). **None of these has been verified visually in a world** — see manual scenarios.

| Feature | Status | How it works | Manual check |
|---|---|---|---|
| HUD per-element opacity | Требует проверки в Minecraft | `Gfx.hudAlpha` multiplies panel/text/fill alpha; item icons (Armor/Inventory HUD) stay opaque | Shift+wheel on an element in HUD Editor |
| HUD guides / accent / monitor toggle | Требует проверки в Minecraft | `HudMath.guideSnap` (tested) | Drag near centre/other element |
| CFG Manager | Реализовано, тесты ConfigStore | profiles, JSON import/export, backups, autosave, server binding, presets | Profiles tab |
| Fullbright | Требует проверки в Minecraft | `LightmapTextureManager.update` gamma arg | Dark cave, toggle |
| Night Vision Fade | Требует проверки в Minecraft | lightmap night-vision arg, linear last 10 s | Short potion |
| Golden Apple Glow | Требует проверки в Minecraft | HUD edge overlay while absorption > 0 | Eat golden apple |
| Entity Outline | Требует проверки в Minecraft | `MinecraftClient.hasOutline` + outline colour redirect (vanilla glow pass) | Other player / mob behind wall |
| Water / Lava / Biome colours | Требует проверки в Minecraft | `BiomeColors` + `FluidRenderer` lava constant; terrain reload after settings settle | Ocean, lava lake, grass |
| Weather Color | Требует проверки в Minecraft | `WeatherRendering.renderPieces` colour/alpha | Rain/snow |
| Hand Glow | Требует проверки в Minecraft | 1–4 enlarged translucent arm copies, fully lit; slight offset from arm pivot possible | First-person with empty hand |
| Hand Outline / Bloom / Blur | **Не реализовано** | need front-face-culled hull pass / post-processing framebuffers; no honest implementation without in-game verification | — |

## Update 0.4.0 (second batch): Anchor Fade/Scale, Totem Fade, Explosion Effects, Target Highlight, Cooldowns, Notifications

CI: Gradle build + JUnit + Minecraft startup smoke test (mixin targets resolved against real 1.21.11 sources). **Not verified visually in a world.**

| Feature | Status | How it works / limits | Manual check |
|---|---|---|---|
| Anchor Fade / Scale | Требует проверки в Minecraft | Client-only ghost of the anchor model (quads drawn through `translucentMovingBlock`), created **only** when an explosion packet with radius exactly 5 and centre exactly at an anchor block's centre arrives and the anchor was seen in the client world (before or just after the block update). Mining/other removal never creates a ghost. Duration 200–500 ms, opacity, scale, tint, easing configurable. No world/hitbox/server changes. | Charge an anchor in the Overworld, click it with a block/glowstone-less hand, watch the spot |
| Totem Fade | Требует проверки в Minecraft | Floating totem quads are re-submitted through the items-atlas translucent layer with alpha (queue proxy matched by call shape). Works with Totem Scale. | Pop a totem |
| Explosion Effects | Требует проверки в Minecraft | Extra dust ring + sparks on every explosion packet within range; uses shared particle budget; Safe Mode suppresses | Crystal / TNT |
| Target Highlight | Требует проверки в Minecraft | Vanilla glow-outline pass for crosshair target and last-hit target | Aim at a mob/player |
| Cooldowns HUD | Требует проверки в Minecraft | `ItemCooldownManager.getCooldownProgress` for stacks in inventory | Throw an ender pearl |
| Notifications HUD | Требует проверки в Minecraft | Totem used (setFloatingItem hook), low health, low armor durability with hysteresis | Take damage / pop totem |
| Enchantment Glint Control, Scoreboard/Chat Fade, Hotbar, ESP, HW/FT Helper | **Отсутствует** | not started | — |
| Hand Outline / Bloom / Blur | **Не реализовано** | Outline needs an inverted-hull pass with front-face culling around the arm pivot; Bloom/Blur need post-processing framebuffers. Neither can be made honest without in-game verification, so no toggle exists. Hand Glow is the implemented alternative. | — |
