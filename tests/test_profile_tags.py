"""Exercise the actual nonblocking public-tag cache without Minecraft or network access."""
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]

class ProfileTagCacheTests(unittest.TestCase):
    def test_deduplicates_reads_expires_results_and_keeps_private_tags_local(self):
        source = ROOT / "modules/internal/src/main/java/org/polyfrost/oneconfig/internal/reconfig/ProfileTagCache.java"
        harness = r'''import java.util.*;
import java.util.concurrent.atomic.*;
import org.polyfrost.oneconfig.internal.reconfig.ProfileTagCache;
class TagCacheProbe {
 public static void main(String[] args) {
  String self="0".repeat(32), peer="1".repeat(32), next="2".repeat(32);
  List<Runnable> jobs=new ArrayList<>(); AtomicLong clock=new AtomicLong(); AtomicInteger reads=new AtomicInteger();
  AtomicReference<ProfileTagCache.Tag> answer=new AtomicReference<>(new ProfileTagCache.Tag("ABCDE","rose"));
  ProfileTagCache cache=new ProfileTagCache(clock::get,jobs::add,id->{reads.incrementAndGet();return answer.get();});
  if(cache.lookup(peer)!=null || !jobs.isEmpty())throw new AssertionError("lookup before identity");
  cache.identity(self);
  if(cache.lookup(peer)!=null || jobs.size()!=1)throw new AssertionError("nonblocking first lookup");
  for(int i=0;i<20;i++)cache.lookup(peer);
  if(jobs.size()!=1)throw new AssertionError("per-frame requests duplicated");
  jobs.remove(0).run();
  if(!cache.lookup(peer).text().equals("ABCDE") || reads.get()!=1 || !jobs.isEmpty())throw new AssertionError("positive cache");
  cache.own(self,new ProfileTagCache.Tag("LOCAL",""));
  if(!cache.lookup(self).text().equals("LOCAL") || !jobs.isEmpty())throw new AssertionError("private own tag caused public read");
  clock.set(300001); answer.set(null); cache.lookup(peer); jobs.remove(0).run();
  if(cache.lookup(peer)!=null || !jobs.isEmpty())throw new AssertionError("withdrawn/private tag retained");
  clock.addAndGet(599999); cache.lookup(peer);
  if(!jobs.isEmpty())throw new AssertionError("negative cache expired early");
  clock.addAndGet(2); cache.lookup(peer);
  int before=reads.get(); cache.identity(next); jobs.remove(0).run();
  if(reads.get()!=before || cache.lookup(next)!=null)throw new AssertionError("identity switch leaked private or queued state");
  if(cache.lookup("bad-id")!=null || !jobs.isEmpty())throw new AssertionError("invalid identity queried");
  try {new ProfileTagCache.Tag("ABCDEF","");throw new AssertionError("six-character tag accepted");}catch(IllegalArgumentException expected){}
  for(int i=3;i<70;i++)cache.lookup(String.format("%032x",i));
  if(jobs.size()>32)throw new AssertionError("unbounded render queue");
 }
}'''
        with tempfile.TemporaryDirectory() as directory:
            test = Path(directory) / "TagCacheProbe.java"
            test.write_text(harness, encoding="utf-8")
            compiled = subprocess.run(["java", "-m", "jdk.compiler/com.sun.tools.javac.Main", "-d", directory, str(source), str(test)], capture_output=True, text=True)
            self.assertEqual(compiled.returncode, 0, compiled.stderr)
            result = subprocess.run(["java", "-cp", directory, "TagCacheProbe"], capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr)
