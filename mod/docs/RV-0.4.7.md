# 0.4.7-rv.1

- Remove HW/FT Trapka, Plast and custom item-use bindings and their implementation.
- Remove Item Swap and its action/implementation from the catalog and JAR.
- Remove the helper item-binds tab. Informational cooldown/name parsing remains.
- Ignore retired item-use binding keys when importing old configurations.
- Keep menu, HUD and visual toggles, friend ping N and .way commands.
- Replace the four-point logo everywhere in the mod UI with an original RV monogram:
  menu header, account entry, module cards, watermark and account screen.
- Add the RV icon to Fabric metadata.

Validated with a clean build, 114 successful tests (1 skipped), an isolated Minecraft
startup/mixin/UI check, and inspection of the remapped JAR: both retired gameplay
implementation classes are absent and the RV icon is packaged. No server rule
compliance claim is made for the remaining mod as a whole.

This supersedes the item-bind instructions in SOCIAL-0.4.6.md. Public website
downloads remain closed. Personal settings and the prior JAR are backed up locally.
