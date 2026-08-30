# ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
# See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
"""Exercise the real replacement against a narrow unresolved-sound fixture.
Minecraft classes are unavailable locally; this does not test mixin application.
"""
import pathlib
import subprocess
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]

class SoundReplacementTest(unittest.TestCase):
    def test_unresolved_eating_sound_preserves_raw_levels_without_crashing(self):
        files = {
            'net/minecraft/resources/Identifier.java': '''package net.minecraft.resources;
public record Identifier(String value) { public static Identifier fromNamespaceAndPath(String n,String p){return new Identifier(n+":"+p);} public String toString(){return value;} }''',
            'net/minecraft/client/resources/sounds/SoundInstance.java': '''package net.minecraft.client.resources.sounds;
import net.minecraft.resources.Identifier;
public interface SoundInstance { Identifier getIdentifier(); Object getSource(); boolean isLooping(); float getVolume(); float getPitch(); double getX(); double getY(); double getZ(); Object getAttenuation(); boolean isRelative(); int getDelay(); static Object createUnseededRandom(){return new Object();} }''',
            'org/polyfrost/oneconfig/internal/mixin/reconfig/AbstractSoundAccessor.java': '''package org.polyfrost.oneconfig.internal.mixin.reconfig;
public interface AbstractSoundAccessor { float reconfig$rawVolume(); float reconfig$rawPitch(); }''',
            'net/minecraft/client/resources/sounds/AbstractSoundInstance.java': '''package net.minecraft.client.resources.sounds;
import net.minecraft.resources.Identifier;
import org.polyfrost.oneconfig.internal.mixin.reconfig.AbstractSoundAccessor;
public abstract class AbstractSoundInstance implements SoundInstance, AbstractSoundAccessor {
protected float volume=1,pitch=1; protected double x,y,z; protected Object attenuation; protected boolean relative,looping; protected int delay; private final Identifier id;
protected AbstractSoundInstance(Identifier id,Object source,Object random){this.id=id;}
public Identifier getIdentifier(){return id;} public Object getSource(){return null;}
public boolean isLooping(){return looping;} public float getVolume(){throw new NullPointerException("sound not resolved");} public float getPitch(){throw new NullPointerException("sound not resolved");}
public double getX(){return x;} public double getY(){return y;} public double getZ(){return z;} public Object getAttenuation(){return attenuation;} public boolean isRelative(){return relative;} public int getDelay(){return delay;}
public float reconfig$rawVolume(){return volume;} public float reconfig$rawPitch(){return pitch;}
}''',
            'org/polyfrost/oneconfig/internal/reconfig/ModuleAccess.java': '''package org.polyfrost.oneconfig.internal.reconfig;
public class ModuleAccess { public static boolean enabled(String id){return true;} public static String choice(String m,String s,String f){return f;} }''',
            'SoundTest.java': '''import net.minecraft.client.resources.sounds.*;
import net.minecraft.resources.Identifier;
import org.polyfrost.oneconfig.internal.reconfig.modules.BetterSounds;
import org.polyfrost.oneconfig.internal.mixin.reconfig.AbstractSoundAccessor;
public class SoundTest {
static class Eating extends AbstractSoundInstance { Eating(){super(new Identifier("minecraft:entity.generic.eat"),null,null);volume=.4f;pitch=.8f;x=10;y=20;z=30;} }
public static void main(String[] args){ SoundInstance s=BetterSounds.replace(new Eating());
if(!s.getIdentifier().toString().equals("reconfig:gameplay.eating"))throw new AssertionError("replacement missing");
if(((AbstractSoundAccessor)s).reconfig$rawVolume()!=.4f || ((AbstractSoundAccessor)s).reconfig$rawPitch()!=.8f)throw new AssertionError("levels lost");
if(s.getX()!=10 || s.getY()!=20 || s.getZ()!=30)throw new AssertionError("position lost");
if(BetterSounds.replace(s)!=s)throw new AssertionError("recursive replacement"); }
}'''
        }
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            for name, content in files.items():
                path = root / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(content)
            source = ROOT / 'minecraft/src/main/java/org/polyfrost/oneconfig/internal/reconfig/modules/BetterSounds.java'
            subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-d',directory,*map(str,root.rglob('*.java')),str(source)],check=True,capture_output=True,text=True)
            result = subprocess.run(['java','-cp',directory,'SoundTest'],capture_output=True,text=True)
            self.assertEqual(result.returncode,0,result.stderr)

if __name__ == '__main__': unittest.main()
