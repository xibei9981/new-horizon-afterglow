package newhorizon.content.campaign;

import arc.struct.Seq;
import mindustry.content.Blocks;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.world.blocks.power.PowerNode;
import mindustry.world.blocks.power.PowerGraph;
import static mindustry.Vars.*;

/** Optional physical works. Only authored neutral buildings can be recovered, once per save. */
public final class AfterglowLandmarks {
    public static int count(){return state.rules.tags.getInt("landmark.count",0);}
    public static int owned(){int n=0;for(int i=0;i<count();i++)if(state.rules.tags.containsKey("landmark.claimed."+i))n++;return n;}
    public static void update(){
        if(state.gameOver||(state.isCampaign()&&state.rules.sector.info.wasCaptured))return;
        for(int i=0;i<count();i++){
            if(state.rules.tags.containsKey("landmark.claimed."+i))continue;
            var repair=world.build(state.rules.tags.getInt("landmark.center."+i,-1));
            if(repair==null||repair.block!=Blocks.mendProjector||repair.team!=state.rules.defaultTeam||!repair.enabled||repair.power.status<.9f)continue;
            String progress="landmark.progress."+i;int seconds=state.rules.tags.getInt(progress,0)+1;state.rules.tags.put(progress,""+seconds);
            if(seconds<20)continue;
            Seq<Building> fixtures=new Seq<>();fixtures.add(repair);
            for(String encoded:state.rules.tags.get("landmark.blocks."+i,"").split(","))if(!encoded.isEmpty()){
                var b=world.build(Integer.parseInt(encoded));
                if(b==null||b.team!=Team.derelict)continue;
                b.changeTeam(state.rules.defaultTeam,false);b.enabled=true;b.noSleep();fixtures.add(b);
            }
            for(var b:fixtures)if(b.block instanceof PowerNode node)for(var other:fixtures)
                if(b!=other&&!b.power.links.contains(other.pos())&&node.linkValid(b,other))b.configureAny(other.pos());
            for(var b:fixtures)if(b.power!=null)new PowerGraph().reflow(b);
            state.rules.tags.put("landmark.claimed."+i,"true");
        }
    }
}
