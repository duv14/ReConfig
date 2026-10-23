"""Exercise notification state independently of Minecraft and network services."""
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


class SocialSyncTest(unittest.TestCase):
    def test_bootstrap_restart_monotonic_cursors_and_sender_identity(self):
        source = ROOT / 'modules/internal/src/main/java/org/polyfrost/oneconfig/internal/reconfig/SocialSyncState.java'
        with tempfile.TemporaryDirectory() as directory:
            harness = Path(directory) / 'SocialSyncTest.java'
            harness.write_text('''import org.polyfrost.oneconfig.internal.reconfig.SocialSyncState;
public class SocialSyncTest {
 static void check(boolean value) { if (!value) throw new AssertionError(); }
 public static void main(String[] args) {
  SocialSyncState state = new SocialSyncState();
  check(state.path().equals("/v2/sync?bootstrap=1"));
  check(!state.isReady());
  try { state.advance(1,2,3,4); throw new AssertionError(); }
  catch (IllegalStateException expected) {}
  state.bootstrap(200,150,50,20);
  check(state.path().equals("/v2/sync?after=200&afterRequest=150&afterDeletion=50&afterInvitation=20"));
  state.advance(199,155,51,19);
  check(state.getMessages()==200 && state.getRequests()==155);
  check(state.getDeletions()==51 && state.getInvitations()==20);
  state.advance(201,155,51,21);
  check(state.getMessages()==201 && state.getInvitations()==21);
  state.reset();
  check(!state.isReady() && state.path().equals("/v2/sync?bootstrap=1"));
  // Every startup takes a server baseline, including a new installation with no saved cursors.
  state.bootstrap(900,300,100,60);
  check(state.getMessages()==900 && state.getInvitations()==60);
  String self="0123456789abcdef0123456789abcdef", peer="fedcba9876543210fedcba9876543210";
  check(SocialSyncState.validIdentity("Player_1",self));
  check(!SocialSyncState.validIdentity("Player", ""));
  check(!SocialSyncState.validIdentity("bad name", self));
  check(!SocialSyncState.validIdentity("Player", "offline"));
  check(SocialSyncState.sentByMe(self,peer,self,peer));
  check(!SocialSyncState.sentByMe(self,peer,peer,self));
  check(SocialSyncState.inConversation(self,peer,peer,self));
  check(!SocialSyncState.inConversation(self,peer,"stranger",self));
  check(!SocialSyncState.inConversation(self,peer,self,"stranger"));
 }
}''', encoding='utf-8')
            compiled = subprocess.run(['java', '-m', 'jdk.compiler/com.sun.tools.javac.Main', '-d', directory, str(source), str(harness)], capture_output=True, text=True)
            self.assertEqual(compiled.returncode, 0, compiled.stderr)
            result = subprocess.run(['java', '-cp', directory, 'SocialSyncTest'], capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr)


if __name__ == '__main__':
    unittest.main()
