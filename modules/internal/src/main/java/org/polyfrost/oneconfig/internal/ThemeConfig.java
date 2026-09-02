/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal;

import org.polyfrost.compose.render.PolyColor;
import org.polyfrost.oneconfig.api.config.v1.Config;
import org.polyfrost.oneconfig.api.config.v1.annotations.Color;
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch;
import org.polyfrost.oneconfig.api.config.v1.annotations.Text;

public class ThemeConfig extends Config {
    @Text(title = "Active Theme")
    public static String activeTheme = "PolyGlass Dark";

    @Color(title = "Accent Color", description = "The accent color of the ReConfig Interface", icon = "paintbrush")
    public static PolyColor accentColor = new PolyColor(0xFF406CAB);

    @Switch(title = "Animations", description = "Smooth, clean animations in the ReConfig Interface", icon = "refresh")
    public static boolean animations = true;

    public ThemeConfig() {
        super("themes.json", "assets/oneconfig/brand/reconfig-logo.png", "Themes", Category.QOL);
    }

    @Override
    protected void initialize(boolean byConfigManager) {
        super.initialize(byConfigManager);
        hideIf("activeTheme", () -> true);
    }
}
