import http.server, shutil, ssl, subprocess, threading
import pytest
from rimvisuals_launcher import core
from rimvisuals_launcher.core import HttpError, http_request, ssl_context

needs_openssl = pytest.mark.skipif(not shutil.which("openssl"), reason="openssl CLI not available")


def test_context_verifies_and_loads_mozilla_roots():
    ctx = ssl_context()
    assert ctx.verify_mode == ssl.CERT_REQUIRED and ctx.check_hostname
    assert len(ctx.get_ca_certs()) > 50            # bundled Mozilla roots are really loaded, not just the OS store


@pytest.fixture()
def https_server(tmp_path):
    key, crt = tmp_path / "k.pem", tmp_path / "c.pem"
    subprocess.run(["openssl", "req", "-x509", "-newkey", "rsa:2048", "-nodes", "-keyout", str(key), "-out", str(crt), "-days", "2", "-subj", "/CN=localhost",
                    "-addext", "subjectAltName=DNS:localhost,IP:127.0.0.1"], check=True, capture_output=True)
    class H(http.server.BaseHTTPRequestHandler):
        def do_GET(s): s.send_response(200); s.send_header("Content-Length", "2"); s.end_headers(); s.wfile.write(b"ok")
        def log_message(s, *a): pass
    srv = http.server.ThreadingHTTPServer(("127.0.0.1", 0), H)
    c = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER); c.load_cert_chain(crt, key)
    srv.socket = c.wrap_socket(srv.socket, server_side=True)
    threading.Thread(target=srv.serve_forever, daemon=True).start()
    yield f"https://localhost:{srv.server_address[1]}/", crt
    srv.shutdown(); srv.server_close()


@needs_openssl
def test_unknown_ca_is_rejected_not_bypassed(https_server, monkeypatch):
    url, _ = https_server
    monkeypatch.setattr(core, "_SSL_CTX", None); monkeypatch.delenv("SSL_CERT_FILE", raising=False)
    with pytest.raises(HttpError) as e:
        http_request(url, timeout=5)
    assert "certificate" in str(e.value).lower() or "ssl" in str(e.value).lower()
    assert "TLS" in core.diagnose_network_error(e.value) or "сертификат" in core.diagnose_network_error(e.value)


@needs_openssl
def test_corporate_root_via_ssl_cert_file_works(https_server, monkeypatch):
    url, crt = https_server
    monkeypatch.setattr(core, "_SSL_CTX", None); monkeypatch.setenv("SSL_CERT_FILE", str(crt))
    assert http_request(url, timeout=5) == b"ok"
    monkeypatch.setattr(core, "_SSL_CTX", None)
