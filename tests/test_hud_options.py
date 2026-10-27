"""Exercise production HUD filtering, timer formatting and safe performance limits."""
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]

class HudOptionsTest(unittest.TestCase):
    def test_filters_timers_and_caps(self):
        with tempfile.TemporaryDirectory() as output:
            source = ROOT / 'minecraft/src/main/java/org/polyfrost/oneconfig/internal/reconfig/modules/HudOptions.java'
            subprocess.run(['java', '-m', 'jdk.compiler/com.sun.tools.javac.Main', '-encoding', 'UTF-8', '-d', output,
                            str(source), str(ROOT / 'tests/HudOptionsTest.java')], check=True, capture_output=True, text=True)
            result = subprocess.run(['java', '-cp', output, 'HudOptionsTest'], capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
