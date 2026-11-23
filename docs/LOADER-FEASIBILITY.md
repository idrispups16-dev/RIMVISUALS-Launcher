# RIMVISUALS Loader — можно ли подключиться к уже запущенному Minecraft?

Вывод: **горячее подключение Fabric-мода к работающей JVM ненадёжно, поэтому Loader его не обещает.**

Почему (технически):
- Fabric-моды загружает Knot при старте JVM: разбор `fabric.mod.json`, entrypoints, регистрация Mixin-конфигов.
- Java Attach API / `Instrumentation` (документированный механизм) позволяет добавить JAR в classpath и ретрансформировать классы, но
  Mixin уже загруженных классов (рендер, GUI) невозможен без добавления полей/методов — `retransformClasses` этого не разрешает;
  entrypoints `ClientModInitializer` повторно не вызываются.
- Нужны доп. права/флаги (`jdk.attach`, `-XX:-DisableAttachMechanism`), поведение отличается между лаунчерами; это ближе к инъекции в чужой процесс, чего проект не делает.

Что делает Loader (`rimvisuals_launcher/loader.py`):
1. Находит запущенный Minecraft (любой лаунчер) и его `--gameDir`, версию, наличие Fabric.
2. Состояния: not found / detected / wrong version / Fabric missing / RIMVISUALS missing / installation required / ready / connecting / connected / failed.
3. Ставит проверенный по SHA-256 JAR в `mods` найденного профиля (существующий `ModOps`/`install`), если его нет.
4. Подключается по существующему loopback-IPC (токен сессии), `Connected` только после успешной авторизации.
5. Перезапуск предлагается только когда он реально нужен (Fabric отсутствует или мод поставлен после старта игры).

Не проверено на Windows-GPU: обнаружение через PowerShell (CIM) протестировано только разбором командной строки (pytest).
Не реализовано пока: GUI-страница Loader, кнопка CONNECT, установка в чужой профиль.
