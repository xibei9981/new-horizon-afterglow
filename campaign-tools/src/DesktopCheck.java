import arc.*;
import arc.files.Fi;
import arc.scene.ui.Dialog;
import arc.util.*;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.type.SectorPreset;
import mindustry.desktop.DesktopLauncher;

public class DesktopCheck {
    static int mission=Integer.getInteger("campaign.start",3);
    static final int end=Integer.getInteger("campaign.end",13);
    static final String[] ids={"supply","ridges","citadel","forge","isles","trident","hunt","storm","march","bastion","arsenal","blackout","daybreak","redgate","saltworks","triune"};
    public static void main(String[] args) {
        Events.on(EventType.ClientLoadEvent.class,e -> {
            Core.settings.put("backgroundpause",false);
            Core.settings.put("musicvol",0);
            Core.settings.put("sfxvol",0);
            if (!Core.bundle.get("afterglow.chapter.0").contains("余烬航线")) throw new IllegalStateException("Chinese campaign bundle did not load: " + Core.bundle.get("afterglow.chapter.0"));
            System.out.println("CLIENT_ZH_OK " + Core.bundle.get("afterglow.chapter.0"));
            var panel=Vars.content.block("new-horizon-photon-panel");
            if(panel==null||panel.techNode.requirements[0].amount!=6)throw new IllegalStateException("Research discount did not load");
            System.out.println("CLIENT_RESEARCH_OK explicit cost = 6");
            Core.settings.put("uiscale",100);
            Timer.schedule(DesktopCheck::load,8f);
        });
        DesktopLauncher.main(new String[0]);
    }
    static void load() {
        try {
            Core.scene.root.getChildren().copy().each(a->{if(a instanceof Dialog d)d.hide();});
            String name=ids[mission];
            SectorPreset p=Vars.content.sectors().find(s->s.name.equals("new-horizon-afterglow-"+name));
            if(p==null)throw new IllegalStateException("Missing sector "+name);
            p.quietUnlock();
            Vars.control.playSector(null,p.sector);
            Timer.schedule(() -> {
                Core.scene.root.getChildren().copy().each(a->{if(a instanceof Dialog d)d.hide();});
                Vars.renderer.setScale(1.5f);
                if(mission==14)System.out.println("CLIENT_CARGO_EARLY tile="+Vars.world.tile(434,214).block()+" build="+Vars.world.build(434,214));
                if(!"true".equals(Vars.state.rules.tags.get("afterglow.physical-logistics")))throw new IllegalStateException("Wrong campaign version");
                if(mission>=3&&mission<13&&Vars.state.rules.tags.getInt("frontier.relay",-1)<0)throw new IllegalStateException("Missing restoration site");
                if(mission>=3&&Vars.state.rules.attributes.get(mindustry.world.meta.Attribute.light)!=1f)throw new IllegalStateException("Planet overwrote chapter irradiance");
                if(Boolean.getBoolean("campaign.scenic")&&mission==4){
                    int at=Vars.state.rules.tags.getInt("frontier.relay",-1);
                    Vars.player.unit().set(arc.math.geom.Point2.x(at)*Vars.tilesize,arc.math.geom.Point2.y(at)*Vars.tilesize);
                }
                if(mission>=13){
                    if(!"0.5.0".equals(Vars.state.rules.tags.get("signature.authored")))throw new IllegalStateException("Missing signature map");
                    Vars.renderer.setScale(1.2f);
                    int[][] scenic={{240,172},{434,214},{126,268}};
                    Vars.player.unit().set(scenic[mission-13][0]*Vars.tilesize,scenic[mission-13][1]*Vars.tilesize);
                }
                if(Boolean.getBoolean("campaign.baseview")){
                    var core=Vars.state.rules.defaultTeam.core();Vars.player.unit().set(core.x,core.y+22*Vars.tilesize);Vars.renderer.setScale(1.2f);
                    if(Boolean.getBoolean("campaign.enemyview")&&mission==12){var gun=mindustry.gen.Groups.build.find(b->b.team==mindustry.game.Team.blue&&b.block.name.equals("new-horizon-eternity"));Vars.control.input.panCamera(new arc.math.geom.Vec2(gun.x,gun.y-20*Vars.tilesize));}
                }
                var preview=mindustry.io.MapIO.generatePreview(Vars.world.tiles);
                arc.graphics.PixmapIO.writePng(new Fi("campaign-tools/previews/native-map-"+(mission+1)+".png"),preview);
                preview.dispose();
                Timer.schedule(() -> {
                    if(mission>=13){
                        System.out.println("CLIENT_RULES chapter="+Vars.state.rules.tags.get("nh-afterglow-chapter")+" initial="+Vars.state.rules.initialWaveSpacing+" mapInitial="+p.generator.map.rules().initialWaveSpacing+" wave="+Vars.state.wavetime);
                        if(Vars.state.rules.tags.getInt("nh-afterglow-chapter",-1)!=mission)throw new IllegalStateException("Wrong chapter after native launch");
                        if(mission==14){
                            var cargo=Vars.world.build(434,214);
                            if(cargo==null||cargo.block!=mindustry.content.Blocks.vault)throw new IllegalStateException("Missing cargo after landing: tile="+Vars.world.tile(434,214).block()+" build="+cargo);
                            System.out.println("CLIENT_CARGO "+cargo.items);
                        }
                        var settled=mindustry.io.MapIO.generatePreview(Vars.world.tiles);arc.graphics.PixmapIO.writePng(new Fi("campaign-tools/previews/native-map-"+(mission+1)+".png"),settled);settled.dispose();
                    }
                    var full=mindustry.io.MapIO.generatePreview(Vars.world.tiles);arc.graphics.PixmapIO.writePng(new Fi("campaign-tools/previews/native-map-"+(mission+1)+".png"),full);full.dispose();
                    ScreenUtils.saveScreenshot(new Fi("campaign-tools/previews/client-"+(mission+1)+".png"));
                    System.out.println("CLIENT_OK mission="+(mission+1));
                    mission++;
                    if(mission<Math.min(end,ids.length)) Timer.schedule(DesktopCheck::load,2f); else Core.app.exit();
                },Math.max(mission==14?45f:0f,Float.parseFloat(System.getProperty("campaign.captureDelay","35"))));
            },5f);
        } catch(Throwable t){t.printStackTrace();System.exit(1);}
    }
}
