import arc.files.Fi;
import arc.util.Time;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.*;
import newhorizon.content.campaign.*;
import static mindustry.Vars.*;

/** Spatial evidence from the actual shipped maps, independent of generator targets. */
public class TerritoryChecks extends CampaignChecks {
    public static void run() throws Exception {
        Time.setDeltaProvider(()->1f);
        String label=System.getProperty("territory.label","current");
        Fi dir=new Fi("campaign-tools/territory-"+label);dir.mkdirs();
        for(int c=Integer.getInteger("territory.start",0);c<Integer.getInteger("territory.end",16);c++){
            var p=AfterglowCampaign.sectors[c];p.sector.clearInfo();logic.reset();
            world.loadSector(p.sector,new WorldParams(){{saveInfo=false;}});logic.play();state.rules.waveTimer=false;
            StringBuilder rows=new StringBuilder("x\ty\tsize\tteam\tblock\tcategory\trange\n");
            int occupied=0,guns=0,covered=0;boolean[] coverage=new boolean[world.width()*world.height()];
            for(var t:world.tiles){
                if(t.build!=null&&t.team()==Team.blue)occupied++;
                if(!t.isCenter()||t.build==null)continue;
                float range=t.block() instanceof BaseTurret turret?turret.range/tilesize:0;
                rows.append(t.x+"\t"+t.y+"\t"+t.block().size+"\t"+t.team().id+"\t"+t.block().name+"\t"+t.block().category+"\t"+range+"\n");
                if(t.team()==Team.blue&&t.block() instanceof Turret){
                    guns++;for(int y=Math.max(0,(int)(t.y-range));y<Math.min(world.height(),t.y+range+1);y++)
                        for(int x=Math.max(0,(int)(t.x-range));x<Math.min(world.width(),t.x+range+1);x++)
                            if(Math.hypot(x-t.x,y-t.y)<=range)coverage[x+y*world.width()]=true;
                }
            }
            for(boolean b:coverage)if(b)covered++;
            int total=coverage.length;
            log(String.format(java.util.Locale.ROOT,"SPATIAL map=%d size=%dx%d enemyGuns=%d occupiedTiles=%d (%.2f%%) weaponEnvelope=%.2f%% districts=%s",c+1,world.width(),world.height(),guns,occupied,occupied*100./total,covered*100./total,state.rules.tags.get("territory.districts","0")));
            dir.child("map-"+(c+1)+".tsv").writeString(rows.toString());
            preview(c);
            new Fi("campaign-tools/previews/mission-"+(c+1)+".png").copyTo(dir.child("map-"+(c+1)+".png"));
            if(!Boolean.getBoolean("territory.baseline")){
                pathCheck();
                for(var t:world.tiles)if(t.isCenter()&&t.build instanceof Turret.TurretBuild gun&&t.team()==Team.blue){
                    for(var b:Groups.build)if(b.team==Team.sharded&&b.block instanceof mindustry.world.blocks.storage.CoreBlock)
                        check(!gun.within(b,gun.range()+b.block.size*tilesize/2f),"enemy gun attacks landing core before any wave: "+t.x+","+t.y);
                }
                int count=state.rules.tags.getInt("territory.districts",0);
                if(c==12||c==15)check(count>=20&&guns>=280,"late theater has substantial multi-district fortifications");
                if(Boolean.getBoolean("territory.live"))liveDistricts(c,count);
            }
        }
        dir.child("metrics.txt").writeString(report.toString());
    }
    static void liveDistricts(int c,int count){
        var targets=new arc.struct.ObjectMap<Turret,Boolean>();
        for(var b:content.blocks())if(b instanceof Turret gun){targets.put(gun,gun.targetBlocks);gun.targetBlocks=false;}
        for(var b:Groups.build)if(b instanceof newhorizon.expand.block.special.JumpGate.JumpGateBuild)b.enabled=false;
        // Prove local generation rather than allowing every outer position to borrow
        // from the old fortress grid. This mutation exists only in the test fixture.
        var cells=new arc.struct.ObjectIntMap<Building>();
        for(int n=0;n<count;n++){
            int pos=state.rules.tags.getInt("territory.cell."+n,-1),x=arc.math.geom.Point2.x(pos),y=arc.math.geom.Point2.y(pos);
            for(var b:Groups.build)if(b.team==Team.blue&&Math.abs(b.tileX()-x)<=19&&b.tileY()>=y-15&&b.tileY()<=y+24)cells.put(b,n);
        }
        for(var b:Groups.build)if(b.power!=null&&b.block instanceof mindustry.world.blocks.power.PowerNode)
            for(int pos:b.power.links.toArray()){
                var other=world.build(pos);if(other!=null&&cells.get(b,-1)!=cells.get(other,-1))b.configureAny(pos);
            }
        for(int second=0;second<120;second++){Groups.unit.copy().each(u->u.remove());ticks(60);}
        for(int n=0;n<count;n++){
            int pos=state.rules.tags.getInt("territory.cell."+n,-1),x=arc.math.geom.Point2.x(pos),y=arc.math.geom.Point2.y(pos);
            int guns=0,working=0;float power=1;
            for(var b:Groups.build)if(b.team==Team.blue&&Math.abs(b.tileX()-x)<=19&&b.tileY()>=y-15&&b.tileY()<=y+24){
                if(b.power!=null&&b.block.consPower!=null){power=Math.min(power,b.power.status);if(b.power.status<.95)log("DISTRICT_LOW_POWER "+b.block+" at="+b.tileX()+","+b.tileY()+" links="+b.power.links+" produced="+b.power.graph.getLastPowerProduced()+" needed="+b.power.graph.getLastPowerNeeded());}
                if(b instanceof Turret.TurretBuild gun){guns++;if(gun.hasAmmo()&&gun.canConsume())working++;}
            }
            log("DISTRICT map="+(c+1)+" at="+x+","+y+" guns="+guns+" ready="+working+" minPower="+power);
            check(guns==working&&power>.95,"district must have real ammunition, fluid and working independent power");
        }
        if(c==12)districtFire(cells);
        for(var e:targets)e.key.targetBlocks=e.value;
    }
    static void districtFire(arc.struct.ObjectIntMap<Building> cells){
        var samples=new arc.struct.Seq<Turret.TurretBuild>();var kinds=new arc.struct.ObjectSet<mindustry.world.Block>();
        for(var b:Groups.build)if(cells.containsKey(b)&&b instanceof Turret.TurretBuild gun&&
            (b.block==newhorizon.content.blocks.TurretBlock.concentration||b.block==newhorizon.content.NHBlocks.endOfEra||b.block==newhorizon.content.NHBlocks.executor)&&kinds.add(b.block))samples.add(gun);
        check(samples.size==3,"sample laser, siege and anti-armour outer districts");
        var targets=new arc.struct.Seq<Unit>();var shots=new arc.struct.ObjectIntMap<Turret.TurretBuild>();
        float old=UnitTypes.eclipse.health;UnitTypes.eclipse.health=1_000_000_000;
        for(var gun:samples){
            shots.put(gun,gun.totalShots);
            var target=UnitTypes.eclipse.spawn(Team.sharded,gun.x,gun.y-18*tilesize);
            target.health=1_000_000_000;target.apply(StatusEffects.unmoving,100000);target.apply(StatusEffects.disarmed,100000);targets.add(target);
        }
        float power=1;long start=System.nanoTime();
        for(int second=0;second<180;second++){ticks(60);for(var gun:samples)if(gun.power!=null)power=Math.min(power,gun.power.status);}
        double damage=0;for(var t:targets){check(t.isAdded()&&!t.dead,"live fire target survives full 180 seconds");damage+=1_000_000_000-t.health;t.remove();}
        UnitTypes.eclipse.health=old;
        for(var gun:samples){log("DISTRICT_FIRE gun="+gun.block+" shots="+(gun.totalShots-shots.get(gun,0))+" liquid="+gun.liquids+" power="+(gun.power==null?"not-required":gun.power.status));check(gun.totalShots>shots.get(gun,0),"outer battery actually fires native weapon");}
        check(power>.95&&damage>10000,"isolated outer districts maintain power and real damage under sustained fire");
        log("DISTRICT_STRESS seconds=180 targets=3 damage="+damage+" minPower="+power+" elapsedWallSeconds="+(System.nanoTime()-start)/1e9);
    }
}
