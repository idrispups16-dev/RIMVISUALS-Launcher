# RIMVISUALS 2.0 — аудит модулей (честная версия)

**Статус референсов: НЕ ПРОВЕРЕНЫ.** В этой среде нельзя воспроизвести YouTube (ни сами ролики, ни кадры), `pulsevisuals.pro` недоступен для чтения, поиск по названиям PulseVisuals/FadingVisual возвращает только посторонние моды (EclipseVisuals, ZeroVisuals, Visual+ и т. п.), а не эти клиенты. Поэтому по правилу ТЗ («не утверждай наличие функции у клиента без подтверждения», «не удаляй вслепую») для каждого модуля решение — **UNVERIFIED / KEEP TEMPORARILY** (не удалять, референс не подтверждён; финальное KEEP/REWORK/REMOVE — после сверки), а колонки PulseVisuals/FadingVisual — **UNVERIFIED**.

## Что нужно от владельца, чтобы продолжить

Записи экрана (или ссылки с таймкодами, которые вы сами видели) именно по этим пунктам: Target ESP (какие стили), Jump Circle, Trails, Particles (форма/движение), Crystal Fade, открытие ClickGUI + смена вкладок, HUD. Достаточно 10–20 сек на пункт, лучше 60 fps. Без этого переделка «до уровня референса» была бы выдумкой, а ТЗ это запрещает.

## Пробелы относительно списка из ТЗ (по коду)

- **Target ESP** — отдельного модуля нет (есть *Target Highlight*).
- **Jump Circle** — отсутствует.
- **Custom Hand / Viewmodel** — есть (*Viewmodel Editor*, *Hand Color/Glow/Lighting*).
- **China Hat, Block Overlay** — проверить наличие в `Modules` (в сводке ниже не найдены автоматически).

## Таблица (из `mod/src/main/java`, 47 модулей с категорией)

| Category | Id | Module | RIMVISUALS | PulseVisuals | FadingVisual | Source | Decision |
|---|---|---|---|---|---|---|---|
| INTERFACE | `interface` | Interface | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| OPTIMIZATION | `performance` | Performance | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| SHADER_HANDS | `hand_color` | Hand Color | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| SHADER_HANDS | `hand_glow` | Hand Glow | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| SHADER_HANDS | `hand_lighting` | Hand Lighting | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| SWING | `swing` | Swing Animations | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| UTILITIES | `shulker_preview` | Shulker Preview | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| UTILITIES | `sound_controller` | Sound Controller | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| UTILITIES | `zoom` | Zoom | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VIEWMODEL | `viewmodel` | Viewmodel Editor | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `anchor_fade` | Anchor Fade / Scale | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `aspect_ratio` | Aspect Ratio | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `atmosphere` | Atmosphere | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `biome_colors` | Biome Colors | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `crit_particles` | Critical Particles | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `crosshair` | Crosshair | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `crystal_fade` | Crystal Fade | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `crystal_scale` | Crystal Scale | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `custom_fog` | Custom Fog | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `custom_sky` | Custom Sky | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `damage_particles` | Damage Particles | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `damage_tint` | Damage Tint | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `entity_outline` | Entity Outline | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `explosion_effects` | Explosion Effects | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `friends_options` | Friends & Markers | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `fullbright` | Fullbright | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `golden_apple_glow` | Golden Apple Glow | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `hit_color` | Hit Color | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `hit_marker` | Hit Marker | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `hit_particles` | Hit Particles | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `hurt_camera` | Hurt Camera | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `item_physics` | Item Physics (visual) | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `kill_effects` | Kill Effects | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `lava_color` | Lava Color | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `night_vision_fade` | Night Vision Fade | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `particles` | Particle Effects | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `pearl_trails` | Pearl Trails | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `projectile_trails` | Projectile Trails | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `pulse_glow` | Pulse Glow | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `sharpness_particles` | Sharpness Particles | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `sword_trails` | Sword Trails | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `target_highlight` | Target Highlight | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `totem_fade` | Totem Fade | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `totem_scale` | Totem Scale | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `visual_hitbox` | Visual Hitbox | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `water_color` | Water Color | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |
| VISUALS | `weather_color` | Weather Color | есть | UNVERIFIED | UNVERIFIED | — | UNVERIFIED / KEEP TEMPORARILY |

Удалено модулей: **0**. Инфраструктура (Launcher, CFG, аккаунты, Keybind Manager, IPC, JARVIS) не затрагивается.
