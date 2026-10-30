from pathlib import Path
import subprocess,tempfile,unittest
ROOT=Path(__file__).resolve().parents[1]
class InputParticleHotfix(unittest.TestCase):
    def test_input_lifecycle_and_particle_snapshot(self):
        with tempfile.TemporaryDirectory() as output:
            files=['tests/HotfixInputParticleTest.java','modules/internal/src/main/java/org/polyfrost/oneconfig/internal/reconfig/BindingInput.java','minecraft/src/main/java/org/polyfrost/oneconfig/internal/reconfig/modules/ParticleRenderSettings.java']
            subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-d',output,*[str(ROOT/f) for f in files]],check=True,capture_output=True,text=True)
            result=subprocess.run(['java','-cp',output,'HotfixInputParticleTest'],capture_output=True,text=True)
            self.assertEqual(result.returncode,0,result.stdout+result.stderr)
    def test_particle_render_hooks_never_read_preferences(self):
        code=(ROOT/'minecraft/src/main/java/org/polyfrost/oneconfig/internal/mixin/reconfig/Mixin_ReConfigParticles.java').read_text()
        self.assertNotIn('ModuleAccess',code)
        self.assertNotIn('Preferences',code)
        self.assertIn('ParticleRenderSettings.state()',code)
