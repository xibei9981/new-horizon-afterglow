import arc.files.Fi;
import arc.struct.*;
import mindustry.ctype.UnlockableContent;
import mindustry.content.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.consumers.*;
import newhorizon.content.campaign.*;
import newhorizon.expand.block.production.factory.RecipeGenericCrafter;
import newhorizon.expand.block.production.drill.OreCollector;
import newhorizon.expand.block.special.JumpGate;
import static mindustry.Vars.*;

/** Extracts actual initialized recipes and actual map resource sources for the closure audit. */
public class ResourceChecks extends CampaignChecks {
    static String names(ItemStack[] a){if(a==null)return "";StringBuilder s=new StringBuilder();for(var x:a)if(x.amount>0)s.append(x.item.name).append(',');return s.toString();}
    static String liquids(LiquidStack[] a){if(a==null)return "";StringBuilder s=new StringBuilder();for(var x:a)if(x.amount>0)s.append(x.liquid.name).append(',');return s.toString();}
    public static void run(){
        StringBuilder data=new StringBuilder();
        ObjectSet<UnlockableContent> inTree=new ObjectSet<>();newhorizon.content.NHTechTree.root.each(n->inTree.add(n.content));
        for(var b:content.blocks())if((b.minfo.mod!=null&&b.minfo.mod.name.equals("new-horizon"))||b==Blocks.kiln||b==Blocks.cultivator||b==Blocks.laserDrill||b==Blocks.graphitePress||b==Blocks.siliconSmelter){
            StringBuilder inputs=new StringBuilder();
            for(var c:b.consumers)if(!c.optional&&!(c instanceof ConsumePower)){
                if(c instanceof ConsumeItems ci)inputs.append(names(ci.items));
                else if(c instanceof ConsumeLiquid cl)inputs.append(cl.liquid.name).append(',');
                else if(c instanceof ConsumeLiquids cl)inputs.append(liquids(cl.liquids));
                else inputs.append('?').append(c.getClass().getSimpleName()).append(',');
            }
            String parents="";if(b.techNode!=null&&b.techNode.parent!=null)parents=b.techNode.parent.content.name;
            data.append("B\t").append(b.name).append('\t').append(AfterglowTech.milestones.get(b,0)).append('\t').append(names(b.requirements)).append('\t').append(inputs).append('\t').append(b.getClass().getSuperclass().getSimpleName()).append('\t').append(parents).append('\t').append(inTree.contains(b)).append('\n');
            if(b instanceof RecipeGenericCrafter rc){for(var r:rc.recipes)data.append("R\t").append(b.name).append('\t').append(names(r.inputItem.toArray(ItemStack.class))).append(liquids(r.inputLiquid.toArray(LiquidStack.class))).append('\t').append(names(r.outputItem.toArray(ItemStack.class))).append(liquids(r.outputLiquid.toArray(LiquidStack.class))).append('\n');}
            else if(b instanceof GenericCrafter c)data.append("R\t").append(b.name).append('\t').append(inputs).append('\t').append(names(c.outputItems)).append(liquids(c.outputLiquids)).append('\n');
            else if(b instanceof newhorizon.expand.block.production.factory.MultiBlockCrafter c)data.append("R\t").append(b.name).append('\t').append(inputs).append('\t').append(names(c.outputItems)).append(liquids(c.outputLiquids)).append('\n');
            if(b instanceof mindustry.world.blocks.defense.turrets.ItemTurret turret){String ammo="";for(var a:turret.ammoTypes.keys())ammo+=a.name+",";data.append("A\t").append(b.name).append('\t').append(ammo).append('\n');}
            if(b instanceof Drill d)data.append("D\t").append(b.name).append('\t').append(d.tier).append('\t').append(d.blockedItem==null?"":d.blockedItem.name).append('\n');
            if(b instanceof JumpGate gate)for(var r:gate.recipeList)data.append("U\t").append(r.unitType.name).append('\t').append(AfterglowTech.milestones.get(r.unitType,0)).append('\t').append(names(r.baseRequirements())).append('\t').append(b.name).append('\t').append(inTree.contains(r.unitType)).append('\n');
        }
        for(var i:content.items())data.append("I\t").append(i.name).append('\t').append(i.hardness).append('\n');
        for(int index=0;index<16;index++){
            var p=AfterglowCampaign.sectors[index];p.sector.clearInfo();logic.reset();world.loadSector(p.sector,new WorldParams(){{saveInfo=false;}});
            boolean[] reachable=new boolean[world.width()*world.height()];java.util.ArrayDeque<Tile> queue=new java.util.ArrayDeque<>();
            var core=state.rules.defaultTeam.core();var start=world.tile(core.tileX(),core.tileY()+4);reachable[start.array()]=true;queue.add(start);
            while(!queue.isEmpty()){
                var t=queue.remove();for(int[]d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){var next=world.tile(t.x+d[0],t.y+d[1]);
                    if(next==null||reachable[next.array()]||next.floor().isDeep()||(next.solid()&&(next.block().isStatic()||(next.team()==state.rules.defaultTeam&&!next.block().teamPassable))))continue;
                    reachable[next.array()]=true;queue.add(next);
                }
            }
            ObjectIntMap<String> resources=new ObjectIntMap<>();
            for(var t:world.tiles){if(reachable[t.array()]&&t.drop()!=null&&!t.floor().isLiquid&&!t.block().solid)resources.increment(t.drop().name,0,1);if(t.floor().liquidDrop!=null)resources.increment(t.floor().liquidDrop.name,0,1);}
            StringBuilder ores=new StringBuilder();for(var e:resources)ores.append(e.key).append(':').append(e.value).append(',');
            data.append("M\t").append(index+1).append('\t').append(ores).append('\t').append(names(state.rules.loadout.toArray(ItemStack.class))).append('\n');
        }
        new Fi("campaign-tools/resource-inputs.tsv").writeString(data.toString());System.out.println("RESOURCE_DATA_EXTRACTED");
    }
}
