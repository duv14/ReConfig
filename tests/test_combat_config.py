# ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
# See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
"""Executable tests of the integrated category/role models, independent of Minecraft."""
import pathlib
import subprocess
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]

class CombatConfigTests(unittest.TestCase):
    def test_categories_colors_membership_and_flash_expiry(self):
        sources = ROOT / 'modules/internal/src/main/java/org/polyfrost/oneconfig/internal/reconfig/combat'
        with tempfile.TemporaryDirectory() as directory:
            result = subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-d',directory,
                                     *map(str,sources.glob('*.java')),str(ROOT/'tests/CombatConfigTest.java')],capture_output=True,text=True)
            self.assertEqual(result.returncode,0,result.stderr)
            result = subprocess.run(['java','-cp',directory,'CombatConfigTest'],capture_output=True,text=True)
            self.assertEqual(result.returncode,0,result.stderr)
