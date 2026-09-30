import arc.files.Fi;
import arc.math.geom.Point2;
import arc.struct.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.maps.Map;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import newhorizon.content.*;
import newhorizon.content.blocks.*;
import newhorizon.content.campaign.SignatureCampaign;
import newhorizon.content.units.GroundUnitTypes;
import java.util.Random;
import static mindustry.Vars.*;

/** Authored macro layouts: an asymmetric split canyon, salt-lake causeways and a concentric ruin city. */
public class SignatureMaps extends CampaignMaps {
    static int op;
    static boolean[] roads;
    public static void generateAll() throws Exception {for(int i=0;i<3;i++)generateOperation(i);}
    static double noise(int x,int y,int scale,int salt){return FrontierMaps.noise(x,y,scale,salt+op*77);}
    static double hash(int x,int y,int salt){return FrontierMaps.hash(x,y,salt+op*77);}
    static Block floor(int x,int y){
        double n=noise(x,y,45,13),strata=Math.sin(y*.07+x*.024+n*3);
        if(op==0)return strata>.5?Blocks.redStone:strata>-.05?Blocks.redmat:strata>-.65?Blocks.sand:Blocks.ferricStone;
        if(op==1)return n>.27?Blocks.salt:n>-.04?Blocks.sand:n>-.3?Blocks.dacite:Blocks.darksand;
        double r=Math.hypot(x-256,y-310);
        return r<72?EnvironmentBlock.zetaCrystalFloor:n>.25?Blocks.sporeMoss:n>-.02?Blocks.moss:n>-.3?Blocks.shale:Blocks.dacite;
    }
    static Block cliff(int x,int y){return op==0?Blocks.redStoneWall:op==1?Blocks.saltWall:floor(x,y)==Blocks.moss||floor(x,y)==Blocks.sporeMoss?Blocks.sporeWall:Blocks.daciteWall;}
    static void generateOperation(int index) throws Exception {
        op=index;chapter=SignatureCampaign.first+op;w=SignatureCampaign.sizes[op][0];h=SignatureCampaign.sizes[op][1];
        sx=SignatureCampaign.starts[op][0];sy=SignatureCampaign.starts[op][1];spawns=SignatureCampaign.spawns[op];
        rng=new Random(831777L+op*113);roads=new boolean[w*h];logic.reset();state.rules=rules();Rules r=state.rules;
        world.loadGenerator(w,h,tiles->{
            tiles.fill();geology();routes();landmarks();resourcesAt(sx,sy);starter();
            if(op==0){
                BaseWorkshop.bridgeHead(240,183);
                var control=place(Blocks.switchBlock,SignatureCampaign.switchX,SignatureCampaign.switchY,playerTeam);control.configureAny(true);
                message(sx+16,sy-11,playerTeam,"调度台：点击左边黄色开关。开启=西峡；关闭=东峡。每六波双路齐攻，空军独立。开关被毁保持上次方向，可原位重建。下一波方向显示在 HUD。");
            }
            if(op==1)quarry();
            if(op==2)citadel();
            for(int[] p:spawns){clearing(p[0],p[1],15);world.tile(p[0],p[1]).setOverlay(Blocks.spawn);}
            for(Tile t:world.tiles)if(t.block()==Blocks.air&&t.build==null&&!t.floor().isLiquid&&!roads[t.array()]&&Math.hypot(t.x-sx,t.y-sy)>62&&hash(t.x,t.y,93)<.018)
                t.setBlock(op==0?Blocks.sandBoulder:op==1?Blocks.daciteBoulder:Blocks.sporeCluster);
            MapPolish.sites(chapter,sx,sy,spawns);
            BaseWorkshop.territory(chapter,spawns);
            MapPolish.resources(chapter,sx,sy);
        });
        FrontierMaps.linkPower();state.rules=r;
        state.map=new Map(StringMap.of("name","余烬航线 "+(chapter+1)+" · "+SignatureCampaign.names[op],"author","Afterglow community / New Horizon","description",SignatureCampaign.briefs[op]));
        state.map.tags.put("genfilters","[]");state.map.tags.put("rules",JsonIO.write(r));
        Fi out=new Fi("assets/maps/afterglow-"+SignatureCampaign.ids[op]+".msav");MapIO.writeMap(out,state.map,false);
        System.out.println("SIGNATURE_MAP "+SignatureCampaign.ids[op]+" "+w+"x"+h+" "+out.length()+" bytes");
    }
    static Rules rules(){
        Rules r=new Rules();r.defaultTeam=playerTeam;r.waveTeam=enemyTeam;r.waves=true;r.waveTimer=true;r.waitEnemies=true;r.wavesSpawnAtCores=false;
        r.attackMode=false;r.winWave=op==0?73:0;r.waveSpacing=(op==0?42:op==1?48:65)*60;r.initialWaveSpacing=(op==0?480:op==1?600:240)*60;
        r.unitCap=260;r.coreDestroyClear=true;r.enemyCoreBuildRadius=80;r.canGameOver=true;r.env=Env.terrestrial|Env.groundWater|NHContent.radioactive;r.planet=NHPlanets.midantha;
        r.placeRangeCheck=false;r.buildSpeedMultiplier=1.5f;r.teams.get(enemyTeam).rtsAi=false;r.teams.get(enemyTeam).buildAi=false;
        r.tags.put(newhorizon.content.campaign.AfterglowCampaign.tag,""+chapter);r.tags.put("signature.authored","0.5.0");r.tags.put("frontier.stable-solar","true");r.attributes.set(Attribute.light,1);
        r.tags.put("nh-raid-scale","0");r.tags.put("nh-intervention-scale","0");r.tags.put("nh-special-event-enabled","false");
        r.loadout=ItemStack.list(Items.copper,4500,Items.lead,4500,Items.titanium,5000,Items.silicon,5000,Items.graphite,4000,Items.coal,3000,Items.sand,3000,
            Items.tungsten,2500,Items.thorium,op==1?600:1500,Items.metaglass,2200,Items.beryllium,2000,Items.carbide,1200,Items.plastanium,1500,Items.surgeAlloy,1600,Items.phaseFabric,800,
            NHItems.silicar,5000,NHItems.hardLight,3000,NHItems.presstanium,2200,NHItems.juniorProcessor,2000,NHItems.metalOxhydrigen,2200,NHItems.multipleSteel,2200,NHItems.seniorProcessor,800,NHItems.zeta,op==1?300:3000);
        r.spawns=new Seq<>();
        if(op==0)for(int wave=1;wave<=72;wave++){
            boolean split=wave%6==0,rest=wave%12==1&&wave>1;int phase=(wave-1)/24;
            for(int lane=0;lane<(split?2:1);lane++){
                add(r,NHUnitTypes.tarlidor,wave,wave,1,rest?4:8+phase*4+wave%6,1,26,lane);
                if(wave>=13&&!rest)add(r,NHUnitTypes.longinus,wave,wave,1,2+phase*2,1,10,lane);
                if(wave%12==0)add(r,NHUnitTypes.hurricane,wave,wave,1,phase+1,1,3,lane);
            }
            if(wave%4==0)add(r,NHUnitTypes.striker,wave,wave,1,4+phase*4,1,20,1);
        }
        if(op==1){
            for(int lane=0;lane<2;lane++){
                add(r,NHUnitTypes.aliotiat,1+lane,Integer.MAX_VALUE,2,9,6,24,lane);
                add(r,NHUnitTypes.tarlidor,13+lane,Integer.MAX_VALUE,4,3,12,8,lane);
                add(r,NHUnitTypes.striker,18+lane,Integer.MAX_VALUE,8,6,9,14,lane);
                add(r,NHUnitTypes.aliotiat,33+lane,Integer.MAX_VALUE,6,5,9,12,lane);
                add(r,NHUnitTypes.hurricane,48+lane,Integer.MAX_VALUE,12,1,40,2,lane);
            }
        }
        if(op==2)for(int lane=0;lane<3;lane++){
            add(r,NHUnitTypes.tarlidor,1+lane,Integer.MAX_VALUE,3,4,8,14,lane);
            add(r,NHUnitTypes.striker,9+lane,Integer.MAX_VALUE,6,5,4,16,lane);
            add(r,NHUnitTypes.longinus,18+lane,Integer.MAX_VALUE,6,6,4,18,lane);
            add(r,NHUnitTypes.hurricane,30+lane,Integer.MAX_VALUE,12,1,24,3,lane);
        }
        return r;
    }
    static void add(Rules r,UnitType type,int from,int to,int every,int amount,float scaling,int max,int lane){
        SpawnGroup g=new SpawnGroup(type);g.begin=from-1;g.end=to==Integer.MAX_VALUE?to:to-1;g.spacing=every;g.unitAmount=amount;g.unitScaling=scaling;g.max=max;g.shieldScaling=0;
        g.spawn=Point2.pack(spawns[lane][0],spawns[lane][1]);r.spawns.add(g);
    }
    static void geology(){
        for(Tile t:world.tiles){
            int x=t.x,y=t.y;double n=noise(x,y,52,15);t.setFloor(floor(x,y).asFloor());
            boolean wet=op==0?y>158&&y<453&&Math.abs(x-240-25*Math.sin(y*.021))<18+noise(x,y,23,31)*12:
                op==1?Math.pow((x-278)/226.,2)+Math.pow((y-245)/170.,2)<1+n*.22:false;
            if(wet)t.setFloor((n>.25?Blocks.sandWater:n>-.15?Blocks.water:Blocks.deepwater).asFloor());
            if(!wet&&(n>.12||x<8||y<8||x>w-9||y>h-9))t.setBlock(cliff(x,y));
        }
    }
    static void clearing(int cx,int cy,int radius){
        for(int y=Math.max(8,cy-radius-4);y<Math.min(h-8,cy+radius+5);y++)for(int x=Math.max(8,cx-radius-4);x<Math.min(w-8,cx+radius+5);x++){
            if(Math.hypot(x-cx,y-cy)>radius+noise(x,y,17,42)*4)continue;
            Tile t=world.tile(x,y);if(t.build!=null)continue;t.setBlock(Blocks.air);if(t.floor().isLiquid)t.setFloor(floor(x,y).asFloor());roads[t.array()]=true;
        }
    }
    static void road(int radius,int... points){
        for(int k=0;k<points.length-2;k+=2){int x=points[k],y=points[k+1],nx=points[k+2],ny=points[k+3];double d=Math.hypot(nx-x,ny-y);
            for(int s=0;s<=d;s+=3){double u=s/d;clearing((int)Math.round(x+(nx-x)*u),(int)Math.round(y+(ny-y)*u),radius);}
        }
    }
    static void routes(){
        clearing(sx,sy,67);
        if(op==0){
            road(19,100,462,70,365,153,302,92,232,155,171,240,118);
            road(29,380,462,380,365,353,275,376,195,310,152,240,118);
            clearing(132,322,38);clearing(360,263,48);
        }
        if(op==1){
            road(22,90,80,94,182,66,278,90,394);
            road(20,90,100,200,103,272,166,318,182,380,214,434,214);
            road(24,434,214,496,270,504,350);
            road(16,94,182,180,216,234,302,353,316,496,270);
            clearing(434,214,85);clearing(234,302,35);
        }
        if(op==2){
            road(26,256,76,256,174,126,268,130,365,256,402,256,482,256,532);
            road(23,256,174,386,268,382,365,256,402);
            road(19,126,268,256,310,386,268);
            road(20,58,424,90,360,126,268);road(20,454,424,422,360,386,268);
            clearing(256,310,53);for(var p:SignatureCampaign.terminals)clearing(p[0],p[1],45);clearing(256,482,48);
        }
    }
    static void terrace(int cx,int cy,int rx,int ry){
        for(int y=Math.max(8,cy-ry);y<=Math.min(h-9,cy+ry);y++)for(int x=Math.max(8,cx-rx);x<=Math.min(w-9,cx+rx);x++){
            Tile t=world.tile(x,y);if(t.build!=null)continue;double d=Math.pow((x-cx)/(double)rx,2)+Math.pow((y-cy)/(double)ry,2);
            if(d>1)continue;t.setBlock(Blocks.air);roads[t.array()]=true;
            t.setFloor((d>.82?Blocks.metalFloor3:d>.60?Blocks.metalFloorDamaged:floor(x,y)).asFloor());
        }
    }
    static void ruin(int x,int y,int rx,int ry){
        for(int a=-rx;a<=rx;a++)for(int b=-ry;b<=ry;b++){
            Tile t=world.tile(x+a,y+b);if(t==null||t.build!=null||t.floor().isLiquid)continue;
            if(hash(t.x,t.y,55)<.18)continue;t.setBlock(Blocks.air);t.setFloor((a%7==0?Blocks.metalFloor2:Blocks.metalFloorDamaged).asFloor());
            if((Math.abs(a)==rx||Math.abs(b)==ry)&&Math.abs(a)>4&&Math.abs(b)>4&&hash(t.x,t.y,56)>.25)t.setBlock(Blocks.metalWall1);
            else if(hash(t.x,t.y,57)>.85)t.setOverlay(Blocks.oreScrap);
        }
    }
    static void landmarks(){
        if(op==0){
            // A breached aqueduct spanning the natural river, plus cut-stone switchbacks.
            terrace(240,172,62,12);ruin(240,191,40,7);ruin(66,330,15,24);ruin(419,283,15,26);
            for(int y=226;y<439;y+=39)for(int dx:new int[]{-29,29}){
                int x=240+(int)(25*Math.sin(y*.021))+dx;ruin(x,y,4,7);
            }
            vein(128,324,15,EnvironmentBlock.oreThoriumDense);vein(367,260,17,EnvironmentBlock.oreZetaDense);
        }
        if(op==1){
            // Curved evaporation pans and the central broken pumping station.
            for(int k=0;k<6;k++){int x=151+k*43,y=348+(int)(14*Math.sin(k));terrace(x,y,17,30);ruin(x,y,12,24);}
            terrace(234,302,28,27);ruin(234,302,20,18);
            terrace(434,214,75,62);terrace(416,215,59,48);terrace(398,213,44,34);
            ruin(450,148,22,10);ruin(312,165,20,10);
        }
        if(op==2){
            // Discontinuous concentric city walls leave four explicit gates, not impassable rings.
            for(Tile t:world.tiles){
                double dx=t.x-256,dy=t.y-310,d=Math.hypot(dx,dy);boolean ring=Math.abs(d-90)<2||Math.abs(d-170)<2;
                if(ring&&!roads[t.array()]&&!t.floor().isLiquid){t.setFloor(Blocks.metalFloorDamaged.asFloor());if(hash(t.x,t.y,63)>.2)t.setBlock(Blocks.metalWall1);}
                if(d<48){t.setBlock(Blocks.air);t.setFloor((d<20?EnvironmentBlock.zetaCrystalFloor:d<27?Blocks.metalFloor3:Blocks.metalFloorDamaged).asFloor());}
            }
            for(int[]p:new int[][]{{170,191},{342,191},{77,312},{435,312},{184,436},{329,436}})ruin(p[0],p[1],18,13);
            for(int[]p:SignatureCampaign.terminals)terrace(p[0],p[1],39,35);
            vein(212,308,13,EnvironmentBlock.oreThoriumDense);vein(299,308,13,EnvironmentBlock.oreZetaDense);
        }
    }
    static void vein(int cx,int cy,int r,Block ore){
        clearing(cx,cy,r+3);
        for(int dy=-r;dy<=r;dy++)for(int dx=-r*2;dx<=r*2;dx++){
            Tile t=world.tile(cx+dx,cy+dy);if(t==null||t.build!=null||t.floor().isLiquid)continue;
            if(dx*dx/2.1+dy*dy<r*r*(.80+noise(t.x,t.y,9,48)*.32)){t.setBlock(Blocks.air);t.setOverlay(ore);}
        }
    }
    static void resourcesAt(int x,int y){
        vein(x-32,y,12,EnvironmentBlock.oreTitaniumDense);vein(x+40,y-4,12,EnvironmentBlock.oreSilicarDense);
        vein(x-36,y+44,12,EnvironmentBlock.oreCoalDense);vein(x+40,y+43,12,EnvironmentBlock.oreCopperDense);vein(x+39,y-38,11,EnvironmentBlock.oreLeadDense);
        vein(x-40,y-34,10,EnvironmentBlock.oreTungstenDense);vein(x+58,y+22,9,EnvironmentBlock.oreBerylliumDense);
        for(int dx=-11;dx<=11;dx++)for(int dy=-6;dy<=6;dy++){
            Tile t=world.tile(x+dx,y-49+dy);t.setBlock(Blocks.air);t.clearOverlay();t.setFloor((dx*dx+dy*dy*2<80?Blocks.water:Blocks.sand).asFloor());
        }
        for(int dx=-2;dx<=2;dx++)for(int dy=-2;dy<=2;dy++)world.tile(x-32+dx,y+dy).setOverlay(EnvironmentBlock.oreTitaniumDense);
    }
    static void starter(){
        BaseWorkshop.player(sx,sy,chapter,false);
    }
    static void solar(int x,int y,Team team,int columns){
        for(int k=0;k<columns;k++)for(int dy:new int[]{0,5})place(Blocks.largeSolarPanel,x+(k-columns/2)*5,y+dy,team);
    }
    static Building service(Block block,int x,int y){
        int low=-(block.size-1)/2;
        for(int dx=low;dx<low+block.size;dx++)for(int dy=low;dy<low+block.size;dy++){
            var t=world.tile(x+dx,y+dy);if(t==null||t.build!=null)throw new IllegalStateException("Port overlap "+block+" at "+x+","+y);
            t.setBlock(Blocks.air);if(t.floor().isLiquid)t.setFloor(Blocks.metalFloorDamaged.asFloor());
        }
        return place(block,x,y,playerTeam);
    }
    static void portBelt(int x,int y,int rotation){
        for(int dx=-3;dx<=3;dx++)for(int dy=-3;dy<=3;dy++){
            var t=world.tile(x+dx,y+dy);if(t!=null&&t.build==null){t.setBlock(Blocks.air);if(t.floor().isLiquid)t.setFloor(Blocks.metalFloorDamaged.asFloor());}
        }
        service(Blocks.titaniumConveyor,x,y).rotation=rotation;
    }
    static void quarry(){
        int x=SignatureCampaign.cargoX,y=SignatureCampaign.cargoY;
        service(Blocks.vault,x,y);message(x+5,y-4,playerTeam,SignatureCampaign.briefs[1]);
        // Separate mineral concessions feed the port along two real, destructible causeways.
        for(int side=0;side<2;side++){
            int mx=side==0?320:506,my=side==0?166:152;
            BaseWorkshop.begin(mx,my,playerTeam,14,-12,-7,12,8);
            for(int dx:new int[]{-7,0,7}){
                BaseWorkshop.mine(dx,0,side==0?Items.thorium:NHItems.zeta,true);
                BaseWorkshop.belt(dx,3,1);
            }
            BaseWorkshop.horizontal(-7,7,4);for(int dx:new int[]{-7,0,7})BaseWorkshop.router(dx,4);
            BaseWorkshop.put(PowerBlock.fluxNodeLargeMK1,0,-5);
        }
        for(int xx=328;xx<=384;xx++)portBelt(xx,170,xx==384?1:0);
        for(int yy=171;yy<=214;yy++)portBelt(384,yy,yy==214?0:1);
        for(int xx=385;xx<=432;xx++)portBelt(xx,214,0);
        for(int xx=514;xx<=536;xx++)portBelt(xx,156,xx==536?1:0);
        for(int yy=157;yy<=214;yy++)portBelt(536,yy,yy==214?2:1);
        for(int xx=535;xx>=436;xx--)portBelt(xx,214,2);
        // A working port fuel plant, armoured storage and NH power generation.
        BaseWorkshop.begin(434,184,playerTeam,14,-10,-6,10,8);BaseWorkshop.fusionFoundry(false);
        for(int gx:new int[]{402,408,414,454,460,466})for(int gy:new int[]{176,184,192}){
            var gen=service(PowerBlock.geologicalPhotothermalGenerator,gx,gy);BaseWorkshop.terrainUnder(gen,Blocks.magmarock);
        }
        service(PowerBlock.armorBatteryLarge,434,203).power.status=1;
        for(int[] n:new int[][]{{340,166},{358,166},{376,166},{376,176},{380,186},{380,204},{398,210},{416,210},{408,180},{408,198},{420,198},{434,198},{434,210},{448,198},{460,180},{460,198},{452,210},{470,210},{488,210},{506,210},{524,210},{528,200},{532,190},{532,172},{520,160}})
            service(PowerBlock.fluxNodeLargeMK1,n[0],n[1]);
        service(Blocks.mendProjector,408,224);service(Blocks.mendProjector,460,224);
        service(PowerBlock.fluxNodeLargeMK1,434,229);
        BaseWorkshop.quarryGuard(x,y);
        for(int dy:new int[]{7,20,30})service(Blocks.powerNodeLarge,x,y+dy);
        // Quay retaining walls protect the power plant without closing the freight approach.
        for(int xx=393;xx<=474;xx++)if(Math.abs(xx-434)>7)for(int yy:new int[]{172,173})
            if(world.tile(xx,yy).build==null)service(DefenseBlock.shapedWall,xx,yy);
        state.rules.tags.put("signature.port-lines","2");
        state.rules.tags.put("signature.port-fuel",""+Point2.pack(434,184));
        for(int k=0;k<6;k++){
            int px=151+k*43,py=348+(int)(14*Math.sin(k));
            terrace(px,py,27,34);
            BaseWorkshop.coastalBastion(px,py,k==5?0:k%3);
        }
    }
    static void citadel(){
        state.rules.tags.put("frontier.depots","4");state.rules.tags.put("frontier.finite-depots","true");
        for(int n=0;n<3;n++){
            int x=SignatureCampaign.terminals[n][0],y=SignatureCampaign.terminals[n][1];
            BaseWorkshop.terminal(x,y,n);
            message(x+41,y-5,playerTeam,"能源终端 "+(n+1)+"：清除敌军电池，在中心三格金属标记上重建大型电池并接电。每秒消耗 600 储能，三处都满足才能同步；断供则同步倒退。北侧太阳能阵列可切断敌人供电，也可留待接管。");
            for(int a=-1;a<=1;a++)for(int b=-1;b<=1;b++)world.tile(x+a,y+b).setFloor(Blocks.metalFloor5.asFloor());
        }
        FrontierMaps.mission=9;BaseWorkshop.enemy(256,510,15,3);
    }
}
