import arc.*;
import arc.files.Fi;
import arc.graphics.*;
import arc.struct.*;
import arc.util.Time;
import mindustry.content.*;
import mindustry.core.GameState.State;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.CoreBlock;
import newhorizon.NHVars;
import newhorizon.content.NHSectorPresents;
import newhorizon.content.campaign.AfterglowCampaign;
import newhorizon.expand.game.*;
import java.util.ArrayDeque;
import static mindustry.Vars.*;

public class CampaignChecks {
    static final StringBuilder report = new StringBuilder();
    static void check(boolean b,String msg){if(!b)throw new AssertionError(msg);}
    static void log(String msg){System.out.println(msg);report.append(msg).append('\n');}
    public static void run() throws Exception {
        check(AfterglowCampaign.supply.techNode.parent.content==NHSectorPresents.edgeZone,"chapter must follow Edge Zone");
        check(AfterglowCampaign.ridges.techNode.parent.content==AfterglowCampaign.supply,"chapter 2 parent");
        check(AfterglowCampaign.citadel.techNode.parent.content==AfterglowCampaign.ridges,"chapter 3 parent");
        Time.setDeltaProvider(() -> 1f);
        for(int c=0;c<3;c++) verify(c);
        log("PASS: all chapter unlock links, maps, paths, power, waves, save/load and capture conditions.");
        new Fi("campaign-tools/verification.txt").writeString(report.toString());
    }
    static void ticks(int count){for(int i=0;i<count;i++){
        // Execute the native app queue just as a rendered frame does (remote-storage registration,
        // load callbacks, etc.). Without it the headless fixture undercounts logistics power.
        try{var field=arc.backend.headless.HeadlessApplication.class.getDeclaredField("runnables");field.setAccessible(true);((arc.util.TaskQueue)field.get(Core.app)).run();}catch(ReflectiveOperationException e){throw new RuntimeException(e);}
        Time.updateGlobal();asyncCore.begin();logic.update();NHVars.core.update();asyncCore.end();}}
    static void verify(int c) throws Exception {
        SectorPreset p=new SectorPreset[]{AfterglowCampaign.supply,AfterglowCampaign.ridges,AfterglowCampaign.citadel}[c];
        check(p.generator.map.filters().isEmpty(),"authored maps must not randomize ores on load");
        p.sector.clearInfo();
        logic.reset();
        world.loadSector(p.sector,new WorldParams(){{saveInfo=false;}});
        logic.play();
        check(AfterglowCampaign.chapter()==c,"chapter rule tag");
        check(world.width()==new int[]{256,320,384}[c],"actual authored map, not placeholder");
        check(Team.sharded.cores().size==1,"player core");
        check(Team.blue.cores().size==(c==0?0:c==1?2:3),"enemy cores");
        check(!RaidState.enabled()&&!InterventionState.enabled()&&!SpecialEventState.enabled(),"random events disabled on authored missions");
        check(state.rules.attackMode==(c>0),"attack rule");
        check(state.rules.winWave==(c==0?25:0),"wave win condition");
        check(Team.sharded.core().items.get(Items.silicon)>=3000,"startup supplies preserved");
        check(Team.sharded.core().storageCapacity>=state.rules.loadout.max(s -> s.amount).amount,"starting loadout fits");
        pathCheck();
        preview(c);
        ticks(600);
        int powered=0;
        for(Building b:Groups.build){
            if(b.team==Team.sharded&&b.power!=null&&b.block.consPower!=null){
                if(b.power.status>.95f)powered++;
                check(b.power.status>.90f,"starter building has no power: "+b.block.name+" "+b.tile.x+","+b.tile.y+" status="+b.power.status);
            }
        }
        check(powered>=3,"powered starter defense");
        check(!state.gameOver,"startup survives");
        // Startup unloaders fill the new manufacturing buffers before net stocks rise.
        // WorkshopChecks separately empties the core and proves five real output chains.
        check(Team.sharded.core().items.get(Items.titanium)>3900,"startup titanium reserve after manufacturing buffers fill");
        state.wave=1;
        logic.runWave();ticks(240);
        check(Groups.unit.contains(u->u.team==Team.blue),"engine wave spawns actual enemy units");
        Groups.unit.copy().each(u->{if(u.team==Team.blue)u.remove();});
        int[] milestoneWaves=c==0?new int[]{7,8,15,16}:c==1?new int[]{9,10,17,18}:new int[]{7,8,11,12,19,20};
        for(int wave:milestoneWaves){state.wave=wave+1;Events.fire(new EventType.WaveEvent());ticks(150);}
        check(state.rules.tags.containsKey("afterglow.ally"),"ally event fired");
        check(state.rules.tags.containsKey("afterglow.air"),"air event fired");
        check(Groups.unit.contains(u->u.team==Team.sharded&&!u.isPlayer()),"allied spawners produced units");
        int before=Groups.unit.size();
        Events.fire(new EventType.WaveEvent());ticks(1);
        check(Groups.unit.size()<=before,"event repeated on same wave");
        Fi save=new Fi("campaign-tools/run/check-"+c+".msav");
        int savedWave=state.wave;
        SaveIO.write(save);
        SaveIO.load(save);
        state.set(State.playing);
        check(state.wave==savedWave,"wave survives save/load");
        check(state.rules.tags.containsKey("afterglow.ally"),"event persistence");
        check(!RaidState.enabled(),"random raids remain disabled after reload");
        ticks(120);
        check(!state.gameOver,"save reload survives");
        if(c==2){
            int titanBefore=Team.sharded.core().items.get(Items.titanium);
            Team.blue.cores().first().tile.remove();ticks(61);
            check(state.rules.tags.containsKey("afterglow.cache-1"),"capture supplies delivered");
            check(Team.sharded.core().items.get(Items.titanium)>=titanBefore,"capture supplies increase inventory");
            int supplied=Team.sharded.core().items.get(Items.titanium);
            ticks(61);
            check(Team.sharded.core().items.get(Items.titanium)-supplied<100,"supply reward repeats every tick");
        }
        // Resolve combat artificially to test the real engine capture conditions, not player balance.
        Groups.unit.copy().each(u->{if(u.team==Team.blue)u.remove();});
        Groups.bullet.clear();
        if(c==0)state.wave=25;
        else Team.blue.cores().copy().each(core->core.tile.remove());
        ticks(300);
        Groups.unit.copy().each(u->{if(u.team==Team.blue)u.remove();});
        ticks(10);
        check(p.sector.info.wasCaptured,"engine victory captured sector");
        if(c==0)check(AfterglowCampaign.ridges.unlocked(),"mission 2 unlocked");
        if(c==1)check(AfterglowCampaign.citadel.unlocked(),"mission 3 unlocked");
        log("PASS "+p.name+" "+world.width()+"x"+world.height()+" | connected paths, "+powered+" powered consumers, events, saved game, victory");
    }
    static void pathCheck(){
        var core=Team.sharded.core();int w=world.width(),h=world.height();
        boolean[] seen=new boolean[w*h];ArrayDeque<Integer> q=new ArrayDeque<>();
        int start=(core.tileY()+4)*w+core.tileX();q.add(start);seen[start]=true;
        while(!q.isEmpty()){
            int n=q.remove(),x=n%w,y=n/w;
            for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){
                int nx=x+d[0],ny=y+d[1];if(nx<0||ny<0||nx>=w||ny>=h)continue;
                int id=ny*w+nx;Tile t=world.tile(nx,ny);
                // Enemy destructible fortifications are legitimate attack targets, not terrain traps.
                boolean blocked=t.floor().isDeep() || (t.solid()&&(t.block().isStatic()||(t.team()==Team.sharded&&!t.block().teamPassable)));
                if(!seen[id]&&!blocked){seen[id]=true;q.add(id);}
            }
        }
        for(Tile t:world.tiles)if(t.overlay()==Blocks.spawn)check(seen[t.array()],"unreachable spawn at "+t.x+","+t.y);
        for(var enemy:Team.blue.cores())check(seen[enemy.tile.array()],"unreachable enemy fortress");
        for(int n=0;n<state.rules.tags.getInt("landmark.count",0);n++){
            var tile=world.tile(state.rules.tags.getInt("landmark.center."+n,-1));
            check(tile!=null&&seen[tile.array()],"unreachable optional works "+n+" at "+tile);
        }
    }
    static void preview(int c){
        Pixmap pix=new Pixmap(world.width(),world.height());
        for(Tile t:world.tiles){
            int color=t.floor().name.contains("dark-")?0x333145ff:0x555067ff;
            if(t.floor().name.contains("cryonite"))color=0x607581ff;
            if(t.floor().name.contains("plating"))color=0x80828fff;
            if(t.floor().isLiquid)color=0x235a8bff;
            if(t.overlay().itemDrop!=null)color=t.overlay().itemDrop.color.rgba();
            if(t.block().isStatic())color=0x151723ff;
            if(t.build!=null)color=t.team()==Team.sharded?0xf5cb6bff:t.team()==Team.blue?0xed6373ff:0x8a8795ff;
            if(t.overlay()==Blocks.spawn)color=0xff8050ff;
            pix.set(t.x,world.height()-1-t.y,color);
        }
        Fi out=new Fi("campaign-tools/previews/mission-"+(c+1)+".png");out.parent().mkdirs();PixmapIO.writePng(out,pix);pix.dispose();
    }
}
