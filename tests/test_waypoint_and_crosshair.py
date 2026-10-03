# ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
# See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
"""Compile/run production math and waypoint persistence without Minecraft dependencies."""
import pathlib
import subprocess
import tempfile
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]

class PureModuleTests(unittest.TestCase):
    def test_persistence_projection_and_crosshair_mask(self):
        names=['WaypointStore','WaypointProjection','CrosshairPattern']
        with tempfile.TemporaryDirectory() as tmp:
            sources=[str(ROOT/'modules/internal/src/main/java/org/polyfrost/oneconfig/internal/reconfig'/f'{n}.java') for n in names]
            tests=[str(ROOT/'tests'/f'{n}Test.java') for n in names]
            result=subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-d',tmp,*sources,*tests],capture_output=True,text=True)
            self.assertEqual(result.returncode,0,result.stderr)
            for name in names:
                run=subprocess.run(['java','-cp',tmp,name+'Test',tmp],capture_output=True,text=True)
                self.assertEqual(run.returncode,0,run.stderr)
