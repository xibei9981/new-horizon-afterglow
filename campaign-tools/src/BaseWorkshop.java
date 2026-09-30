import arc.math.geom.Point2;
import mindustry.ai.BaseRegistry.BasePart;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.Building;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.Conveyor;
import mindustry.world.blocks.power.PowerNode;
import mindustry.world.blocks.production.Drill;
import newhorizon.content.*;
import newhorizon.content.blocks.*;
import static mindustry.Vars.*;

/** Complete base plans composed from official v160.4 resource-fed schematics plus NH facilities.
 * The original prefabs retain their conveyors, pumps, crafters, repairers and configurations.
 * Source: Anuken/Mindustry core/assets/baseparts (GPL-3.0); see BASE-DESIGN.md.
 */
public final class BaseWorkshop {
    static int ox,oy,chapter;static Team team;
    static void begin(int x,int y,Team t,int c,int left,int bottom,int right,int top){
        ox=x;oy=y;team=t;chapter=c;
        for(int dx=left;dx<=right;dx++)for(int dy=bottom;dy<=top;dy++){
            var tile=world.tile(x+dx,y+dy);if(tile==null)throw new IllegalStateException("Base outside map: "+x+","+y);
            if(tile.build!=null)throw new IllegalStateException("Overlapping base: "+tile.x+","+tile.y+" "+tile.block());
            tile.setBlock(Blocks.air);
            if(tile.floor().isLiquid)tile.setFloor(Blocks.dacite.asFloor());
            if(Math.abs(dx)%30<=1||Math.abs(dy)%28<=1){tile.setFloor(Blocks.metalFloorDamaged.asFloor());tile.clearOverlay();}
        }
        state.rules.tags.put("afterglow.physical-logistics","true");
    }
    static boolean free(Block b,int x,int y){
        int lo=-(b.size-1)/2;for(int a=0;a<b.size;a++)for(int d=0;d<b.size;d++){
            var t=world.tile(x+lo+a,y+lo+d);if(t==null||t.build!=null||t.block().isStatic())return false;
        }
        if(b instanceof newhorizon.expand.block.BasicMultiBlock multi)for(int i=0;i<multi.linkSize();i++){
            var link=multi.getLink(i);var t=world.tile(x+link.x,y+link.y);
            if(t==null||t.build!=null||t.block().isStatic())return false;
        }
        return true;
    }
    static Building put(Block b,int dx,int dy){
        int x=ox+dx,y=oy+dy;
        if(!free(b,x,y))throw new IllegalStateException("Overlap "+b+" at "+x+","+y+" on "+world.tile(x,y).block());
        int lo=-(b.size-1)/2;
        for(int a=0;a<b.size;a++)for(int d=0;d<b.size;d++){
            var t=world.tile(x+lo+a,y+lo+d);t.setFloor(Blocks.metalFloorDamaged.asFloor());t.clearOverlay();
        }
        world.tile(x,y).setBlock(b,team,0);var build=world.build(x,y);
        if(team==Team.blue&&build instanceof mindustry.world.blocks.defense.turrets.Turret.TurretBuild turret)turret.rotation=270f;
        if(build instanceof newhorizon.expand.block.BasicMultiBlock.BasicMultiBuilding multi)multi.updateLinkBlock();
        return build;
    }
    static void belt(int dx,int dy,int direction){
        var t=world.tile(ox+dx,oy+dy);
        if(t.block() instanceof Conveyor && (t.build.rotation%2)!=(direction%2)){t.setBlock(Blocks.junction,team);return;}
        if(t.build!=null)throw new IllegalStateException("Belt collision "+t.x+","+t.y+" "+t.block());
        t.setBlock(Blocks.titaniumConveyor,team,direction);
    }
    static void horizontal(int x1,int x2,int y){for(int x=x1;;x+=x2>x1?1:-1){belt(x,y,x2>=x1?0:2);if(x==x2)break;}}
    static void vertical(int x,int y1,int y2){for(int y=y1;;y+=y2>y1?1:-1){belt(x,y,y2>=y1?1:3);if(y==y2)break;}}
    static Building drill(int x,int y,Item item){
        var b=put(ProductionBlock.interlockingDrill,x,y);ore(b,item);return b;
    }
    static void ore(Building b,Item item){
        Block ore=bases.ores.get(item);int lo=-(b.block.size-1)/2;
        for(int a=0;a<b.block.size;a++)for(int d=0;d<b.block.size;d++){
            var t=world.tile(b.tileX()+lo+a,b.tileY()+lo+d);
            if(item==Items.sand){t.setFloor(Blocks.sand.asFloor());t.clearOverlay();}
            else{t.setFloor(Blocks.stone.asFloor());t.setOverlay(ore);}
        }
    }
    static void prefab(Item item,int index,int x,int y,int rotation){
        if(chapter>=8 && item==Items.thorium && index==15){
            at(x,y,()->{for(int gx:new int[]{-6,-2,2,6})for(int gy:new int[]{-4,0,4})geothermal(gx,gy);
                put(PowerBlock.fluxNodeMK2,0,0);for(int gy:new int[]{-4,-2,2,4})put(PowerBlock.armorBattery,0,gy).power.status=1;});
            return;
        }
        BasePart part=bases.forResource(item).get(index);
        // Rotation uses engine scratch schematics and mutable point configurations.
        // Own the input and normalize the rotated footprint before placing it.
        var schematic=Schematics.readBase64(new Schematics().writeBase64(part.schematic));
        if(rotation!=0){
            schematic=Schematics.rotate(schematic,rotation);
            int minX=Integer.MAX_VALUE,minY=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE,maxY=Integer.MIN_VALUE;
            for(var t:schematic.tiles){int lo=-(t.block.size-1)/2;minX=Math.min(minX,t.x+lo);minY=Math.min(minY,t.y+lo);maxX=Math.max(maxX,t.x+lo+t.block.size-1);maxY=Math.max(maxY,t.y+lo+t.block.size-1);}
            for(var t:schematic.tiles){t.x-=minX;t.y-=minY;}
            schematic.width=maxX-minX+1;schematic.height=maxY-minY+1;
        }
        int bx=ox+x-schematic.width/2,by=oy+y-schematic.height/2;
        for(var tile:schematic.tiles){
            if(!free(tile.block,bx+tile.x,by+tile.y))throw new IllegalStateException("Prefab overlap "+item+"/"+index+" at "+(bx+tile.x)+","+(by+tile.y)+" "+tile.block);
            tile.block.iterateTaken(bx+tile.x,by+tile.y,(tx,ty)->{var t=world.tile(tx,ty);t.setFloor(Blocks.stone.asFloor());t.clearOverlay();});
        }
        Schematics.place(schematic,ox+x,oy+y,team,true);
        for(var tile:schematic.tiles){
            var b=world.build(bx+tile.x,by+tile.y);
            if(b==null||b.block!=tile.block)throw new IllegalStateException("Prefab failed "+tile.block);
            if(b.block instanceof Drill){ore(b,item);b.items.add(item,b.block.itemCapacity);}
            if(b.block instanceof mindustry.world.blocks.power.Battery)b.power.status=1;
            // A deployed battery starts with finite reserve ammunition in its real magazines.
            if(b.block instanceof mindustry.world.blocks.storage.StorageBlock && !(b.block instanceof mindustry.world.blocks.storage.CoreBlock))b.items.add(item,b.block.itemCapacity);
            // Vanilla base parts sometimes include optional factories with no selected plan.
            // These plans are used only for their connected industrial/defense portions.
        }
        System.out.println("BASE_PART chapter="+(chapter+1)+" team="+team+" resource="+item+" index="+index+" center="+(ox+x)+","+(oy+y));
    }
    static void grid(int left,int bottom,int right,int top){
        for(int x=left+4;x<right;x+=18)for(int y=bottom+4;y<top;y+=18){
            boolean placed=false;
            for(int radius=0;radius<=3&&!placed;radius++)for(int dx=-radius;dx<=radius&&!placed;dx++)for(int dy=-radius;dy<=radius&&!placed;dy++){
                if(free(PowerBlock.fluxNodeLargeMK1,ox+x+dx,oy+y+dy)){put(PowerBlock.fluxNodeLargeMK1,x+dx,y+dy);placed=true;}
            }
        }
    }
    static void maintenance(int x,int y){
        if(free(Blocks.mendProjector,ox+x,oy+y))put(Blocks.mendProjector,x,y);
    }
    static Block wallFor(int stage){
        return stage>=10?DefenseBlock.shapedWall:stage>=8?DefenseBlock.setonPhasedWall:
            stage>=3?DefenseBlock.refactoringMultiWall:stage>=1?DefenseBlock.presstaniumWall:DefenseBlock.titaniumWall;
    }
    static void perimeter(int left,int bottom,int right,int top,boolean heavy){
        Block wall=wallFor(chapter);
        int layers=chapter>=10?3:chapter>=4?2:1;
        // NH adaptive walls let friendly units pass. Do not leave broad, undefended holes.
        // Contiguous layers share incoming damage; isolated decorative posts cannot do that.
        for(int layer=0;layer<layers;layer++){
            int l=left+layer,r=right-layer,b=bottom+layer,t=top-layer;
            for(int x=l;x<=r;x++)for(int y:new int[]{b,t})if(free(wall,ox+x,oy+y))put(wall,x,y);
            for(int y=b+1;y<t;y++)for(int x:new int[]{l,r})if(free(wall,ox+x,oy+y))put(wall,x,y);
        }
    }
    static void at(int dx,int dy,Runnable plan){int oldX=ox,oldY=oy;ox+=dx;oy+=dy;try{plan.run();}finally{ox=oldX;oy=oldY;}}
    static void panels(int x1,int y1,int x2,int y2){
        for(int x=x1;x<=x2;x+=2)for(int y=y1;y<=y2;y+=2)if(free(PowerBlock.photonPanel,ox+x,oy+y))put(PowerBlock.photonPanel,x,y);
    }
    static Building mine(int x,int y,Item item,boolean advanced){
        var b=put(advanced?ProductionBlock.beamMiningFacility:ProductionBlock.interlockingDrill,x,y);ore(b,item);return b;
    }
    static void router(int x,int y){
        var t=world.tile(ox+x,oy+y);if(t.build==null)put(Blocks.router,x,y);
        else if(t.block() instanceof Conveyor)t.setBlock(Blocks.router,team);
        else if(t.block()!=Blocks.router)throw new IllegalStateException("Router collision "+t.block()+" "+t.x+","+t.y);
    }
    static void pipe(int x,int y,int dir){var b=put(Blocks.pulseConduit,x,y);b.rotation=dir;}
    static void liquidH(int a,int b,int y){for(int x=a;;x+=b>a?1:-1){pipe(x,y,b>=a?0:2);if(x==b)break;}}
    static void liquidV(int x,int a,int b){for(int y=a;;y+=b>a?1:-1){pipe(x,y,b>=a?1:3);if(y==b)break;}}
    static void rapidBattery(int level,boolean missiles){
        Item ammo=missiles||level>=8?NHItems.zeta:level>=4?Items.tungsten:Items.titanium;
        boolean advanced=level>=4;
        for(int x:new int[]{-8,0,8}){
            var drill=mine(x,-7,ammo,advanced);
            vertical(x,advanced?-4:-5,-3);
        }
        // Two-stage distribution avoids a conveyor pointing into the side of another belt.
        horizontal(10,-10,-2);for(int x:new int[]{-8,0,8})router(x,-2);
        for(int row=0;row<3;row++){
            int gy=row*6;
            if(row>0){vertical(-11,row==1?-1:4,gy-4);router(-11,gy-3);horizontal(-10,10,gy-3);}
            for(int gx:new int[]{-8,0,8}){
                boolean itemGun=advanced || row==0;
                Block gun=itemGun?(missiles?TurretBlock.slavio:level==0?TurretBlock.pulse:TurretBlock.synchro):TurretBlock.thermo;
                put(gun,gx,gy);
                if(row==0){if(level==0)belt(gx,-1,1);}else if(itemGun){router(gx,gy-3);belt(gx,gy-2,1);}
            }
        }
        // First row size-three guns need their input at -2; pulse is size two, input at -1.

        router(-11,-2);
        for(int[]q:new int[][]{{-4,1},{4,7},{-4,12}})maintenance(q[0],q[1]);
        if(level>=6)put(DefenseBlock.standardForceProjector,4,1);
        else put(Blocks.battery,4,1).power.status=1;
        for(int gx=-10;gx<=10;gx++)if(Math.abs(gx)%8!=4&&free(wallFor(level),ox+gx,oy+14))put(wallFor(level),gx,14);
    }
    // Multi-press + tungsten mine -> two rolling mills -> vortex, with a real finite magazine.
    static void steelBattery(){
        var store=put(Blocks.vault,0,0);
        store.items.add(Items.graphite,80);store.items.add(Items.tungsten,80);
        for(int gx:new int[]{-4,4}){put(Blocks.unloader,gx<0?-2:2,0);put(CraftingBlock.mixedRollingMill,gx,0);belt(gx,2,1);}
        horizontal(-4,-1,3);horizontal(4,1,3);belt(0,3,1);vertical(0,4,6);put(TurretBlock.vortex,0,8);
        mine(0,-7,Items.tungsten,true);vertical(0,-4,-2);
        mine(5,-7,Items.coal,true);mine(10,-7,Items.coal,true);
        for(int gx:new int[]{5,10})belt(gx,-9,3);
        horizontal(10,-8,-10);router(5,-10);belt(-9,-10,1);vertical(-9,-9,-7);belt(-9,-6,0);
        put(Blocks.multiPress,-7,-6);put(Blocks.waterExtractor,-5,-9);liquidV(-5,-7,-6);world.build(ox-5,oy-6).rotation=2;
        belt(-7,-4,1);horizontal(-7,-1,-3);world.build(ox-1,oy-3).rotation=1;belt(-1,-2,1);
        for(int gx:new int[]{-8,8})put(NHBlocks.interferon,gx,8);
        maintenance(-5,5);maintenance(5,5);
        put(DefenseBlock.standardForceProjector,0,12);
    }
    // Coal -> graphite -> xen, plus thorium. Optional fusion line makes prism ammunition.
    static void xenBattery(boolean prism){
        mine(-9,-8,Items.coal,true);mine(-4,-8,Items.coal,true);
        horizontal(-9,-2,-5);router(-4,-5);belt(-2,-4,0);put(Blocks.multiPress,0,-4);
        put(Blocks.waterExtractor,0,-9);liquidV(0,-7,-6);
        put(Blocks.unloader,2,-4).configureAny(Items.graphite);
        var store=put(Blocks.vault,4,-4);store.items.add(Items.graphite,80);store.items.add(Items.thorium,80);
        mine(9,-9,Items.thorium,true);belt(7,-9,1);vertical(7,-8,-7);belt(7,-6,2);belt(6,-6,1);belt(6,-5,2);
        // Direct recipe-aware unloaders cannot clog a shared belt with excess thorium.
        put(Blocks.unloader,4,-2);put(CraftingBlock.plasmaActivator,4,0);
        put(Blocks.unloader,6,-4);put(CraftingBlock.plasmaActivator,8,-4);
        pipe(4,2,1);put(Blocks.liquidRouter,4,3);liquidH(4,-6,4);
        world.tile(ox-6,oy+4).setBlock(Blocks.liquidRouter,team);
        liquidV(-6,5,6);put(TurretBlock.concentration,-6,9);
        put(DefenseBlock.standardRegenProjector,-6,1);pipe(-6,3,3);
        var xenBridge=put(Blocks.phaseConduit,8,-2);put(Blocks.phaseConduit,8,0);xenBridge.configureAny(new Point2(0,2));
        put(Blocks.liquidRouter,8,1);pipe(7,1,2);put(Blocks.liquidRouter,6,1);pipe(6,2,1);pipe(6,3,2);pipe(5,3,2);
        if(prism){
            mine(-9,-2,Items.titanium,true);belt(-6,-2,0);belt(-5,-2,0);put(CraftingBlock.subCooler,-4,-3);
            put(Blocks.waterExtractor,-2,7);var waterBridge=put(Blocks.phaseConduit,-2,6);put(Blocks.phaseConduit,-2,-2);waterBridge.configureAny(new Point2(0,-8));
            pipe(-4,-1,0);var bridge=put(Blocks.phaseConduit,-3,-1);put(Blocks.phaseConduit,6,-1);bridge.configureAny(new Point2(9,0));
            liquidH(7,12,-1);world.build(ox+12,oy-1).rotation=1;liquidV(12,0,3);world.build(ox+12,oy+3).rotation=2;pipe(11,3,2);
            put(CraftingBlock.fusionCoreEnergyFactory,9,3);pipe(9,1,1);
            belt(9,5,1);horizontal(9,6,6);world.build(ox+6,oy+6).rotation=1;belt(6,7,1);put(NHBlocks.prism,6,9);
        }else{
            liquidH(5,6,4);world.tile(ox+4,oy+4).setBlock(Blocks.liquidRouter,team);world.build(ox+6,oy+4).rotation=1;liquidV(6,5,6);put(TurretBlock.concentration,6,9);
        }
        maintenance(0,9);
    }
    static void terrainUnder(Building b,Block floor){
        int lo=-(b.block.size-1)/2;for(int x=0;x<b.block.size;x++)for(int y=0;y<b.block.size;y++)world.tile(b.tileX()+lo+x,b.tileY()+lo+y).setFloor(floor.asFloor());
    }
    static void geothermal(int x,int y){terrainUnder(put(PowerBlock.geologicalPhotothermalGenerator,x,y),Blocks.magmarock);}
    static void pump(int x,int y){terrainUnder(put(LiquidBlock.turboPump,x,y),Blocks.water);}
    static void liquidBridge(int x,int y,int tx,int ty){
        var bridge=put(Blocks.phaseConduit,x,y);put(Blocks.phaseConduit,tx,ty);bridge.configureAny(new Point2(tx-x,ty-y));
    }
    // Tactical siege emplacement. Ammunition is a finite deployed military reserve;
    // required antimatter is continuously manufactured from pumped water, not prefilled.
    static void siegeBattery(){
        put(NHBlocks.endOfEra,0,10);
        var nodex=put(team==Team.sharded?SpecialBlock.remoteStorage:SpecialBlock.heavyStorage,0,0);
        if(team!=Team.sharded)nodex.items.add(NHItems.nodexPlate,4000);
        else for(int side:new int[]{-1,1}){
            var feed=put(DistributionBlock.conveyorUnloaderFast,side*2,0);feed.rotation=side<0?2:0;feed.configureAny(NHItems.fusionEnergy);
            itemBridge(side*3,0,side*3,5);horizontal(side*4,side*6,5);
        }
        var nodexFeed=put(DistributionBlock.conveyorUnloaderFast,0,2);nodexFeed.rotation=1;nodexFeed.configureAny(NHItems.nodexPlate);vertical(0,3,6);
        for(int x:new int[]{-8,8}){
            put(NHBlocks.prism,x,11);
            var reserve=put(SpecialBlock.heavyStorage,x,5);reserve.items.add(NHItems.fusionEnergy,4000);
            var fusionFeed=put(DistributionBlock.conveyorUnloaderFast,x,7);fusionFeed.rotation=1;fusionFeed.configureAny(NHItems.fusionEnergy);vertical(x,8,9);
        }
        for(int x:new int[]{-10,-4,2,8})pump(x,-9);
        for(int x=-11;x<=11;x++)put(Blocks.liquidRouter,x,-7);
        for(int x:new int[]{-11,-8,-5,-2,1,4,7,10})put(ProductionBlock.decoherenceReverser,x,-6);
        for(int x=-11;x<=11;x++)put(Blocks.liquidRouter,x,-4);
        for(int x:new int[]{-7,7}){
            put(CraftingBlock.reverseCollapseFacility,x,-1);pipe(x,-3,1);
            liquidV(x,1,2);liquidBridge(x,3,x<0?-2:2,3);liquidV(x<0?-2:2,4,6);
        }
        for(int x:new int[]{-11,11})for(int y:new int[]{1,5})geothermal(x,y);
        geothermal(-4,3);geothermal(4,3);
        maintenance(-5,7);maintenance(5,7);put(DefenseBlock.plasmaMembrane,11,8);
        for(int x:new int[]{-4,4})put(PowerBlock.fluxNodeMK2,x,-3);
        put(PowerBlock.fluxNodeMK2,-4,10);put(PowerBlock.fluxNodeMK2,5,10);
    }
    static void itemBridge(int x,int y,int tx,int ty){
        var bridge=put(Blocks.phaseConveyor,x,y);put(Blocks.phaseConveyor,tx,ty);bridge.configureAny(new Point2(tx-x,ty-y));
    }
    // Six heavy lasers share a protected chemical works. Each of four activators
    // receives both recipe items directly from a buffer, avoiding mixed-belt starvation.
    static void laserBattery(){
        for(int x:new int[]{-8,0,8})for(int y:new int[]{7,13})put(TurretBlock.concentration,x,y);
        for(int x:new int[]{-7,7}){
            var buffer=put(Blocks.vault,x,-4);buffer.items.add(Items.graphite,80);buffer.items.add(Items.thorium,80);
        }
        for(int x:new int[]{-9,-5,5,9}){
            put(CraftingBlock.plasmaActivator,x,0);put(Blocks.unloader,x<0?(x==-9?-8:-6):(x==5?6:8),-2);
            liquidV(x,2,3);
        }
        for(int x=-12;x<=10;x++)put(Blocks.liquidRouter,x,4);
        liquidV(-12,5,9);for(int x=-12;x<=10;x++)put(Blocks.liquidRouter,x,10);
        mine(0,-8,Items.thorium,true);horizontal(-5,5,-5);router(0,-5);
        // Explicitly split the thorium trunk toward both independent stores.
        for(int x=-5;x<0;x++)world.build(ox+x,oy-5).rotation=2;
        put(Blocks.multiPress,0,-2);
        for(int side:new int[]{-1,1}){
            mine(side*9,-9,Items.coal,true);
            int edge=side<0?-11:12;vertical(edge,-9,-3);itemBridge(edge,-2,side*2,-2);
        }
        router(0,0);horizontal(-1,-2,0);horizontal(1,2,0);
        itemBridge(-3,0,-3,-4);horizontal(-4,-5,-4);itemBridge(3,0,3,-4);horizontal(4,5,-4);
        put(Blocks.waterExtractor,0,2);pipe(-1,2,2);liquidBridge(-2,2,-2,-1);
        for(int x:new int[]{-4,4})for(int y:new int[]{-8,7,13})geothermal(x,y);
        put(Blocks.mendProjector,-4,2);put(Blocks.mendProjector,3,2);
        put(Blocks.mender,-12,13);put(Blocks.mender,12,13);put(Blocks.mender,0,16);
        // Overlapping native shields protect the chemical line and front rank.
        put(DefenseBlock.standardForceProjector,11,1);
        for(int x:new int[]{-4,4})for(int y:new int[]{-2,9})put(PowerBlock.fluxNodeMK2,x,y);
    }
    // Heavy anti-armour battery with native negative-core manufacturing and a finite
    // material reserve. Destroying its chemical works or ammo trunk stops replenishment.
    // Final citadel command battery: 150-tile denial with finite dark-energy reserve.
    // Six real water -> quantum -> antimatter chains provide 72/s for its 60/s coolant.
    static void commandBattery(){
        put(NHBlocks.eternity,0,4);
        var magazine=put(SpecialBlock.heavyStorage,0,-8);magazine.items.add(NHItems.darkEnergy,4000);
        var feed=put(DistributionBlock.conveyorUnloaderFast,0,-6);feed.rotation=1;feed.configureAny(NHItems.darkEnergy);vertical(0,-5,-4);
        for(int side:new int[]{-1,1}){
            for(int y=-6;y<=14;y++)if(y!=-6&&y!=4&&y!=14)pipe(side*10,y,y<4?1:3);
            for(int dy:new int[]{-8,2,12})at(side*18,dy,()->{
                put(CraftingBlock.reverseCollapseFacility,0,0);
                for(int x=-5;x<=5;x++)put(Blocks.liquidRouter,x,-2);
                for(int x:new int[]{-5,-2,1,4})put(ProductionBlock.decoherenceReverser,x,-4);
                for(int x=-5;x<=5;x++)put(Blocks.liquidRouter,x,-5);
                pump(-5,-7);pump(4,-7);
                liquidBridge(0,2,-side*8,2);
            });
            pipe(side*9,4,side<0?0:2);if(side<0)pipe(-8,4,0);
            for(int y:new int[]{-13,-9})for(int x:new int[]{-6,6})if(free(PowerBlock.geologicalPhotothermalGenerator,ox+x,oy+y))geothermal(x,y);
            maintenance(side*10,-9);put(PowerBlock.fluxNodeMK2,side*9,-12);
            for(int y:new int[]{-8,2,12})put(PowerBlock.fluxNodeMK2,side*25,y);
        }
        for(int x:new int[]{-4,4})put(PowerBlock.armorBatteryHuge,x,15).power.status=1;
        maintenance(-5,-5);maintenance(5,-5);maintenance(-10,16);maintenance(10,16);put(PowerBlock.fluxNodeMK2,0,16);
    }
    static void executorBattery(){
        var ammo=put(SpecialBlock.heavyStorage,0,0);ammo.items.add(NHItems.thermoCoreNegative,4000);
        var feed=put(DistributionBlock.conveyorUnloaderFast,0,2);feed.rotation=1;feed.configureAny(NHItems.thermoCoreNegative);
        horizontal(-8,8,3);router(0,3);
        for(int x=-8;x<0;x++)world.build(ox+x,oy+3).rotation=2;
        for(int x:new int[]{-8,0,8}){router(x,3);vertical(x,4,5);put(NHBlocks.executor,x,8);}
        for(int x:new int[]{-7,7}){
            var raw=put(SpecialBlock.heavyStorage,x,-9);raw.items.add(NHItems.fissileMatter,2000);raw.items.add(NHItems.zeta,1000);
            put(Blocks.unloader,x,-7);put(CraftingBlock.negativePhaseDecayer,x,-5);belt(x,-2,x<0?0:2);
            horizontal(x+(x<0?1:-1),x<0?-2:2,-2);world.build(ox+(x<0?-2:2),oy-2).rotation=1;
            vertical(x<0?-2:2,-1,0);world.build(ox+(x<0?-2:2),oy).rotation=x<0?0:2;
        }
        put(CraftingBlock.xenSeparator,0,-5);put(Blocks.liquidRouter,0,-3);
        liquidBridge(-1,-3,-3,-3);liquidBridge(1,-3,4,-3);
        var carbon=put(SpecialBlock.heavyStorage,-4,-10);carbon.items.add(Items.graphite,4000);
        put(Blocks.unloader,-2,-10).configureAny(Items.graphite);belt(-1,-10,0);put(CraftingBlock.particleActivator,0,-10);
        liquidBridge(2,-10,2,-5);
        for(int x:new int[]{-11,11})geothermal(x,0);
        for(int x:new int[]{-4,4})for(int y:new int[]{3,12})maintenance(x,y);
        put(DefenseBlock.plasmaMembrane,0,13);
        for(int x:new int[]{-4,4})put(PowerBlock.fluxNodeMK2,x,5);
        put(PowerBlock.fluxNodeMK2,3,-9);put(PowerBlock.fluxNodeMK2,-10,-8);
    }
    static void warYard(int level,int index){
        Block block=level>=11?UnitBlock.jumpGateHyper:level>=7?UnitBlock.jumpGateStandard:level>=3?UnitBlock.jumpGatePrimary:UnitBlock.jumpGateBasic;
        UnitType unit=level>=11?(index%2==0?NHUnitTypes.longinus:newhorizon.content.units.GroundUnitTypes.annihilation):level>=7?(index%2==0?NHUnitTypes.tarlidor:NHUnitTypes.striker):level>=3?(index%2==0?NHUnitTypes.warper:NHUnitTypes.aliotiat):NHUnitTypes.sharp;
        var gate=(newhorizon.expand.block.special.JumpGate.JumpGateBuild)put(block,0,4);
        var type=(newhorizon.expand.block.special.JumpGate)block;
        int recipe=type.recipeList.indexOf(r->r.unitType==unit);
        if(recipe<0)throw new IllegalStateException("No native yard recipe "+unit);
        var depot=put(SpecialBlock.heavyStorage,0,-7);
        int batches=level>=11?3:level>=7?4:6;
        for(var stack:type.recipeList.get(recipe).baseRequirements())depot.items.add(stack.item,Math.min(depot.block.itemCapacity,stack.amount*batches));
        put(Blocks.unloader,0,-5);vertical(0,-4,4-(block.size-1)/2-1);
        gate.configureAny(arc.struct.IntSeq.with(0,recipe,1,1));
        // Rally outside the rear wall; the native spawner and unit AI handle movement.
        gate.linkPos(new Point2(ox,oy+18));
        maintenance(-6,4);maintenance(6,4);maintenance(0,-10);
        for(int dx:new int[]{-9,9}){
            put(level>=4?NHBlocks.interferon:TurretBlock.thermo,dx,9);
            put(Blocks.batteryLarge,dx,-6).power.status=1;
        }
        if(level>=6)put(DefenseBlock.standardForceProjector,0,11);
        state.rules.tags.put("workshop.yard."+index,gate.pos()+"");
        state.rules.tags.put("workshop.yard-stock."+index,depot.pos()+"");
    }
    // Existing rear industrial district. Native NH recipes, no scripted item injection.
    // Each foundry returns fuel to the shared core; each weapon hub draws it through
    // native remote storage. The power cost and ten-hub player limit remain unchanged.
    static void fusionFoundry(){
        put(SpecialBlock.remoteStorage,0,0);
        for(int x:new int[]{-5,5}){
            put(CraftingBlock.fusionCoreEnergyFactory,x,0);
            if(x<0)horizontal(-3,-2,0);else horizontal(3,2,0);
        }
        for(int y:new int[]{-4,4}){put(CraftingBlock.plasmaActivator,0,y);put(Blocks.unloader,0,y<0?-2:2);}
        liquidH(-2,-5,4);world.build(ox-5,oy+4).rotation=3;liquidV(-5,3,2);
        liquidH(2,5,-4);world.build(ox+5,oy-4).rotation=1;liquidV(5,-3,-2);
        put(Blocks.unloader,1,2).configureAny(Items.titanium);
        horizontal(2,3,2);world.build(ox+3,oy+2).rotation=1;vertical(3,3,5);world.build(ox+3,oy+5).rotation=0;belt(4,5,0);
        put(CraftingBlock.subCooler,5,5);pump(7,5);
        pipe(5,4,3);put(Blocks.liquidRouter,5,3);pipe(5,2,3);
        liquidBridge(5,7,-7,7);liquidV(-7,6,1);world.build(ox-7,oy+1).rotation=0;
        put(PowerBlock.fluxNodeMK2,-3,-5);put(PowerBlock.fluxNodeMK2,3,-5);
    }
    // Two item-production lines around one logistics hub. Outputs return through
    // separate belts, so an unloader cannot pull its own finished products in a loop.
    static void materialsFoundry(boolean advanced,boolean electronics){
        put(SpecialBlock.remoteStorage,0,0);
        Block left=electronics?CraftingBlock.processorEtchingFacility:advanced?CraftingBlock.nodexFactory:CraftingBlock.mixedRollingMill;
        Block right=electronics?CraftingBlock.surgeSynthesizer:advanced?CraftingBlock.tandemFactory:CraftingBlock.mixedRollingMill;
        int lx=left==CraftingBlock.processorEtchingFacility?-5:left.size==4?-5:-4;
        int rx=right==CraftingBlock.tandemFactory||right==CraftingBlock.surgeSynthesizer?5:4;
        put(left,lx,0);put(right,rx,0);put(Blocks.unloader,-2,0);put(Blocks.unloader,2,0);
        for(int x:new int[]{lx,rx}){
            vertical(x,x==lx&&left.size==2?-1:-2,-3);world.build(ox+x,oy-3).rotation=x<0?0:2;
            horizontal(x+(x<0?1:-1),x<0?-1:1,-3);
            world.build(ox+(x<0?-1:1),oy-3).rotation=1;belt(x<0?-1:1,-2,1);
        }
        if(electronics){
            put(Blocks.unloader,0,2);put(CraftingBlock.plasmaActivator,0,4);
            liquidH(2,5,4);world.build(ox+5,oy+4).rotation=3;liquidV(5,3,2);
            pump(-6,7);pipe(-6,6,3);put(ProductionBlock.decoherenceReverser,-6,4);
            liquidBridge(-4,4,-4,2);
        }
        put(PowerBlock.fluxNodeMK2,-7,-3);put(PowerBlock.fluxNodeMK2,7,-3);
    }
    static Building[] lateIndustry(int c){
        var core=world.build(ox,oy);
        Building first=null;
        for(int gy:new int[]{-26,-11})for(int gx:new int[]{-46,-38,-30,-22,-14,-6}){
            boolean extraOre=gy==-26&&(gx==-14||gx==-6);
            mine(gx,gy,extraOre?(gx==-6?Items.titanium:Items.thorium):NHItems.silicar,true);
            if(extraOre)vertical(gx,gy+3,gy+8);
            else{
                vertical(gx,gy+3,gy+4);var factory=put(CraftingBlock.silicarCrusher,gx,gy+5);if(first==null)first=factory;
                vertical(gx,gy+7,gy+8);
            }
            if(gy==-11)belt(gx,-2,1);
        }
        horizontal(-46,-3,-17);horizontal(-46,-3,-1);
        for(int gx:new int[]{-46,-38,-30,-22,-14,-6}){router(gx,-17);router(gx,-1);}
        world.build(ox-3,oy-17).rotation=1;vertical(-3,-16,-3);belt(-3,-2,0);
        belt(-3,0,0);
        if(c<11){belt(-2,-2,0);belt(-1,-2,1);belt(-2,-1,0);belt(-2,0,0);}
        Item[] raw={Items.titanium,Items.titanium,Items.thorium,Items.thorium};
        for(int i=0;i<raw.length;i++){int gy=-28+i*6;mine(4,gy,raw[i],true);}
        vertical(7,-28,-4);for(int gy=-18;gy<=-5;gy++)world.tile(ox+7,oy+gy).setBlock(Blocks.armoredConveyor,team,1);vertical(2,-16,-5);put(Blocks.junction,2,-4);belt(2,-3,1);if(c<11){belt(2,-2,1);belt(2,-1,2);}world.build(ox+7,oy-4).rotation=2;horizontal(6,3,-4);horizontal(1,0,-4);world.build(ox,oy-4).rotation=1;vertical(0,-3,c>=11?-3:-2);
        for(int gy:new int[]{-28,-22})router(7,gy);for(int gy:new int[]{-16,-10})router(2,gy);
        mine(-7,12,NHItems.zeta,true);vertical(-4,12,1);router(-4,12);world.build(ox-4,oy+1).rotation=0;
        int shift=c>=11?2:0;
        put(Blocks.unloader,-1,2+shift).configureAny(Items.titanium);vertical(-1,3+shift,4+shift);
        put(Blocks.unloader,1,2+shift).configureAny(Items.graphite);vertical(1,3+shift,4+shift);belt(1,5+shift,2);
        put(CraftingBlock.stampingFacility,-1,5+shift);belt(-2,5+shift,2);vertical(-3,5+shift,1);
        put(Blocks.unloader,2+shift,0).configureAny(Items.silicon);horizontal(3+shift,7,0);
        put(CraftingBlock.processorPrinter,9,0);vertical(9,-1,-3);world.build(ox+9,oy-3).rotation=2;horizontal(8,4,-3);world.build(ox+4,oy-3).rotation=1;belt(4,-2,2);
        if(c<11){belt(3,-2,2);}
        put(ProductionBlock.decoherenceReverser,12,3);pump(15,3);pipe(14,3,2);
        liquidH(11,10,3);world.build(ox+10,oy+3).rotation=3;pipe(10,2,3);
        at(-40,5,()->materialsFoundry(c>=10,false));at(-20,5,()->materialsFoundry(c>=10,true));
        for(int x:new int[]{20,40})for(int y:(c>=11?new int[]{-23,-5}:new int[]{-23}))at(x,y,()->fusionFoundry());
        put(PowerBlock.armorBatteryLarge,10,-12).power.status=1;
        put(c>=10?UnitBlock.jumpGateHyper:UnitBlock.jumpGateStandard,10,9);
        for(int gx=-50;gx<=50;gx++)for(int gy=-31;gy<=13;gy++){
            var tile=world.tile(ox+gx,oy+gy);if(tile.block()==Blocks.titaniumConveyor)tile.setBlock(DistributionBlock.conveyor,team,tile.build.rotation);
        }
        return new Building[]{first,first};
    }
    public static void player(int x,int y,int c,boolean secondary){
        begin(x,y,Team.sharded,c,-53,-34,53,71);
        int hubShift=c>=11?2:0;
        if(!secondary && c>=8){
            // Sector loading resets core contents from this loadout; store reserves here,
            // not only in the authored core inventory that the engine discards on landing.
            for(var stack:ItemStack.with(NHItems.seniorProcessor,1200,NHItems.setonAlloy,1600,NHItems.surgeAlloy,1800,Items.carbide,2400,NHItems.zeta,1800,Items.tungsten,1800,Items.thorium,1200,NHItems.fusionEnergy,1200)){
                var old=state.rules.loadout.find(a->a.item==stack.item);if(old==null)state.rules.loadout.add(stack);else old.amount=Math.max(old.amount,stack.amount);
            }
            if(c>=11)for(var stack:ItemStack.with(NHItems.nodexPlate,4000,NHItems.ancimembrane,3000,NHItems.setonAlloy,4000,NHItems.irayrondPanel,3000,NHItems.fusionEnergy,4000,NHItems.thermoCoreNegative,3000)){
                var old=state.rules.loadout.find(a->a.item==stack.item);if(old==null)state.rules.loadout.add(stack);else old.amount=Math.max(old.amount,stack.amount);
            }
        }
        var core=put(c>=11?SpecialBlock.coreCluster:SpecialBlock.coreConflux,0,0);if(!secondary)core.items.add(state.rules.loadout);
        Building graphite,silicon;
        if(c>=8){
            var products=lateIndustry(c);graphite=products[0];silicon=products[1];
        }else{
        // Production exports are real conveyors into the core, independent from gun batteries.
        drill(-26,0,Items.titanium);horizontal(-24,c>=11?-3:-2,0);
        drill(-26,4,Items.titanium);drill(-26,8,Items.titanium);vertical(-24,9,1);
        world.tile(x-24,y).setBlock(Blocks.router,team);
        drill(-40,-15,Items.coal);horizontal(-38,-26,-15);
        graphite=put(Blocks.graphitePress,-25,-15);horizontal(-23,-4,-15);vertical(-3,-15,-1);
        world.tile(x-3,y).setBlock(Blocks.router,Team.sharded);
        drill(-40,-10,Items.coal);horizontal(-38,-26,-10);put(Blocks.graphitePress,-25,-10);horizontal(-23,-4,-10);
        drill(-23,-26,Items.coal);horizontal(-21,-9,-26);
        silicon=put(Blocks.siliconSmelter,-8,-26);drill(5,-26,Items.sand);horizontal(3,-6,-26);
        vertical(-8,-24,-5);horizontal(-8,-1,-4);vertical(0,-4,c>=11?-3:-2);
        drill(-23,-20,Items.coal);horizontal(-21,-15,-20);put(Blocks.siliconSmelter,-14,-20);
        drill(5,-20,Items.sand);horizontal(3,-12,-20);
        vertical(-14,-18,-17);horizontal(-14,-9,-16);
        put(Blocks.unloader,-1,2+hubShift).configureAny(Items.titanium);vertical(-1,3+hubShift,4+hubShift);
        put(Blocks.unloader,1,2+hubShift).configureAny(Items.graphite);vertical(1,3+hubShift,4+hubShift);belt(1,5+hubShift,2);
        put(CraftingBlock.stampingFacility,-1,5+hubShift);belt(-2,5+hubShift,2);vertical(-3,5+hubShift,1);
        put(Blocks.unloader,2+hubShift,0).configureAny(Items.silicon);horizontal(3+hubShift,8,0);
        put(CraftingBlock.processorManuFactory,9,0);vertical(11,0,-2);horizontal(11,3,-3);belt(2,-3,1);if(c<11){belt(2,-2,1);belt(2,-1,2);}
        // Two mined-thorium RTG banks stay self-sufficient during zero irradiance, including early sectors.
        prefab(Items.thorium,15,24,-23,0);prefab(Items.thorium,15,44,-23,2);
        put(Blocks.batteryLarge,8,-8).power.status=1;put(Blocks.batteryLarge,14,-8).power.status=1;
        for(int dx:new int[]{20,25,30,35,40,45})for(int dy:new int[]{-9,-4})put(Blocks.largeSolarPanel,dx,dy);
        put(c>=10?UnitBlock.jumpGateHyper:c>=5?UnitBlock.jumpGateStandard:c>=3?UnitBlock.jumpGatePrimary:UnitBlock.jumpGateBasic,10,9);
        }
        // Authored NH batteries: ammunition caliber and support industries follow research.
        int cell=0;
        for(int dy:new int[]{25,54})for(int dx:new int[]{-39,-13,13,39}){
            final int kind=cell++;
            at(dx,dy,()->{
                if(c>=12 && kind%3==0) siegeBattery();
                else if(kind==0&&c>=7) steelBattery();
                else if(kind==1&&c>=8&&c<12) xenBattery(c>=11);
                else if(c>=8) laserBattery();
                else rapidBattery(c, c>=4 && (kind+c)%3==0);
            });
        }
        for(int[]p:new int[][]{{-9,7},{-2,10},{-43,-3},{43,10}})maintenance(p[0],p[1]);
        // Extra rooftop photovoltaic reserve belongs to the real grid.
        if(c<8){panels(-49,-30,1,-18);panels(19,-16,49,-13);}
        else{
            // Service spaces are a real, damageable NH geothermal grid, independent of sunlight.
            for(int gy=-29;gy<=12;gy+=4)for(int gx=-48;gx<=48;gx+=4)if(free(PowerBlock.geologicalPhotothermalGenerator,ox+gx,oy+gy))geothermal(gx,gy);
            for(int gx=-48;gx<=48;gx+=4)if(free(PowerBlock.geologicalPhotothermalGenerator,ox+gx,oy+42))geothermal(gx,42);
        }
        perimeter(-52,-33,52,70,c>=8);grid(-52,-32,52,68);
        state.rules.tags.put("workshop.player."+(secondary?1:0),Point2.pack(x,y)+"");
        state.rules.tags.put("workshop.graphite."+(secondary?1:0),graphite.pos()+"");
        state.rules.tags.put("workshop.silicon."+(secondary?1:0),silicon.pos()+"");
    }
    public static void enemy(int x,int y,int c,int index){
        boolean late=c>=10;int span=late?61:46;
        begin(x,y,Team.blue,c,-span,-(late?55:45),span,late?55:47);
        put(c>=11?SpecialBlock.coreCluster:Blocks.coreFoundation,0,0);
        // Forward fire line, inner flanking batteries, and rear heavy weapon workshops.
        int level=Math.min(12,c+1);
        if(late){
            for(int dx:new int[]{-42,-14,14,42})at(dx,-32,()->{if(level>=11)siegeBattery();else rapidBattery(level,(index+c)%2==0);});
            for(int dx:new int[]{-42,42})at(dx,0,()->laserBattery());
            boolean command=c>=12&&(index==2||c==15);
            if(command)at(0,35,()->commandBattery());
            for(int dx:new int[]{-42,-14,14,42}){
                if(command&&Math.abs(dx)<20)continue;
                final boolean wing=Math.abs(dx)>20;
                at(dx,32,()->{if(wing){if(dx<0)warYard(level,index);else if(level>=12)executorBattery();else steelBattery();}else if(level>=12)laserBattery();else xenBattery(level>=11);});
            }
            prefab(Items.thorium,15,-10,14,0);prefab(Items.thorium,15,10,14,2);
            put(Blocks.batteryLarge,-7,0).power.status=1;put(Blocks.batteryLarge,7,0).power.status=1;
            put(level>=12?DefenseBlock.largeShieldGenerator:DefenseBlock.standardForceProjector,0,5);
            panels(-24,-49,24,-42);
        }else{
            for(int dx:new int[]{-29,29})for(int dy:new int[]{-27,3,32}){
                final int py=dy;
                at(dx,dy,()->{
                    if(py==32&&dx<0)warYard(level,index);
                    else if(level>=8 && py==32)xenBattery(false);
                    else if(level>=8 && py==3)laserBattery();
                    else if(level>=7 && py==3)steelBattery();
                    else rapidBattery(level,level>=4 && (py<0 || index%2==0));
                });
            }
            prefab(Items.thorium,15,0,34,0);prefab(Items.thorium,15,0,-29,0);
            put(Blocks.batteryLarge,-7,17).power.status=1;put(Blocks.batteryLarge,7,17).power.status=1;
            if(level>=6){put(DefenseBlock.standardForceProjector,-7,7);put(DefenseBlock.standardForceProjector,7,7);}
            panels(-13,-40,13,-19);
        }
        // A visible primary hub feeds actual NH guns through unloaders and conveyors.
        var depot=put(Blocks.vault,-8,-9);depot.items.add(Items.titanium,1000);
        state.rules.tags.put("frontier.depot."+index,depot.pos()+"");
        var unloader=put(Blocks.unloader,-6,-9);unloader.configureAny(Items.titanium);
        horizontal(-5,8,-9);
        for(int gx:new int[]{-3,5}){
            world.tile(ox+gx,oy-9).setBlock(Blocks.router,team);
            vertical(gx,-10,-12);
            var gun=put(c<3?TurretBlock.pulse:TurretBlock.synchro,gx,-14);
            // Size 2 and size 3 both accept ammunition through the upper adjacent cell at -12.
            if(gx==-3)state.rules.tags.put("frontier.test-gun."+index,gun.pos()+"");
        }
        for(int[]p:new int[][]{{-13,-12},{13,-12},{0,8},{0,18}})maintenance(p[0],p[1]);

        perimeter(-span+1,-(late?54:44),span-1,late?54:44,late);
        if(late){panels(-58,-49,-56,49);panels(56,-49,58,49);
            if(c>=12){for(int gx=-24;gx<=24;gx+=4)for(int gy=-49;gy<=18;gy+=4)if(free(PowerBlock.geologicalPhotothermalGenerator,ox+gx,oy+gy))geothermal(gx,gy);}
            else{panels(-25,-2,-18,18);panels(18,-2,25,18);}}
        grid(-span+1,-(late?53:43),span-1,late?53:43);
        state.rules.tags.put("workshop.enemy."+index,Point2.pack(x,y)+"");
    }
    public static void terminal(int x,int y,int index){
        begin(x,y,Team.blue,15,-38,-39,38,38);
        put(Blocks.batteryLarge,0,0);
        at(-24,-22,()->siegeBattery());at(24,-22,()->siegeBattery());
        at(-24,4,()->laserBattery());at(24,7,()->xenBattery(true));
        prefab(Items.thorium,15,0,-25,0);prefab(Items.thorium,15,29,28,1);
        for(int dx=-20;dx<=15;dx+=5)for(int dy:new int[]{23,28})put(Blocks.largeSolarPanel,dx,dy);
        for(int[]d:new int[][]{{0,-9},{0,9},{0,19},{-10,0},{10,0},{-10,16},{13,16}})put(Blocks.powerNodeLarge,d[0],d[1]);
        var depot=put(Blocks.vault,-12,12);depot.items.add(Items.titanium,1000);state.rules.tags.put("frontier.depot."+index,depot.pos()+"");
        put(Blocks.unloader,-10,12).configureAny(Items.titanium);horizontal(-9,9,12);
        for(int gx:new int[]{-7,7}){world.tile(ox+gx,oy+12).setBlock(Blocks.router,team);vertical(gx,11,-5);var gun=put(TurretBlock.pulse,gx,-7);if(gx==-7)state.rules.tags.put("frontier.test-gun."+index,gun.pos()+"");}
        maintenance(-13,-4);maintenance(13,-4);
        perimeter(-37,-38,37,37,true);grid(-37,-37,37,36);
    }
    public static void quarryGuard(int x,int y){
        begin(x,y+44,Team.sharded,14,-39,-14,39,17);
        at(-24,0,()->laserBattery());at(4,0,()->siegeBattery());
        prefab(Items.thorium,15,28,0,0);maintenance(24,-10);grid(-38,-13,38,13);
    }

}
