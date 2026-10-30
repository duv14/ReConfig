import org.polyfrost.oneconfig.internal.reconfig.*;
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindRecordingBus;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import java.util.concurrent.atomic.AtomicInteger;

public class ModuleCaptureIntegrationTest {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        var capture=KeyCapture.INSTANCE;
        var zoom=ModuleCatalog.INSTANCE.byId("zoom");
        capture.begin("zoom","activation_key");
        check(KeyCapture.consumeMouse(3,1),"B4 press consumed");
        check(zoom.key("activation_key")==-97,"actual persisted zoom bind is mouse4");
        check(capture.label(zoom.key("activation_key")).equals("Mouse 4"),"mouse label");
        check(!KeyCapture.isRecording()&&KeyCapture.consumeMouse(3,0),"finish then drain release");
        check(!KeyCapture.consumeMouse(3,1),"normal B4 works outside capture");
        capture.begin("zoom","activation_key");
        KeyCapture.consume(256,1);KeyCapture.consume(256,0);
        check(zoom.key("activation_key")==-97,"escape cancels without overwriting");
        capture.begin("zoom","activation_key");
        KeyCapture.consume(259,1);KeyCapture.consume(259,0);
        check(zoom.key("activation_key")==0,"backspace clears");
        capture.begin("zoom","activation_key");
        KeyCapture.consume(300,1);KeyCapture.consume(300,0);
        check(zoom.key("activation_key")==300&&!KeyCapture.getBlocksText(),"F11 records and restores text after release");
        AtomicInteger mouse=new AtomicInteger(-1);
        Function0<Unit> escape=()->Unit.INSTANCE;
        Function1<Integer,Unit> handler=button->{mouse.set(button);return Unit.INSTANCE;};
        KeybindRecordingBus.INSTANCE.setEscapeHandler(escape);
        KeybindRecordingBus.INSTANCE.setMouseHandler(handler);
        check(KeybindRecordingBus.consumeMouse(4,1)&&mouse.get()==4,"generic recording receives B5");
        check(!KeybindRecordingBus.isRecording()&&KeybindRecordingBus.consumeMouse(4,0),"generic mouse capture finishes immediately and drains release");
        check(!KeybindRecordingBus.consumeMouse(4,1),"generic navigation restored");
        System.out.println("PASS: packaged module capture saves B4, labels, escape/clear/F11 and generic mouse recording without player preferences");
    }
}
