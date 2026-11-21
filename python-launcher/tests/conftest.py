import sys, pathlib
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import pytest
from fakeserver import Fake, patch_ms_get
from rimvisuals_launcher.minecraft import Endpoints

@pytest.fixture()
def fake():
    f = Fake(); patch_ms_get(f)
    yield f
    f.close()

@pytest.fixture()
def ep(fake):
    return Endpoints(manifest=fake.base + "/manifest", resources=fake.base + "/res",
                     fabric_meta=fake.base, modrinth=fake.base)

@pytest.fixture()
def home(tmp_path, monkeypatch):
    monkeypatch.setenv("RIMVISUALS_HOME", str(tmp_path / "RV"))
    return tmp_path / "RV"
