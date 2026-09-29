package newhorizon.content.campaign;

import arc.Core;
import arc.Events;
import arc.struct.StringMap;
import arc.util.Time;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.ctype.UnlockableContent;
import mindustry.game.EventType;
import mindustry.game.Rules;
import mindustry.gen.Call;
import mindustry.type.SectorPreset;
import mindustry.type.UnitType;
import newhorizon.content.*;
import newhorizon.content.blocks.*;
import newhorizon.content.units.GroundUnitTypes;
import newhorizon.expand.game.InterventionState;
import newhorizon.expand.game.RaidState;
import newhorizon.expand.game.SpecialEventState;
import newhorizon.util.func.NHFunc;

import static mindustry.Vars.*;

/** Afterglow: a three-mission community continuation of Midantha. */
public final class AfterglowCampaign {
    public static SectorPreset supply, ridges, citadel;
    public static SectorPreset[] sectors;
    public static final String tag = "nh-afterglow-chapter";
    private static float hudTimer;
    private static boolean showingHud;

    public static void loadSectors() {
        supply = sector("afterglow-supply", 43, 3, 25);
        ridges = sector("afterglow-ridges", 44, 5, 0);
        citadel = sector("afterglow-citadel", 45, 7, 0);
        sectors = new SectorPreset[3 + FrontierMissions.all.length];
        sectors[0]=supply; sectors[1]=ridges; sectors[2]=citadel;
        for(int i=0;i<FrontierMissions.all.length;i++){
            var m=FrontierMissions.all[i];
            sectors[i+3]=sector("afterglow-"+m.id,46+i,m.difficulty,m.waves==0||i==9?0:m.waves+1);

        }
    }

    private static SectorPreset sector(String name, int id, int difficulty, int winWave) {
        SectorPreset p = new SectorPreset(name, NHPlanets.midantha, id);
        p.showHidden = true;
        p.addStartingItems = true;
        p.difficulty = difficulty;
        p.captureWave = winWave;
        p.allowLaunchLoadout = false;
        p.allowLaunchSchematics = false;
        p.overrideLaunchDefaults = true;
        return p;
    }

    public static void applyRules(Rules r) {
        for (SectorPreset p : sectors) {
            if (p == null || r.sector != p.sector || p.generator.map == null) continue;
            // Map rules may be reapplied on sector launch. Keep campaign event progress on save/load.
            StringMap progress = new StringMap();
            r.tags.each((key, value) -> {
                if (key.startsWith("afterglow.")) progress.put(key, value);
            });
            p.generator.map.rules(r);
            r.tags.putAll(progress);
            r.sector = p.sector;
            r.planet = NHPlanets.midantha;
            int c=r.tags.getInt(tag,-1);
            if(c>=3)FrontierCampaign.restoreRules(r,c-3);
        }
    }

    /** Register after NHLogic so chapter rules take precedence over random default raids. */
    public static void loadEvents() {
        Events.on(EventType.WorldLoadEvent.class, e -> prepareWorld());
        Events.on(EventType.PlayEvent.class, e -> {
            prepareWorld();
            if (chapter() < 0 || net.client()) return;
            unlockFieldKit();
            if (once("intro")) announce("intro." + chapter());
            if (chapter()>=3) FrontierCampaign.start(chapter()-3);
        });
        Events.on(EventType.WaveEvent.class, e -> onWave());
        Events.run(EventType.Trigger.update, AfterglowCampaign::update);
        Events.on(EventType.ResetEvent.class, e -> clearHud());
        Events.on(EventType.SectorCaptureEvent.class, e -> {
            if (e.sector == supply.sector) ridges.quietUnlock();
            if (e.sector == ridges.sector) citadel.quietUnlock();
            if (e.sector == NHSectorPresents.edgeZone.sector) supply.quietUnlock();
            for(int i=2;i<sectors.length-1;i++) if(e.sector==sectors[i].sector) sectors[i+1].quietUnlock();
            if (e.sector == sectors[sectors.length-1].sector && e.initialCapture) announce("frontier.ending");
        });
    }

    public static int chapter() {
        if (state == null || state.rules == null) return -1;
        String id = state.rules.tags.get(tag, "");
        try {int c=Integer.parseInt(id);return c>=0&&c<13?c:-1;} catch(NumberFormatException ignored){return -1;}
    }

    private static void prepareWorld() {
        hudTimer = 0;
        if (chapter() < 0) { clearHud(); return; }
        RaidState.setScale(0);
        InterventionState.setScale(0);
        SpecialEventState.setEnabled(false);
        if(chapter()>=3) FrontierCampaign.prepare(chapter()-3);
    }

    static boolean once(String key) {
        key = "afterglow." + key;
        if (state.rules.tags.containsKey(key)) return false;
        state.rules.tags.put(key, "true");
        return true;
    }

    static void announce(String key) {
        String text = Core.bundle.get("afterglow." + key);
        if (!headless) ui.showInfoToast(text, 10f);
        if (net.server()) Call.infoToast(text, 10f);
    }

    public static void onWave() {
        int c = chapter(), wave = state.wave - 1;
        if (c < 0 || net.client() || state.gameOver) return;
        if (c >= 3) {FrontierCampaign.wave(c-3,wave);return;}
        if (c == 0) {
            if (wave == 7 && once("air-warning")) announce("warning.air");
            if (wave == 8 && once("air")) fleet(NHUnitTypes.sharp, 6, false, .88f, .58f);
            if (wave == 15 && once("ally-warning")) announce("warning.ally");
            if (wave == 16 && once("ally")) {
                fleet(GroundUnitTypes.thynomo, 5, true, .5f, .25f);
                announce("allies");
            }
        } else if (c == 1) {
            if (wave == 9 && once("ally-warning")) announce("warning.ally");
            if (wave == 10 && once("ally")) {fleet(GroundUnitTypes.thynomo, 8, true, .5f, .25f); announce("allies");}
            if (wave == 17 && once("air-warning")) announce("warning.air");
            if (wave == 18 && once("air")) fleet(NHUnitTypes.branch, 4, false, .9f, .62f);
        } else {
            if (wave == 7 && once("air-warning")) announce("warning.air");
            if (wave == 8 && once("air")) fleet(NHUnitTypes.branch, 6, false, .12f, .6f);
            if (wave == 11 && once("ally-warning")) announce("warning.ally");
            if (wave == 12 && once("ally")) {fleet(NHUnitTypes.aliotiat, 4, true, .5f, .27f); announce("allies");}
            if (wave == 19 && once("heavy-warning")) announce("warning.heavy");
            if (wave == 20 && once("heavy")) fleet(NHUnitTypes.tarlidor, 1, false, .5f, .86f);
        }
    }

    static void fleet(UnitType type, int count, boolean allied, float x, float y) {
        // New Horizon's serializable Spawner entity handles warm-up and reload across saved games.
        boolean spawned = NHFunc.spawnUnit(allied ? state.rules.defaultTeam : state.rules.waveTeam,
                world.width() * tilesize * x, world.height() * tilesize * y,
                allied ? 90f : 270f, 100f, 90f, 10f, type, count);
        if (!spawned) arc.util.Log.warn("Afterglow: no valid landing zone for @", type.name);
    }

    public static void update() {
        int c = chapter();
        if (c < 0 || !state.isGame()) {clearHud(); return;}
        if (state.isPaused() || state.gameOver) return;
        hudTimer += Time.delta;
        if (hudTimer < 60f) return;
        hudTimer = 0;
        if(c>=3 && !net.client()) FrontierCampaign.update(c-3);
        if (!net.client() && c == 2) {
            int remaining = state.rules.waveTeam.cores().size;
            if (remaining < 3 && once("cache-1")) supplyCache();
            if (remaining < 2 && once("cache-2")) supplyCache();
        }
        if (!headless) {
            String objective = c == 0 ? Core.bundle.format("afterglow.hud.defend", Math.min(24, state.wave - 1))
                    : Core.bundle.format("afterglow.hud.attack", state.rules.waveTeam.cores().size);
            if(c>=3) objective=FrontierCampaign.objective(c-3);
            if (state.isCampaign() && state.rules.sector.info.wasCaptured) objective = Core.bundle.get("afterglow.hud.complete");
            ui.hudfrag.setHudText("[accent]" + Core.bundle.get("afterglow.chapter." + c) + "[]\n" + objective);
            showingHud = true;
        }
    }

    private static void supplyCache() {
        var core = state.rules.defaultTeam.core();
        if (core == null) return;
        core.items.add(Items.titanium, Math.min(2000, Math.max(0, core.storageCapacity - core.items.get(Items.titanium))));
        core.items.add(Items.silicon, Math.min(1500, Math.max(0, core.storageCapacity - core.items.get(Items.silicon))));
        core.items.add(NHItems.zeta, Math.min(1000, Math.max(0, core.storageCapacity - core.items.get(NHItems.zeta))));
        announce("cache");
    }

    private static void clearHud() {
        if (showingHud && !headless && ui != null) ui.hudfrag.toggleHudText(false);
        showingHud = false;
    }

    private static void unlockFieldKit() {
        // A mission field kit, not a global all-tech unlock; later research remains intact.
        UnlockableContent[] kit = {
                ProductionBlock.interlockingDrill, ProductionBlock.resonanceMiningFacility,
                ProductionBlock.sandCracker, ProductionBlock.titaniumReconstructor, ProductionBlock.tungstenReconstructor,
                DistributionBlock.conveyor, DistributionBlock.conveyorBridge, DistributionBlock.logisticsRouter,
                DistributionBlock.conduit, DistributionBlock.conveyorUnloader,
                PowerBlock.fluxNodeMK1, PowerBlock.fluxNodeLargeMK1, PowerBlock.photothermalGenerator,
                UnitBlock.jumpGateBasic, UnitBlock.jumpGatePrimary,
                TurretBlock.thermo, TurretBlock.pulse, TurretBlock.beam,
                GroundUnitTypes.origin, GroundUnitTypes.thynomo, NHUnitTypes.sharp, NHUnitTypes.branch,
                Blocks.titaniumConveyor, Blocks.mechanicalDrill, Blocks.pneumaticDrill,
                Blocks.mechanicalPump, Blocks.conduit, Blocks.graphitePress, Blocks.siliconSmelter,
                Blocks.powerNodeLarge, Blocks.largeSolarPanel, Blocks.batteryLarge, Blocks.steamGenerator,
                Blocks.scatter, Blocks.titaniumWall, Blocks.unloader, Blocks.router, Blocks.junction
        };
        for (UnlockableContent entry : kit) {
            if(chapter()>=3&&(entry==ProductionBlock.resonanceMiningFacility||entry==ProductionBlock.titaniumReconstructor||
                entry==ProductionBlock.tungstenReconstructor||entry==TurretBlock.beam))continue;
            entry.quietUnlock();
        }
    }
}
