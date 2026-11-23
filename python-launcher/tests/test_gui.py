import os
import sys

import pytest

tk = pytest.importorskip("tkinter")


@pytest.mark.skipif(sys.platform.startswith("linux") and not os.environ.get("DISPLAY"), reason="no display")
def test_gui_selftest(tmp_path):
    from rimvisuals_launcher.gui.app import selftest
    assert selftest(str(tmp_path / "home"), None) == 0
