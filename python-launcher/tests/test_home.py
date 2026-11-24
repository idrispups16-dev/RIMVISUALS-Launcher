from rimvisuals_launcher import homemodel as H
from rimvisuals_launcher.jarvis.chats import ChatStore, DAILY


def test_versions_have_no_1_20_1():
    assert H.VERSIONS == ["1.21.11", "1.21.4"]


def test_status_and_play_actions():
    off = H.GameLink()
    assert "не подключена" in H.status_line(off, "1.21.11") and H.play_action(off, "1.21.11")[0] == "message"
    assert "недоступна" in H.status_line(off, "1.21.4")
    on = H.GameLink(connected=True, pid=77, minecraft="1.21.11", player="Rimzuk222")
    assert H.default_selection(on) == "1.21.11" and H.play_action(on, "1.21.11") == ("focus", "77")
    kind, text = H.play_action(on, "1.21.4")
    assert kind == "message" and "недоступна" in text
    assert "потеряно" in H.status_line(H.GameLink(lost=True), "1.21.11")
    assert "Подключение" in H.status_line(H.GameLink(connecting=True), "1.21.11")
    assert "Не удалось" in H.status_line(H.GameLink(error="порт закрыт"), "1.21.11")


def test_quota_free_redelivery_and_limit(tmp_path):
    t = [1_700_000_000.0]
    s = ChatStore(tmp_path / "c.json", clock=lambda: t[0])
    assert s.remaining() == DAILY
    assert s.charge("a") and s.remaining() == DAILY - 1
    assert s.charge("a") and s.remaining() == DAILY - 1          # redelivery of the same request is free
    for i in range(DAILY - 1): assert s.charge(f"r{i}")
    assert s.remaining() == 0 and not s.charge("new")
    assert s.charge("a")                                          # already counted -> still allowed
    assert ChatStore(tmp_path / "c.json", clock=lambda: t[0]).remaining() == 0     # persisted
    t[0] += 86400 * 1.1
    assert ChatStore(tmp_path / "c.json", clock=lambda: t[0]).remaining() == DAILY  # next day resets
    assert s.next_reset()


def test_history_persists_and_titles(tmp_path):
    s = ChatStore(tmp_path / "c.json"); c = s.new_chat()
    s.add(c, "me", "Включи Zoom"); s.add(c, "bot", "Готов предложить", card={"kind": "confirm"})
    again = ChatStore(tmp_path / "c.json")
    assert again.chats()[0]["title"] == "Включи Zoom" and len(again.chats()[0]["msgs"]) == 2
