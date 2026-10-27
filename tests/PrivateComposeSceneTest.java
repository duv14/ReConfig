import java.lang.reflect.*;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function2;
import org.jetbrains.skia.Surface;

/** A real scene is created with foreign public Compose earlier on the classpath. */
public class PrivateComposeSceneTest {
    public static void main(String[] args) throws Exception {
        String prefix="dev.duv14.reconfig.shaded.androidx.compose.ui.scene.";
        Class<?> factory=Class.forName(prefix+"CanvasLayersComposeScene_skikoKt");
        Method create=null;
        for(Method method:factory.getDeclaredMethods())
            if(method.getName().startsWith("CanvasLayersComposeScene-") && method.getName().endsWith("$default"))create=method;
        if(create==null)throw new AssertionError("Missing private scene factory");
        Object[] params=new Object[create.getParameterCount()];
        for(int i=0;i<params.length;i++) {
            Class<?> type=create.getParameterTypes()[i];
            if(type==int.class)params[i]=31;
            else if(type==Function0.class)params[i]=(Function0<Unit>)()->Unit.INSTANCE;
        }
        Object scene;
        try {scene=create.invoke(null,params);}
        catch(InvocationTargetException ex){throw new AssertionError("Private scene creation failed",ex.getCause());}
        if(scene==null)throw new AssertionError("No scene returned");
        Class<?> sceneType=Class.forName(prefix+"ComposeScene");
        sceneType.getMethod("setContent",Function2.class).invoke(scene,(Function2<Object,Integer,Unit>)(composer,flags)->Unit.INSTANCE);
        try(var surface=Surface.Companion.makeRasterN32Premul(64,64)) {
            Class<?> canvasType=Class.forName("dev.duv14.reconfig.shaded.androidx.compose.ui.graphics.Canvas");
            Class<?> adapter=Class.forName("dev.duv14.reconfig.shaded.androidx.compose.ui.graphics.SkiaBackedCanvas_skikoKt");
            Object canvas=adapter.getMethod("asComposeCanvas",org.jetbrains.skia.Canvas.class).invoke(null,surface.getCanvas());
            sceneType.getMethod("render",canvasType,long.class).invoke(scene,canvas,0L);
            sceneType.getMethod("render",canvasType,long.class).invoke(scene,canvas,16_666_667L);
        } finally { sceneType.getMethod("close").invoke(scene); }
        System.out.println("PASS: private CanvasLayersComposeScene created, composed, rendered through native Skia twice and closed with foreign Compose present");
        System.exit(0);
    }
}
