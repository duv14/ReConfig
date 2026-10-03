# ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
# See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
import pathlib
import subprocess
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]

class StreamerRedactionTest(unittest.TestCase):
    def test_current_server_address_is_hidden_without_hiding_unrelated_text(self):
        source = ROOT / 'minecraft/src/main/java/org/polyfrost/oneconfig/internal/reconfig/modules/AddressRedactor.java'
        self.assertTrue(source.exists(), 'address redactor missing')
        with tempfile.TemporaryDirectory() as directory:
            harness = pathlib.Path(directory) / 'RedactorTest.java'
            harness.write_text('''import org.polyfrost.oneconfig.internal.reconfig.modules.AddressRedactor;
public class RedactorTest { public static void main(String[] args){
if(!AddressRedactor.mask("Join PLAY.EXAMPLE.NET:25565", "play.example.net:25565").equals("Join [hidden server]"))throw new AssertionError();
if(!AddressRedactor.mask("Address: play.example.net", "play.example.net:25565").equals("Address: [hidden server]"))throw new AssertionError();
if(!AddressRedactor.mask("Play with friends", "").equals("Play with friends"))throw new AssertionError();
if(!AddressRedactor.mask("Friends", "play.example.net").equals("Friends"))throw new AssertionError();
if(!AddressRedactor.mask("[::1]:25565", "[::1]:25565").equals("[hidden server]"))throw new AssertionError();
}}''')
            result = subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-d',directory,str(source),str(harness)],capture_output=True,text=True)
            self.assertEqual(result.returncode,0,result.stderr)
            result = subprocess.run(['java','-cp',directory,'RedactorTest'],capture_output=True,text=True)
            self.assertEqual(result.returncode,0,result.stderr)
