# RIMVISUALS 0.4.12-hw-api.1 — функции для HolyWorld

Всего 223 идентификатора: 81 модуль, логические подфункции и служебные разделы. Все отправляются в checkFeatures, включая выключенные.

Если запрещена подфункция, консервативно отключается и скрывается весь родительский модуль. Конфигурация пользователя не стирается. Связанные HUD-панели также скрываются. Друзья и метки отключаются совместно. При любых блокировках скрываются профили и монитор производительности, чтобы не показывать сохранённые названия функций.

Entity Outline и Target Highlight рисуют свечение сквозь стены — это явно раскрывается для решения администрации. FT Helper включает ручной Item Swap; он не является HP-автоматикой и не предназначен для HolyWorld. HW Helper управляет отдельно установленными Auction Price и Zako Health Indicator; эти сторонние JAR в пакет не включены.

| ID | Название | Краткое описание из текущего кода |
|---|---|---|
| friends_options | Friends & Markers | Mutual account friends, shared pings (N), local .way name X Y Z. Match the room on multi-server networks. |
| friends_options.share_rimvisuals_status_with_friends | Friends & Markers / Share RIMVISUALS status with friends | Boolean sub-option of friends_options; if blocked, the complete parent module is disabled and hidden. |
| friends_options.receive_friend_markers | Friends & Markers / Receive friend markers | Boolean sub-option of friends_options; if blocked, the complete parent module is disabled and hidden. |
| friends_options.rim_friend_name_badges | Friends & Markers / RIM friend name badges | Boolean sub-option of friends_options; if blocked, the complete parent module is disabled and hidden. |
| info_hud | Info HUD | FPS, ping and coordinates |
| info_hud.custom_accent | Info HUD / Custom accent | Boolean sub-option of info_hud; if blocked, the complete parent module is disabled and hidden. |
| info_hud.show_fps | Info HUD / Show FPS | Boolean sub-option of info_hud; if blocked, the complete parent module is disabled and hidden. |
| info_hud.show_ping | Info HUD / Show ping | Boolean sub-option of info_hud; if blocked, the complete parent module is disabled and hidden. |
| info_hud.show_xyz | Info HUD / Show XYZ | Boolean sub-option of info_hud; if blocked, the complete parent module is disabled and hidden. |
| watermark | Watermark | RIMVISUALS tag |
| watermark.custom_accent | Watermark / Custom accent | Boolean sub-option of watermark; if blocked, the complete parent module is disabled and hidden. |
| watermark.show_rv_logo | Watermark / Show RV logo | Boolean sub-option of watermark; if blocked, the complete parent module is disabled and hidden. |
| pulse_glow | Pulse Glow | Soft glowing screen edges |
| pulse_glow.use_theme_accent | Pulse Glow / Use theme accent | Boolean sub-option of pulse_glow; if blocked, the complete parent module is disabled and hidden. |
| particles | Particle Effects | Decorative floating particles |
| particles.in_game_overlay | Particle Effects / In-game overlay | Boolean sub-option of particles; if blocked, the complete parent module is disabled and hidden. |
| particles.menu_particles | Particle Effects / Menu particles | Boolean sub-option of particles; if blocked, the complete parent module is disabled and hidden. |
| interface | Interface | Theme, colors, scale and touch mode |
| interface.auto_save_cfg | Interface / Auto Save CFG | Boolean sub-option of interface; if blocked, the complete parent module is disabled and hidden. |
| interface.compact_sidebar | Interface / Compact sidebar | Boolean sub-option of interface; if blocked, the complete parent module is disabled and hidden. |
| performance | Performance | CPU submission timings, bounded effects; no GPU/FPS claims |
| performance.performance_safe_mode | Performance / Performance Safe Mode | Boolean sub-option of performance; if blocked, the complete parent module is disabled and hidden. |
| performance.reduced_motion | Performance / Reduced motion | Boolean sub-option of performance; if blocked, the complete parent module is disabled and hidden. |
| performance.adaptive_particles | Performance / Adaptive particles | Boolean sub-option of performance; if blocked, the complete parent module is disabled and hidden. |
| performance.performance_monitor | Performance / Performance monitor | Boolean sub-option of performance; if blocked, the complete parent module is disabled and hidden. |
| performance.diagnostic_cpu_timings | Performance / Diagnostic CPU timings | Boolean sub-option of performance; if blocked, the complete parent module is disabled and hidden. |
| viewmodel | Viewmodel Editor | Per-hand position, scale, rotation and item depth; cosmetic only |
| viewmodel.item_category_transforms | Viewmodel Editor / Item category transforms | Boolean sub-option of viewmodel; if blocked, the complete parent module is disabled and hidden. |
| viewmodel.smooth_transitions | Viewmodel Editor / Smooth transitions | Boolean sub-option of viewmodel; if blocked, the complete parent module is disabled and hidden. |
| swing | Swing Animations | Adds submission-time swing transforms; attack timing unchanged. Reduced Motion disables this. |
| hand_lighting | Hand Lighting | Item illumination only. Bloom/outline/blur shaders are not implemented; no Iris pipeline hooks. |
| hand_lighting.main_hand_lighting | Hand Lighting / Main hand lighting | Boolean sub-option of hand_lighting; if blocked, the complete parent module is disabled and hidden. |
| hand_lighting.off_hand_lighting | Hand Lighting / Off hand lighting | Boolean sub-option of hand_lighting; if blocked, the complete parent module is disabled and hidden. |
| crystal_scale | Crystal Scale | Changes how large End Crystals are drawn and lets new crystals grow in. Visual only; hitbox is unchanged. |
| crystal_fade | Crystal Fade | Makes End Crystals translucent and lets new crystals fade in. Visual only; minimum opacity is limited so crystals stay visible. |
| totem_scale | Totem Scale | Changes the size of the totem pop animation and can shrink it away at the end. Visual only. |
| totem_scale.shrink_at_end | Totem Scale / Shrink at end | Boolean sub-option of totem_scale; if blocked, the complete parent module is disabled and hidden. |
| hit_color | Hit Color | Changes the colour of the damage flash on every entity (including you). One small texture upload when the setting changes; no per-frame cost. |
| hit_particles | Hit Particles | Spawns coloured particles when you attack a living entity. Count obeys the performance budget and Safe Mode cap. |
| crit_particles | Critical Particles | Adds your own coloured burst when an attack looks like a critical hit (falling, not sprinting, cooldown nearly full). The server decides the real crit; vanilla particles are unchanged. |
| damage_particles | Damage Particles | Spawns particles on entities that lose health, scaled by damage taken. Server hurt packets by default; optional synced-health delta mode. Hurt packets do not reveal damage amount. |
| pearl_trails | Pearl Trails | Coloured particle trail behind ender pearls in flight. Count per tick is capped by the performance budget (Safe Mode: 6). |
| pearl_trails.own_projectiles_only | Pearl Trails / Own projectiles only | Boolean sub-option of pearl_trails; if blocked, the complete parent module is disabled and hidden. |
| projectile_trails | Projectile Trails | Coloured particle trail behind arrows, tridents and thrown items in flight (not pearls). Count per tick is capped by the performance budget (Safe Mode: 6). |
| projectile_trails.own_projectiles_only | Projectile Trails / Own projectiles only | Boolean sub-option of projectile_trails; if blocked, the complete parent module is disabled and hidden. |
| sword_trails | Sword Trails | Coloured particles along the sword tip while swinging a sword. Count per tick is capped by the performance budget (Safe Mode: 6). |
| sharpness_particles | Sharpness Particles | Adds a coloured burst on hits made with a Sharpness-enchanted held item (level read from the item you hold). Obeys the performance budget. |
| inventory_hud | Inventory HUD | Main inventory grid; hotbar is not duplicated |
| inventory_hud.custom_accent | Inventory HUD / Custom accent | Boolean sub-option of inventory_hud; if blocked, the complete parent module is disabled and hidden. |
| inventory_hud.show_stack_counts | Inventory HUD / Show stack counts | Boolean sub-option of inventory_hud; if blocked, the complete parent module is disabled and hidden. |
| hit_marker | Hit Marker | Shows a marker at the crosshair when you attack a living entity. Hidden with F1, like the vanilla HUD. |
| kill_effects | Kill Effects | Spawns a burst where your last target dies (health reaches 0 within 6 s of your hit). Obeys the performance budget. |
| damage_tint | Damage Tint | Tints the screen edges when you take damage. 2D overlay drawn only while the hurt timer runs. |
| cps_hud | CPS | Clicks per second (attack and use key presses seen per rendered frame; very fast clicking above your FPS is under-counted). |
| cps_hud.custom_accent | CPS / Custom accent | Boolean sub-option of cps_hud; if blocked, the complete parent module is disabled and hidden. |
| cps_hud.show_right_click | CPS / Show right click | Boolean sub-option of cps_hud; if blocked, the complete parent module is disabled and hidden. |
| keystrokes_hud | Keystrokes | Movement keys and mouse buttons using your configured key bindings |
| keystrokes_hud.custom_accent | Keystrokes / Custom accent | Boolean sub-option of keystrokes_hud; if blocked, the complete parent module is disabled and hidden. |
| keystrokes_hud.show_mouse_buttons | Keystrokes / Show mouse buttons | Boolean sub-option of keystrokes_hud; if blocked, the complete parent module is disabled and hidden. |
| keystrokes_hud.show_jump_bar | Keystrokes / Show jump bar | Boolean sub-option of keystrokes_hud; if blocked, the complete parent module is disabled and hidden. |
| potion_hud | Potion HUD | Active effects with level and time left |
| potion_hud.custom_accent | Potion HUD / Custom accent | Boolean sub-option of potion_hud; if blocked, the complete parent module is disabled and hidden. |
| potion_hud.show_level | Potion HUD / Show level | Boolean sub-option of potion_hud; if blocked, the complete parent module is disabled and hidden. |
| potion_hud.highlight_last_10_s | Potion HUD / Highlight last 10 s | Boolean sub-option of potion_hud; if blocked, the complete parent module is disabled and hidden. |
| speed_hud | Speed (BPS) | Your horizontal speed in blocks per second |
| speed_hud.custom_accent | Speed (BPS) / Custom accent | Boolean sub-option of speed_hud; if blocked, the complete parent module is disabled and hidden. |
| speed_hud.show_unit | Speed (BPS) / Show unit | Boolean sub-option of speed_hud; if blocked, the complete parent module is disabled and hidden. |
| session_hud | Session Info | Play time in this world and the current server address |
| session_hud.custom_accent | Session Info / Custom accent | Boolean sub-option of session_hud; if blocked, the complete parent module is disabled and hidden. |
| session_hud.show_play_time | Session Info / Show play time | Boolean sub-option of session_hud; if blocked, the complete parent module is disabled and hidden. |
| session_hud.show_server | Session Info / Show server | Boolean sub-option of session_hud; if blocked, the complete parent module is disabled and hidden. |
| hand_color | Hand Color | Tints the first-person arm: solid, gradient or rainbow, with per-hand opacity. Multiplies the skin colour; item models are not changed. |
| hand_color.main_hand | Hand Color / Main hand | Boolean sub-option of hand_color; if blocked, the complete parent module is disabled and hidden. |
| hand_color.off_hand | Hand Color / Off hand | Boolean sub-option of hand_color; if blocked, the complete parent module is disabled and hidden. |
| custom_fog | Custom Fog | Visible range changes render-distance fog in blocks; Legacy preserves percentage configs. Custom Fog takes priority over Atmosphere. Iris shader packs may override the result. |
| custom_fog.override_color | Custom Fog / Override color | Boolean sub-option of custom_fog; if blocked, the complete parent module is disabled and hidden. |
| custom_fog.affect_fluid_fog | Custom Fog / Affect fluid fog | Boolean sub-option of custom_fog; if blocked, the complete parent module is disabled and hidden. |
| hand_glow | Hand Glow | Translucent glowing halo around your first-person arm (stacked enlarged copies of the arm, fully lit). Geometry halo, not a bloom filter. Safe Mode suppresses the halo; Balanced caps it at one layer. |
| hand_glow.rainbow | Hand Glow / Rainbow | Boolean sub-option of hand_glow; if blocked, the complete parent module is disabled and hidden. |
| hand_glow.main_hand | Hand Glow / Main hand | Boolean sub-option of hand_glow; if blocked, the complete parent module is disabled and hidden. |
| hand_glow.off_hand | Hand Glow / Off hand | Boolean sub-option of hand_glow; if blocked, the complete parent module is disabled and hidden. |
| water_color | Water Color | Recolours water in the world (blocks and cauldrons). Terrain is rebuilt shortly after you change a setting. |
| lava_color | Lava Color | Recolours lava surfaces. Terrain is rebuilt shortly after you change a setting. |
| weather_color | Weather Color | Tints falling rain and snow. Applies live, no rebuild needed. |
| biome_colors | Biome Colors | Overrides the biome tint on grass, ferns and leaves. Terrain is rebuilt shortly after you change a setting. |
| biome_colors.tint_grass | Biome Colors / Tint grass | Boolean sub-option of biome_colors; if blocked, the complete parent module is disabled and hidden. |
| biome_colors.tint_leaves | Biome Colors / Tint leaves | Boolean sub-option of biome_colors; if blocked, the complete parent module is disabled and hidden. |
| entity_outline | Entity Outline | Outlines selected entities through walls using the game's glow outline. Your own body is not outlined. |
| entity_outline.players | Entity Outline / Players | Boolean sub-option of entity_outline; if blocked, the complete parent module is disabled and hidden. |
| entity_outline.hostile_mobs | Entity Outline / Hostile mobs | Boolean sub-option of entity_outline; if blocked, the complete parent module is disabled and hidden. |
| entity_outline.other_living_entities | Entity Outline / Other living entities | Boolean sub-option of entity_outline; if blocked, the complete parent module is disabled and hidden. |
| target_highlight | Target Highlight | Outlines the entity under your crosshair and/or the entity you hit last, through walls. |
| target_highlight.crosshair_target | Target Highlight / Crosshair target | Boolean sub-option of target_highlight; if blocked, the complete parent module is disabled and hidden. |
| target_highlight.last_hit_target | Target Highlight / Last hit target | Boolean sub-option of target_highlight; if blocked, the complete parent module is disabled and hidden. |
| totem_fade | Totem Fade | Fades the totem-pop item out near the end of its animation (works with Totem Scale). Visual only. |
| anchor_fade | Anchor Fade / Scale | When a Respawn Anchor explodes, a client-only copy of its model fades and shrinks where it stood. Visual only: no block, hitbox or server change. |
| anchor_fade.fade | Anchor Fade / Scale / Fade | Boolean sub-option of anchor_fade; if blocked, the complete parent module is disabled and hidden. |
| anchor_fade.scale | Anchor Fade / Scale / Scale | Boolean sub-option of anchor_fade; if blocked, the complete parent module is disabled and hidden. |
| anchor_fade.tint | Anchor Fade / Scale / Tint | Boolean sub-option of anchor_fade; if blocked, the complete parent module is disabled and hidden. |
| explosion_effects | Explosion Effects | Extra shockwave ring and sparks for explosions near you (crystals, TNT, anchors, beds). Uses the shared particle budget; Safe Mode suppresses it. |
| explosion_effects.shockwave_ring | Explosion Effects / Shockwave ring | Boolean sub-option of explosion_effects; if blocked, the complete parent module is disabled and hidden. |
| explosion_effects.sparks | Explosion Effects / Sparks | Boolean sub-option of explosion_effects; if blocked, the complete parent module is disabled and hidden. |
| cooldowns_hud | Cooldowns | Active item cooldowns with progress bars |
| cooldowns_hud.custom_accent | Cooldowns / Custom accent | Boolean sub-option of cooldowns_hud; if blocked, the complete parent module is disabled and hidden. |
| cooldowns_hud.progress_bar | Cooldowns / Progress bar | Boolean sub-option of cooldowns_hud; if blocked, the complete parent module is disabled and hidden. |
| cooldowns_hud.show_percent | Cooldowns / Show percent | Boolean sub-option of cooldowns_hud; if blocked, the complete parent module is disabled and hidden. |
| notifications | Notifications | Short alerts: totem used, low health, low armor durability |
| notifications.custom_accent | Notifications / Custom accent | Boolean sub-option of notifications; if blocked, the complete parent module is disabled and hidden. |
| notifications.totem_used | Notifications / Totem used | Boolean sub-option of notifications; if blocked, the complete parent module is disabled and hidden. |
| notifications.low_health | Notifications / Low health | Boolean sub-option of notifications; if blocked, the complete parent module is disabled and hidden. |
| notifications.low_armor_durability | Notifications / Low armor durability | Boolean sub-option of notifications; if blocked, the complete parent module is disabled and hidden. |
| fullbright | Fullbright | Lights dark areas by raising the lightmap gamma. Your Brightness option is left unchanged. |
| night_vision_fade | Night Vision Fade | Night Vision fades out smoothly over its last 10 seconds instead of flickering. |
| golden_apple_glow | Golden Apple Glow | Gold screen-edge glow while you have absorption. 2D overlay, drawn only while active. |
| golden_apple_glow.pulse | Golden Apple Glow / Pulse | Boolean sub-option of golden_apple_glow; if blocked, the complete parent module is disabled and hidden. |
| custom_sky | Custom Sky | Blends the daytime sky dome colour with your colour; strength 0 is vanilla. Does not change the horizon fog (use Custom Fog) or the End/Nether sky. |
| hurt_camera | Hurt Camera | Scales the camera shake/roll when damaged on top of the vanilla Damage Tilt option (0% removes it). Death animation is unchanged. |
| armor_hud | Armor HUD | Worn armour pieces with durability percent and bar |
| armor_hud.custom_accent | Armor HUD / Custom accent | Boolean sub-option of armor_hud; if blocked, the complete parent module is disabled and hidden. |
| armor_hud.vertical_layout | Armor HUD / Vertical layout | Boolean sub-option of armor_hud; if blocked, the complete parent module is disabled and hidden. |
| armor_hud.durability_percent | Armor HUD / Durability percent | Boolean sub-option of armor_hud; if blocked, the complete parent module is disabled and hidden. |
| armor_hud.durability_bar | Armor HUD / Durability bar | Boolean sub-option of armor_hud; if blocked, the complete parent module is disabled and hidden. |
| armor_hud.hide_empty_slots | Armor HUD / Hide empty slots | Boolean sub-option of armor_hud; if blocked, the complete parent module is disabled and hidden. |
| target_hud | Target HUD | Name, health bar and distance of your current target. Uses health the game already syncs to you. |
| target_hud.custom_accent | Target HUD / Custom accent | Boolean sub-option of target_hud; if blocked, the complete parent module is disabled and hidden. |
| target_hud.use_crosshair_target | Target HUD / Use crosshair target | Boolean sub-option of target_hud; if blocked, the complete parent module is disabled and hidden. |
| target_hud.show_distance | Target HUD / Show distance | Boolean sub-option of target_hud; if blocked, the complete parent module is disabled and hidden. |
| performance_hud | Performance Monitor | CPU submission times, not GPU timings |
| performance_hud.custom_accent | Performance Monitor / Custom accent | Boolean sub-option of performance_hud; if blocked, the complete parent module is disabled and hidden. |
| local_marks | Local Markers | Local .way markers and temporary friend pings for this server and dimension |
| local_marks.custom_accent | Local Markers / Custom accent | Boolean sub-option of local_marks; if blocked, the complete parent module is disabled and hidden. |
| totem_bar | Totem Bar | Own-player / loaded-world information; no server or inventory actions |
| totem_bar.custom_accent | Totem Bar / Custom accent | Boolean sub-option of totem_bar; if blocked, the complete parent module is disabled and hidden. |
| saturation_hud | Saturation HUD | Own-player / loaded-world information; no server or inventory actions |
| saturation_hud.custom_accent | Saturation HUD / Custom accent | Boolean sub-option of saturation_hud; if blocked, the complete parent module is disabled and hidden. |
| low_hp_hud | Low HP | Own-player / loaded-world information; no server or inventory actions |
| low_hp_hud.custom_accent | Low HP / Custom accent | Boolean sub-option of low_hp_hud; if blocked, the complete parent module is disabled and hidden. |
| tnt_timer | TNT Timer | Own-player / loaded-world information; no server or inventory actions |
| tnt_timer.custom_accent | TNT Timer / Custom accent | Boolean sub-option of tnt_timer; if blocked, the complete parent module is disabled and hidden. |
| zoom | Zoom | Camera FOV only. Assign Toggle Zoom in Keybinds; no attack, movement or sensitivity changes |
| zoom.smooth_transition | Zoom / Smooth transition | Boolean sub-option of zoom; if blocked, the complete parent module is disabled and hidden. |
| crosshair | Crosshair | Own first-person crosshair; does not change aim, attacks or targeting |
| crosshair.center_dot | Crosshair / Center dot | Boolean sub-option of crosshair; if blocked, the complete parent module is disabled and hidden. |
| crosshair.attack_cooldown_bar | Crosshair / Attack cooldown bar | Boolean sub-option of crosshair; if blocked, the complete parent module is disabled and hidden. |
| aspect_ratio | Aspect Ratio | Changes horizontal camera projection; viewport, GUI and mouse targeting stay vanilla |
| atmosphere | Atmosphere | Shared sky/fog colour grading. Existing Custom Sky/Fog take priority. No time, weather or lighting simulation changes |
| item_physics | Item Physics (visual) | Cosmetic dropped-item tilt/spin/scale. Vanilla bob remains; item position, pickup and physics are unchanged |
| china_hat | China Hat | Depth-tested conical player hat; third-person self, optional nearby players. Safe Mode suppresses geometry |
| china_hat.other_players | China Hat / Other players | Boolean sub-option of china_hat; if blocked, the complete parent module is disabled and hidden. |
| block_overlay | Block Overlay | Translucent selected-block shape bounding box; no interaction or hitbox changes. Safe Mode suppresses geometry |
| block_overlay.other_players | Block Overlay / Other players | Boolean sub-option of block_overlay; if blocked, the complete parent module is disabled and hidden. |
| sound_controller | Sound Controller | Scales sound categories at playback; Minecraft volume options remain unchanged |
| shulker_preview | Shulker Preview | 27-slot tooltip from container data already supplied by Minecraft. No server inventory queries |
| hotkeys_hud | Assigned Keys HUD | Assigned RIM key bindings, up to eight rows; no input simulation |
| hotkeys_hud.custom_accent | Assigned Keys HUD / Custom accent | Boolean sub-option of hotkeys_hud; if blocked, the complete parent module is disabled and hidden. |
| compass_hud | Compass HUD | Own heading and yaw |
| compass_hud.custom_accent | Compass HUD / Custom accent | Boolean sub-option of compass_hud; if blocked, the complete parent module is disabled and hidden. |
| visual_hitbox | Visual Hitbox | Depth-tested visual outlines of existing player/mob bounds; width is in world units. No hitbox, reach or server changes |
| visual_hitbox.players | Visual Hitbox / Players | Boolean sub-option of visual_hitbox; if blocked, the complete parent module is disabled and hidden. |
| visual_hitbox.mobs | Visual Hitbox / Mobs | Boolean sub-option of visual_hitbox; if blocked, the complete parent module is disabled and hidden. |
| visual_hitbox.selected_target_only | Visual Hitbox / Selected target only | Boolean sub-option of visual_hitbox; if blocked, the complete parent module is disabled and hidden. |
| hw_helper | HW Helper | Own server HUD and literal parsing rules. Blank rules do nothing; timer syntax: prefix + seconds or mm:ss. No commands. |
| hw_helper.item_swap | HW Helper / Item Swap | Boolean sub-option of hw_helper; if blocked, the complete parent module is disabled and hidden. |
| hw_helper.auction_price | HW Helper / Auction Price | Boolean sub-option of hw_helper; if blocked, the complete parent module is disabled and hidden. |
| hw_helper.zako_health_indicator | HW Helper / Zako Health Indicator | Boolean sub-option of hw_helper; if blocked, the complete parent module is disabled and hidden. |
| hw_helper.read_system_chat | HW Helper / Read system chat | Boolean sub-option of hw_helper; if blocked, the complete parent module is disabled and hidden. |
| hw_helper.read_action_bar | HW Helper / Read action bar | Boolean sub-option of hw_helper; if blocked, the complete parent module is disabled and hidden. |
| hw_helper.read_sidebar | HW Helper / Read sidebar | Boolean sub-option of hw_helper; if blocked, the complete parent module is disabled and hidden. |
| hw_helper.event_notifications | HW Helper / Event notifications | Boolean sub-option of hw_helper; if blocked, the complete parent module is disabled and hidden. |
| hw_helper.auction_match_notification | HW Helper / Auction match notification | Boolean sub-option of hw_helper; if blocked, the complete parent module is disabled and hidden. |
| hw_helper.low_hp_warning | HW Helper / Low HP warning | Boolean sub-option of hw_helper; if blocked, the complete parent module is disabled and hidden. |
| hw_helper.armor_warning | HW Helper / Armor warning | Boolean sub-option of hw_helper; if blocked, the complete parent module is disabled and hidden. |
| hw_helper.track_events | HW Helper / Track events | Boolean sub-option of hw_helper; if blocked, the complete parent module is disabled and hidden. |
| hw_helper.holyworld_public_api | HW Helper / HolyWorld public API | Boolean sub-option of hw_helper; if blocked, the complete parent module is disabled and hidden. |
| hw_helper.track_pvp_cooldowns | HW Helper / Track PvP cooldowns | Boolean sub-option of hw_helper; if blocked, the complete parent module is disabled and hidden. |
| hw_helper.estimated_use_attempt_timers | HW Helper / Estimated use-attempt timers | Boolean sub-option of hw_helper; if blocked, the complete parent module is disabled and hidden. |
| hw_helper.show_trapka_timer | HW Helper / Show Trapka timer | Boolean sub-option of hw_helper; if blocked, the complete parent module is disabled and hidden. |
| hw_helper.show_plast_timer | HW Helper / Show Plast timer | Boolean sub-option of hw_helper; if blocked, the complete parent module is disabled and hidden. |
| hw_helper.helper_sounds | HW Helper / Helper sounds | Boolean sub-option of hw_helper; if blocked, the complete parent module is disabled and hidden. |
| hw_helper.cooldown_ready_notifications | HW Helper / Cooldown READY notifications | Boolean sub-option of hw_helper; if blocked, the complete parent module is disabled and hidden. |
| ft_helper | FT Helper | FunTime HUD, received event data and manual offhand Item Swap. Item Swap: one configured key press; minimum four ticks, no health trigger. |
| ft_helper.item_swap | FT Helper / Item Swap | Boolean sub-option of ft_helper; if blocked, the complete parent module is disabled and hidden. |
| ft_helper.auction_price | FT Helper / Auction Price | Boolean sub-option of ft_helper; if blocked, the complete parent module is disabled and hidden. |
| ft_helper.zako_health_indicator | FT Helper / Zako Health Indicator | Boolean sub-option of ft_helper; if blocked, the complete parent module is disabled and hidden. |
| ft_helper.read_system_chat | FT Helper / Read system chat | Boolean sub-option of ft_helper; if blocked, the complete parent module is disabled and hidden. |
| ft_helper.read_action_bar | FT Helper / Read action bar | Boolean sub-option of ft_helper; if blocked, the complete parent module is disabled and hidden. |
| ft_helper.read_sidebar | FT Helper / Read sidebar | Boolean sub-option of ft_helper; if blocked, the complete parent module is disabled and hidden. |
| ft_helper.event_notifications | FT Helper / Event notifications | Boolean sub-option of ft_helper; if blocked, the complete parent module is disabled and hidden. |
| ft_helper.auction_match_notification | FT Helper / Auction match notification | Boolean sub-option of ft_helper; if blocked, the complete parent module is disabled and hidden. |
| ft_helper.low_hp_warning | FT Helper / Low HP warning | Boolean sub-option of ft_helper; if blocked, the complete parent module is disabled and hidden. |
| ft_helper.armor_warning | FT Helper / Armor warning | Boolean sub-option of ft_helper; if blocked, the complete parent module is disabled and hidden. |
| ft_helper.track_events | FT Helper / Track events | Boolean sub-option of ft_helper; if blocked, the complete parent module is disabled and hidden. |
| ft_helper.holyworld_public_api | FT Helper / HolyWorld public API | Boolean sub-option of ft_helper; if blocked, the complete parent module is disabled and hidden. |
| ft_helper.track_pvp_cooldowns | FT Helper / Track PvP cooldowns | Boolean sub-option of ft_helper; if blocked, the complete parent module is disabled and hidden. |
| ft_helper.estimated_use_attempt_timers | FT Helper / Estimated use-attempt timers | Boolean sub-option of ft_helper; if blocked, the complete parent module is disabled and hidden. |
| ft_helper.show_trapka_timer | FT Helper / Show Trapka timer | Boolean sub-option of ft_helper; if blocked, the complete parent module is disabled and hidden. |
| ft_helper.show_plast_timer | FT Helper / Show Plast timer | Boolean sub-option of ft_helper; if blocked, the complete parent module is disabled and hidden. |
| ft_helper.helper_sounds | FT Helper / Helper sounds | Boolean sub-option of ft_helper; if blocked, the complete parent module is disabled and hidden. |
| ft_helper.cooldown_ready_notifications | FT Helper / Cooldown READY notifications | Boolean sub-option of ft_helper; if blocked, the complete parent module is disabled and hidden. |
| hw_helper_hud | HW Helper HUD | Received server information only; edit layout here, parsing rules in Utilities |
| hw_helper_hud.custom_accent | HW Helper HUD / Custom accent | Boolean sub-option of hw_helper_hud; if blocked, the complete parent module is disabled and hidden. |
| ft_helper_hud | FT Helper HUD | Received server information only; edit layout here, parsing rules in Utilities |
| ft_helper_hud.custom_accent | FT Helper HUD / Custom accent | Boolean sub-option of ft_helper_hud; if blocked, the complete parent module is disabled and hidden. |
| hw_helper_events | HW Helper EVENTS | Gameplay settings: Utilities > HW/FT Settings |
| hw_helper_events.custom_accent | HW Helper EVENTS / Custom accent | Boolean sub-option of hw_helper_events; if blocked, the complete parent module is disabled and hidden. |
| ft_helper_events | FT Helper EVENTS | Gameplay settings: Utilities > HW/FT Settings |
| ft_helper_events.custom_accent | FT Helper EVENTS / Custom accent | Boolean sub-option of ft_helper_events; if blocked, the complete parent module is disabled and hidden. |
| hw_helper_cooldowns | HW Helper COOLDOWNS | Gameplay settings: Utilities > HW/FT Settings |
| hw_helper_cooldowns.custom_accent | HW Helper COOLDOWNS / Custom accent | Boolean sub-option of hw_helper_cooldowns; if blocked, the complete parent module is disabled and hidden. |
| ft_helper_cooldowns | FT Helper COOLDOWNS | Gameplay settings: Utilities > HW/FT Settings |
| ft_helper_cooldowns.custom_accent | FT Helper COOLDOWNS / Custom accent | Boolean sub-option of ft_helper_cooldowns; if blocked, the complete parent module is disabled and hidden. |
| hw_helper_regions | HW Helper REGIONS | Gameplay settings: Utilities > HW/FT Settings |
| hw_helper_regions.custom_accent | HW Helper REGIONS / Custom accent | Boolean sub-option of hw_helper_regions; if blocked, the complete parent module is disabled and hidden. |
| ft_helper_regions | FT Helper REGIONS | Gameplay settings: Utilities > HW/FT Settings |
| ft_helper_regions.custom_accent | FT Helper REGIONS / Custom accent | Boolean sub-option of ft_helper_regions; if blocked, the complete parent module is disabled and hidden. |
| hw_helper_notifications | HW Helper NOTIFICATIONS | Gameplay settings: Utilities > HW/FT Settings |
| hw_helper_notifications.custom_accent | HW Helper NOTIFICATIONS / Custom accent | Boolean sub-option of hw_helper_notifications; if blocked, the complete parent module is disabled and hidden. |
| ft_helper_notifications | FT Helper NOTIFICATIONS | Gameplay settings: Utilities > HW/FT Settings |
| ft_helper_notifications.custom_accent | FT Helper NOTIFICATIONS / Custom accent | Boolean sub-option of ft_helper_notifications; if blocked, the complete parent module is disabled and hidden. |
| keybinds | keybinds | RIMVISUALS keyboard and mouse binding manager and dispatch. |
| profiles | profiles | Local saved settings, presets, backups, import/export and server profile binding. |
| hud_editor | hud_editor | Move, resize, recolour and align visible permitted HUD elements. |
| client_settings | client_settings | Legacy local launcher settings import; no executable downloads in this mod. |
