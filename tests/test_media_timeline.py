import pathlib
import subprocess
import tempfile
import unittest
ROOT = pathlib.Path(__file__).resolve().parents[1]
class MediaTimelineTest(unittest.TestCase):
    def test_play_pause_unknown_duration_and_stale_snapshot(self):
        source = ROOT / 'minecraft/src/main/java/org/polyfrost/oneconfig/internal/reconfig/modules/MediaTimeline.java'
        with tempfile.TemporaryDirectory() as tmp:
            harness = pathlib.Path(tmp) / 'MediaTest.java'
            harness.write_text("""import org.polyfrost.oneconfig.internal.reconfig.modules.MediaTimeline;
class MediaTest {
 static void check(boolean value){if(!value)throw new AssertionError();}
 public static void main(String[] args){
  check(MediaTimeline.position(30,100,true,1000,4500)==33);
  check(MediaTimeline.position(30,100,false,1000,4500)==30);
  check(MediaTimeline.position(99,100,true,1000,4500)==100);
  check(MediaTimeline.position(30,100,true,1000,1000000)==40);
  check(MediaTimeline.position(30,0,true,1000,4500)==33);
  check(MediaTimeline.position(30,100,true,1000,0)==30);
  check(MediaTimeline.progress(50,100)==.5);
  check(MediaTimeline.progress(50,0)==0);
  check(MediaTimeline.progress(150,100)==1);
  check(MediaTimeline.progress(-1,100)==0);
 }
}""",encoding='utf-8')
            subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-d',tmp,str(harness),str(source)],check=True,capture_output=True,text=True)
            result=subprocess.run(['java','-cp',tmp,'MediaTest'],capture_output=True,text=True)
            self.assertEqual(result.returncode,0,result.stderr)
