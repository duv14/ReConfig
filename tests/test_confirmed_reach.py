import pathlib
import subprocess
import tempfile
import unittest
ROOT = pathlib.Path(__file__).resolve().parents[1]
class ConfirmedReachTest(unittest.TestCase):
    def test_confirmation_expiry_and_invalid_samples(self):
        source = ROOT / 'minecraft/src/main/java/org/polyfrost/oneconfig/internal/reconfig/modules/ConfirmedReach.java'
        with tempfile.TemporaryDirectory() as tmp:
            harness = pathlib.Path(tmp) / 'ReachTest.java'
            harness.write_text("""import org.polyfrost.oneconfig.internal.reconfig.modules.ConfirmedReach;
class ReachTest {
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args){
  ConfirmedReach r=new ConfirmedReach();
  r.attempt(2,3.1,100);check(!r.visible(100,3000));
  r.confirm(2,3,1,110);check(!r.visible(110,3000));
  r.confirm(4,1,1,120);check(!r.visible(120,3000));
  r.confirm(2,1,1,130);check(r.visible(130,3000));check(r.distance()==3.1);
  r.confirm(2,1,1,200);check(!r.visible(3130,3000));
  r.clear();r.attempt(2,4,100);r.confirm(2,1,1,2200);check(!r.visible(2200,3000));
  r.attempt(2,Double.NaN,3000);r.confirm(2,1,1,3100);check(!r.visible(3100,3000));
  r.attempt(2,-1,3000);r.confirm(2,1,1,3100);check(!r.visible(3100,3000));
 }
}""")
            subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-d',tmp,str(harness),str(source)],check=True,capture_output=True,text=True)
            result=subprocess.run(['java','-cp',tmp,'ReachTest'],capture_output=True,text=True)
            self.assertEqual(result.returncode,0,result.stderr)
