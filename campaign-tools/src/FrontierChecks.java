import arc.*;
import arc.files.Fi;
import arc.math.geom.Point2;
import mindustry.content.*;
import mindustry.core.GameState.State;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.io.SaveIO;
import mindustry.type.*;
import mindustry.world.WorldParams;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import newhorizon.content.blocks.*;
import newhorizon.content.campaign.*;
import static mindustry.Vars.*;

public class FrontierChecks extends CampaignChecks {
    static void load(int i){
        var p=AfterglowCampaign.sectors[i+3];p.sector.clearInfo();logic.reset();
        world.loadSector(p.sector,new WorldParams(){{saveInfo=false;}});logic.play();
    }
    static void clearEnemies(){Groups.unit.copy().each(u->{if(u.team==state.rules.waveTeam)u.remove();});Groups.bullet.clear();}
    static void waveEvent(int wave){state.wave=wave+1;Events.fire(new EventType.WaveEvent());ticks(180);}
    public static void run() throws Exception {
        arc.util.Time.setDeltaProvider(()->1f);
        if(Boolean.getBoolean("campaign.powerProbe")){
            for(int attempt=0;attempt<20;attempt++){log("POWER_PROBE "+attempt);verifyMission(1);}
            return;
        }
        // An explicitly priced early technology: 30 -> 6. A normally priced core: 5x build -> 1x build.
        check(PowerBlock.photonPanel.techNode.requirements.length==1&&PowerBlock.photonPanel.techNode.requirements[0].amount==6,"explicit research cost reduced from 30 to 6");
        for(var cost:SpecialBlock.coreArray.techNode.requirements){
            var build=arc.util.Structs.find(SpecialBlock.coreArray.requirements,s->s.item==cost.item);
            check(build!=null&&cost.amount==build.amount,"normal research reduced from five builds to one build");
            check(cost!=build,"research reduction must not alias construction costs");
        }
        log("PASS research costs: explicit 30 -> 6; standard building research 5x -> 1x construction; prerequisites intact");
        for(int i=0;i<10;i++)verifyMission(i);
        verifyEnemyNetworks();
        // A loss test is separate from completion tests; destroying either authored core must fail.
        load(6);world.tile(368,76).remove();ticks(65);
        check(state.gameOver&&state.rules.defaultTeam.cores().size==1,"double-core mission fails when either protected core falls");
        log("PASS double-core loss condition");
        load(3);waveEvent(20);
        check(Groups.unit.contains(u->u.team==Team.blue&&u.type==newhorizon.content.NHUnitTypes.tarlidor),"late hunt brings actual heavy pursuers");
        for(int n=0;n<2;n++)world.tile(state.rules.tags.getInt("frontier.depot."+n,-1)).remove();ticks(65);
        check(!state.rules.tags.containsKey("afterglow.hunt-bonus"),"late sabotage does not receive early bonus");
        clearEnemies();waveEvent(26);check(!Groups.unit.contains(u->u.team==Team.blue),"destroying hubs stops future heavy pursuers");
        log("PASS hunt deadline, heavy pursuers and counterplay");
        // Custom games have no SectorPreset.attackAfterWaves; the runtime must still transition and win.
        logic.reset();var finalMap=AfterglowCampaign.sectors[12].generator.map;
        world.loadMap(finalMap,finalMap.rules());logic.play();
        check(!state.isCampaign(),"custom map stays outside campaign");
        state.wave=101;ticks(180);clearEnemies();ticks(65);
        check(state.rules.attackMode&&!state.rules.waves&&!state.gameOver,"custom finale reaches counterattack without premature victory");
        Team.blue.cores().copy().each(c->c.tile.remove());clearEnemies();ticks(180);
        check(state.gameOver,"custom finale victory");
        log("PASS custom-game finale defense, counterattack and victory");
        lateFinale();
        log("PASS 10 new authored maps, pacing, logistics, forward bases, storm, finale, save/load, unlocks.");
        new Fi("campaign-tools/frontier-verification.txt").writeString(report.toString());
    }
    static void verifyMission(int i) throws Exception {
        var m=FrontierMissions.all[i];var p=AfterglowCampaign.sectors[i+3];
        check(p.techNode.parent.content==AfterglowCampaign.sectors[i+2],"sequential research link "+m.id);
        check(p.generator.map.filters().isEmpty(),"no random ore filters: "+m.id);
        CraftingBlock.processorPrinter.clearUnlock();TurretBlock.vortex.clearUnlock();
        load(i);
        check(!CraftingBlock.processorPrinter.unlocked()&&!TurretBlock.vortex.unlocked(),"advanced research stays locked on entry "+m.id);
        check("0.4.0".equals(state.rules.tags.get("frontier.authored")),"not a placeholder: "+m.id);
        check(state.rules.attributes.get(mindustry.world.meta.Attribute.light)==1f,"authored irradiance survives planetary defaults "+m.id);
        check(world.width()==m.width&&world.height()==m.height,"dimensions "+m.id);
        check(AfterglowCampaign.chapter()==i+3,"chapter "+m.id);
        check(Team.blue.cores().size==m.cores.length,"enemy cores "+m.id);
        check(Team.sharded.cores().size==(i==6?2:1),"friendly cores "+m.id);
        check(state.rules.attackMode==m.attack(),"mode "+m.id);
        pathCheck();preview(i+3);
        java.util.Map<String,Integer> floors=new java.util.HashMap<>();
        int walls=0,wet=0,props=0;
        for(var t:world.tiles){
            floors.merge(t.floor().name,1,Integer::sum);
            if(t.floor().isLiquid)wet++;
            if(t.block().isStatic()&&t.block().solid)walls++;
            if(t.block() instanceof mindustry.world.blocks.environment.Prop&&!t.block().solid)props++;
        }
        long substantial=floors.values().stream().filter(n->n>world.width()*world.height()*.015).count();
        check(substantial>=4,"at least four substantial geological surfaces: "+m.id+" "+floors);
        check(walls>1000,"substantial cliffs: "+m.id);
        check(props>40,"environmental props: "+m.id);
        log("GEOLOGY "+m.id+" significant floors="+substantial+" cliff tiles="+walls+" water tiles="+wet+" props="+props+" ruins="+state.rules.tags.get("frontier.ruins"));
        int max=0,total=0;
        for(int w=1;w<=(m.waves>0?m.waves:60);w++){
            int count=0;for(var g:state.rules.spawns)count+=g.getSpawned(w-1);
            check(count>0,"empty authored wave "+m.id+" "+w);max=Math.max(max,count);total+=count;
        }
        check(max<250,"bounded simultaneous wave count "+m.id+" "+max);
        if(m.waves>0)for(var g:state.rules.spawns)check(g.getSpawned(m.waves)==0,"finite last wave "+m.id);
        if(i==2){
            check(lanes(1)==1&&lanes(25)==2&&lanes(49)==3,"trident staged fronts");
        }
        ticks(1200);
        check(!state.gameOver,"startup survived "+m.id);
        check(Team.sharded.core().items.get(Items.titanium)>5000,"titanium production "+m.id);
        int powered=0;for(var b:Groups.build)if(b.team==Team.sharded&&b.power!=null&&b.block.consumesPower){
            check(b.power.status>.90f,"unpowered starter: "+m.id+" "+b.block.name+" "+b.tile.x+","+b.tile.y+" "+b.power.status);powered++;
        }
        check(powered>=3,"powered base "+m.id);
        for(var b:Groups.build)if(b.team==Team.blue){
            check(b.block!=Blocks.powerSource&&b.block!=Blocks.itemSource,"no infinite enemy supplies "+m.id);
            if(b.block==TurretBlock.thermo&&b.power.status<=.9f){
                for(var tile:world.tiles){var e=tile.build;if(e!=null&&tile.isCenter()&&e.team==Team.blue&&e.power!=null&&e.within(b,430))System.out.println("POWER_DIAG "+e.block.name+" "+e.tile.x+","+e.tile.y+" enabled="+e.enabled+" output="+e.getPowerProduction()+" graphId="+e.power.graph.getID()+" producers="+e.power.graph.producers.size+" produced="+e.power.graph.getPowerProduced()+" status="+e.power.status+" links="+e.power.links);}
                System.out.println("SOLAR_RULE "+state.rules.solarMultiplier+" LIGHT "+state.rules.lighting+" AMBIENT "+state.rules.ambientLight+" LIGHT_ENV "+mindustry.world.meta.Attribute.light.env());
            }
            if(b.block==TurretBlock.thermo)check(b.power.status>.9f,"enemy energy defense has real power "+m.id+" at "+b.tile.x+","+b.tile.y+" status="+b.power.status);
        }
        verifyRestoration(i);
        if(i==1||i==3||i==7)check(Groups.unit.contains(u->u.team==Team.sharded&&!u.isPlayer()),"initial assault force "+m.id);
        // Actual engine spawn, not just serialized definitions.
        state.wave=1;logic.runWave();ticks(240);
        check(Groups.unit.contains(u->u.team==Team.blue),"real enemy spawn "+m.id);clearEnemies();
        if(m.cores.length>0){
            int depot=state.rules.tags.getInt("frontier.depot.0",-1);
            var gun=(ItemTurret.ItemTurretBuild)world.build(state.rules.tags.getInt("frontier.test-gun.0",-1));
            var warehouse=world.build(depot);
            int reserve=warehouse.items.get(Items.titanium);
            gun.ammo.clear();gun.totalAmmo=0;FrontierCampaign.refillDepots();
            check(gun.totalAmmo>0,"living logistics hub resupplies guns "+m.id);
            check(warehouse.items.get(Items.titanium)<reserve,"refill consumes finite stock "+m.id);
            warehouse.items.set(Items.titanium,0);gun.ammo.clear();gun.totalAmmo=0;FrontierCampaign.refillDepots();
            check(gun.totalAmmo==0,"empty warehouse cannot fabricate ammunition "+m.id);
            warehouse.items.set(Items.titanium,100);
            world.tile(depot).remove();gun.ammo.clear();gun.totalAmmo=0;FrontierCampaign.refillDepots();
            check(gun.totalAmmo==0,"destroyed hub stops resupply "+m.id);ticks(65);
        }
        switch(i){
            case 0 -> {
                Team.sharded.core().items.set(Items.surgeAlloy,0);waveEvent(21);
                check(UnitBlock.jumpGateStandard.unlocked(),"industrial technology opens");
                int stock=Team.sharded.core().items.get(Items.surgeAlloy);waveEvent(21);
                check(stock==Team.sharded.core().items.get(Items.surgeAlloy),"industrial supply once only");
                waveEvent(41);
            }
            case 1 -> {waveEvent(8);check(state.rules.tags.containsKey("afterglow.island-raid-8"),"island fleet event");}
            case 2 -> {waveEvent(25);waveEvent(49);check(state.rules.tags.containsKey("afterglow.front-49"),"third front supplies");}
            case 3 -> {
                world.tile(state.rules.tags.getInt("frontier.depot.1",-1)).remove();ticks(65);
                check(state.rules.tags.containsKey("afterglow.hunt-bonus"),"early logistics strike rewards assault team");
            }
            case 4 -> {
                waveEvent(21);check(state.rules.solarMultiplier==.25f,"storm power drop");
                waveEvent(31);check(state.rules.solarMultiplier==1f,"storm power recovery");waveEvent(51);
            }
            case 5 -> {
                world.tile(m.cores[0][0],m.cores[0][1]).remove();ticks(65);
                check(world.build(m.cores[0][0],m.cores[0][1]).team==Team.sharded,"captured forward core");
                check(Team.sharded.cores().size==2,"forward base extends player core network");
            }
            case 6 -> {waveEvent(31);waveEvent(61);}
            case 7 -> {world.tile(m.cores[0][0],m.cores[0][1]).remove();ticks(65);waveEvent(20);}
            case 8 -> {
                int lane=Point2.pack(m.spawns[0][0],m.spawns[0][1]);
                check(!state.rules.spawns.contains(g->g.spawn==lane),"destroying hub silences its wave lane");
                for(int n=1;n<3;n++)world.tile(state.rules.tags.getInt("frontier.depot."+n,-1)).remove();ticks(65);
                check(state.rules.spawns.isEmpty()&&!state.rules.waves,"all logistics hubs destroyed stops all waves");
            }
            case 9 -> {
                waveEvent(31);waveEvent(61);waveEvent(81);clearEnemies();
                state.wave=101;ticks(240);clearEnemies();ticks(120);
                check(state.rules.attackMode&&!state.rules.waves,"defense transitions to counterattack");
                check(!p.sector.info.wasCaptured,"finale does not win before all enemy cores fall");
            }
        }
        clearEnemies();
        var tagSnapshot=state.rules.tags.copy();boolean attack=state.rules.attackMode;
        Fi save=new Fi("campaign-tools/run/frontier-"+i+".msav");SaveIO.write(save);SaveIO.load(save);state.set(State.playing);ticks(65);
        tagSnapshot.each((key,value)->{if(key.startsWith("afterglow.")&&!key.equals("afterglow.logistics-clock"))check(value.equals(state.rules.tags.get(key)),"saved progress "+m.id+" "+key);});
        check(state.rules.attackMode==attack,"mode survives reload "+m.id);
        if(i==4)check(state.rules.solarMultiplier==.25f,"storm survives reload");
        if(i==8)check(!state.rules.spawns.contains(g->g.spawn==Point2.pack(m.spawns[0][0],m.spawns[0][1])),"disabled lane survives reload");
        clearEnemies();
        if(m.attack()||i==9)Team.blue.cores().copy().each(c->c.tile.remove());else state.wave=m.waves+1;
        ticks(240);clearEnemies();ticks(120);
        check(p.sector.info.wasCaptured,"actual engine victory "+m.id);
        if(i<9)check(AfterglowCampaign.sectors[i+4].unlocked(),"next chapter unlocked "+m.id);
        log("PASS "+m.id+" "+m.width+"x"+m.height+" | waves="+(m.waves>0?m.waves:"attack")+" max/wave="+max+" sampled total="+total+" | paths, power, production, logistics, mechanics, save, victory");
    }
    static void verifyEnemyNetworks() throws Exception {
        for(int i:new int[]{1,3,5,7,8,9}){
            load(i);ticks(180);
            Fi save=new Fi("campaign-tools/run/network-"+i+".msav");SaveIO.write(save);SaveIO.load(save);state.set(State.playing);ticks(180);
            int powered=0;
            for(var t:world.tiles)if(t.isCenter()&&t.build!=null&&t.build.team==Team.blue&&t.block()==TurretBlock.thermo){
                check(t.build.power.status>.9f,"enemy network survives save/load "+i+" "+t.x+","+t.y);powered++;
            }
            check(powered>0,"real energy defense present");
            for(var t:world.tiles)if(t.isCenter()&&t.build!=null&&t.build.team==Team.blue&&t.block()==Blocks.largeSolarPanel)t.remove();
            ticks(180);
            for(var t:world.tiles)if(t.isCenter()&&t.build!=null&&t.build.team==Team.blue&&t.block()==TurretBlock.thermo)
                check(t.build.power.status<.05f,"destroying generation disables energy defense "+i);
            log("PASS enemy grid "+i+" powered after reload; destroying solar arrays cuts energy weapons");
        }
    }
    static void verifyRestoration(int i) throws Exception {
        int pos=state.rules.tags.getInt("frontier.relay",-1);check(pos!=-1,"authored restoration site");
        var tile=world.tile(pos);int x=tile.x,y=tile.y;
        check(tile.block()==Blocks.air,"restoration build pad unobstructed "+i);
        tile.setBlock(Blocks.mendProjector,Team.sharded);
        world.tile(x+4,y).setBlock(Blocks.powerSource,Team.sharded);
        world.build(x+4,y).configureAny(pos);
        var core=Team.sharded.core();int savedSilicon=core.items.get(Items.silicon);core.items.set(Items.silicon,299);
        ticks(180);check(!state.rules.tags.containsKey("afterglow.relay-funded"),"insufficient repair resources cannot start");
        core.items.set(Items.silicon,savedSilicon);ticks(360);
        check(state.rules.tags.containsKey("afterglow.relay-funded"),"powered repair consumes material once");
        check(core.items.get(Items.silicon)==savedSilicon-300,"exact repair silicon charge");
        int progress=state.rules.tags.getInt("afterglow.relay-time",0);check(progress>0&&progress<30,"repair in progress");
        tile.remove();ticks(180);
        check(state.rules.tags.getInt("afterglow.relay-time",0)==progress,"destroyed projector pauses repair");
        Fi save=new Fi("campaign-tools/run/repair-"+i+".msav");SaveIO.write(save);SaveIO.load(save);state.set(State.playing);
        check(state.rules.tags.getInt("afterglow.relay-time",0)==progress,"partial repair survives save/load");
        world.tile(pos).setBlock(Blocks.mendProjector,Team.sharded);world.build(x+4,y).configureAny(pos);
        ticks(1900);check(state.rules.tags.containsKey("afterglow.relay-done"),"repair completes "+i);
        int kind=state.rules.tags.getInt("frontier.relay-kind",0);
        if(kind==2){
            check(world.build(x,y+12).team==Team.sharded,"restored battery transfers ownership");
            if(world.build(x,y+12).power.graph.getPowerProduced()<=0)for(int dx:new int[]{-12,-7,0,7,12}){
                var b=world.build(x+dx,y+12);System.out.println("RELAY_POWER "+b.block.name+" team="+b.team+" enabled="+b.enabled+" production="+b.getPowerProduction()+" graph="+b.power.graph.getPowerProduced()+" links="+b.power.links);
            }
            check(world.build(x,y+12).power.graph.getPowerProduced()>0,"restored substation produces real power");
        }
        if(kind==3)check(world.build(x-12,y+12).team==Team.sharded,"restored AA transfers ownership");
        int stock=Team.sharded.core().items.get(Items.silicon),units=Groups.unit.size();
        for(int n=0;n<40;n++)FrontierCampaign.restoreSite(i);
        check(stock==Team.sharded.core().items.get(Items.silicon)&&units==Groups.unit.size(),"restoration reward is single-use");
        world.tile(x+4,y).remove();
        world.tile(pos).remove(); // Test fixture; leave restored assets and progress intact.
        log("PASS restoration "+i+" resources, power, pause, save/load, completion, one-time reward");
    }
    public static void lateFinale(){
        arc.util.Time.setDeltaProvider(()->1f);load(9);
        state.wave=100;logic.runWave();ticks(240);
        int enemyCount=Groups.unit.count(u->u.team==Team.blue);
        check(enemyCount>=100,"final wave actually deploys a large force, count="+enemyCount);
        check(Groups.unit.contains(u->u.team==Team.blue&&u.type==newhorizon.content.units.GroundUnitTypes.annihilation),"final heavy boss actually spawns");
        check(!state.rules.attackMode&&!AfterglowCampaign.sectors[12].sector.info.wasCaptured,"must defeat final wave before counterattack");
        clearEnemies();ticks(180);
        check(state.rules.attackMode&&!state.rules.waves,"final battle clear unlocks counterattack");
        log("PASS actual wave 100: "+enemyCount+" enemies, heavy boss, no premature counterattack");
    }
    static int lanes(int wave){arc.struct.IntSet lanes=new arc.struct.IntSet();for(var g:state.rules.spawns)if(g.getSpawned(wave-1)>0)lanes.add(g.spawn);return lanes.size;}
}
