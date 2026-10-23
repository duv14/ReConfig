"""Run the production verification retry gate with a deterministic clock."""
from pathlib import Path
import subprocess
import tempfile
ROOT = Path(__file__).resolve().parents[1]
def test_verification_rate_limit_cannot_be_retried_by_ui_or_background_poll():
    source = ROOT / 'modules/internal/src/main/java/org/polyfrost/oneconfig/internal/reconfig/EnrollmentRetry.java'
    harness = '''import java.util.concurrent.atomic.AtomicLong;
import org.polyfrost.oneconfig.internal.reconfig.EnrollmentRetry;
class EnrollmentProbe {
 public static void main(String[] args) {
  AtomicLong now = new AtomicLong(1000); EnrollmentRetry gate = new EnrollmentRetry(now::get);
  gate.beforeAttempt();
  RuntimeException wrapped = new RuntimeException(new Exception("RateLimiter disallowed request"));
  String message = gate.failed(wrapped).getMessage();
  if(!message.contains("Minecraft") || !message.contains("automatically"))throw new AssertionError(message);
  for(int i=0;i<100;i++){try{gate.beforeAttempt();throw new AssertionError("poll bypassed cooldown");}catch(IllegalStateException expected){}}
  now.addAndGet(299999);try{gate.beforeAttempt();throw new AssertionError("early retry");}catch(IllegalStateException expected){}
  now.incrementAndGet(); gate.beforeAttempt();gate.failed(wrapped);
  now.addAndGet(300000);try{gate.beforeAttempt();throw new AssertionError("backoff not increasing");}catch(IllegalStateException expected){}
  now.addAndGet(300000);gate.beforeAttempt();gate.succeeded();gate.beforeAttempt();
  String generic=gate.failed(new Exception("secret-should-not-be-shown")).getMessage();
  if(generic.contains("secret"))throw new AssertionError("raw exception leaked");
  gate.reset();gate.beforeAttempt();
 }
}'''
    with tempfile.TemporaryDirectory() as directory:
        probe = Path(directory) / 'EnrollmentProbe.java'
        probe.write_text(harness)
        compiled = subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-d',directory,str(source),str(probe)],capture_output=True,text=True)
        assert compiled.returncode == 0, compiled.stderr
        run = subprocess.run(['java','-cp',directory,'EnrollmentProbe'],capture_output=True,text=True)
        assert run.returncode == 0, run.stderr
