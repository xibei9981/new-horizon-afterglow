import arc.*;
import arc.backend.headless.HeadlessApplication;
import arc.files.Fi;
import arc.util.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.net.Net;
import mindustry.mod.Mod;
import mindustry.ui.*;
import static mindustry.Vars.*;

public class CampaignHarness implements ApplicationListener {
    public static void main(String[] args) {
        Vars.platform = new Platform(){};
        Vars.net = new Net(platform.getNet());
        new HeadlessApplication(new CampaignHarness(), t -> {t.printStackTrace(); System.exit(1);});
    }
    public void init() {
        try {
            Core.settings.setDataDirectory(Core.files.local("campaign-tools/run"));
            loadLocales = false;
            headless = true;
            Vars.loadSettings();
            Vars.init();
            UI.loadColors();
            Fonts.loadContentIconsHeadless();
            content.createBaseContent();
            mods.loadScripts();
            content.createModContent();
            content.init();
            if(mods.hasContentErrors()) throw new IllegalStateException("Mod content errors");
            bases.load();
            logic = new Logic();
            netServer = new NetServer();
            mods.eachClass(Mod::init);
            System.out.println("BOOT_OK blocks="+content.blocks().size+" units="+content.units().size);
            if(System.getProperty("campaign.frontier") != null) mods.getMod("new-horizon").loader.loadClass("FrontierMaps").getMethod("generateAll").invoke(null);
            if(System.getProperty("campaign.generate") != null) mods.getMod("new-horizon").loader.loadClass("CampaignMaps").getMethod("generateAll").invoke(null);
            if(System.getProperty("campaign.verify") != null) mods.getMod("new-horizon").loader.loadClass("CampaignChecks").getMethod("run").invoke(null);
            if(System.getProperty("campaign.frontierVerify") != null) mods.getMod("new-horizon").loader.loadClass("FrontierChecks").getMethod("run").invoke(null);
            if(System.getProperty("campaign.lateVerify") != null) mods.getMod("new-horizon").loader.loadClass("FrontierChecks").getMethod("lateFinale").invoke(null);
            System.exit(0);
        } catch(Throwable t) {t.printStackTrace(); System.exit(1);}
    }
}
