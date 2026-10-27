package reconfig.tests;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.metadata.*;
import net.fabricmc.loader.impl.discovery.ModCandidateImpl;
import net.fabricmc.loader.impl.discovery.ModResolver;

/** Use Fabric's real solver, rather than Java classpath order, for renderer selection. */
public final class FabricRuntimeSelectionTest {
    static final VersionOverrides versions = new VersionOverrides();
    static final DependencyOverrides dependencies = new DependencyOverrides(Path.of("."));
    static final List<ModCandidateImpl> candidates = new ArrayList<>();
    static ModCandidateImpl candidate(String json, String name, List<ModCandidateImpl> nested, boolean root) throws Exception {
        nested=new ArrayList<>(nested);
        var metadata = ModMetadataParser.parseMetadata(new ByteArrayInputStream(json.getBytes(java.nio.charset.StandardCharsets.UTF_8)), name, List.of(), versions, dependencies, false);
        var create=ModCandidateImpl.class.getDeclaredMethod(root?"createPlain":"createNested",
                root?new Class<?>[]{List.class,LoaderModMetadata.class,boolean.class,Collection.class}
                    :new Class<?>[]{String.class,long.class,LoaderModMetadata.class,boolean.class,Collection.class});
        create.setAccessible(true);
        var mod=(ModCandidateImpl)create.invoke(null,root?new Object[]{List.of(Path.of(name)),metadata,false,nested}
                :new Object[]{name,(long)name.hashCode(),metadata,false,nested});
        var addParent=ModCandidateImpl.class.getDeclaredMethod("addParent",ModCandidateImpl.class);
        addParent.setAccessible(true);
        for(var child:nested)addParent.invoke(child,mod);
        candidates.add(mod);
        return mod;
    }
    static ModCandidateImpl archive(byte[] bytes, String name, boolean root) throws Exception {
        String metadata=null;
        var nested=new ArrayList<ModCandidateImpl>();
        try(var zip=new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for(ZipEntry entry;(entry=zip.getNextEntry())!=null;) {
                if(entry.getName().equals("fabric.mod.json"))metadata=new String(zip.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
                else if(entry.getName().endsWith(".jar")) {
                    var child=archive(zip.readAllBytes(),name+"!"+entry.getName(),false);
                    if(child!=null)nested.add(child);
                }
            }
        }
        if(metadata==null)return null; // API shim resources are not Fabric mods.
        return candidate(metadata,name,nested,root);
    }
    static ModCandidateImpl simple(String id,String version,boolean root) throws Exception {
        return candidate("{\"schemaVersion\":1,\"id\":\""+id+"\",\"version\":\""+version+"\"}",id+".jar",List.of(),root);
    }
    public static void main(String[] args) throws Exception {
        archive(Files.readAllBytes(Path.of(args[0])),args[0],true);
        // The foreign bundle/version and renderer match the affected player's log.
        var foreignRenderer=simple("org_jetbrains_skiko_skiko-awt","0.154.0",false);
        var foreignNative=simple("org_jetbrains_skiko_skiko-awt-runtime-windows-x64","0.154.0",false);
        candidate("{\"schemaVersion\":1,\"id\":\"compose-multiplatform\",\"version\":\"1.0.6+compose.1.13.0-alpha02.fabric\"}","foreign-compose.jar",List.of(foreignRenderer,foreignNative),true);
        simple("minecraft","1.21.11",true);
        simple("java","21",true);
        simple("fabricloader","0.19.5",true);
        simple("fabric-api","0.141.6+1.21.11",true);
        simple("fabric-language-kotlin","1.14.2+kotlin.2.4.21",true);
        var selected=ModResolver.resolve(candidates,EnvType.CLIENT,new HashMap<>());
        var renderer=selected.stream().filter(m->m.getId().equals("org_jetbrains_skiko_skiko-awt")).findFirst().orElseThrow();
        if(!renderer.getVersion().getFriendlyString().equals("0.999.5"))throw new AssertionError("Wrong renderer selected: "+renderer);
        var nativeRenderer=selected.stream().filter(m->m.getId().equals("org_jetbrains_skiko_skiko-awt-runtime-windows-x64")).findFirst().orElseThrow();
        if(!nativeRenderer.getVersion().getFriendlyString().equals("0.999.5"))throw new AssertionError("Wrong native renderer selected: "+nativeRenderer);
        if(selected.stream().noneMatch(m->m.getId().equals("compose-multiplatform")))throw new AssertionError("Foreign Compose displaced");
        if(selected.stream().noneMatch(m->m.getId().startsWith("reconfig_rt_")&&m.getMetadata().getName().contains("Compose")))throw new AssertionError("Private runtime missing");
        System.out.println("PASS: Fabric selects ReConfig's matching Skiko 0.999.5 alongside foreign Compose 1.13");
    }
}
