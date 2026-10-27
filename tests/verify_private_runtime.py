"""Validate release packaging recursively; accepts a finished distribution JAR."""
from zipfile import ZipFile
from io import BytesIO
from pathlib import Path
import json,sys

def verify(path):
    classes={};modids={};metadata=[]
    def visit(data,location):
        with ZipFile(BytesIO(data)) as jar:
            assert jar.testzip() is None, ('Corrupt archive',location)
            for name in jar.namelist():
                if name.endswith('.jar'):visit(jar.read(name),location+'!'+name)
                elif name.endswith('.class'):classes.setdefault(name,[]).append((location,jar.read(name)))
                elif name=='fabric.mod.json':
                    meta=json.loads(jar.read(name));metadata.append(meta);modids.setdefault(meta['id'],[]).append((location,meta['version']))
    visit(Path(path).read_bytes(),str(path))
    scene='dev/duv14/reconfig/shaded/androidx/compose/ui/scene/CanvasLayersComposeScene_skikoKt.class'
    assert scene in classes,'Private Compose scene missing: foreign mods can still replace the runtime'
    public=[name for name in classes if name.startswith(('androidx/','org/jetbrains/compose/'))]
    assert not public,('Unisolated runtime classes',public[:5])
    stale=[name for name,entries in classes.items() if any(
        b'Landroidx/' in data or b'Lorg/jetbrains/compose/' in data for _,data in entries)]
    assert not stale,('Public runtime descriptors remain in callers or annotations',stale[:5])
    duplicates={name:entries for name,entries in modids.items() if
                (name.startswith('reconfig_rt_') and len(entries)>1) or
                len({version for _,version in entries})>1}
    assert not duplicates,('Duplicate nested mod IDs',duplicates)
    assert 'org/jetbrains/skiko/Library.class' in classes,'JNI names must stay intact'
    renderer='org_jetbrains_skiko_skiko-awt'
    version=modids[renderer][0][1]
    assert any(meta['id'].startswith('reconfig_rt_') and
               meta.get('depends',{}).get(renderer)=='='+version for meta in metadata), 'Private UI must require its matching native renderer ABI'
    screen='org/polyfrost/oneconfig/internal/ui/compose/ComposeScreen.class'
    assert b'dev/duv14/reconfig/shaded/androidx/compose' in classes[screen][0][1]
    race='org/polyfrost/oneconfig/internal/mixin/skia/Mixin_FixComposeRaceCondition.class'
    assert b'Ldev/duv14/reconfig/shaded/androidx/compose/ui/platform/GlobalSnapshotManager;ensureStarted()V' in classes[race][0][1], 'Mixin member selectors must follow the private runtime'
    assert b'Landroidx/compose/' not in classes[race][0][1], 'Stale public Compose mixin target'
    print('PASS: private Compose classes, rewritten caller, unique nested libraries, preserved Skiko JNI namespace')
    return classes

if __name__=='__main__':verify(sys.argv[1])
