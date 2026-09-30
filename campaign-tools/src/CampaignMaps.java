import arc.files.Fi;
import arc.graphics.Pixmap;
import arc.graphics.PixmapIO;
import arc.math.Mathf;
import arc.struct.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.Building;
import mindustry.io.*;
import mindustry.maps.Map;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.logic.MessageBlock;
import mindustry.world.blocks.power.PowerNode;
import mindustry.world.meta.Env;
import newhorizon.content.*;
import newhorizon.content.blocks.*;
import newhorizon.content.units.GroundUnitTypes;
import java.util.Random;
import static mindustry.Vars.*;

/** Deterministic, editable mission maps; generated once and shipped as real .msav assets. */
public class CampaignMaps {
    static final String[] ids = {"afterglow-supply", "afterglow-ridges", "afterglow-citadel"};
    static final String[] names = {"余烬航线 I · 断流峡谷", "余烬航线 II · 双脊封锁", "余烬航线 III · 寂光中枢"};
    static final String[] briefs = {
        "穿过边缘地带后，我们找到了失联的补给走廊。\n恢复钛矿输送并建立双向防线。坚守 24 波，清除剩余敌人。\n第 8 波后敌机会从侧翼突入；第 16 波后友军抵达。",
        "敌人把两座能源据点嵌入山脊。\n摧毁北部两座核心。中路最短但火力密集，东西山口可绕后。\n第 10 波后获得友军跃迁增援，第 18 波后敌方空军增援。",
        "两座外围节点为寂光中枢提供掩护。\n摧毁西、东和北方的三座核心。前两座敌方核心被摧毁时各返还一次补给。\n预留防空火力，建立持续的跃迁部队生产。"
    };
    static int chapter,w,h,sx,sy;
    static int[][] enemies,spawns;
    static Random rng;
    static final Team playerTeam=Team.sharded, enemyTeam=Team.blue;

    public static void generateAll() throws Exception {
        for(int i=0;i<3;i++) generate(i);
    }
    static void generate(int index) throws Exception {
        chapter=index; w=new int[]{256,320,384}[index]; h=new int[]{288,320,352}[index]; sx=w/2;sy=42;
        enemies=index==0 ? new int[0][] : index==1 ? new int[][]{{88,238},{232,250}} : new int[][]{{100,226},{284,226},{192,302}};
        spawns=index==0 ? new int[][]{{52,258},{204,258}} : index==1 ? new int[][]{{160,296}} : new int[][]{{54,314},{330,314}};
        rng=new Random(90317+index*701);
        logic.reset();
        Rules rules=rules(index);
        state.rules=rules;
        world.loadGenerator(w,h,tiles -> {
            tiles.fill();
            terrain();
            resources();
            playerBase();
            for(int j=0;j<enemies.length;j++) enemyBase(enemies[j][0],enemies[j][1], j);
            for(int[] p:spawns){clearCircle(p[0],p[1],13);world.tile(p[0],p[1]).setOverlay(Blocks.spawn);}
            ruins(w/2-35,h/2+20);
            ruins(w/2+46,h/2-10);
            MapPolish.sites(chapter,sx,sy,spawns);
            BaseWorkshop.territory(chapter,spawns);
            MapPolish.resources(chapter,sx,sy);

        });
        // Resolve links after world generation; the engine spatial index is empty inside the generator.
        Seq<Building> powered = new Seq<>();
        for (Tile t : world.tiles) if (t.isCenter() && t.build != null && t.build.power != null) powered.add(t.build);
        for (int pass=0; pass<2; pass++) for (Building node : powered) if (node.block instanceof PowerNode pn) {
            for (Building other : powered) {
                if ((other.block instanceof PowerNode) != (pass==0)) continue;
                if (node != other && other.team == node.team && node.power.graph != other.power.graph && pn.linkValid(node,other)) node.configureAny(other.pos());
            }
        }
        state.rules=rules;
        state.map=new Map(StringMap.of("name",names[index],"author","New Horizon / Afterglow community chapter","description",briefs[index]));
        state.map.tags.put("genfilters","[]");
        state.map.tags.put("rules",JsonIO.write(rules));
        Fi file=new Fi("assets/maps/"+ids[index]+".msav");
        MapIO.writeMap(file,state.map,false);
        Fi png=new Fi("campaign-tools/previews/"+ids[index]+".png");png.parent().mkdirs();
        Pixmap preview=MapIO.generatePreview(world.tiles);PixmapIO.writePng(png,preview);preview.dispose();
        System.out.println("GENERATED "+ids[index]+" "+w+"x"+h+" "+file.length()+" bytes");
    }
    static Rules rules(int c){
        Rules r=new Rules();
        r.defaultTeam=playerTeam;r.waveTeam=enemyTeam;
        r.waves=true;r.waveTimer=true;r.waitEnemies=true;
        r.attackMode=c>0;r.winWave=c==0?25:0;
        r.waveSpacing=(c==0?65:95)*60f;r.initialWaveSpacing=240*60f;
        r.unitCap=120+c*40;r.coreDestroyClear=true;r.enemyCoreBuildRadius=120;
        r.canGameOver=true;r.env=Env.terrestrial|Env.groundWater|NHContent.radioactive;
        r.planet=NHPlanets.midantha;
        r.placeRangeCheck=false;r.buildSpeedMultiplier=1.25f;
        r.teams.get(enemyTeam).rtsAi=false;
        r.teams.get(enemyTeam).buildAi=false;
        r.fog=false;r.staticFog=false;
        r.tags.put("nh-afterglow-chapter",String.valueOf(c));
        r.tags.put("nh-raid-scale","0");r.tags.put("nh-intervention-scale","0");r.tags.put("nh-special-event-enabled","false");
        r.loadout=ItemStack.list(Items.copper,3000,Items.lead,3000,Items.titanium,4000,Items.silicon,3500,
            Items.graphite,3000,Items.coal,1500,Items.sand,2500,Items.tungsten,2000,Items.thorium,1000,
            Items.metaglass,1000,Items.beryllium,1000,Items.carbide,400,
            NHItems.silicar,2500,NHItems.hardLight,2000,NHItems.presstanium,1200+400*c,
            NHItems.juniorProcessor,1000+300*c,NHItems.metalOxhydrigen,800+300*c,
            NHItems.multipleSteel,500+300*c,NHItems.zeta,1500+1000*c);
        r.spawns=new Seq<>();
        group(r,GroundUnitTypes.origin,0,c==0?23:Integer.MAX_VALUE,1,3+c*2,1.8f,18+c*6);
        group(r,NHUnitTypes.sharp,4,c==0?23:Integer.MAX_VALUE,3,2+c,3f,8+c*3);
        group(r,GroundUnitTypes.thynomo,10,c==0?23:Integer.MAX_VALUE,3,1+c,3f,7+c*3);
        if(c>0)group(r,NHUnitTypes.branch,13,Integer.MAX_VALUE,4,1+c,5f,4+c*2);
        if(c==0)group(r,GroundUnitTypes.thynomo,23,23,1,6,99f,6);
        if(c==2)group(r,NHUnitTypes.aliotiat,23,Integer.MAX_VALUE,6,1,10f,3);
        return r;
    }
    static void group(Rules r,UnitType u,int begin,int end,int spacing,int count,float scale,int max){
        SpawnGroup g=new SpawnGroup(u);g.begin=begin;g.end=end;g.spacing=spacing;g.unitAmount=count;g.unitScaling=scale;g.max=max;g.shieldScaling=0;r.spawns.add(g);
    }
    static void terrain(){
        for(Tile t:world.tiles){
            int x=t.x,y=t.y;
            double n=Math.sin(x*.095+Math.sin(y*.048)*2)+Math.cos(y*.083)+Math.sin((x+y)*.036);
            Floor floor=(n>1?EnvironmentBlock.conglomerate:EnvironmentBlock.darkConglomerate).asFloor();
            t.setFloor(floor);
            boolean border=x<7||y<7||x>=w-7||y>=h-7;
            boolean rock=n>1.75;
            if(chapter==0){double center=w*.5+Math.sin(y*.033)*28;rock|=Math.abs(x-center)>42 && y>76 && y<220 && n>-.6;}
            if(chapter==1)rock|=(Math.abs(y-132)<9 || Math.abs(y-194)<8) && x>17&&x<w-17;
            if(chapter==2){double d=Math.hypot(x-w*.5,(y-214)*.88);rock|=(d>100&&d<115)|| (Math.abs(y-269)<5&&Math.abs(x-sx)<65);}
            if(rock||border)t.setBlock(EnvironmentBlock.darkConglomerateWall);
            else if(n< -1.55&&y>80)t.setFloor(EnvironmentBlock.cryonite.asFloor());
        }
        clearCircle(sx,sy,43);
        if(chapter==0){corridor(sx,sy,sx,130,16);corridor(sx,130,52,258,12);corridor(sx,130,204,258,12);corridor(52,170,204,170,8);}
        if(chapter==1){corridor(sx,sy,92,125,15);corridor(sx,sy,226,125,15);corridor(92,125,88,238,12);corridor(226,125,232,250,12);corridor(88,238,160,296,10);corridor(232,250,160,296,10);corridor(56,165,262,165,10);}
        if(chapter==2){corridor(sx,sy,sx,126,19);corridor(sx,126,100,226,12);corridor(sx,126,284,226,12);corridor(100,226,192,302,11);corridor(284,226,192,302,11);corridor(54,314,100,226,10);corridor(330,314,284,226,10);}
        for(int[] p:enemies)clearCircle(p[0],p[1],34);
        for(int y=sy-13;y<sy+18;y++) for(int x=sx-12;x<sx+13;x++) if(Math.abs(x-sx)<3||Math.abs(y-sy)<3)world.tile(x,y).setFloor(EnvironmentBlock.platingFloor1.asFloor());
    }
    static void corridor(int x1,int y1,int x2,int y2,int r){
        int len=(int)Math.hypot(x2-x1,y2-y1);
        for(int i=0;i<=len;i++){float t=i/(float)len;clearCircle(Math.round(x1+(x2-x1)*t),Math.round(y1+(y2-y1)*t),r);}
    }
    static void clearCircle(int cx,int cy,int radius){
        for(int y=Math.max(7,cy-radius);y<Math.min(h-7,cy+radius+1);y++)for(int x=Math.max(7,cx-radius);x<Math.min(w-7,cx+radius+1);x++){
            if(Mathf.dst2(x,y,cx,cy)>radius*radius)continue;
            Tile t=world.tile(x,y);if(t.block().isStatic())t.setBlock(Blocks.air);
            if(t.floor().isLiquid)t.setFloor(EnvironmentBlock.darkConglomerate.asFloor());
        }
    }
    static void resources(){
        ore(sx-26,sy,8,EnvironmentBlock.oreTitaniumDense);
        ore(sx+27,sy,8,EnvironmentBlock.oreSilicarDense);
        ore(sx-24,sy+28,8,EnvironmentBlock.oreCoalDense);
        ore(sx+27,sy+27,8,EnvironmentBlock.oreCopperDense);
        ore(sx-23,sy-24,7,EnvironmentBlock.oreLeadDense);
        ore(sx+23,sy-24,7,EnvironmentBlock.oreTungstenDense);
        for(int k=0;k<3;k++){
            int x=chapter==0?(k%2==0?75:182):(k%2==0?92:w-92),y=105+k*45;
            clearCircle(x,y,16);
            ore(x,y,9,k==0?EnvironmentBlock.oreThoriumDense:k==1?EnvironmentBlock.oreZetaDense:EnvironmentBlock.oreSilicarDense);
        }
        for(int y=sy+12;y<sy+24;y++)for(int x=sx+44;x<sx+58;x++){
            Tile t=world.tile(x,y);t.setBlock(Blocks.air);t.setFloor(Blocks.water.asFloor());t.clearOverlay();
        }
        for(int y=sy-25;y<sy-14;y++)for(int x=sx-13;x<sx+5;x++){Tile t=world.tile(x,y);t.setFloor(Blocks.sand.asFloor());t.clearOverlay();}
        ore(sx+54,sy+42,7,EnvironmentBlock.oreBerylliumDense);
        clearCircle(sx+54,sy+42,12);
    }
    static void ore(int cx,int cy,int r,Block ore){
        for(int y=cy-r;y<=cy+r;y++)for(int x=cx-r;x<=cx+r;x++){
            Tile t=world.tile(x,y);if(t!=null&&Mathf.dst2(x,y,cx,cy)<r*r*(.8+rng.nextDouble()*.4)&&!t.floor().isLiquid){t.setBlock(Blocks.air);t.setOverlay(ore);}
        }
    }
    static Building place(Block b,int x,int y,Team team){
        int off=-(b.size-1)/2;
        for(int dx=0;dx<b.size;dx++)for(int dy=0;dy<b.size;dy++){
            Tile t=world.tile(x+off+dx,y+off+dy);
            if(t.block()!=Blocks.air&&!t.block().isStatic()&&!(t.block() instanceof mindustry.world.blocks.environment.Prop))throw new IllegalStateException("Overlapping buildings "+b.name+" at "+x+","+y+" with "+t.block().name+" on "+t.x+","+t.y);
            t.setBlock(Blocks.air);
        }
        world.tile(x,y).setBlock(b,team,0);return world.tile(x,y).build;
    }
    static void playerBase(){
        BaseWorkshop.player(sx,sy,chapter,false);
    }
    static void ammo(Building b,Item item,int n){
        if(b instanceof ItemTurret.ItemTurretBuild turret)for(int i=0;i<n;i++)turret.handleItem(null,item);
    }
    static void enemyBase(int x,int y,int index){
        BaseWorkshop.enemy(x,y,chapter,index);
    }
    static void ruins(int x,int y){
        clearCircle(x,y,10);
        for(int dx=-5;dx<6;dx++)if(rng.nextFloat()<.7)place(Blocks.copperWall,x+dx,y-5,Team.derelict);
        place(Blocks.container,x,y,Team.derelict).items.add(Items.titanium,300);
    }
    static void message(int x,int y,Team t,String text){
        MessageBlock.MessageBuild b=(MessageBlock.MessageBuild)place(Blocks.message,x,y,t);b.configureAny(text);
    }
}
