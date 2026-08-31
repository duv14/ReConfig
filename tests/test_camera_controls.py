# ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
# See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
"""Run the camera/input state machines without a Minecraft installation."""
import pathlib
import subprocess
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]

class CameraControlsTest(unittest.TestCase):
    def test_mouse_camera_uses_public_inversion_getters(self):
        source = (ROOT / 'minecraft/src/main/java/org/polyfrost/oneconfig/internal/mixin/reconfig/Mixin_ReConfigMouseCamera.java').read_text()
        self.assertIn('options.invertMouseX().get()', source)
        self.assertIn('options.invertMouseY().get()', source)
        self.assertNotIn('options.invertXMouse()', source)
        self.assertNotIn('options.invertYMouse()', source)

    def test_entrypoint_and_platform_require_fabric_api(self):
        import json
        for module in ('bootstrap', 'minecraft'):
            descriptor = json.loads((ROOT / module / 'src/main/resources/fabric.mod.json').read_text())
            self.assertIn('fabric-api', descriptor['depends'])

    def test_toggle_hook_is_scoped_to_key_input_not_saved_options(self):
        source = (ROOT / 'minecraft/src/main/java/org/polyfrost/oneconfig/internal/mixin/reconfig/Mixin_ReConfigToggleOptions.java').read_text()
        self.assertIn('@Mixin(ToggleKeyMapping.class)', source)
        self.assertNotIn('@Mixin(OptionInstance.class)', source)
        self.assertIn('BooleanSupplier', source)
        runtime = (ROOT / 'minecraft/src/main/kotlin/org/polyfrost/oneconfig/internal/reconfig/ReConfigModuleRuntime.kt').read_text()
        self.assertIn('reconfigResetToggle()', runtime)

    def test_hold_toggle_blocking_and_frame_independent_zoom(self):
        source = ROOT / 'minecraft/src/main/java/org/polyfrost/oneconfig/internal/reconfig/modules'
        self.assertTrue((source / 'ActivationState.java').exists(), 'activation controller missing')
        with tempfile.TemporaryDirectory() as directory:
            harness = pathlib.Path(directory) / 'CameraTest.java'
            harness.write_text('''import org.polyfrost.oneconfig.internal.reconfig.modules.*;
public class CameraTest {
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args){
  ActivationState state=new ActivationState();
  check(state.update(true,true,true,false));
  check(!state.update(true,false,true,false));
  check(state.update(true,true,false,false));
  check(state.update(true,true,false,false));
  check(state.update(true,false,false,false));
  check(!state.update(true,true,false,false));
  check(!state.update(true,true,false,true));
  check(!state.update(true,true,false,false));
  state.update(true,false,false,false);
  check(state.update(true,true,false,false));
  check(!state.update(false,true,false,false));
  SmoothZoom a=new SmoothZoom(), b=new SmoothZoom();
  for(int i=0;i<60;i++)a.step(.25,1.0/60,.15);
  for(int i=0;i<120;i++)b.step(.25,1.0/120,.15);
  check(Math.abs(a.value()-b.value())<1e-9);
  check(a.value()>=.25 && a.value()<.26);
  double before=a.value();a.step(1,1.0/60,.15);
  check(a.value()>before && a.value()<1);
  a.reset();check(a.value()==1);
  a.step(Double.NaN,0,.15);check(Double.isFinite(a.value()));
 }
}''')
            compiled = subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-d',directory,str(harness),str(source/'ActivationState.java'),str(source/'SmoothZoom.java')],capture_output=True,text=True)
            self.assertEqual(compiled.returncode,0,compiled.stderr)
            result = subprocess.run(['java','-cp',directory,'CameraTest'],capture_output=True,text=True)
            self.assertEqual(result.returncode,0,result.stderr)
