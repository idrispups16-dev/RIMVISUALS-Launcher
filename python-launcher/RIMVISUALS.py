"""RIMVISUALS launcher. Open this file in IDLE and press F5 (Run > Run Module), or double-click it.

Needs Python 3.12+ (tested with 3.14) with tkinter, which is included in the python.org installer.
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
if HERE not in sys.path:
    sys.path.insert(0, HERE)

if __name__ == "__main__":
    from rimvisuals_launcher.cli import main
    # IDLE passes no arguments: open the window. From a terminal all CLI commands work as well.
    sys.exit(main(sys.argv[1:]))
