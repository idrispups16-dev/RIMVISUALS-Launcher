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

Статус реализации (ветка `feature/rimvisuals-loader`):
- Реализовано и проверено на Windows CI: обнаружение процесса (PowerShell/CIM) с разбором папки игры, версии, Fabric; установка проверенного JAR в найденный профиль;
  GUI-страница Loader; CONNECT с реальным IPC-handshake (в CI — на протокольной заглушке); каталог модулей из запущенной игры; пошаговая диагностика IPC;
  CLI `loader-scan`, `loader-connect`, `loader-diagnose`, `jar-info`.
- Проверено ранее на CI в живом клиенте: сервер IPC в моде, токен, выполнение команд в игровом потоке (`RIM_JARVIS_OK`).
- Не проверено: CONNECT к настоящей игре на ПК владельца (Legacy Launcher + GPU); офлайн-каталог модулей без запущенной игры (нужен файл каталога при сборке мода).
- Не делается намеренно: горячая инъекция, скрытая DLL-инъекция, обход античита.
