package newhorizon.content.campaign;

import arc.Core;
import arc.Events;
import arc.math.geom.Point2;
import mindustry.content.*;
import mindustry.ctype.UnlockableContent;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import newhorizon.content.*;
import newhorizon.content.blocks.*;
import newhorizon.content.units.GroundUnitTypes;
import static mindustry.Vars.*;
import static newhorizon.content.campaign.AfterglowCampaign.*;

/** Mission-specific objectives and finite, save-safe rewards. No wall-clock timers. */
public final class FrontierCampaign {
    /** Planet rules are reapplied after loading saves; derive dynamic rules from saved progress last. */
    public static void restoreRules(mindustry.game.Rules r,int i){
        if(i==9&&r.tags.containsKey("afterglow.counterattack")){r.attackMode=true;r.waves=false;}
        if(i==4)r.solarMultiplier="true".equals(r.tags.get("afterglow.storm-active"))?.25f:1f;
        if(i==8){
            var m=FrontierMissions.all[i];int disabled=0;
            for(int n=0;n<m.spawns.length;n++)if(r.tags.containsKey("afterglow.depot-"+n)){
                int pos=Point2.pack(m.spawns[n][0],m.spawns[n][1]);
                r.spawns.removeAll(g->g.spawn==pos);disabled++;
            }
            if(disabled==m.spawns.length)r.waves=false;
        }
    }
    public static void prepare(int i){
        if(i==4) storm();
        if(i==9 && state.rules.tags.containsKey("afterglow.counterattack")){
            state.rules.attackMode=true; state.rules.waves=false;
        }
        if(i==8) silenceLanes();
    }
    public static void start(int i){
        // Advanced industry remains researchable at the reduced material cost.
        Blocks.mendProjector.quietUnlock(); // Optional restoration objectives require this basic tool.
        if(!once("frontier-start"))return;
        if(i==1) allies(i,NHUnitTypes.branch,10);
        if(i==3) allies(i,NHUnitTypes.aliotiat,8);
        if(i==7){allies(i,NHUnitTypes.tarlidor,4);allies(i,NHUnitTypes.aliotiat,16);}
    }
    public static void wave(int i,int w){
        if(w<=0)return;
        switch(i){
            case 0 -> {
                if(w==20)announce("frontier.industry-warning");
                if(w==21&&once("industry")){UnitBlock.jumpGateStandard.quietUnlock();grant(2);announce("frontier.industry");}
                if(w==41&&once("armor")){allies(i,NHUnitTypes.tarlidor,3);grant(1);}
            }
            case 1 -> {
                if(w%8==7 && activeDepots()>0)announce("warning.air");
                if(w%8==0&&once("island-raid-"+w)){
                    var m=FrontierMissions.all[i];
                    for(int n=0;n<m.cores.length;n++)if(depotAlive(n))
                        enemies(i,NHUnitTypes.branch,4+Math.min(6,w/8),m.cores[n][0],m.cores[n][1]+35);
                }
            }
            case 2 -> {
                if(w==24||w==48)announce("frontier.lane-warning");
                if((w==25||w==49)&&once("front-"+w)){grant(1);allies(i,NHUnitTypes.aliotiat,w==25?8:12);announce("frontier.new-front");}
            }
            case 3 -> {
                if(w==19 && activeDepots()>0)announce("frontier.hunt-warning");
                if(w>=20&&(w-20)%6==0&&activeDepots()>0&&once("hunter-"+w))
                    enemies(i,NHUnitTypes.tarlidor,2,208,366);
            }
            case 4 -> {
                if(w==20||w==50)announce("frontier.storm-warning");
                storm();
                if((w==21||w==51)&&once("storm-"+w))announce("frontier.storm");
                if((w==31||w==61)&&once("clear-"+w)){grant(1);allies(i,NHUnitTypes.warper,8);announce("frontier.clear");}
            }
            case 5 -> {if(w%15==0&&once("march-supply-"+w)&&w<=60)grant(1);}
            case 6 -> {
                if(w==60)announce("frontier.twin-warning");
                if((w==31||w==61)&&once("twin-"+w)){
                    grant(2);allies(i,NHUnitTypes.aliotiat,10);
                    fleet(NHUnitTypes.aliotiat,10,true,368f/480f,120f/448f);
                }
            }
            case 7 -> {if(w==20&&once("armor-reserve")){allies(i,NHUnitTypes.tarlidor,3);grant(2);}}
            case 8 -> silenceLanes();
            case 9 -> {
                if((w==31||w==61)&&once("final-supply-"+w)){grant(3);announce("frontier.industry");}
                if(w==81&&once("final-allies")){allies(i,NHUnitTypes.tarlidor,5);allies(i,NHUnitTypes.aliotiat,16);}
            }
        }
        if("0.4.0".equals(state.rules.tags.get("frontier.authored"))){
            int next=FrontierSites.waveRole(i,w+1);
            if(next==1)announce("frontier.fast-warning");
            if(next==2)announce("frontier.siege-warning");
            if(next==3)announce("frontier.recovery-warning");
        }
    }
    static void storm(){
        int w=state.wave-1;
        boolean active=(w>=21&&w<=30)||(w>=51&&w<=60);
        state.rules.tags.put("afterglow.storm-active",String.valueOf(active));
        state.rules.solarMultiplier=active?.25f:1f;
    }
    public static boolean depotAlive(int n){
        int pos=state.rules.tags.getInt("frontier.depot."+n,-1);
        var b=pos==-1?null:world.build(pos);
        return b!=null&&b.team==state.rules.waveTeam&&(b.block==Blocks.itemSource||b.block==Blocks.vault);
    }
    public static int activeDepots(){
        int total=state.rules.tags.getInt("frontier.depots",0), alive=0;
        for(int n=0;n<total;n++)if(depotAlive(n))alive++;
        return alive;
    }
    static void silenceLanes(){
        var m=FrontierMissions.all[8];
        if(activeDepots()==0)state.rules.waves=false;
        for(int n=0;n<m.spawns.length;n++)if(!depotAlive(n)){
            int pos=Point2.pack(m.spawns[n][0],m.spawns[n][1]);
            state.rules.spawns.removeAll(g->g.spawn==pos);
        }
    }
    public static void update(int i){
        if(state.gameOver || (state.isCampaign() && state.rules.sector.info.wasCaptured))return;
        var m=FrontierMissions.all[i];
        // Refills require a live, visible, destroyable logistics hub. Its position is authored into the map.
        int clock=state.rules.tags.getInt("afterglow.logistics-clock",0)+1;
        state.rules.tags.put("afterglow.logistics-clock",String.valueOf(clock%5));
        if(clock>=5)refillDepots();
        restoreSite(i);
        if(i==4)storm();
        if(i==8)silenceLanes();
        for(int n=0;n<m.cores.length;n++){
            if(!depotAlive(n)&&once("depot-"+n)){
                announce("frontier.depot-down");
                if(i==1||i==3||i==8)grant(1);
            }
            int[] at=m.cores[n];
            var b=world.build(at[0],at[1]);
            if((b==null||b.team!=state.rules.waveTeam)&&once("outpost-"+n)){
                if(i==5&&n<3){
                    // Enemy core already gone; the advance team establishes a real friendly core.
                    world.tile(at[0],at[1]).setBlock(Blocks.coreFoundation,state.rules.defaultTeam,0);
                    grant(2); announce("frontier.forward-base");
                }
                if(i==7)grant(2);
            }
        }
        if(i==3&&activeDepots()==0&&state.wave-1<20&&once("hunt-bonus")){
            allies(i,NHUnitTypes.tarlidor,3);grant(2);announce("frontier.hunt-bonus");
        }
        if(i==6){
            for(int x:new int[]{112,368}){
                var b=world.build(x,76);
                if(b==null||b.team!=state.rules.defaultTeam||!(b.block instanceof mindustry.world.blocks.storage.CoreBlock)){
                    state.gameOver=true;
                    Events.fire(new EventType.GameOverEvent(state.rules.waveTeam));
                    announce("frontier.twin-lost");return;
                }
            }
        }
        if(i==9&&state.wave>=101&&state.enemies==0&&!spawner.isSpawning()&&once("counterattack")){
            state.rules.attackMode=true;state.rules.waves=false;
            allies(i,NHUnitTypes.tarlidor,8);allies(i,NHUnitTypes.aliotiat,20);grant(3);
            announce("frontier.counterattack");
        }
    }
    public static void refillDepots(){
        int count=state.rules.tags.getInt("frontier.depots",0);
        for(int n=0;n<count;n++)if(depotAlive(n)){
            var depot=world.build(state.rules.tags.getInt("frontier.depot."+n,-1));
            boolean finite="true".equals(state.rules.tags.get("frontier.finite-depots"));
            for(var b:Groups.build)if(b.team==state.rules.waveTeam&&b.within(depot,(finite?60:42)*tilesize)&&b instanceof ItemTurret.ItemTurretBuild t){
                var turret=(ItemTurret)b.block;
                Item ammo=turret.ammoTypes.containsKey(Items.titanium)?Items.titanium:
                    turret.ammoTypes.containsKey(Items.graphite)?Items.graphite:
                    turret.ammoTypes.containsKey(Items.lead)?Items.lead:null;
                if(ammo!=null)for(int k=0;k<20&&t.acceptItem(depot,ammo);k++){
                    if(finite&&depot.items.get(ammo)<=0)break;
                    if(finite)depot.items.remove(ammo,1);
                    t.handleItem(depot,ammo);
                }
            }
        }
    }
    /** Repair state is serialized in rule tags; losing the projector pauses, never charges twice. */
    public static void restoreSite(int i){
        int pos=state.rules.tags.getInt("frontier.relay",-1);
        if(pos==-1||state.rules.tags.containsKey("afterglow.relay-done"))return;
        var repair=world.build(pos);
        if(repair==null||repair.block!=Blocks.mendProjector||repair.team!=state.rules.defaultTeam||repair.power.status<.9f)return;
        var core=state.rules.defaultTeam.core();if(core==null)return;
        if(!state.rules.tags.containsKey("afterglow.relay-funded")){
            if(core.items.get(Items.silicon)<300||core.items.get(Items.titanium)<200||core.items.get(Items.graphite)<150)return;
            core.items.remove(Items.silicon,300);core.items.remove(Items.titanium,200);core.items.remove(Items.graphite,150);
            state.rules.tags.put("afterglow.relay-funded","true");
        }
        int elapsed=state.rules.tags.getInt("afterglow.relay-time",0)+1;
        state.rules.tags.put("afterglow.relay-time",String.valueOf(elapsed));
        if(elapsed<30||!once("relay-done"))return;
        int kind=state.rules.tags.getInt("frontier.relay-kind",0);
        if(kind==0)grant(2);
        if(kind==1)allies(i,NHUnitTypes.branch,8);
        if(kind==3)allies(i,NHUnitTypes.aliotiat,6);
        if(kind==2||kind==3){
            // Sleeping fixtures are not necessarily in Groups.build. Address the authored tiles.
            int x=Point2.x(pos),y=Point2.y(pos);
            int[][] offsets=kind==2?new int[][]{{-12,12},{-7,12},{7,12},{12,12},{0,12},{0,8}}:new int[][]{{-12,12},{12,12}};
            arc.struct.Seq<Building> fixtures=new arc.struct.Seq<>();
            for(int[]off:offsets){
                var b=world.build(x+off[0],y+off[1]);
                if(b==null||b.team!=Team.derelict)continue;
                if(b.block!=Blocks.largeSolarPanel&&b.block!=Blocks.batteryLarge&&b.block!=Blocks.powerNodeLarge&&b.block!=Blocks.scatter)continue;
                b.changeTeam(state.rules.defaultTeam,false);b.enabled=true;b.noSleep();fixtures.add(b);
            }
            for(var node:fixtures)if(node.block instanceof mindustry.world.blocks.power.PowerNode block){
                for(var other:fixtures)if(other!=node&&other.power!=null&&!node.power.links.contains(other.pos())&&block.linkValid(node,other))node.configureAny(other.pos());
                new mindustry.world.blocks.power.PowerGraph().reflow(node);
            }
        }
        announce("frontier.relay-done");
    }
    static void grant(int scale){
        var core=state.rules.defaultTeam.core();if(core==null)return;
        ItemStack[] stock=ItemStack.with(Items.titanium,1600,Items.silicon,1200,Items.graphite,800,
            Items.thorium,700,Items.surgeAlloy,350,NHItems.presstanium,700,NHItems.juniorProcessor,600,
            NHItems.metalOxhydrigen,700,NHItems.multipleSteel,500,NHItems.zeta,1000);
        for(var s:stock)core.items.add(s.item,Math.min(s.amount*scale,Math.max(0,core.storageCapacity-core.items.get(s.item))));
        announce("frontier.resupply");
    }
    static void allies(int i,UnitType type,int count){
        var m=FrontierMissions.all[i];fleet(type,count,true,m.x/(float)m.width,(m.y+44f)/m.height);
    }
    static void enemies(int i,UnitType type,int count,int x,int y){
        var m=FrontierMissions.all[i];fleet(type,count,false,x/(float)m.width,y/(float)m.height);
    }
    public static String objective(int i){
        var m=FrontierMissions.all[i];
        String goal=m.waves>0?Core.bundle.format("afterglow.frontier.defend",Math.min(m.waves,state.wave-1),m.waves):
            Core.bundle.format("afterglow.hud.attack",state.rules.waveTeam.cores().size);
        if(i==1||i==3||i==8)goal+="\n"+Core.bundle.format("afterglow.frontier.depots",activeDepots(),m.cores.length);
        if(i==3)goal+=" · "+Core.bundle.get(state.wave<=20?"afterglow.frontier.rush-open":"afterglow.frontier.rush-closed");
        if(i==4)goal+="\n"+Core.bundle.format("afterglow.frontier.solar",Math.round(state.rules.solarMultiplier*100));
        if(i==6)goal+="\n"+Core.bundle.get("afterglow.frontier.protect-both");
        if(i==9)goal+="\n"+Core.bundle.format("afterglow.hud.attack",state.rules.waveTeam.cores().size);
        int relay=state.rules.tags.getInt("frontier.relay",-1);
        if(relay!=-1){
            int kind=state.rules.tags.getInt("frontier.relay-kind",0);
            goal+="\n"+FrontierSites.names[kind]+"（"+Point2.x(relay)+","+Point2.y(relay)+"）："+
                (state.rules.tags.containsKey("afterglow.relay-done")?"已修复":state.rules.tags.getInt("afterglow.relay-time",0)+"/30 秒 · 中心建修理投影器并供电");
        }
        return goal;
    }
}
