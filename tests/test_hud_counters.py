# ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
# See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
"""Runs real CPS/combo logic without Minecraft or Gradle dependencies."""
import pathlib
import shutil
import subprocess
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]


class HudCountersTest(unittest.TestCase):
    def test_click_window_and_confirmed_damage_streak(self):
        java = shutil.which("java") or "/usr/lib/jvm/java-17-openjdk-amd64/bin/java"
        package = "org/polyfrost/oneconfig/internal/reconfig/modules"
        sources = [ROOT / "minecraft/src/main/java" / package / "HudCounters.java",
                   ROOT / "minecraft/src/test/java" / package / "HudCountersTest.java"]
        with tempfile.TemporaryDirectory() as directory:
            subprocess.run([java, "-m", "jdk.compiler/com.sun.tools.javac.Main", "-d", directory,
                            *map(str, sources)], check=True, capture_output=True, text=True)
            result = subprocess.run([java, "-ea", "-cp", directory,
                                     package.replace("/", ".") + ".HudCountersTest"],
                                    capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr)


if __name__ == "__main__":
    unittest.main()
