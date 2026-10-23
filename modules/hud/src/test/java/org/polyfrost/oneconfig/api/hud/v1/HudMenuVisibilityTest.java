package org.polyfrost.oneconfig.api.hud.v1;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;
class HudMenuVisibilityTest {
 private static void edit(boolean value)throws Exception {var field=HudManager.class.getDeclaredField("isEditorOpen");field.setAccessible(true);field.setBoolean(null,value);}
 @AfterEach void restore()throws Exception{HudManager.isConfigUiOpen=false;edit(false);HudManager.isGuiScreenOpen=false;HudManager.overrideShowInScreens=false;}
 @Test void menuSuppressesEvenHudsConfiguredToShowInScreens()throws Exception {
  var hud=new TextHud("menu-visibility-test","Test",Hud.Category.Companion.getINFO(),"",""){
   @Override public String getText(){return "Visible";}
   @Override public boolean isAvailable(){return true;}
  };
  hud.setShowInScreens(true);HudManager.overrideShowInScreens=true;HudManager.isGuiScreenOpen=true;
  var shown=HudManager.class.getDeclaredMethod("isShown",Hud.class);shown.setAccessible(true);
  HudManager.isConfigUiOpen=false;assertEquals(true,shown.invoke(HudManager.INSTANCE,hud));
  HudManager.isConfigUiOpen=true;assertEquals(false,shown.invoke(HudManager.INSTANCE,hud));
  edit(true);assertEquals(true,shown.invoke(HudManager.INSTANCE,hud));
  edit(false);assertEquals(false,shown.invoke(HudManager.INSTANCE,hud));
  HudManager.isConfigUiOpen=false;assertEquals(true,shown.invoke(HudManager.INSTANCE,hud));
 }
}
