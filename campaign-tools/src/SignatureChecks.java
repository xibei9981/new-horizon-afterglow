import arc.*;
import arc.files.Fi;
import arc.math.geom.Point2;
import arc.util.Time;
import mindustry.content.*;
import mindustry.core.GameState.State;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.io.SaveIO;
import mindustry.world.*;
import mindustry.world.blocks.power.PowerGraph;
import newhorizon.content.*;
import newhorizon.content.blocks.*;
import newhorizon.content.campaign.*;
import static mindustry.Vars.*;

public class SignatureChecks extends CampaignChecks {
    static void load(int i){
        var p=AfterglowCampaign.sectors[SignatureCampaign.first+i];p.sector.clearInfo();logic.reset();
        world.loadSector(p.sector,new WorldParams(){{saveInfo=false;}});logic.play();
    }
    static void clear(){Groups.unit.copy().each(u->{if(u.team==Team.blue)u.remove();});Groups.bullet.clear();}
    static void reload(String id){
        var save=new Fi("campaign-tools/run/signature-"+id+".msav");SaveIO.write(save);SaveIO.load(save);state.set(State.playing);
        check(CraftingBlock.processorPrinter.unlocked()&&TurretBlock.vortex.unlocked(),"advanced research persists through saves");
    }
    public static void run() throws Exception {
        report.setLength(0);Time.setDeltaProvider(()->1f);
        research();
        for(int i=0;i<3;i++){
            CraftingBlock.processorPrinter.quietUnlock();TurretBlock.vortex.quietUnlock();
            load(i);
            check(CraftingBlock.processorPrinter.unlocked()&&TurretBlock.vortex.unlocked(),"previous advanced research survives entering new operation");
            var p=AfterglowCampaign.sectors[13+i];
            check(AfterglowCampaign.chapter()==13+i,"new chapter recognized");
            check(p.techNode.parent.content==AfterglowCampaign.sectors[12+i],"sequential unlock "+i);
            check("0.5.0".equals(state.rules.tags.get("signature.authored")),"native signature map "+i);
            check(p.generator.map.filters().isEmpty(),"no random ore filters");
            check(world.width()==SignatureCampaign.sizes[i][0]&&world.height()==SignatureCampaign.sizes[i][1],"map size");
            check(state.rules.attributes.get(mindustry.world.meta.Attribute.light)==1,"solar input survives planet rules");
            pathCheck();preview(13+i);
            java.util.Map<String,Integer> surfaces=new java.util.TreeMap<>();int walls=0,water=0,metal=0;
            for(var t:world.tiles){surfaces.merge(t.floor().name,1,Integer::sum);if(t.floor().isLiquid)water++;if(t.block().isStatic()&&t.solid())walls++;if(t.floor().name.contains("metal"))metal++;}
            check(walls>1500&&metal>800,"authored terrain walls and landmarks "+i);
            check(surfaces.values().stream().filter(v->v>world.width()*world.height()*.01).count()>=4,"substantial terrain diversity "+i);
            if(i==1)check(water>30000,"salt lake remains a lake, not a recolored open plain");
            int peak=0;for(int wave=0;wave<72;wave++){int count=0;for(var g:state.rules.spawns)count+=g.getSpawned(wave);check(count>0,"nonempty wave "+i+"/"+wave);peak=Math.max(count,peak);}
            check(peak<200,"bounded wave peak "+peak);log("MAP "+SignatureCampaign.ids[i]+" surfaces="+surfaces.size()+" walls="+walls+" water="+water+" metal="+metal+" wave peak="+peak);
            for(var stack:state.rules.loadout)check(Team.sharded.core().items.get(stack.item)>=Math.min(stack.amount,Team.sharded.core().storageCapacity),"initial signature loadout delivered "+stack.item);
            ticks(1800);check(!state.gameOver,"starter survived"); // Net production is checked by WorkshopChecks with empty stocks.
            for(var b:Groups.build)if(b.team==Team.sharded&&b.block.consumesPower&&b.block.consPower!=null&&!b.block.consPower.buffered){
                if(b.power.status<=.9f)for(var tile:world.tiles){var q=tile.build;if(q!=null&&tile.isCenter()&&q.team==Team.sharded&&q.power!=null)System.out.println("POWER "+q.block.name+" "+q.tileX()+","+q.tileY()+" graph="+q.power.graph.getID()+" produced="+q.power.graph.getPowerProduced()+" status="+q.power.status+" links="+q.power.links);}
                check(b.power.status>.9f,"powered starter "+b.block.name+" "+b.tileX()+","+b.tileY()+" "+b.power.status);
            }
            if(i==1){var b=world.build(SignatureCampaign.cargoX,SignatureCampaign.cargoY);check(b.items.get(Items.thorium)>0&&b.items.get(NHItems.zeta)>0,"actual tier-eight mining and two conveyors supply cargo");log("MINING cargo after 30s: thorium="+b.items.get(Items.thorium)+" zeta="+b.items.get(NHItems.zeta));}
            state.wave=1;logic.runWave();ticks(240);check(Groups.unit.contains(u->u.team==Team.blue),"real wave units spawned");clear();
            if(i==0)gate();if(i==1)cargo();if(i==2)grid();
            log("PASS "+SignatureCampaign.names[i]+": native map, paths, power, mining, waves, persistent objectives, engine capture.");
        }
        // Test independent custom-map capture as well as planet progression.
        logic.reset();var map=AfterglowCampaign.sectors[14].generator.map;world.loadMap(map,map.rules());logic.play();
        state.rules.tags.put("afterglow.signature-cargo","12");state.wave=49;SignatureCampaign.update(1);ticks(180);check(state.gameOver,"custom cargo victory");
        log("PASS custom-game objective capture; research remains at previous 20% cost.");
        new Fi("campaign-tools/signature-verification.txt").writeString(report.toString());
    }
    static void research(){
        boolean[] prior=new boolean[16];for(int k=0;k<16;k++){prior[k]=AfterglowCampaign.sectors[k].sector.info.wasCaptured;AfterglowCampaign.sectors[k].sector.info.wasCaptured=false;}
        for(var e:AfterglowTech.milestones)check(e.value<=12,"all technology available before final four maps");
        check(AfterglowTech.milestones.size>60,"technology progression covers real branches, not a few cosmetic rewards");
        for(int cleared=0;cleared<=12;cleared++){
            for(var e:AfterglowTech.milestones){
                check(AfterglowTech.available(e.key)==(e.value<=cleared),"technology available at intended chapter: "+e.key.name);
                for(var node:e.key.techNodes)if(node.rootNode==NHTechTree.root||node.content.minfo.mod==newhorizon.NewHorizon.MOD)check(node.objectives.contains(o->o instanceof mindustry.game.Objectives.SectorComplete sc&&sc.preset==AfterglowCampaign.sectors[e.value-1]),"every research entry carries the completion requirement "+e.key.name);
            }
            if(cleared<12)AfterglowCampaign.sectors[cleared].sector.info.wasCaptured=true;
        }
        check(!AfterglowTech.milestones.containsKey(Blocks.blastDrill),"Serpulo technology outside this campaign is unchanged");
        check(AfterglowTech.milestones.get(UnitBlock.jumpGateStandard,0)==5,"standard gate after fifth mission");
        check(AfterglowTech.milestones.get(NHUnitTypes.tarlidor,0)==7,"heavy tank after seventh mission");
        check(AfterglowTech.milestones.get(UnitBlock.jumpGateHyper,0)==10,"hyper gate after tenth mission");
        for(int k=0;k<16;k++)AfterglowCampaign.sectors[k].sector.info.wasCaptured=prior[k];
        log("PASS staged research: "+AfterglowTech.milestones.size+" contents across the first 12 victories; all alternate entries gated, prerequisites retained, no existing unlock revoked.");
    }
    static void gate(){
        var control=world.build(SignatureCampaign.switchX,SignatureCampaign.switchY);control.configureAny(false);SignatureCampaign.update(0);
        int east=Point2.pack(380,462),west=Point2.pack(100,462);
        check(state.rules.spawns.find(g->g.begin==1&&!g.type.flying).spawn==east,"switch routes next ground wave east");
        reload("gate");check(state.rules.spawns.find(g->g.begin==1&&!g.type.flying).spawn==east,"selected lane restored after planet reapplication");
        world.build(SignatureCampaign.switchX,SignatureCampaign.switchY).configureAny(true);SignatureCampaign.update(0);
        check(state.rules.spawns.find(g->g.begin==1&&!g.type.flying).spawn==west,"switch routes west");
        check(state.rules.spawns.contains(g->g.begin==5&&g.spawn==east)&&state.rules.spawns.contains(g->g.begin==5&&g.spawn==west),"sixth wave cannot be diverted");
        state.wave=72;logic.runWave();ticks(220);check(state.enemies>0&&!AfterglowCampaign.sectors[13].sector.info.wasCaptured,"72nd wave is a real final battle");
        clear();ticks(200);check(AfterglowCampaign.sectors[13].sector.info.wasCaptured,"gate capture");check(AfterglowCampaign.sectors[14].unlocked(),"cargo unlocked");
    }
    static void cargo(){
        int x=SignatureCampaign.cargoX,y=SignatureCampaign.cargoY;var b=world.build(x,y);b.items.clear();int before=SignatureCampaign.sent();
        for(int k=0;k<70;k++)SignatureCampaign.cargo();check(SignatureCampaign.sent()==before,"empty warehouse cannot ship");
        b.items.set(Items.thorium,600);b.items.set(NHItems.zeta,299);SignatureCampaign.cargo();check(b.items.get(Items.thorium)==600&&SignatureCampaign.sent()==before,"insufficient mixed cargo is not partially deducted");
        b.items.set(NHItems.zeta,300);SignatureCampaign.cargo();check(SignatureCampaign.sent()==before+1&&b.items.get(Items.thorium)==0&&b.items.get(NHItems.zeta)==0,"real cargo consumption");
        for(int k=0;k<17;k++)SignatureCampaign.cargo();reload("cargo");check(SignatureCampaign.sent()==before+1&&state.rules.tags.getInt("afterglow.signature-loading",0)==17,"cargo load and timer persist");
        world.tile(x,y).remove();for(int k=0;k<80;k++)SignatureCampaign.cargo();check(SignatureCampaign.sent()==before+1,"destroyed freight station cannot send");
        world.tile(x,y).setBlock(Blocks.vault,Team.sharded,0);b=world.build(x,y);
        while(SignatureCampaign.sent()<12){b.items.set(Items.thorium,600);b.items.set(NHItems.zeta,300);for(int k=0;k<60;k++)SignatureCampaign.cargo();}
        SignatureCampaign.update(1);check(!state.rules.tags.containsKey("afterglow.signature-ready"),"early shipments do not bypass minimum defense");
        state.wave=49;SignatureCampaign.update(1);reload("cargo-ready");check(!state.rules.waveTimer&&state.rules.winWave==1,"pending capture restored on reload");
        ticks(220);clear();ticks(100);check(AfterglowCampaign.sectors[14].sector.info.wasCaptured,"cargo capture");check(AfterglowCampaign.sectors[15].unlocked(),"city unlocked");
    }
    static void grid(){
        SignatureCampaign.synchronize();check(SignatureCampaign.charge()==0,"enemy terminals cannot synchronize");
        // This is the post-assault capture fixture. Real play must clear the courtyard
        // weapons before replacing a battery; otherwise they correctly shoot captured equipment.
        clear();
        for(var t:world.tiles)if(t.isCenter()&&t.build!=null&&t.team()==Team.blue&&t.block() instanceof mindustry.world.blocks.defense.turrets.BaseTurret)
            for(var center:SignatureCampaign.terminals)if(t.build!=null&&t.build.within(center[0]*tilesize,center[1]*tilesize,70*tilesize)){t.remove();break;}
        var first=SignatureCampaign.terminals[0];world.tile(first[0],first[1]).setBlock(Blocks.batteryLarge,Team.sharded,0);
        SignatureCampaign.captureGrids();ticks(600);
        check(world.build(first[0],first[1]+23).team==Team.sharded,"live solar field captured with terminal");
        check(SignatureCampaign.terminal(0).power.graph.getBatteryStored()>600,"captured solar grid actually generates useful energy");
        check(SignatureCampaign.charge()==0,"one captured grid cannot complete three-point synchronization");
        for(var p:SignatureCampaign.terminals){world.tile(p[0],p[1]).setBlock(Blocks.batteryLarge,Team.sharded,0);new PowerGraph().reflow(world.build(p[0],p[1]));world.build(p[0],p[1]).power.status=1f;}
        float stored=0;for(int k=0;k<3;k++)stored+=SignatureCampaign.terminal(k).power.graph.getBatteryStored();
        SignatureCampaign.synchronize();float after=0;for(int k=0;k<3;k++)after+=SignatureCampaign.terminal(k).power.graph.getBatteryStored();
        check(SignatureCampaign.charge()==1&&Math.abs(stored-after-1800)<1,"synchronization consumes actual energy");
        for(int k=0;k<11;k++)SignatureCampaign.synchronize();reload("sync");check(SignatureCampaign.charge()==12,"synchronization survives save/load");
        var p=SignatureCampaign.terminals[1];world.tile(p[0],p[1]).remove();SignatureCampaign.synchronize();check(SignatureCampaign.charge()==10,"lost node rolls back progress");
        world.tile(p[0],p[1]).setBlock(Blocks.batteryLarge,Team.sharded,0);new PowerGraph().reflow(world.build(p[0],p[1]));
        SignatureCampaign.synchronize();check(SignatureCampaign.charge()==8,"empty battery cannot synchronize");
        // Shared grid must pay for all three terminals; a single 600-unit reserve cannot pay three times.
        var shared=new PowerGraph();for(int k=0;k<3;k++){var b=SignatureCampaign.terminal(k);b.power.graph.remove(b);shared.add(b);b.power.status=0;}
        SignatureCampaign.terminal(0).power.status=900f/Blocks.batteryLarge.consPower.capacity;
        SignatureCampaign.synchronize();check(SignatureCampaign.charge()==6&&Math.abs(shared.getBatteryStored()-900)<1,"shared-grid insufficient energy is atomic");
        for(int step=0;step<174;step++){for(int k=0;k<3;k++)SignatureCampaign.terminal(k).power.status=1;SignatureCampaign.synchronize();}
        check(state.rules.tags.containsKey("afterglow.signature-synced"),"synchronization completed");
        SignatureCampaign.update(2);check(!state.rules.tags.containsKey("afterglow.signature-ready"),"live enemy core prevents capture");
        state.rules.waveTeam.cores().copy().each(c->c.tile.remove());clear();SignatureCampaign.update(2);ticks(240);clear();ticks(100);
        check(AfterglowCampaign.sectors[15].sector.info.wasCaptured,"city objective and core capture");
    }
}
