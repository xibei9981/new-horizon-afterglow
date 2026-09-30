import arc.math.geom.Point2;
import mindustry.content.*;
import mindustry.game.Team;
import mindustry.world.*;
import newhorizon.content.blocks.*;
import static mindustry.Vars.*;

/** Map-wide authored infrastructure and geological ore lenses, serialized into .msav files. */
public final class MapPolish {
    static double noise(int x,int y,int scale,int salt){return FrontierMaps.noise(x,y,scale,salt);}
    static double hash(int x,int y,int salt){return FrontierMaps.hash(x,y,salt);}
    static boolean siteFits(int x,int y,int[][] spawns){
        for(var p:spawns)if(Math.hypot(x-p[0],y-p[1])<45)return false;
        int wet=0;
        for(int dx=-24;dx<=24;dx++)for(int dy=-20;dy<=25;dy++){
            var t=world.tile(x+dx,y+dy);if(t==null||t.build!=null)return false;
            if(t.floor().isLiquid)wet++;
        }
        return wet<160;
    }
    static boolean[] reachable(int sx,int sy){
        int w=world.width(),h=world.height();boolean[] seen=new boolean[w*h];
        var queue=new java.util.ArrayDeque<Integer>();queue.add((sy+4)*w+sx);seen[(sy+4)*w+sx]=true;
        while(!queue.isEmpty()){
            int at=queue.remove();
            for(var d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){
                var t=world.tile(at%w+d[0],at/w+d[1]);if(t==null||seen[t.array()])continue;
                if(t.floor().isDeep()||(t.solid()&&(t.block().isStatic()||(t.team()==Team.sharded&&!t.block().teamPassable))))continue;
                seen[t.array()]=true;queue.add(t.array());
            }
        }
        return seen;
    }
    public static void sites(int c,int sx,int sy,int[][] spawns){
        boolean[] accessible=reachable(sx,sy);
        int wanted=c>=13?3:2,placed=0;
        int[][] preferred=c==13?new int[][]{{127,211},{357,207},{145,329}}:
            c==14?new int[][]{{234,302},{291,149},{162,251}}:
            c==15?new int[][]{{256,310},{156,180},{357,180}}:new int[0][];
        for(int attempt=0;attempt<1500&&placed<wanted;attempt++){
            int x,y;
            if(attempt<preferred.length){x=preferred[attempt][0];y=preferred[attempt][1];}
            else{
                x=28+(int)(hash(attempt,c,7341)*(world.width()-56));
                y=sy+65+(int)(hash(attempt,c,8223)*(world.height()-sy-99));
            }
            boolean compact=attempt>=900;
            if(!accessible[x+y*world.width()]||Math.hypot(x-sx,y-sy)<92||!(compact?smallSiteFits(x,y,spawns):siteFits(x,y,spawns)))continue;
            int kind=compact?3:c>=8?(c+placed)%3:placed%2;
            BaseWorkshop.waystation(x,y,c,kind,placed);
            String name=kind==0?"矿物集散站":kind==1?"硅碳加工厂":kind==2?"聚变燃料工坊":"岸边采矿栈桥";
            var sign=world.tile(x,y-(compact?13:19));sign.setBlock(Blocks.air);if(sign.floor().isLiquid)sign.setFloor(Blocks.metalFloorDamaged.asFloor());
            CampaignMaps.message(x,y-(compact?13:19),Team.sharded,name+"：在中央亮色地板建造修理投影器并供电 20 秒，接管现存矿机、加工、仓储与电站。断电暂停，可存档。产物留在站内仓库，需自行运输；聚变原料库存有限。");
            placed++;
        }
        if(placed!=wanted)throw new IllegalStateException("Incomplete field infrastructure chapter="+(c+1)+" placed="+placed);
        state.rules.tags.put("landmark.count",""+placed);
    }
    static boolean smallSiteFits(int x,int y,int[][] spawns){
        for(var p:spawns)if(Math.hypot(x-p[0],y-p[1])<35)return false;
        int wet=0;for(int dx=-13;dx<=13;dx++)for(int dy=-15;dy<=14;dy++){
            var t=world.tile(x+dx,y+dy);if(t==null||t.build!=null)return false;if(t.floor().isLiquid)wet++;
        }
        return wet<27*30*.6;
    }
    static boolean natural(Tile t){
        return t!=null&&t.build==null&&!t.floor().isLiquid&&!t.block().isStatic()&&
            !t.floor().name.contains("metal")&&!t.floor().name.contains("plating")&&t.overlay()!=Blocks.spawn;
    }
    public static void resources(int c,int sx,int sy){
        int w=world.width(),h=world.height();
        // Keep the starting mineral fields and all actual mine footprints. Break remote
        // circular ore carpets into stratified lenses before adding distributed small veins.
        for(Tile t:world.tiles)if(natural(t)&&t.overlay().itemDrop!=null&&t.overlay().itemDrop!=Items.scrap&&Math.hypot(t.x-sx,t.y-sy)>84){
            double strata=Math.sin(t.x*.27+t.y*.16+noise(t.x,t.y,19,c+903)*4);
            if(strata<-.32||hash(t.x,t.y,c+882)<.12)t.clearOverlay();
        }
        Block[] ores={EnvironmentBlock.oreCopperDense,EnvironmentBlock.oreLeadDense,EnvironmentBlock.oreCoalDense,
            EnvironmentBlock.oreTitaniumDense,EnvironmentBlock.oreSilicarDense,EnvironmentBlock.oreThoriumDense,
            EnvironmentBlock.oreTungstenDense,EnvironmentBlock.oreBerylliumDense,EnvironmentBlock.oreZetaDense};
        int patches=0;
        for(int cy=16;cy<h-15;cy+=22)for(int cx=16;cx<w-15;cx+=22){
            int x=cx+(int)(hash(cx,cy,c+343)*15)-7,y=cy+(int)(hash(cx,cy,c+661)*15)-7;
            Tile at=world.tile(x,y);if(!natural(at)||Math.hypot(x-sx,y-sy)<70)continue;
            // Coal follows sediment, titanium/tungsten the rock; Zeta and thorium are
            // less frequent in early chapters. Nearby lenses share geology, not a checkerboard.
            double region=noise(x,y,58,c+717),pick=hash(cx,cy,c+392);
            int kind=pick<.15?0:pick<.30?1:pick<.45?2:pick<.59?3:pick<.73?4:pick<.82?5:pick<.90?6:pick<.96?7:8;
            if(kind>=5&&c<3&&pick>.87)kind=3;
            if(region<-.2&&kind<5)kind=2;
            int rx=4+(int)(hash(cx,cy,c+944)*6),ry=2+(int)(hash(cx,cy,c+478)*4),tiles=0;
            double angle=region*3.5+noise(x,y,103,193)*2,co=Math.cos(angle),si=Math.sin(angle);
            for(int dx=-12;dx<=12;dx++)for(int dy=-12;dy<=12;dy++){
                Tile t=world.tile(x+dx,y+dy);if(!natural(t)||t.overlay().itemDrop!=null)continue;
                double a=dx*co+dy*si,b=-dx*si+dy*co;
                if(a*a/(rx*rx)+b*b/(ry*ry)> .72+noise(x+dx,y+dy,7,c+481)*.45)continue;
                if(hash(x+dx,y+dy,c+199)<.10)continue;
                t.setOverlay(ores[kind]);tiles++;
            }
            if(tiles>=4)patches++;
        }
        state.rules.tags.put("landscape.version","0.8.0");
        state.rules.tags.put("landscape.veins",""+patches);
        System.out.println("POLISH chapter="+(c+1)+" resourceLenses="+patches+" sites="+state.rules.tags.get("landmark.count","0"));
    }
}
