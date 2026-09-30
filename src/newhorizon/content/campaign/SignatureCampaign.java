package newhorizon.content.campaign;

import arc.Core;
import mindustry.content.*;
import mindustry.game.Rules;
import mindustry.gen.Building;
import mindustry.world.blocks.logic.SwitchBlock;
import mindustry.world.blocks.power.PowerGraph;
import newhorizon.content.*;
import newhorizon.content.blocks.*;
import static mindustry.Vars.*;
import static newhorizon.content.campaign.AfterglowCampaign.*;

/** Three authored operations. All progress uses the existing saved campaign tag namespace. */
public final class SignatureCampaign {
    public static final int first=3+FrontierMissions.all.length;
    public static final String[] ids={"redgate","saltworks","triune"};
    public static final String[] names={"赤峡闸口","白海矿驿","三相遗城"};
    public static final int[][] sizes={{480,512},{576,448},{512,576}};
    public static final int[][] starts={{240,74},{90,80},{256,76}};
    public static final int[][][] spawns={{{100,462},{380,462}},{{90,394},{504,350}},{{58,424},{454,424},{256,532}}};
    public static final int[][] terminals={{126,268},{386,268},{256,402}};
    public static final int switchX=252,switchY=63,cargoX=434,cargoY=214;
    public static final int loads=12,thoriumPerLoad=600,zetaPerLoad=300,syncSeconds=180;
    public static final String[] briefs={
        "坚守 72 波。基地黄色调度台上的开关：开=西峡、关=东峡，影响下一波地面主力；每 6 波双路齐攻，空军不受调度影响。西峡狭长，东峡短而宽。第 25、49 波获得一次工业/装甲增援。",
        "在东部货站 (434,214) 建造仓库，运输 12 批矿物：每批 600 钍 + 300 Zeta，每 60 秒装运一批；货物必须实际进入货站仓库。至少守住 48 波；提前交货获得装甲支援，48 波后未完成仍有敌军。货站被毁可原位重建，已发货物不丢失。",
        "夺下三座能源庭院，在标记中心 (126,268)、(386,268)、(256,402) 重建大型电池并连接供电。三处同时有电，累计同步 180 秒，每处每秒消耗 600 储能；缺一处则每秒倒退 2 秒。同步后获得突击部队，再摧毁北部核心。可以先突袭核心，但仍须完成同步。"
    };
    public static void restoreRules(Rules r,int i){
        if(i==0)route(r,r.tags.getInt("afterglow.signature-lane",0));
        if(r.tags.containsKey("afterglow.signature-ready")){r.waveTimer=false;r.winWave=1;}
    }
    public static void prepare(int i){restoreRules(state.rules,i);}
    public static void start(int i){
        Blocks.switchBlock.quietUnlock();Blocks.vault.quietUnlock();Blocks.batteryLarge.quietUnlock();
        if(i==1&&AfterglowTech.available(ProductionBlock.beamMiningFacility))ProductionBlock.beamMiningFacility.quietUnlock();
        if(!once("signature-start"))return;
        if(i==2){fleet(NHUnitTypes.aliotiat,16,true,.5f,.23f);fleet(NHUnitTypes.branch,10,true,.5f,.23f);}
    }
    /** Single-wave authored groups allow selecting a lane without recreating scaling or saved waves. */
    static void route(Rules r,int lane){
        int pos=arc.math.geom.Point2.pack(spawns[0][lane][0],spawns[0][lane][1]);
        for(var g:r.spawns)if(!g.type.flying&&(g.begin+1)%6!=0)g.spawn=pos;
    }
    public static void wave(int i,int w){
        if(i==0){
            if(w%6==5)announce("signature.split");
            if(w==25&&once("signature-industry")){FrontierCampaign.grant(2);announce("signature.industry");}
            if(w==49&&once("signature-armor")){FrontierCampaign.grant(2);fleet(NHUnitTypes.tarlidor,4,true,.5f,.24f);}
        }
        if(i==1&&w==47)announce("signature.cargo-warning");
    }
    public static void update(int i){
        if(state.gameOver||(state.isCampaign()&&state.rules.sector.info.wasCaptured))return;
        if(i==0){
            var b=world.build(switchX,switchY);
            int lane=b instanceof SwitchBlock.SwitchBuild&&b.team==state.rules.defaultTeam?(b.enabled?0:1):state.rules.tags.getInt("afterglow.signature-lane",0);
            state.rules.tags.put("afterglow.signature-lane",""+lane);route(state.rules,lane);
        }
        if(i==1)cargo();
        if(i==2){
            captureGrids();synchronize();
            int clock=state.rules.tags.getInt("afterglow.signature-logistics",0)+1;
            state.rules.tags.put("afterglow.signature-logistics",""+(clock%5));
            if(clock%5==0)FrontierCampaign.refillDepots();
        }
        if((i==1&&sent()>=loads&&state.wave>48)||(i==2&&state.rules.tags.containsKey("afterglow.signature-synced")&&state.rules.waveTeam.cores().isEmpty())){
            // Keep waves=true for the engine's normal capture path; stop the timer and wait for all live enemies.
            state.rules.tags.put("afterglow.signature-ready","true");state.rules.waveTimer=false;state.rules.winWave=1;
        }
    }
    public static int sent(){return state.rules.tags.getInt("afterglow.signature-cargo",0);}
    public static void cargo(){
        if(sent()>=loads)return;
        var b=world.build(cargoX,cargoY);
        if(b==null||b.block!=Blocks.vault||b.team!=state.rules.defaultTeam)return;
        int elapsed=Math.min(60,state.rules.tags.getInt("afterglow.signature-loading",0)+1);
        state.rules.tags.put("afterglow.signature-loading",""+elapsed);
        if(elapsed<60||b.items.get(Items.thorium)<thoriumPerLoad||b.items.get(NHItems.zeta)<zetaPerLoad)return;
        b.items.remove(Items.thorium,thoriumPerLoad);b.items.remove(NHItems.zeta,zetaPerLoad);
        state.rules.tags.put("afterglow.signature-loading","0");state.rules.tags.put("afterglow.signature-cargo",""+(sent()+1));
        if(sent()%4==0){FrontierCampaign.grant(1);fleet(NHUnitTypes.aliotiat,8,true,.72f,.45f);announce("signature.cargo-sent");}
        if(sent()==loads){fleet(NHUnitTypes.tarlidor,4,true,.72f,.45f);announce("signature.cargo-done");}
    }
    public static void captureGrids(){
        for(int n=0;n<3;n++){
            if(terminal(n)==null||!once("signature-grid-"+n))continue;
            int x=terminals[n][0],y=terminals[n][1];
            arc.struct.Seq<Building> fixtures=new arc.struct.Seq<>();fixtures.add(terminal(n));
            for(int dx=-20;dx<=15;dx+=5)for(int dy:new int[]{23,28}){
                var b=world.build(x+dx,y+dy);
                if(b!=null&&b.block==Blocks.largeSolarPanel&&b.team==state.rules.waveTeam){b.changeTeam(state.rules.defaultTeam,false);b.enabled=true;b.noSleep();fixtures.add(b);}
            }
            for(int[]d:new int[][]{{0,-9},{0,9},{0,19},{-13,0},{13,0},{-25,9},{25,9}}){
                var b=world.build(x+d[0],y+d[1]);
                if(b!=null&&b.block==Blocks.powerNodeLarge&&b.team==state.rules.waveTeam){b.changeTeam(state.rules.defaultTeam,false);b.enabled=true;b.noSleep();fixtures.add(b);}
            }
            for(var b:fixtures)if(b.block instanceof mindustry.world.blocks.power.PowerNode node){
                for(var other:fixtures)if(other!=b&&!b.power.links.contains(other.pos())&&node.linkValid(b,other))b.configureAny(other.pos());
                new PowerGraph().reflow(b);
            }
        }
    }
    public static int charge(){return state.rules.tags.getInt("afterglow.signature-sync",0);}
    public static Building terminal(int n){
        var p=terminals[n];var b=world.build(p[0],p[1]);
        return b!=null&&b.block==Blocks.batteryLarge&&b.team==state.rules.defaultTeam&&b.enabled?b:null;
    }
    public static int held(){int n=0;for(int k=0;k<3;k++)if(terminal(k)!=null)n++;return n;}
    public static void synchronize(){
        if(state.rules.tags.containsKey("afterglow.signature-synced"))return;
        java.util.HashMap<PowerGraph,Integer> loadsByGrid=new java.util.HashMap<>();
        boolean ready=true;
        for(int n=0;n<3;n++){
            var b=terminal(n);if(b==null){ready=false;continue;}
            loadsByGrid.merge(b.power.graph,1,Integer::sum);
        }
        for(var entry:loadsByGrid.entrySet())if(entry.getKey().getBatteryStored()+.01f<entry.getValue()*600f)ready=false;
        if(ready)for(var entry:loadsByGrid.entrySet())entry.getKey().useBatteries(entry.getValue()*600f);
        int progress=Math.max(0,Math.min(syncSeconds,charge()+(ready?1:-2)));
        state.rules.tags.put("afterglow.signature-sync",""+progress);
        if(progress==syncSeconds&&once("signature-synced")){
            fleet(NHUnitTypes.tarlidor,6,true,.5f,.55f);fleet(NHUnitTypes.aliotiat,16,true,.5f,.55f);
            FrontierCampaign.grant(3);announce("signature.synced");
        }
    }
    public static String objective(int i){
        if(state.rules.tags.containsKey("afterglow.signature-ready"))return Core.bundle.get("afterglow.signature.clear");
        if(i==0)return Core.bundle.format("afterglow.signature.hud-gate",Math.min(72,state.wave-1),state.rules.tags.getInt("afterglow.signature-lane",0)==0?"WEST / 西":"EAST / 东",state.wave%6==0?"双路 / BOTH":"单路 / SINGLE");
        if(i==1)return Core.bundle.format("afterglow.signature.hud-cargo",sent(),Math.min(48,state.wave-1),state.rules.tags.getInt("afterglow.signature-loading",0));
        return state.rules.tags.containsKey("afterglow.signature-synced")?Core.bundle.format("afterglow.signature.hud-assault",state.rules.waveTeam.cores().size):Core.bundle.format("afterglow.signature.hud-sync",held(),charge());
    }
}
