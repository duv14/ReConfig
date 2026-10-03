package org.polyfrost.oneconfig.internal.reconfig;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Local-only waypoint storage. Failed writes never replace the in-memory list. */
public final class WaypointStore {
    public record Entry(String id, String world, String dimension, String name,
                        double x, double y, double z, int color, boolean visible) {
        public Entry {
            UUID.fromString(id);
            if (world == null || world.isBlank() || dimension == null || dimension.isBlank())
                throw new IllegalArgumentException("Join a world before creating waypoints");
            if (name == null || name.isBlank() || name.length() > 48 || name.chars().anyMatch(Character::isISOControl))
                throw new IllegalArgumentException("Name must contain 1–48 printable characters");
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || Math.abs(x)>30_000_000 || Math.abs(z)>30_000_000 || Math.abs(y)>2048)
                throw new IllegalArgumentException("Coordinates are outside the supported world range");
        }
    }
    private final Path file;
    private List<Entry> entries = List.of();
    public WaypointStore(Path file) throws IOException {
        this.file = file.toAbsolutePath();
        if (!Files.exists(file)) return;
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(file)) { p.load(r); }
        try {
            if (!"1".equals(p.getProperty("version"))) throw new IllegalArgumentException("Unknown waypoint format");
            int count = Integer.parseInt(p.getProperty("count"));
            if (count<0 || count>256) throw new IllegalArgumentException("Invalid waypoint count");
            List<Entry> loaded = new ArrayList<>();
            Set<String> ids = new HashSet<>();
            for(int i=0;i<count;i++) {
                String k=i+".";
                Entry e=new Entry(p.getProperty(k+"id"),p.getProperty(k+"world"),p.getProperty(k+"dimension"),p.getProperty(k+"name"),
                    Double.parseDouble(p.getProperty(k+"x")),Double.parseDouble(p.getProperty(k+"y")),Double.parseDouble(p.getProperty(k+"z")),
                    Integer.parseInt(p.getProperty(k+"color")),Boolean.parseBoolean(p.getProperty(k+"visible")));
                if(!ids.add(e.id()))throw new IllegalArgumentException("Duplicate waypoint ID");
                loaded.add(e);
            }
            entries=List.copyOf(loaded);
        } catch(RuntimeException e) { throw new IOException("Cannot read waypoints; original file preserved: "+file,e); }
    }
    public synchronized List<Entry> all() { return entries; }
    public synchronized List<Entry> visible(String world,String dimension) {
        return entries.stream().filter(e->e.visible() && e.world().equals(world) && e.dimension().equals(dimension)).toList();
    }
    public synchronized Entry add(String world,String dimension,String name,double x,double y,double z,int color) throws IOException {
        Entry e=new Entry(UUID.randomUUID().toString(),world,dimension,name.trim(),x,y,z,color,true);
        put(e); return e;
    }
    public synchronized void put(Entry entry) throws IOException {
        List<Entry> next=new ArrayList<>(entries);
        int found=-1;for(int i=0;i<next.size();i++)if(next.get(i).id().equals(entry.id()))found=i;
        if(found>=0)next.set(found,entry);else next.add(entry);
        if(next.size()>256)throw new IllegalArgumentException("Maximum 256 waypoints; delete an old marker first");
        save(next);
    }
    public synchronized void remove(String id) throws IOException { save(entries.stream().filter(e->!e.id().equals(id)).toList()); }
    private void save(List<Entry> next) throws IOException {
        Properties p=new Properties();p.setProperty("version","1");p.setProperty("count",Integer.toString(next.size()));
        for(int i=0;i<next.size();i++) {
            Entry e=next.get(i);String k=i+".";
            p.setProperty(k+"id",e.id());p.setProperty(k+"world",e.world());p.setProperty(k+"dimension",e.dimension());p.setProperty(k+"name",e.name());
            p.setProperty(k+"x",Double.toString(e.x()));p.setProperty(k+"y",Double.toString(e.y()));p.setProperty(k+"z",Double.toString(e.z()));
            p.setProperty(k+"color",Integer.toString(e.color()));p.setProperty(k+"visible",Boolean.toString(e.visible()));
        }
        Files.createDirectories(file.getParent());
        Path temp=Files.createTempFile(file.getParent(),"waypoints-",".tmp");
        try {
            try(Writer w=Files.newBufferedWriter(temp)){p.store(w,"ReConfig waypoints — duv14; built on Polyfrost OneConfig");}
            try{Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}
            entries=List.copyOf(next);
        } finally { Files.deleteIfExists(temp); }
    }
}
