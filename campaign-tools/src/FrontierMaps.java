import arc.files.Fi;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.struct.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.maps.Map;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.power.PowerNode;
import mindustry.world.meta.Env;
import newhorizon.content.*;
import newhorizon.content.blocks.*;
import newhorizon.content.campaign.FrontierMissions;
import newhorizon.content.campaign.FrontierSites;
import newhorizon.content.units.GroundUnitTypes;
import java.util.Random;
import static mindustry.Vars.*;

/** Ten distinct terrain plans and wave compositions; stores native maps, not runtime terrain patches. */
public class FrontierMaps extends CampaignMaps {
    static FrontierMissions.Mission m;
    static int mission;
    public static void generateAll() throws Exception {
        for(int i=0;i<FrontierMissions.all.length;i++)generateFrontier(i);
    }
    static void generateFrontier(int i) throws Exception {
        mission=i;chapter=i+3;m=FrontierMissions.all[i];w=m.width;h=m.height;sx=m.x;sy=m.y;
        enemies=m.cores;spawns=m.spawns;rng=new Random(710031+i*991L);
        logic.reset();Rules r=frontierRules();state.rules=r;
        world.loadGenerator(w,h,tiles->{
            tiles.fill();landscape();shorelines();scenery();
            localResources(sx,sy);
            base(sx,sy,false);
            if(i==6){localResources(368,76);base(368,76,true);}
            for(int n=0;n<enemies.length;n++)fortress(enemies[n][0],enemies[n][1],n);
            for(int[] p:spawns){clearCircle(p[0],p[1],12);world.tile(p[0],p[1]).setOverlay(Blocks.spawn);}
            repairSite();
            // Guarded forward resource fields encourage expansion out of the initial base.
            for(int n=0;n<4;n++){
                int x=(n%2==0?w/3:2*w/3),y=h/3+(n/2)*h/4;
                if(nearBase(x,y,48))continue;
                clearCircle(x,y,12);ore(x,y,7,n%2==0?EnvironmentBlock.oreThoriumDense:EnvironmentBlock.oreZetaDense);
            }
        });
        linkPower();state.rules=r;
        state.map=new Map(StringMap.of("name","余烬航线 "+(i+4)+" · "+m.name,"author","New Horizon / Afterglow community chapter","description",m.brief));
        state.map.tags.put("genfilters","[]");
        state.map.tags.put("rules",JsonIO.write(r));
        Fi out=new Fi("assets/maps/afterglow-"+m.id+".msav");MapIO.writeMap(out,state.map,false);
        System.out.println("FRONTIER_MAP "+m.id+" "+w+"x"+h+" "+out.length()+" bytes");
    }
    static Rules frontierRules(){
        Rules r=new Rules();r.defaultTeam=playerTeam;r.waveTeam=enemyTeam;
        r.waves=true;r.waveTimer=true;r.waitEnemies=true;r.wavesSpawnAtCores=false;
        r.attackMode=m.attack();
        // The finale has two required stages; only the chapter controller may finish defense.
        r.winWave=m.waves>0&&mission!=9?m.waves+1:0;
        r.waveSpacing=m.spacing*60f;r.initialWaveSpacing=m.grace*60f;
        r.unitCap=mission==7||mission==9?300:240;
        r.coreDestroyClear=true;r.enemyCoreBuildRadius=96;r.canGameOver=true;
        r.env=Env.terrestrial|Env.groundWater|NHContent.radioactive;r.planet=NHPlanets.midantha;
        r.placeRangeCheck=false;r.buildSpeedMultiplier=1.5f;
        r.teams.get(enemyTeam).rtsAi=false;r.teams.get(enemyTeam).buildAi=false;
        r.fog=false;r.staticFog=false;
        r.tags.put("nh-afterglow-chapter",String.valueOf(mission+3));
        r.tags.put("frontier.authored","0.4.0");
        r.tags.put("frontier.depots",String.valueOf(enemies.length));
        r.tags.put("frontier.finite-depots","true");
        r.tags.put("frontier.stable-solar","true");
        r.attributes.set(mindustry.world.meta.Attribute.light,1f);
        r.tags.put("nh-raid-scale","0");r.tags.put("nh-intervention-scale","0");r.tags.put("nh-special-event-enabled","false");
        int industry=mission==0?1000:mission==7?4000:1600;
        r.loadout=ItemStack.list(Items.copper,4500,Items.lead,4500,Items.titanium,5000,Items.silicon,5000,
            Items.graphite,4000,Items.coal,3000,Items.sand,3000,Items.tungsten,3000,Items.thorium,mission==7?2500:1200,
            Items.metaglass,2200,Items.beryllium,2000,Items.carbide,1200,Items.plastanium,1500,Items.surgeAlloy,mission==7?4000:600,
            Items.phaseFabric,600,NHItems.silicar,4500,NHItems.hardLight,2500,NHItems.presstanium,industry,
            NHItems.juniorProcessor,industry,NHItems.metalOxhydrigen,industry,NHItems.multipleSteel,industry,
            NHItems.seniorProcessor,mission==7?1600:400,NHItems.zeta,4000);
        r.spawns=new Seq<>();waves(r);paceWaves(r);return r;
    }
    static void wave(Rules r,UnitType type,int from,int to,int every,int amount,float scaling,int max,int lane){
        SpawnGroup g=new SpawnGroup(type);g.begin=from-1;g.end=to==Integer.MAX_VALUE?to:to-1;
        g.spacing=every;g.unitAmount=amount;g.unitScaling=scaling;g.max=max;g.shieldScaling=0;
        g.spawn=Point2.pack(spawns[lane][0],spawns[lane][1]);r.spawns.add(g);
    }
    static void waves(Rules r){
        var origin=GroundUnitTypes.origin;var thy=GroundUnitTypes.thynomo;
        var ali=NHUnitTypes.aliotiat;var tar=NHUnitTypes.tarlidor;
        var sharp=NHUnitTypes.sharp;var branch=NHUnitTypes.branch;var warper=NHUnitTypes.warper;
        int inf=Integer.MAX_VALUE;
        switch(mission){
            case 0 -> {
                wave(r,origin,1,20,1,5,1.5f,20,0);wave(r,sharp,9,20,4,4,8,6,0);
                wave(r,origin,21,40,1,24,1,44,0);wave(r,thy,23,40,3,4,4,9,0);
                wave(r,origin,41,60,1,40,1,64,0);wave(r,ali,41,60,2,6,3,14,0);
                wave(r,branch,32,60,4,7,4,14,0);wave(r,tar,45,45,1,1,1,1,0);wave(r,tar,60,60,1,3,1,3,0);
            }
            case 1 -> {
                for(int n=0;n<3;n++){wave(r,sharp,1+n,inf,3,4,5,14,n);wave(r,branch,12+n,inf,6,3,7,8,n);}
                wave(r,thy,10,inf,5,4,7,10,0);
            }
            case 2 -> {
                wave(r,origin,1,24,1,10,1.5f,28,0);wave(r,thy,10,24,3,3,5,6,0);
                for(int n=0;n<2;n++){wave(r,origin,25,48,1,14,2,28,n);wave(r,thy,29,48,3,4,4,8,n);}
                for(int n=0;n<3;n++){wave(r,origin,49,72,1,18,2,32,n);wave(r,ali,53,72,3,3,4,8,n);wave(r,tar,72,72,1,1,1,1,n);}
                wave(r,branch,17,72,5,4,8,12,0);
            }
            case 3 -> {
                for(int n=0;n<2;n++){wave(r,origin,1+n,inf,2,8,3,36,n);wave(r,thy,12+n,inf,4,4,6,14,n);}
                wave(r,branch,6,inf,5,5,6,12,1);
            }
            case 4 -> {
                for(int n=0;n<3;n++){
                    wave(r,sharp,1+n,30,3,8,2,20,n);wave(r,branch,21+n,60,3,8,4,18,n);
                    wave(r,warper,51+n,80,4,4,5,10,n);wave(r,sharp,61+n,80,3,26,3,36,n);
                }
                wave(r,thy,15,75,15,8,6,15,2);wave(r,warper,80,80,1,14,1,14,0);
            }
            case 5 -> {
                for(int n=0;n<4;n++){wave(r,origin,1+n,inf,4,7,5,28,n);wave(r,ali,16+n,inf,8,3,10,10,n);}
                wave(r,branch,9,inf,6,5,7,14,3);
            }
            case 6 -> {
                for(int n=0;n<2;n++){
                    wave(r,origin,1+n,60,2,12,3,38,n);wave(r,thy,13+n,60,4,4,7,10,n);
                    wave(r,origin,61,90,1,24,3,38,n);wave(r,ali,61,90,3,5,6,11,n);
                    wave(r,branch,25+n,89,6,5,8,12,n);wave(r,tar,75,90,15,2,10,3,n);
                }
            }
            case 7 -> {
                for(int n=0;n<5;n++){wave(r,thy,1+n,inf,5,5,8,14,n);wave(r,ali,15+n,inf,10,3,12,8,n);}
                wave(r,tar,25,inf,15,2,12,4,4);wave(r,branch,8,inf,7,8,8,16,2);
            }
            case 8 -> {
                for(int n=0;n<3;n++){
                    wave(r,origin,1+n,inf,3,14,3,42,n);wave(r,thy,9+n,inf,6,5,7,14,n);
                    wave(r,branch,16+n,inf,8,6,8,14,n);wave(r,tar,28+n,inf,15,1,15,3,n);
                }
            }
            case 9 -> {
                // Three military phases: combined-arms screening, heavy penetration, final siege.
                for(int n=0;n<3;n++){
                    wave(r,tar,1+n,30,3,8,4,15,n);wave(r,NHUnitTypes.striker,1+n,30,3,4,8,6,n);wave(r,warper,8+n,30,6,3,8,6,n);
                    wave(r,tar,31+n,60,3,10,8,16,n);wave(r,NHUnitTypes.striker,31+n,60,6,4,8,8,n);
                    wave(r,NHUnitTypes.longinus,43+n,99,8,2,12,5,n);
                    wave(r,tar,61+n,99,3,14,8,20,n);wave(r,NHUnitTypes.hurricane,67+n,99,12,1,20,2,n);
                    wave(r,GroundUnitTypes.annihilation,80+n,99,12,1,20,2,n);
                }
                wave(r,NHUnitTypes.longinus,1,30,5,2,20,3,1);wave(r,NHUnitTypes.hurricane,10,30,10,2,20,3,2);
                for(int n=0;n<3;n++){
                    wave(r,tar,100,100,1,16,1,16,n);wave(r,NHUnitTypes.striker,100,100,1,12,1,12,n);wave(r,NHUnitTypes.longinus,100,100,1,4,1,4,n);
                }
                wave(r,NHUnitTypes.guardian,90,90,1,1,1,1,1);
                wave(r,GroundUnitTypes.annihilation,100,100,1,4,1,4,1);
                wave(r,NHUnitTypes.hurricane,100,100,1,3,1,3,0);
                wave(r,NHUnitTypes.pester,100,100,1,1,1,1,2);
            }
        }
    }
    static void paceWaves(Rules r){
        if(m.waves==0)return;
        Seq<SpawnGroup> original=r.spawns; r.spawns=new Seq<>();
        for(int wave=1;wave<=m.waves;wave++){
            int role=FrontierSites.waveRole(mission,wave);
            boolean rest=role==3,assault=role==1,siege=role==2;
            for(var group:original){
                int count=group.getSpawned(wave-1);if(count==0)continue;
                UnitType type=group.type;
                if(mission>=5&&mission<9){
                    if(type==GroundUnitTypes.origin)type=NHUnitTypes.aliotiat;
                    else if(type==GroundUnitTypes.thynomo)type=NHUnitTypes.tarlidor;
                    else if(type==NHUnitTypes.branch)type=NHUnitTypes.striker;
                }
                if(rest){count=Math.max(1,count/3);if(mission<5)type=mission==4?NHUnitTypes.sharp:GroundUnitTypes.origin;}
                SpawnGroup g=new SpawnGroup(type);g.begin=g.end=wave-1;g.unitAmount=g.max=count;g.spawn=group.spawn;g.unitScaling=Float.MAX_VALUE;r.spawns.add(g);
            }
            if(assault||siege){
                int lane=mission==2?(wave<25?0:wave<49?1:2):0;
                UnitType type=assault?((mission==0||mission==2)?GroundUnitTypes.origin:NHUnitTypes.sharp):mission==4?(wave<30?NHUnitTypes.branch:NHUnitTypes.warper):(wave<30?GroundUnitTypes.thynomo:NHUnitTypes.tarlidor);
                if(mission>=5)type=assault?NHUnitTypes.striker:(wave<60?NHUnitTypes.longinus:NHUnitTypes.hurricane);
                wave(r,type,wave,wave,1,assault?((mission==0||mission==2)?18+wave/4:8+wave/10):1+wave/40,Float.MAX_VALUE,30,lane);
            }
        }
    }
    static boolean nearBase(int x,int y,int dist){
        if(Mathf.dst(x,y,sx,sy)<dist||(mission==6&&Mathf.dst(x,y,368,76)<dist))return true;
        for(int[] p:enemies)if(Mathf.dst(x,y,p[0],p[1])<dist)return true;
        return false;
    }
    static void landscape(){
        routes=new boolean[w*h];
        for(Tile t:world.tiles){
            int x=t.x,y=t.y;
            double warp=noise(x,y,53,2)*19, n=noise(x,y,31,6);
            double xx=x+warp, yy=y+noise(x,y,47,7)*15;
            boolean rock=false,water=false;
            switch(mission){
                case 0 -> rock=(yy>158&&Math.abs(xx-210-22*Math.sin(y*.023))>38+noise(x,y,39,4)*18)||n>.65;
                case 1 -> {water=true;}
                case 2 -> rock=Math.abs(xx-150-23*Math.sin(y*.018))<18||Math.abs(xx-300-17*Math.sin(y*.025))<17||n>.65;
                case 3 -> rock=(Math.abs(yy-173-19*Math.sin(x*.021))<21||Math.abs(yy-256-22*Math.sin(x*.018))<19)&&Math.abs(xx-208)>23||n>.61;
                case 4 -> {double d=Math.hypot(xx-208,(yy-208)*1.08);water=d>83&&d<128;rock=n>.64;}
                case 5 -> {water=Math.abs(xx-(226+Math.sin(y*.020)*92+Math.sin(y*.044)*17))<23+noise(x,y,70,3)*12;rock=n>.56;}
                case 6 -> {water=Math.abs(xx-239-16*Math.sin(y*.025))<23&&yy>119;rock=n>.57;}
                case 7 -> rock=(Math.abs(yy-285)<16||Math.abs(yy-410)<14)&&x>60&&x<w-60||n>.66;
                case 8 -> {rock=(Math.abs(xx-164)<20||Math.abs(xx-316)<20)&&yy>145||n>.69;water=!nearBase(x,y,78)&&noise(x,y,54,9)>.42&&!rock;}
                case 9 -> {double d=Math.hypot((xx-256)*1.08,yy-358);rock=Math.abs(d-148)<17||Math.abs(d-74)<14||n>.69;}
            }
            t.setFloor(ground(x,y).asFloor());
            if(water)t.setFloor(Blocks.deepwater.asFloor());
            if(!water&&rock)t.setBlock(cliff(x,y));
            if(x<7||y<7||x>=w-7||y>=h-7){t.setFloor(ground(x,y).asFloor());t.setBlock(cliff(x,y));}
        }
        clearCircle(sx,sy,66);
        switch(mission){
            case 0 -> {corridor(sx,sy,210,180,30);corridor(210,180,210,376,18);clearCircle(210,118,83);}
            case 1 -> {
                clearCircle(100,265,58);clearCircle(334,166,60);clearCircle(330,315,58);
                corridor(96,110,185,165,7);corridor(185,165,145,225,7);corridor(145,225,100,265,9);
                corridor(185,165,334,166,8);corridor(334,166,388,241,6);corridor(388,241,330,315,7);
                for(int[]p:spawns){clearCircle(p[0],p[1],19);}
                corridor(100,265,100,328,9);corridor(334,166,386,230,9);corridor(330,315,384,348,9);
            }
            case 2 -> {for(int[]p:spawns)corridor(sx,sy+45,p[0],p[1],16);corridor(70,185,378,185,11);}
            case 3 -> {
                corridor(sx,sy,208,220,13);corridor(208,220,92,303,11);corridor(208,220,324,303,11);
                corridor(65,100,50,300,9);corridor(351,100,366,300,9);
                corridor(sx,sy,65,100,13);corridor(sx,sy,351,100,13);
                corridor(50,300,92,303,10);corridor(366,300,324,303,10);
                corridor(92,303,54,368,12);corridor(324,303,362,368,12);
            }
            case 4 -> {for(int[]p:spawns)corridor(sx,sy,p[0],p[1],10);clearCircle(sx,sy,70);}
            case 5 -> {
                int px=sx,py=sy;for(int[]p:enemies){corridor(px,py,p[0],p[1],16);px=p[0];py=p[1];}
                for(int n=0;n<spawns.length;n++)corridor(enemies[n][0],enemies[n][1],spawns[n][0],spawns[n][1],12);
            }
            case 6 -> {clearCircle(368,76,66);corridor(112,76,368,76,29);corridor(112,100,112,397,22);corridor(368,100,368,397,22);corridor(112,245,368,245,10);}
            case 7 -> {
                corridor(sx,sy,256,325,32);corridor(108,222,402,222,20);corridor(130,369,382,369,18);
                for(int[]p:enemies)corridor(256,Math.min(340,p[1]),p[0],p[1],16);
                for(int n=0;n<spawns.length;n++)corridor(enemies[n][0],enemies[n][1],spawns[n][0],spawns[n][1],12);
            }
            case 8 -> {for(int n=0;n<3;n++){corridor(sx,sy,enemies[n][0],enemies[n][1],15);corridor(enemies[n][0],enemies[n][1],spawns[n][0],spawns[n][1],11);}corridor(70,205,410,205,10);}
            case 9 -> {
                for(int[]p:spawns)corridor(sx,sy+42,p[0],p[1],18);
                corridor(72,304,94,388,13);corridor(440,304,418,388,13);corridor(94,388,256,478,13);corridor(418,388,256,478,13);
                corridor(256,315,256,478,12);clearCircle(sx,125,65);
            }
        }
        for(int[]p:enemies)clearCircle(p[0],p[1],45);
        if(mission==1){ // Offshore shoals and smaller outcrops break up the otherwise empty ocean.
            for(int[] p:new int[][]{{46,180,19},{242,285,27},{228,73,22},{401,88,16},{190,349,15}})clearCircle(p[0],p[1],p[2]);
        }
    }
    // Deterministic, coherent fields: large landforms, medium strata and small weathering.
    static boolean[] routes;
    static double hash(int x,int y,int salt){
        long z=x*374761393L+y*668265263L+(mission*991L+salt*127L)*1442695040888963407L;
        z=(z^(z>>>13))*1274126177L;z^=z>>>16;
        return (z&0xffffff)/(double)0xffffff;
    }
    static double value(double x,double y,double scale,int salt){
        x/=scale;y/=scale;int ix=(int)Math.floor(x),iy=(int)Math.floor(y);
        double a=x-ix,b=y-iy;a=a*a*(3-2*a);b=b*b*(3-2*b);
        return ((1-a)*(1-b)*hash(ix,iy,salt)+a*(1-b)*hash(ix+1,iy,salt)+(1-a)*b*hash(ix,iy+1,salt)+a*b*hash(ix+1,iy+1,salt))*2-1;
    }
    static double noise(double x,double y,double scale,int salt){
        return value(x,y,scale,salt)*.64+value(x,y,scale*.46,salt+23)*.26+value(x,y,scale*.19,salt+59)*.10;
    }
    static Block ground(int x,int y){
        double n=noise(x,y,44,13),band=Math.sin(y*.085+x*.031+noise(x,y,61,21)*3);
        return switch(mission){
            case 0 -> {
                double fault=Math.abs(Math.sin(x*.028-y*.016+noise(x,y,65,29)*2));
                yield fault<.08?Blocks.magmarock:fault<.24?Blocks.hotrock:n<-.25?Blocks.charr:n>.1?Blocks.basalt:Blocks.darksand;
            }
            case 1 -> n>.25?EnvironmentBlock.conglomerateDense:n>-.1?EnvironmentBlock.conglomerateSparse:n>-.32?Blocks.darksand:Blocks.sand;
            case 2 -> band>.48?Blocks.yellowStone:band>-.05?Blocks.sand:band>-.60?EnvironmentBlock.erodeRock:EnvironmentBlock.erodeRockDense;
            case 3 -> n>.30?Blocks.sporeMoss:n>-.12?Blocks.moss:n>-.38?Blocks.shale:Blocks.dirt;
            case 4 -> n>.27?EnvironmentBlock.cryonite:n>.04?EnvironmentBlock.cryoniteSparse:n>-.23?Blocks.iceSnow:Blocks.snow;
            case 5 -> n>.26?EnvironmentBlock.siliceoustone:n>-.10?Blocks.dacite:n>-.33?Blocks.stone:Blocks.darksand;
            case 6 -> band>.5?Blocks.salt:band>-.1?EnvironmentBlock.conglomerateSparse:band>-.6?Blocks.dacite:Blocks.stone;
            case 7 -> n>.28?Blocks.ferricCraters:n>-.06?Blocks.ferricStone:n>-.33?Blocks.basalt:Blocks.darksand;
            case 8 -> n>.22?Blocks.salt:n>-.05?Blocks.mud:n>-.31?Blocks.shale:Blocks.sporeMoss;
            default -> {
                double d=Math.hypot(x-256,y-358),scar=Math.abs(Math.sin(Math.atan2(y-358,x-256)*5+d*.013+noise(x,y,45,51)));
                yield scar<.16?EnvironmentBlock.zetaCrystalFloor:d<70?EnvironmentBlock.thoriumStoneDense:d<105?EnvironmentBlock.thoriumStoneSparse:n>.15?Blocks.craters:n>-.25?EnvironmentBlock.darkConglomerate:Blocks.charr;
            }
        };
    }
    static Block cliff(int x,int y){
        return switch(mission){
            case 0 -> Blocks.carbonWall;
            case 1 -> EnvironmentBlock.conglomerateWall;
            case 2 -> ground(x,y)==Blocks.yellowStone?Blocks.yellowStoneWall:Blocks.sandWall;
            case 3 -> ground(x,y)==Blocks.sporeMoss||ground(x,y)==Blocks.moss?Blocks.sporeWall:Blocks.shaleWall;
            case 4 -> Blocks.iceWall;
            case 5,6 -> Blocks.daciteWall;
            case 7 -> Blocks.ferricStoneWall;
            case 8 -> ground(x,y)==Blocks.salt?Blocks.saltWall:Blocks.shaleWall;
            default -> Math.hypot(x-256,y-358)<110?EnvironmentBlock.thoriumStoneWall:EnvironmentBlock.darkConglomerateWall;
        };
    }
    static void clearCircle(int cx,int cy,int radius){
        int extent=(int)(radius*1.2)+3;
        for(int y=Math.max(7,cy-extent);y<Math.min(h-7,cy+extent+1);y++)for(int x=Math.max(7,cx-extent);x<Math.min(w-7,cx+extent+1);x++){
            // A guaranteed inner clearance plus coherent, weathered edges, not a perfect disc.
            double edge=radius*(1+noise(x,y,24,35)*.22);
            if(Math.hypot(x-cx,y-cy)>edge)continue;
            Tile t=world.tile(x,y);if(t.block().isStatic()||t.block() instanceof mindustry.world.blocks.environment.Prop)t.setBlock(Blocks.air);
            if(t.floor().isLiquid)t.setFloor(ground(x,y).asFloor());

        }
    }
    static void corridor(int x1,int y1,int x2,int y2,int r){
        double len=Math.hypot(x2-x1,y2-y1);if(len<1){clearCircle(x1,y1,r);return;}
        for(int step=0;step<=len;step++){
            double u=step/len,bend=Math.sin(u*Math.PI)*Math.sin(u*Math.PI*2+mission)*Math.min(13,len*.09);
            int x=(int)Math.round(x1+(x2-x1)*u-(y2-y1)/len*bend),y=(int)Math.round(y1+(y2-y1)*u+(x2-x1)/len*bend);
            clearCircle(x,y,(int)(r*(1+.13*Math.sin(u*11+mission))));
            for(int dx=-r;dx<=r;dx++)for(int dy=-r;dy<=r;dy++){
                if(dx*dx+dy*dy>r*r||x+dx<0||y+dy<0||x+dx>=w||y+dy>=h)continue;
                routes[x+dx+(y+dy)*w]=true;
            }
        }
    }
    static void shorelines(){
        // Distance from each side of the coast provides continuous beaches and three-depth water.
        int[] coast=new int[w*h];java.util.Arrays.fill(coast,9999);
        java.util.ArrayDeque<Integer> queue=new java.util.ArrayDeque<>();
        for(Tile t:world.tiles)for(int[]d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){
            Tile next=world.tile(t.x+d[0],t.y+d[1]);
            if(next!=null&&t.floor().isLiquid!=next.floor().isLiquid){int pos=t.x+t.y*w;coast[pos]=0;queue.add(pos);break;}
        }
        while(!queue.isEmpty()){
            int p=queue.remove(),x=p%w,y=p/w;
            if(coast[p]>=12)continue;
            for(int[]d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){int nx=x+d[0],ny=y+d[1];if(nx<0||ny<0||nx>=w||ny>=h)continue;int q=nx+ny*w;
                if(coast[q]>coast[p]+1){coast[q]=coast[p]+1;queue.add(q);}
            }
        }
        for(Tile t:world.tiles){
            int d=coast[t.x+t.y*w];boolean wet=t.floor().isLiquid;
            if(wet){
                Block floor=mission==8?(d<3?Blocks.darksandTaintedWater:d<8?Blocks.taintedWater:Blocks.deepTaintedWater):d<3?(mission==1?Blocks.sandWater:Blocks.darksandWater):d<8?Blocks.water:Blocks.deepwater;
                t.setFloor(floor.asFloor());
            }else if(d<5+noise(t.x,t.y,18,34)*3&&!nearBase(t.x,t.y,52)){
                t.setFloor((mission==4?Blocks.ice:mission==8?Blocks.salt:mission==1?Blocks.sand:Blocks.darksand).asFloor());
            }
        }
    }
    static void scenery(){
        // Environmental storytelling: collapsed facilities and exposed mineral outcrops off the routes.
        int wanted=mission==7?20:mission==3||mission==8?12:6,placed=0;
        for(int attempt=0;attempt<700&&placed<wanted;attempt++){
            int x=25+rng.nextInt(w-50),y=25+rng.nextInt(h-50),rw=7+rng.nextInt(10),rh=5+rng.nextInt(9);
            if(nearBase(x,y,mission==1?74:87))continue;
            boolean valid=true;
            for(int dx=-rw-3;dx<=rw+3&&valid;dx++)for(int dy=-rh-3;dy<=rh+3;dy++){
                Tile t=world.tile(x+dx,y+dy);if(t==null||t.floor().isLiquid||routes[t.x+t.y*w]||t.block()!=Blocks.air){valid=false;break;}
            }
            if(!valid)continue;
            for(int dx=-rw;dx<=rw;dx++)for(int dy=-rh;dy<=rh;dy++){
                Tile t=world.tile(x+dx,y+dy);double wear=noise(t.x,t.y,9,80+placed);
                if(wear>-.32)t.setFloor((Math.abs(dx)%7==0?Blocks.metalFloor2:Blocks.metalFloorDamaged).asFloor());
                if((Math.abs(dx)==rw||Math.abs(dy)==rh)&&hash(t.x,t.y,88)>.32&&Math.abs(dx)>2&&Math.abs(dy)>2)t.setBlock(Blocks.metalWall1);
                else if(wear<-.1&&hash(t.x,t.y,90)>.87)t.setOverlay(Blocks.oreScrap);
            }
            placed++;
        }
        if(mission==1){
            // A deliberately placed offshore wreck; random ruin siting cannot fit these small islets.
            for(int dx=-14;dx<=14;dx++)for(int dy=-5;dy<=5;dy++){
                if(Math.abs(dx)>10&&Math.abs(dy)>3)continue;
                Tile t=world.tile(242+dx,285+dy);
                if(t.floor().isLiquid)throw new IllegalStateException("Offshore wreck must remain on its shoal");
                t.setFloor((dy==0?Blocks.metalFloor3:Blocks.metalFloorDamaged).asFloor());
                if(Math.abs(dy)==5&&dx<8&&hash(t.x,t.y,117)>.3)t.setBlock(Blocks.metalWall1);
                else if(hash(t.x,t.y,118)>.67)t.setOverlay(Blocks.oreScrap);
            }
            placed++;
        }
        for(Tile t:world.tiles){
            int x=t.x,y=t.y;
            if(t.block()!=Blocks.air||t.floor().isLiquid||nearBase(x,y,52))continue;
            double v=hash(x,y,103);
            if(v<.012){
                Block prop=switch(mission){case 0 -> Blocks.basaltBoulder;case 1,2 -> Blocks.sandBoulder;case 3 -> Blocks.sporeCluster;case 4 -> Blocks.snowBoulder;case 5,6 -> Blocks.daciteBoulder;case 7 -> Blocks.ferricBoulder;case 8 -> Blocks.shaleBoulder;default -> EnvironmentBlock.darkConglomerateBoulder;};
                if(!prop.solid)t.setBlock(prop);
            }
            if(v>.995&&!routes[x+y*w]&&noise(x,y,28,105)>.05)t.setBlock(mission==4||mission==9?EnvironmentBlock.oreClusterZeta:mission==2||mission==5?EnvironmentBlock.oreClusterSlicar:EnvironmentBlock.oreClusterTitanium);
        }
        state.rules.tags.put("frontier.ruins",String.valueOf(placed));
    }
    static void ore(int cx,int cy,int radius,Block ore){
        // Meandering mineral lenses, with different orientation per field and chapter.
        double angle=hash(cx,cy,120)*Math.PI,c=Math.cos(angle),s=Math.sin(angle);
        int reach=radius*2;
        for(int dy=-reach;dy<=reach;dy++)for(int dx=-reach;dx<=reach;dx++){
            Tile t=world.tile(cx+dx,cy+dy);if(t==null||t.build!=null||t.floor().isLiquid)continue;
            double a=(dx*c+dy*s)/(radius*1.45),b=(-dx*s+dy*c+Math.sin(a*3)*radius*.19)/(radius*.65);
            if(a*a+b*b<1+noise(t.x,t.y,6,121)*.35){t.setBlock(Blocks.air);t.setOverlay(ore);}
        }
    }
    static int mineOffset(){return (mission%2==0?-1:1)*(28+5*(mission%3));}
    static void localResources(int x,int y){
        int mine=mineOffset();
        clearCircle(x+mine,y,17);ore(x+mine,y,12,EnvironmentBlock.oreTitaniumDense);
        Block[] ores={EnvironmentBlock.oreSilicarDense,EnvironmentBlock.oreCoalDense,EnvironmentBlock.oreCopperDense,EnvironmentBlock.oreLeadDense};
        for(int n=0;n<ores.length;n++){
            double angle=(45+n*83+mission*23)*Math.PI/180;
            int radius=42+(mission+n)%3*6,ox=(int)(Math.cos(angle)*radius),oy=(int)(Math.sin(angle)*radius);
            // Keep other deposits away from the guaranteed starter titanium vein.
            if(Math.hypot(ox-mine,oy)<27){oy+=oy>=0?25:-25;}
            clearCircle(x+ox,y+oy,15);ore(x+ox,y+oy,10,ores[n]);
        }
        for(int dx=-2;dx<=2;dx++)for(int dy=-2;dy<=2;dy++)world.tile(x+mine+dx,y+dy).setOverlay(EnvironmentBlock.oreTitaniumDense);
        int poolX=x-(mine<0?-51:51),poolY=y-27;
        for(int dy=-11;dy<=11;dy++)for(int dx=-13;dx<=13;dx++){
            Tile t=world.tile(poolX+dx,poolY+dy);double d=dx*dx/(mission%2==0?1.75:.85)+dy*dy;
            if(d<82+noise(t.x,t.y,10,140)*35){t.setBlock(Blocks.air);t.setFloor((d>45?Blocks.sandWater:Blocks.water).asFloor());t.clearOverlay();}
        }
        for(int dy=-9;dy<=9;dy++)for(int dx=-16;dx<=16;dx++){
            Tile t=world.tile(x+dx,y-48+dy);
            if(dx*dx/2.9+dy*dy<70+noise(t.x,t.y,12,141)*35){t.setBlock(Blocks.air);t.setFloor(Blocks.sand.asFloor());t.clearOverlay();}
        }
    }
    static void base(int x,int y,boolean secondary){
        BaseWorkshop.player(x,y,mission+3,secondary);
    }
    static int fx,fy,turn;
    static int rx(int dx,int dy){return turn==0?dx:turn==1?-dy:turn==2?-dx:dy;}
    static int ry(int dx,int dy){return turn==0?dy:turn==1?dx:turn==2?-dy:-dx;}
    static Building fort(Block b,int dx,int dy){
        int half=b.size%2==0?1:0;
        int x=fx+(rx(dx*2+half,dy*2+half)-half)/2,y=fy+(ry(dx*2+half,dy*2+half)-half)/2,off=-(b.size-1)/2;
        for(int a=0;a<b.size;a++)for(int c=0;c<b.size;c++){
            Tile tile=world.tile(x+off+a,y+off+c);if(tile.floor().isLiquid)tile.setFloor(ground(tile.x,tile.y).asFloor());
        }
        return place(b,x,y,enemyTeam);
    }
    static void fortress(int x,int y,int index){
        BaseWorkshop.enemy(x,y,mission+3,index);
    }
    static void repairSite(){
        int[] at=FrontierSites.positions[mission];int x=at[0],y=at[1];
        int bx=sx,by=sy;double best=Double.MAX_VALUE;
        for(Tile t:world.tiles)if(routes[t.x+t.y*w]){
            double d=Math.hypot(t.x-x,t.y-y);if(d<best){best=d;bx=t.x;by=t.y;}
        }
        corridor(bx,by,x,y,13);clearCircle(x,y,26);
        for(int[]p:new int[][]{{-24,0},{24,0},{0,-26}})corridor(x,y,x+p[0],y+p[1],10);
        ore(x-24,y,11,EnvironmentBlock.oreTungstenDense);ore(x+24,y,11,EnvironmentBlock.oreThoriumDense);ore(x,y-26,11,EnvironmentBlock.oreBerylliumDense);
        for(int dx=-7;dx<=7;dx++)for(int dy=-7;dy<=7;dy++){
            Tile t=world.tile(x+dx,y+dy);t.setBlock(Blocks.air);t.clearOverlay();t.setFloor((Math.abs(dx)<=1&&Math.abs(dy)<=1?EnvironmentBlock.platingFloor1:Blocks.metalFloorDamaged).asFloor());
        }
        state.rules.tags.put("frontier.relay",String.valueOf(Point2.pack(x,y)));
        int kind=FrontierSites.kinds[mission];state.rules.tags.put("frontier.relay-kind",String.valueOf(kind));
        message(x+6,y-5,playerTeam,"可修复遗迹 · "+FrontierSites.names[kind]+"\n在中心完整金属地板上建造修理投影器并供电。修复消耗 300 硅 / 200 钛 / 150 石墨，持续 30 秒。拆除或断电暂停，不会重复扣料。\n"+FrontierSites.rewards[kind]);
        if(kind==2){
            for(int dx:new int[]{-12,-7,7,12})place(Blocks.largeSolarPanel,x+dx,y+12,Team.derelict);
            place(Blocks.batteryLarge,x,y+12,Team.derelict);place(Blocks.powerNodeLarge,x,y+8,Team.derelict);
        }
        if(kind==3){
            for(int dx:new int[]{-12,12})ammo(place(Blocks.scatter,x+dx,y+12,Team.derelict),Items.lead,150);
        }
    }
    static void linkPower(){
        Seq<Building> all=new Seq<>();for(Tile t:world.tiles)if(t.isCenter()&&t.build!=null&&t.build.power!=null)all.add(t.build);
        for(int pass=0;pass<2;pass++)for(var b:all)if(b.block instanceof PowerNode node)
            for(var other:all)if((other.block instanceof PowerNode)==(pass==0)&&b!=other&&b.team==other.team&&b.power.graph!=other.power.graph&&node.linkValid(b,other))b.configureAny(other.pos());
    }
}
