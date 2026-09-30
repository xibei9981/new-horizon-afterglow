import arc.files.Fi;
import arc.math.geom.Point2;
import arc.util.Time;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.power.PowerGraph;
import mindustry.world.blocks.defense.turrets.Turret;
import newhorizon.content.NHItems;
import newhorizon.content.campaign.*;
import static mindustry.Vars.*;

public class PolishChecks extends CampaignChecks {
    public static void run() throws Exception {
        Time.setDeltaProvider(()->1f);report.setLength(0);
        boolean baseline=Boolean.getBoolean("polish.baseline");
        Fi out=new Fi("campaign-tools/polish-"+(baseline?"0.7.0":"0.8.0"));out.mkdirs();
        for(int c=Integer.getInteger("polish.start",0);c<Integer.getInteger("polish.end",16);c++){
            var p=AfterglowCampaign.sectors[c];p.sector.clearInfo();logic.reset();world.loadSector(p.sector,new WorldParams(){{saveInfo=false;}});logic.play();state.rules.waveTimer=false;
            ores(c);preview(c);new Fi("campaign-tools/previews/mission-"+(c+1)+".png").copyTo(out.child("map-"+(c+1)+".png"));
            if(!baseline){
                check("0.8.0".equals(state.rules.tags.get("landscape.version")),"new ore layout loaded");
                check(state.rules.tags.getInt("landscape.veins",0)>=40,"distributed small ore lenses in every map");
                pathCheck();sites(c);
            }
        }
        String reportName=System.getProperty("polish.start")==null&&System.getProperty("polish.end")==null?"verification.txt":"verification-subset.txt";
        out.child(reportName).writeString(report.toString());
    }
    static void ores(int c){
        int w=world.width(),h=world.height(),tiles=0,patches=0,small=0,large=0,remote=0;
        boolean[] seen=new boolean[w*h],bins=new boolean[64];
        var core=state.rules.defaultTeam.core();
        for(var t:world.tiles)if(t.build==null&&!t.block().isStatic()&&t.overlay().itemDrop!=null&&t.overlay().itemDrop!=Items.scrap){
            tiles++;if(Math.hypot(t.x-core.tileX(),t.y-core.tileY())>90)remote++;
            bins[Math.min(7,t.x*8/w)+8*Math.min(7,t.y*8/h)]=true;
            if(seen[t.array()])continue;
            var queue=new java.util.ArrayDeque<Integer>();queue.add(t.array());seen[t.array()]=true;int size=0;
            while(!queue.isEmpty()){
                int id=queue.remove(),x=id%w,y=id/w;size++;
                for(var d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){
                    var other=world.tile(x+d[0],y+d[1]);if(other==null||other.build!=null||other.block().isStatic()||other.overlay()!=t.overlay()||seen[other.array()])continue;
                    seen[other.array()]=true;queue.add(other.array());
                }
            }
            if(size>=4){patches++;if(size<=100)small++;else large++;}
        }
        int occupied=0;for(boolean b:bins)if(b)occupied++;
        log("ORES map="+(c+1)+" mineableTiles="+tiles+" remoteTiles="+remote+" patches4plus="+patches+" small4to100="+small+" large="+large+" occupied8x8Bins="+occupied+"/64");
    }
    static void sites(int c){
        int count=AfterglowLandmarks.count();check(count==(c>=13?3:2),"every map has complete optional works");
        var ceasefire=new arc.struct.ObjectMap<Turret,Boolean>();for(var b:content.blocks())if(b instanceof Turret t){ceasefire.put(t,t.targetBlocks);t.targetBlocks=false;}
        for(var b:Groups.build)if(b instanceof newhorizon.expand.block.special.JumpGate.JumpGateBuild)b.enabled=false;
        for(int n=0;n<count;n++){
            int pos=state.rules.tags.getInt("landmark.center."+n,-1),x=Point2.x(pos),y=Point2.y(pos),kind=state.rules.tags.getInt("landmark.kind."+n,-1);
            check(world.tile(x,y).build==null,"repair marker is empty");
            // Test-only external commissioning power. It is removed before production proof.
            world.tile(x,y).setBlock(Blocks.mendProjector,Team.sharded,0);var repair=world.build(x,y);repair.enabled=false;
            check(world.tile(x+3,y).build==null,"commissioning power does not overwrite a facility");
            world.tile(x+3,y).setBlock(Blocks.powerSource,Team.sharded,0);var source=world.build(x+3,y);
            var graph=new PowerGraph();graph.add(source);graph.add(repair);repair.power.status=1;
            AfterglowLandmarks.update();check(!state.rules.tags.containsKey("landmark.claimed."+n),"disabled projector cannot claim");
            repair.enabled=true;repair.power.status=0;AfterglowLandmarks.update();check(!state.rules.tags.containsKey("landmark.claimed."+n),"unpowered projector cannot claim");
            repair.power.status=1;
            if(c==0&&n==0){
                // Exercise the real campaign timer and mid-recovery save, not only the helper.
                for(int frame=0;frame<600;frame++)AfterglowCampaign.update();
                check(state.rules.tags.getInt("landmark.progress.0",0)==10,"campaign timer advances recovery once per second");
                var partial=new Fi("campaign-tools/run/polish-partial.msav");mindustry.io.SaveIO.write(partial);mindustry.io.SaveIO.load(partial);state.set(mindustry.core.GameState.State.playing);
                check(state.rules.tags.getInt("landmark.progress.0",0)==10,"partial recovery persists, world loading adds no time");
                repair=world.build(x,y);source=world.build(x+3,y);repair.power.status=1;
                for(int frame=0;frame<540;frame++)AfterglowCampaign.update();
            }else for(int second=0;second<19;second++)AfterglowLandmarks.update();
            check(!state.rules.tags.containsKey("landmark.claimed."+n),"recovery takes actual time");
            if(c==0&&n==0)for(int frame=0;frame<60;frame++)AfterglowCampaign.update();else AfterglowLandmarks.update();
            check(state.rules.tags.containsKey("landmark.claimed."+n),"authored works captured");
            source.tile.remove();new PowerGraph().reflow(repair);
            for(int sec=0;sec<120;sec++){Groups.unit.copy().each(u->u.remove());ticks(60);}
            int products=0,facilities=0;
            for(String encoded:state.rules.tags.get("landmark.blocks."+n,"").split(","))if(!encoded.isEmpty()){
                var b=world.build(Integer.parseInt(encoded));check(b!=null&&b.team==Team.sharded,"recorded facility still present and owned");facilities++;
                if(b.block==newhorizon.content.blocks.SpecialBlock.heavyStorage)products+=kind==2?b.items.get(NHItems.fusionEnergy):b.items.total();
                if(b.power!=null&&b.block.consPower!=null&&!b.block.consPower.buffered)check(b.power.status>.9f,"recovered facility powered "+b.block+" at "+b.tileX()+","+b.tileY());
            }
            check(products>10,"restored factory physically delivers products without commissioning source: kind="+kind+" products="+products);
            log("WORKS map="+(c+1)+" site="+n+" kind="+kind+" buildings="+facilities+" actualProducts="+products+" after120s at="+x+","+y);
        }
        var save=new Fi("campaign-tools/run/polish-save.msav");mindustry.io.SaveIO.write(save);mindustry.io.SaveIO.load(save);
        check(AfterglowLandmarks.owned()==count,"all recovered works persist through save/load");
        for(int s=0;s<30;s++)AfterglowLandmarks.update();check(AfterglowLandmarks.owned()==count,"no repeated capture");
        for(var entry:ceasefire)entry.key.targetBlocks=entry.value;
    }
}
