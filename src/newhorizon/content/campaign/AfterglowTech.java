package newhorizon.content.campaign;

import arc.Core;
import arc.struct.ObjectIntMap;
import arc.struct.Seq;
import mindustry.content.TechTree;
import mindustry.ctype.UnlockableContent;
import mindustry.game.Objectives.SectorComplete;
import newhorizon.content.*;
import newhorizon.content.blocks.*;
import newhorizon.content.units.GroundUnitTypes;
import static mindustry.Vars.*;

/** Research permissions advance with sector victories. Never clears an existing player's unlocks. */
public final class AfterglowTech {
    public static final ObjectIntMap<UnlockableContent> milestones=new ObjectIntMap<>();
    private static void tier(int cleared,UnlockableContent... entries){for(var entry:entries)milestones.put(entry,cleared);}
    private static void supplyTier(int stage,String... names){
        for(String name:names){
            UnlockableContent entry=content.block(name);if(entry==null)entry=content.unit(name);
            if(entry==null)throw new IllegalArgumentException("Unknown campaign technology: "+name);
            milestones.put(entry,Math.max(stage,milestones.get(entry,0)));
        }
    }
    public static void load(){
        milestones.clear();
        tier(1,ProductionBlock.scanCollector,ProductionBlock.resonanceMiningFacility,TurretBlock.beam,TurretBlock.synchro,CraftingBlock.crystallizer);
        tier(2,PowerBlock.fluxNodeMK2,PowerBlock.fluxNodeLargeMK2,PowerBlock.armorBatteryLarge,DistributionBlock.conveyorUnloaderFast,DistributionBlock.conveyorBridgeExtend);
        tier(3,UnitBlock.jumpGatePrimary,NHUnitTypes.aliotiat,NHUnitTypes.assaulter,CraftingBlock.multipleRollingMill,CraftingBlock.metalOxhydrigenRestructuror,CraftingBlock.processorPrinter,DefenseBlock.standardRegenProjector);
        tier(4,ProductionBlock.beamMiningFacility,CraftingBlock.heavyStampingFacility,CraftingBlock.mixedRollingMill,CraftingBlock.denseFactory,TurretBlock.electro);
        tier(5,UnitBlock.jumpGateStandard,NHUnitTypes.naxos,NHUnitTypes.striker,NHUnitTypes.macrophage,DistributionBlock.stackRail);
        tier(6,PowerBlock.hydrazineGenerator,DefenseBlock.standardForceProjector,NHUnitTypes.warper,NHBlocks.antiBulletTurret,TurretBlock.argmot);
        tier(7,NHUnitTypes.tarlidor,NHUnitTypes.zarkov,DefenseBlock.heavyRegenProjector,TurretBlock.vortex);
        tier(8,PowerBlock.fissionReactor,CraftingBlock.processorEtchingFacility,CraftingBlock.irayrondFactory,TurretBlock.concentration);
        tier(9,PowerBlock.armorBatteryHuge,DefenseBlock.largeShieldGenerator,DistributionBlock.multiArmorConveyor,DistributionBlock.heavyStackLoader,SpecialBlock.coreNexus);
        tier(10,UnitBlock.jumpGateHyper,PowerBlock.fusionReactor,CraftingBlock.fusionCoreEnergyFactory,CraftingBlock.zetaFactory,TurretBlock.bombard);
        tier(11,GroundUnitTypes.annihilation,NHUnitTypes.destruction,NHUnitTypes.longinus,NHUnitTypes.hurricane,NHBlocks.prism);
        tier(12,CraftingBlock.positivePhaseDecayer,CraftingBlock.negativePhaseDecayer,DefenseBlock.riftShield,CraftingBlock.processorCompactor,NHBlocks.executor);
        tier(10,ProductionBlock.implosionMiningFacility,CraftingBlock.heavyRollingMill,CraftingBlock.largeIrayrondFactory,CraftingBlock.ancimembraneConcentrator,CraftingBlock.hyperProcessor);
        tier(11,CraftingBlock.nodexFactory,CraftingBlock.hadronCompositeBuilder,CraftingBlock.darkEnergyTrap,PowerBlock.hyperReactor,DefenseBlock.plasmaMembrane,SpecialBlock.coreCluster);
        tier(12,NHUnitTypes.collapser,NHUnitTypes.guardian,NHUnitTypes.pester,NHBlocks.dendrite,NHBlocks.endOfEra);
        tier(12,NHBlocks.eternity);
        // Production chains must exist before the equipment that consumes their materials.
        tier(2,CraftingBlock.multipleRollingMill);
        tier(3,CraftingBlock.fusionCoreEnergyFactory);
        tier(4,CraftingBlock.zetaFactory);
        tier(5,CraftingBlock.processorEtchingFacility);
        tier(8,CraftingBlock.processorCompactor);
        tier(10,CraftingBlock.nodexFactory,CraftingBlock.positivePhaseDecayer,CraftingBlock.negativePhaseDecayer);
        tier(1,mindustry.content.Blocks.laserDrill);
        // Recipe-audited gates: construction, operating input and at least one ammunition chain.
        supplyTier(1, "new-horizon-crucible-foundry", "new-horizon-fire-extinguisher", "new-horizon-heavy-liquid-storage", "new-horizon-heavy-storage", "new-horizon-histone", "new-horizon-multiple-launcher", "new-horizon-refactoring-multi-wall", "new-horizon-standard-liquid-storage", "new-horizon-xen-separator");
        supplyTier(2, "new-horizon-core-array", "new-horizon-fabric-restructuror", "new-horizon-hyper-cooler", "new-horizon-irdryon-fluid-factory", "new-horizon-laser-wall", "new-horizon-logistics-extend-liquid-bridge", "new-horizon-multi-junction", "new-horizon-multi-router", "new-horizon-rapid-unloader", "new-horizon-thorium-transmuter");
        supplyTier(3, "new-horizon-air-raider", "new-horizon-alloy-smelter", "new-horizon-bomb-launcher");
        supplyTier(4, "new-horizon-antibody", "new-horizon-armor-battery-large", "new-horizon-blaster", "new-horizon-casting-foundry", "new-horizon-flux-node-large-mk2", "new-horizon-interferon", "new-horizon-junior-module-beacon", "new-horizon-phase-rectificatior", "new-horizon-plasma-activator", "new-horizon-senior-module-beacon", "new-horizon-slavio");
        supplyTier(5, "new-horizon-dense-factory", "new-horizon-gravity-trap", "new-horizon-hive", "new-horizon-irdryon-phase-ascender", "new-horizon-surge-synthesizer");
        supplyTier(8, "new-horizon-atom-separator", "new-horizon-fabric-synthesizer", "new-horizon-remote-storage", "new-horizon-reverse-collapse-facility", "new-horizon-seton-phased-wall");
        supplyTier(10, "new-horizon-ancient-laser-wall", "new-horizon-blood-star", "new-horizon-hyper-space-warper", "new-horizon-rail-gun", "new-horizon-shaped-wall");
        // Children cannot be researched before their parent tier, including alternative tree entries.
        boolean changed;
        do{
            changed=false;
            for(var node:TechTree.all)if(node.parent!=null&&(node.rootNode==NHTechTree.root||node.content.minfo.mod==newhorizon.NewHorizon.MOD)){
                int parent=milestones.get(node.parent.content,0),own=milestones.get(node.content,0);
                if(parent>own&&!(node.content instanceof mindustry.type.SectorPreset)){
                    milestones.put(node.content,parent);changed=true;
                }
            }
        }while(changed);
        for(var node:TechTree.all){
            int cleared=milestones.get(node.content,0);
            if(cleared>0&&(node.rootNode==NHTechTree.root||node.content.minfo.mod==newhorizon.NewHorizon.MOD))node.objectives.add(new SectorComplete(AfterglowCampaign.sectors[cleared-1]));
        }
        for(int i=0;i<AfterglowCampaign.sectors.length;i++){
            var sector=AfterglowCampaign.sectors[i];sector.description=(sector.description==null?"":sector.description)+"\n\n"+reward(i+1);
        }
    }
    public static boolean available(UnlockableContent content){
        int tier=milestones.get(content,0);
        return tier==0||AfterglowCampaign.sectors[tier-1].sector.info.wasCaptured;
    }
    public static String reward(int cleared){
        Seq<String> names=new Seq<>();
        for(var entry:milestones)if(entry.value==cleared)names.add(entry.key.localizedName);
        names.sort();
        int total=names.size;if(total>6)names.truncate(6);
        return names.isEmpty()?"":Core.bundle.get("afterglow.tech-open")+names.toString("、")+(total>6?" … ("+total+")":"");
    }
    public static void announce(int cleared){
        String text=reward(cleared);if(text.isEmpty())return;
        if(!headless)ui.showInfoToast(text,15f);
        if(net.server())mindustry.gen.Call.infoToast(text,15f);
    }
}
