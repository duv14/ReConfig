import org.polyfrost.oneconfig.internal.reconfig.BindingInput;
import org.polyfrost.oneconfig.internal.reconfig.modules.ParticleRenderSettings;

public class HotfixInputParticleTest {
    static void check(boolean condition,String why){if(!condition)throw new AssertionError(why);}
    public static void main(String[] args){
        int side=BindingInput.mouseCode(3);
        check(side<0 && BindingInput.mouseButton(side)==3,"B4 round-trips separately from keyboard keys");
        check(BindingInput.normalize(side)==side,"mouse binds survive persistence validation");
        check(BindingInput.normalize(66)==66 && BindingInput.normalize(-1)==0,"keyboard and unknown codes");
        var gate=new BindingInput.CaptureGate();
        check(gate.consume(side,1,true),"recording owns the side-button press before navigation");
        check(gate.blocksText(),"captured held input keeps text/actions suppressed");
        check(gate.consume(side,2,false),"repeat does not leak after recording commits");
        check(gate.consume(side,0,false),"recording owns release even after it ends");
        check(!gate.blocksText() && !gate.consume(side,1,false),"normal navigation resumes after release");
        check(gate.consume(300,1,true) && gate.consume(300,0,false),"function keys are captured without global shortcuts");
        ParticleRenderSettings.update(false,300,0);
        var state=ParticleRenderSettings.state();
        check(state.scale(2)==2 && state.tint(0x80808080)==0x80808080 && !state.invisible(),"disabled particles are untouched");
        ParticleRenderSettings.update(true,150,50);
        state=ParticleRenderSettings.state();
        check(state.scale(2)==3 && state.tint(0x80808080)==0x40808080,"size and existing alpha combine correctly");
        for(int i=0;i<1000;i++)ParticleRenderSettings.update(true,150,50);
        check(ParticleRenderSettings.state()==state,"unchanged settings reuse the snapshot");
        ParticleRenderSettings.update(true,Float.NaN,Float.POSITIVE_INFINITY);
        check(ParticleRenderSettings.state().scale(2)==2 && ParticleRenderSettings.state().tint(0x80808080)==0x80808080,"invalid settings never reach particle vertices");
        ParticleRenderSettings.update(true,999,-20);
        check(ParticleRenderSettings.state().scale(2)==6 && ParticleRenderSettings.state().invisible(),"bounds and fully invisible particles");
        System.out.println("PASS: mouse persistence, exclusive input/release drain, particle transforms, finite values and snapshot reuse");
    }
}
