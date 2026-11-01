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
| Launcher | Windows EXE | Реализовано и проверено — установка/открытие | User confirmed 0.1.5 starts; baseline opens another launcher, not standalone game launch. |
| Launcher | Independent Directory | Отсутствует | No implementation/render hook found in baseline. |
| Launcher | Minecraft Installation | Отсутствует | No implementation/render hook found in baseline. |
| Launcher | Fabric Installation | Отсутствует | No implementation/render hook found in baseline. |
| Launcher | Library Installation | Отсутствует | No implementation/render hook found in baseline. |
| Launcher | Microsoft OAuth | Отсутствует | No implementation/render hook found in baseline. |
| Launcher | License Check | Отсутствует | No implementation/render hook found in baseline. |
| Launcher | Encrypted Token Storage | Отсутствует | No implementation/render hook found in baseline. |
| Launcher | Direct Java Launch | Отсутствует | No implementation/render hook found in baseline. |
| Launcher | RAM / Java Choice | Частично реализовано | Existing source/UI; no recorded per-feature in-world acceptance or performance measurement. |
| Launcher | Bundled Visuals Updates | Отсутствует | No implementation/render hook found in baseline. |
| Launcher | Integrity Check | Отсутствует | No implementation/render hook found in baseline. |
| Launcher | Launch Logs | Отсутствует | No implementation/render hook found in baseline. |
| Launcher | Bidirectional Sync | Отсутствует | No implementation/render hook found in baseline. |
| Launcher | Profile Import | Отсутствует | No implementation/render hook found in baseline. |
| Launcher | Profile Export | Отсутствует | No implementation/render hook found in baseline. |
| Launcher | Backups | Отсутствует | No implementation/render hook found in baseline. |
| Launcher | Restore | Отсутствует | No implementation/render hook found in baseline. |
| Helpers | FT Helper | Требует уточнения | User will provide exact functions; no empty menu entry. |
| Helpers | HV Helper | Требует уточнения | User will provide exact functions; no empty menu entry. |
