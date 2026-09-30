import arc.*;import arc.files.Fi;import arc.util.Time;import mindustry.content.*;import mindustry.game.*;import mindustry.gen.*;import mindustry.world.*;import mindustry.world.blocks.defense.MendProjector;import mindustry.world.blocks.defense.turrets.*;import mindustry.world.blocks.power.*;import newhorizon.content.campaign.*;import static mindustry.Vars.*;
public class WorkshopChecks extends CampaignChecks{
 public static void run()throws Exception{
  Time.setDeltaProvider(()->1f);
  var legacy=new Rules();legacy.sector=AfterglowCampaign.sectors[4].sector;legacy.tags.put(AfterglowCampaign.tag,"4");legacy.tags.put("afterglow.depot-0","true");
  AfterglowCampaign.applyRules(legacy);
  check("false".equals(legacy.tags.get("afterglow.physical-logistics"))&&legacy.tags.containsKey("afterglow.depot-0"),"old save retains old supply controller and mission progress");
  log("PASS legacy rule migration: 0.5 layout retains legacy logistics and completed depot state.");
  for(int seed=0;seed<3000;seed++){arc.math.Mathf.rand.setSeed(seed);newhorizon.content.NHPostProcess.generate(1f,new arc.math.Rand(seed),false,false,false);}
  log("PASS upstream wave progression: 3000 seeded high-difficulty generations, valid tier indices.");
  if(Boolean.getBoolean("workshop.assaultOnly")){enemyManufacturing(Integer.getInteger("workshop.start",12));dynamicAssault(Integer.getInteger("workshop.start",12));return;}
  for(int c=Integer.getInteger("workshop.start",0);c<Integer.getInteger("workshop.end",16);c++){
   var p=AfterglowCampaign.sectors[c];p.sector.clearInfo();logic.reset();world.loadSector(p.sector,new WorldParams(){{saveInfo=false;}});logic.play();state.rules.waveTimer=false;state.rules.waves=true;
   // Production is checked under ceasefire. The arsenal map starts inside mutual
   // artillery range; live fire can legitimately destroy the line being measured.
   var buildingTargets=new arc.struct.ObjectMap<Turret,Boolean>();for(var block:content.blocks())if(block instanceof Turret turret){buildingTargets.put(turret,turret.targetBlocks);turret.targetBlocks=false;}
   for(var b:Groups.build)if(b.team==Team.blue&&b instanceof newhorizon.expand.block.special.JumpGate.JumpGateBuild)b.enabled=false;
   check((state.rules.env&mindustry.world.meta.Env.groundWater)!=0,"campaign planet preserves authored groundwater");pathCheck();if(c==14)log("CARGO_INITIAL "+world.tile(434,214).block()+" build="+world.build(434,214));var core=state.rules.defaultTeam.core();for(var item:new mindustry.type.Item[]{Items.titanium,Items.graphite,Items.silicon,newhorizon.content.NHItems.presstanium,newhorizon.content.NHItems.juniorProcessor})core.items.set(item,0);
   for(int step=0;step<120;step++){Groups.unit.copy().each(u->u.remove());ticks(60);}
   if(c==14)log("CARGO_AFTER "+world.tile(434,214).block()+" build="+world.build(434,214));
   log("ACTUAL_CORE "+core.block+" size="+core.block.size+" at="+core.tileX()+","+core.tileY());
   if(c>=8&&core.items.get(Items.titanium)==0)for(var b:Groups.build)if(b.team==Team.sharded&&Math.abs(b.tileX()-core.tileX())<=10&&b.tileY()<core.tileY()+2)System.out.println("INTAKE "+b.block+" rel="+(b.tileX()-core.tileX())+","+(b.tileY()-core.tileY())+" rot="+b.rotation+" items="+b.items+" power="+(b.power==null?1:b.power.status));
   log("WORKSHOP "+(c+1)+" core titanium="+core.items.get(Items.titanium)+" graphite="+core.items.get(Items.graphite)+" silicon="+core.items.get(Items.silicon)+" presstanium="+core.items.get(newhorizon.content.NHItems.presstanium)+" processor="+core.items.get(newhorizon.content.NHItems.juniorProcessor));
   if(Boolean.getBoolean("industry.probe")&&c>=8)for(var b:Groups.build)if(b.team==Team.sharded&&b.tileY()<core.tileY()+14&&(b.block instanceof mindustry.world.blocks.production.GenericCrafter||b.block instanceof newhorizon.expand.block.production.factory.MultiBlockCrafter||b.block instanceof newhorizon.expand.block.special.RemoteCoreStorage))System.out.println("PLANT "+b.block+" rel="+(b.tileX()-core.tileX())+","+(b.tileY()-core.tileY())+" items="+b.items+" xen="+(b.liquids==null?0:b.liquids.get(newhorizon.content.NHLiquids.xenFluid))+" cryo="+(b.liquids==null?0:b.liquids.get(Liquids.cryofluid))+" eff="+b.efficiency+" power="+(b.power==null?1:b.power.status));
   for(var item:new mindustry.type.Item[]{Items.titanium,Items.graphite,Items.silicon,newhorizon.content.NHItems.presstanium,newhorizon.content.NHItems.juniorProcessor})check(core.items.get(item)>10,"sustainable core production "+c+" "+item);
   if(c==0){var seen=new arc.struct.ObjectSet<mindustry.world.blocks.power.PowerGraph>();for(var b:Groups.build)if(b.team==Team.sharded&&b.power!=null&&seen.add(b.power.graph)){var g=b.power.graph;System.out.println("POWER_GRAPH at="+b.tileX()+","+b.tileY()+" blocks="+g.all.size+" producers="+g.producers.size+" output="+g.getPowerProduced()+" demand="+g.getPowerNeeded()+" stored="+g.getBatteryStored());}}
   int dry=0;for(var t:world.tiles)if(t.isCenter()&&t.build instanceof ItemTurret.ItemTurretBuild gun&&t.team()!=Team.derelict&&!gun.hasAmmo()){System.out.println("DRY_GUN chapter="+(c+1)+" team="+gun.team+" type="+gun.block+" at="+t.x+","+t.y);dry++;}
   if(dry>0){
    for(var b:Groups.build)if(b instanceof Turret.TurretBuild t&&t.totalShots>0)System.out.println("IDLE_CROSSFIRE team="+t.team+" gun="+t.block+" at="+t.tileX()+","+t.tileY()+" shots="+t.totalShots+" target="+t.target);
    for(var t:world.tiles)if(t.isCenter()&&t.team()==Team.blue&&t.build instanceof ItemTurret.ItemTurretBuild gun&&!gun.hasAmmo())for(var b:Groups.build)if(b.team==Team.blue&&b.within(gun,18*tilesize)&&(b.block instanceof mindustry.world.blocks.distribution.Conveyor||b.block instanceof mindustry.world.blocks.production.Drill||b instanceof ItemTurret.ItemTurretBuild))System.out.println("DRY_LOCAL "+b.block+" at="+b.tileX()+","+b.tileY()+" rotation="+b.rotation+" items="+b.items+" efficiency="+b.efficiency+" power="+(b.power==null?1:b.power.status));
    log("DRY_ENV env="+state.rules.env+" water="+mindustry.world.meta.Attribute.water.env());
    for(var t:world.tiles)if(t.isCenter()&&t.build!=null&&t.team()==Team.blue&&Math.abs(t.x-108)<18&&t.y>=168&&t.y<210){var b=t.build;if(b.block instanceof mindustry.world.blocks.production.Drill||b.block instanceof mindustry.world.blocks.production.GenericCrafter||b.block instanceof mindustry.world.blocks.production.SolidPump||b.block instanceof mindustry.world.blocks.storage.Unloader)System.out.println("DRY_FACTORY "+b.block+" "+t.x+","+t.y+" items="+b.items+" config="+b.config()+" enabled="+b.enabled+" water="+(b.liquids==null?-1:b.liquids.get(Liquids.water))+" eff="+b.efficiency+" power="+(b.power==null?"none":b.power.status));}
   }
   if(dry>0)for(var b:Groups.build)if(b.block instanceof mindustry.world.blocks.production.GenericCrafter || b.block instanceof mindustry.world.blocks.production.Drill)if(b.team==Team.sharded)System.out.println("INDUSTRY "+b.block+" "+b.tileX()+","+b.tileY()+" items="+b.items+" water="+(b.liquids==null?0:b.liquids.get(Liquids.water))+" xen="+(b.liquids==null?0:b.liquids.get(newhorizon.content.NHLiquids.xenFluid))+" cryo="+(b.liquids==null?0:b.liquids.get(Liquids.cryofluid))+" eff="+b.efficiency+" power="+(b.power==null?0:b.power.status));
   if(dry>0)for(var b:Groups.build)if(b.team==Team.sharded&&b.liquids!=null&&Math.abs(b.tileX()-(core.tileX()-13))<14&&Math.abs(b.tileY()-(core.tileY()+25))<15)System.out.println("LIQUID "+b.block+" "+(b.tileX()-core.tileX()+13)+","+(b.tileY()-core.tileY()-25)+" water="+b.liquids.get(Liquids.water)+" xen="+b.liquids.get(newhorizon.content.NHLiquids.xenFluid)+" cryo="+b.liquids.get(Liquids.cryofluid)+" rot="+b.rotation+" config="+b.config()+" power="+(b.power==null?0:b.power.status));
   check(dry==0,"all authored item turrets receive real ammunition, dry="+dry);
   int enemyGuns=0;for(var t:world.tiles)if(t.isCenter()&&t.team()==Team.blue&&t.block() instanceof BaseTurret)enemyGuns++;
   int guns=0,belts=0,repair=0;Building patient=null;
   for(var t:world.tiles)if(t.isCenter()&&t.build!=null&&t.team()==state.rules.defaultTeam){var b=t.build;
    if(b.block instanceof BaseTurret)guns++;
    if(b.block instanceof mindustry.world.blocks.distribution.Conveyor)belts++;
    if(b.block instanceof MendProjector mend){repair++;if(patient==null)for(var other:Groups.build)if(other!=b&&other.team==b.team&&other.block instanceof BaseTurret&&other.within(b,mend.range)){patient=other;break;}}
   }
   check(guns>=(c>=8?30:50)&&belts>=150&&repair>=4,"complete player base "+c+" guns="+guns+" belts="+belts+" repair="+repair);
   for(var b:Groups.build)if(b.team!=Team.derelict&&b.block instanceof BaseTurret){check(b.block.name.startsWith("new-horizon-"),"authored weapon belongs to NH campaign: "+b.block);if(b.team==Team.sharded)check(newhorizon.content.campaign.AfterglowTech.milestones.get(b.block,0)<=c,"player can research and rebuild starting weapon at arrival: "+b.block);}
   if(c>=12){
    int siege=Groups.build.count(b->b.team==Team.sharded&&b.block==newhorizon.content.NHBlocks.endOfEra);
    int longRange=Groups.build.count(b->b.team==Team.sharded&&b.block instanceof Turret t&&t.range>=60*tilesize);
    check(siege>=3&&longRange>=guns*.8f,"final-stage starting arsenal is predominantly heavy ranged weapons: siege="+siege+" long="+longRange+" guns="+guns);
   }
   for(var b:Groups.build)if(b.team==Team.sharded&&b.block instanceof mindustry.world.blocks.defense.Wall)check(b.block.name.startsWith("new-horizon-"),"campaign walls use NH defensive technology "+b.block);
   check(patient!=null,"repair covers a real defense");patient.health=patient.maxHealth*.5f;float health=patient.health;ticks(1200);if(patient.health<=health+5)for(var b:Groups.build)if(b.team==Team.sharded&&b.block instanceof MendProjector)System.out.println("REPAIR "+b.tileX()+","+b.tileY()+" power="+b.power.status+" eff="+b.efficiency+" patient="+patient.tileX()+","+patient.tileY()+" distance="+b.dst(patient));check(patient.health>health+5,"repair actually heals defense "+c);
   for(var b:Groups.build)if(b instanceof mindustry.world.blocks.distribution.ItemBridge.ItemBridgeBuild bridge&&bridge.link!=-1)check(((mindustry.world.blocks.distribution.ItemBridge)b.block).linkValid(b.tile,world.tile(bridge.link)),"authored bridge has a valid in-range orthogonal link: "+b.block+" at "+b.tileX()+","+b.tileY());
   if(c>=8)industryProduction(c);
   wallProtection(c);
   if(state.rules.tags.containsKey("frontier.test-gun.0"))physicalFeed(0);
   for(var entry:buildingTargets)entry.key.targetBlocks=entry.value;
   if(Boolean.getBoolean("workshop.combat")){if(c>=12){enemyManufacturing(c);emptyMagazines();}sustainedFire();dynamicAssault(c);yardProduction(c);openingBattle(c);}
   log("PASS WORKSHOP "+(c+1)+": guns="+guns+" enemy-guns="+enemyGuns+" conveyors="+belts+" repair="+repair+"; mining, graphite, silicon, healing and physical ammunition verified.");
  }
  new Fi(Boolean.getBoolean("workshop.combat")?"campaign-tools/combat-verification.txt":"campaign-tools/workshop-verification.txt").writeString(report.toString());
 }
 static void industryProduction(int c){
  var core=state.rules.defaultTeam.core();
  int hubs=Groups.build.count(b->b.team==Team.sharded&&b.block==newhorizon.content.blocks.SpecialBlock.remoteStorage);
  check(hubs<=10,"native player remote-storage limit, deployed="+hubs);
  check(newhorizon.NHGroups.placedRemoteCore[Team.sharded.id].size==hubs,"native app registration and logistics power cost are active");
  for(var b:Groups.build)if(b.team==Team.sharded&&b.tileY()<core.tileY()+14){
   check(b.block!=Blocks.rtgGenerator&&b.block!=Blocks.graphitePress&&b.block!=Blocks.siliconSmelter,"advanced main industrial district uses NH machinery: "+b.block);
   if(b.block instanceof mindustry.world.blocks.production.GenericCrafter||b.block instanceof newhorizon.expand.block.production.factory.MultiBlockCrafter){
    check(newhorizon.content.campaign.AfterglowTech.milestones.get(b.block,0)<=c,"industry can be researched and rebuilt on arrival "+b.block);
    check(b.block.techNode!=null,"authored production machine is present in the technology tree "+b.block);
   }
  }
  for(var item:new mindustry.type.Item[]{newhorizon.content.NHItems.fusionEnergy,newhorizon.content.NHItems.seniorProcessor})core.items.set(item,0);
  if(c>=10)core.items.set(newhorizon.content.NHItems.nodexPlate,0);
  var nodexFactories=Groups.build.copy().select(b->b.team==Team.sharded&&b.block==newhorizon.content.blocks.CraftingBlock.nodexFactory);
  nodexFactories.each(b->b.enabled=false);ticks(120*60);
  int fuel=core.items.get(newhorizon.content.NHItems.fusionEnergy),processor=core.items.get(newhorizon.content.NHItems.seniorProcessor),plate=core.items.get(newhorizon.content.NHItems.nodexPlate);
  nodexFactories.each(b->b.enabled=true);if(c>=10){ticks(60*60);plate=core.items.get(newhorizon.content.NHItems.nodexPlate);}
  log("INDUSTRY RECOVERY "+(c+1)+": hubs="+hubs+" fusion="+fuel+" senior="+processor+" nodex="+plate+" (product core stocks emptied; recipes consume physical inputs)");
  check(fuel>100&&processor>15&&(c<10||plate>40),"advanced manufacturing produces and delivers actual output");
  for(var b:Groups.build)if(b.team==Team.sharded&&b.block==newhorizon.content.blocks.CraftingBlock.fusionCoreEnergyFactory&&b.tileY()<core.tileY()+14){
   check(b.liquids.get(newhorizon.content.NHLiquids.xenFluid)>0&&b.liquids.get(Liquids.cryofluid)>0,"both required factory fluids reach "+b.tileX()+","+b.tileY());
  }
 }
 static void wallProtection(int c){
  Building wall=null;for(var tile:world.tiles)if(tile.isCenter()&&tile.team()==Team.sharded&&tile.build instanceof newhorizon.expand.block.defence.AdaptWall.AdaptWallBuild w&&w.proximity.count(q->q.block==w.block)>=2){wall=w;break;}
  check(wall!=null&&wall.block.teamPassable&&!wall.checkSolid(),"continuous native defensive walls admit friendly units");
  var before=new arc.struct.ObjectFloatMap<Building>();for(var tile:world.tiles)if(tile.isCenter()&&tile.team()==wall.team&&tile.block()==wall.block)before.put(tile.build,tile.build.health);
  wall.damage(1000f);int shared=0;for(var e:before)if(e.key!=wall&&e.key.health<e.value)shared++;
  check(shared>=2&&wall.isValid(),"native wall damage is shared across connected layers, neighbors="+shared);
  for(var e:before)e.key.health=e.value;
  log("PASS WALL "+(c+1)+": "+wall.block+" distributes a real 1000-damage hit to "+shared+" neighboring walls; friendly passage preserved.");
 }
 static void emptyMagazines(){
  for(var b:Groups.build)if(b.team==Team.sharded){
   if(b.items!=null){b.items.set(newhorizon.content.NHItems.fusionEnergy,0);b.items.set(newhorizon.content.NHItems.nodexPlate,0);}
   if(b instanceof ItemTurret.ItemTurretBuild gun&&(b.block==newhorizon.content.NHBlocks.prism||b.block==newhorizon.content.NHBlocks.endOfEra)){gun.ammo.clear();gun.totalAmmo=0;}
  }
  ticks(90*60);
  for(var b:Groups.build)if(b.team==Team.sharded&&b instanceof ItemTurret.ItemTurretBuild gun&&(b.block==newhorizon.content.NHBlocks.prism||b.block==newhorizon.content.NHBlocks.endOfEra))check(gun.hasAmmo(),"empty advanced magazines refill from real factories, "+b.block+" at "+b.tileX()+","+b.tileY());
  log("PASS EMPTY MAGAZINES: core, warehouses, in-transit fuel/nodex and heavy magazines emptied; native factories restore every heavy gun in 90 seconds, without inventory injection.");
 }
 static void sustainedFire(){
  Groups.unit.copy().each(u->u.remove());var core=state.rules.defaultTeam.core();
  var targets=new arc.struct.Seq<Unit>();
  float oldGroundHealth=UnitTypes.reign.health,oldAirHealth=UnitTypes.eclipse.health;
  UnitTypes.reign.health=UnitTypes.eclipse.health=100000000;
  for(int dx:new int[]{-32,0,32})for(var type:new mindustry.type.UnitType[]{UnitTypes.reign,UnitTypes.eclipse}){
   Unit target=type.spawn(Team.blue,core.x+dx*tilesize,core.y+74*tilesize);
   target.health=100000000;target.apply(StatusEffects.unmoving,100000);target.apply(StatusEffects.disarmed,100000);targets.add(target);
  }
  check(!state.getSector().isCaptured(),"stress fixture keeps campaign combat active");
  ticks(180*60);
  float damage=0;for(var target:targets){System.out.println("STRESS_TARGET "+target.type+" health="+target.health+" added="+target.isAdded()+" dead="+target.dead+" at="+target.x+","+target.y);check(target.isAdded()&&target.health>0,"stress targets stay alive for entire firing interval");damage+=100000000-target.health;}
  check(damage>10000,"deployed batteries damage actual ground and air units");
  int rapid=0,loaded=0;float power=1;
  for(var b:Groups.build)if(b.team==Team.sharded){
   if(b.block==newhorizon.content.blocks.TurretBlock.synchro){rapid++;if(((ItemTurret.ItemTurretBuild)b).hasAmmo())loaded++;}
   if(b.power!=null&&b.block.consPower!=null)power=Math.min(power,b.power.status);
  }
  check(rapid==0||loaded>=rapid*0.65f,"front batteries retain ammunition during 180-second sustained fire: "+loaded+"/"+rapid);
  if(power<.9f){var seenGraphs=new java.util.HashSet<mindustry.world.blocks.power.PowerGraph>();for(var b:Groups.build)if(b.team==Team.sharded&&b.power!=null&&b.block.consPower!=null&&b.power.status<.9f){System.out.println("LOW_POWER "+b.block+" at="+b.tileX()+","+b.tileY()+" status="+b.power.status+" graph="+b.power.graph.getID());if(seenGraphs.add(b.power.graph))System.out.println("LOW_GRAPH produced="+b.power.graph.getLastPowerProduced()+" needed="+b.power.graph.getLastPowerNeeded()+" stored="+b.power.graph.getBatteryStored()+" members="+b.power.graph.all.size);}}
  check(power>.9f,"industrial power remains stable under fire "+power);
  int advancedShots=0;for(var b:Groups.build)if(b.team==Team.sharded&&b instanceof Turret.TurretBuild tb&&(b.block==newhorizon.content.blocks.TurretBlock.vortex||b.block==newhorizon.content.NHBlocks.prism||b.block==newhorizon.content.NHBlocks.endOfEra)){System.out.println("HEAVY_FIRE "+b.block+" shots="+tb.totalShots+" ammo="+((ItemTurret.ItemTurretBuild)b).totalAmmo);check(tb.totalShots>3,"advanced cannon actually fires "+b.block);advancedShots+=tb.totalShots;}
  targets.each(u->u.remove());UnitTypes.reign.health=oldGroundHealth;UnitTypes.eclipse.health=oldAirHealth;ticks(60*60);
  for(var b:Groups.build)if(b.team==Team.sharded&&b.block==newhorizon.content.blocks.TurretBlock.synchro)check(((ItemTurret.ItemTurretBuild)b).hasAmmo(),"front ammunition recovers after battle");
  log("PASS SUSTAINED FIRE: 180 seconds, six stationary disarmed ground/air targets; damage="+damage+" loaded rapid batteries="+loaded+"/"+rapid+" minimum power="+power+"; 60-second recovery. This is a supply stress test, not a campaign victory simulation.");
 }
 static void enemyManufacturing(int c){
  var p=AfterglowCampaign.sectors[c];p.sector.clearInfo();logic.reset();world.loadSector(p.sector,new WorldParams(){{saveInfo=false;}});logic.play();state.rules.waveTimer=false;state.rules.waves=true;
  for(var b:Groups.build)if(b.team==Team.blue){if(b instanceof newhorizon.expand.block.special.JumpGate.JumpGateBuild)b.enabled=false;if(b.items!=null)b.items.set(newhorizon.content.NHItems.thermoCoreNegative,0);if(b.block==newhorizon.content.NHBlocks.executor){var t=(ItemTurret.ItemTurretBuild)b;t.ammo.clear();t.totalAmmo=0;}}
  for(int sec=0;sec<120;sec++){ticks(60);Groups.unit.copy().each(u->u.remove());}
  int guns=0;for(var b:Groups.build)if(b.team==Team.blue&&b.block==newhorizon.content.NHBlocks.executor){guns++;check(((ItemTurret.ItemTurretBuild)b).hasAmmo(),"enemy executor factory refills an empty gun "+b.tileX()+","+b.tileY());}
  for(var b:Groups.build)if(b.team==Team.blue&&b.block==newhorizon.content.NHBlocks.eternity){var gun=(ItemTurret.ItemTurretBuild)b;log("COMMAND READY ammo="+gun.totalAmmo+" antimatter="+b.liquids.get(newhorizon.content.NHLiquids.antiMatter)+" power="+b.power.status+" graph-output="+b.power.graph.getPowerProduced()+" demand="+b.power.graph.getPowerNeeded());check(gun.hasAmmo()&&b.liquids.get(newhorizon.content.NHLiquids.antiMatter)>0,"command battery receives real ammunition and coolant");}
  check(guns>0,"enemy advanced executor battery exists");log("PASS ENEMY MANUFACTURING: "+guns+" executor magazines and all negative-core stores emptied; native decayers replenish all guns in 120 seconds from finite fissile/zeta feedstock.");
 }
 static void dynamicAssault(int c){
  var p=AfterglowCampaign.sectors[c];p.sector.clearInfo();logic.reset();world.loadSector(p.sector,new WorldParams(){{saveInfo=false;}});logic.play();state.rules.waveTimer=false;state.rules.waves=true;
  Groups.unit.copy().each(u->u.remove());ticks(120*60);Groups.unit.copy().each(u->u.remove());
  var command=Groups.build.find(b->b.team==Team.blue&&b.block==newhorizon.content.NHBlocks.eternity);var enemy=command==null?Team.blue.cores().first():command.closestCore();var attackers=new arc.struct.Seq<Unit>();var starts=new arc.struct.IntMap<arc.math.geom.Vec2>();
  int buildings=Groups.build.count(b->b.team==Team.blue);
  var guns=Groups.build.copy().select(b->b.team==Team.blue&&b instanceof Turret.TurretBuild&&b.within(enemy,100*tilesize));
  var oldShots=new arc.struct.ObjectIntMap<Building>();guns.each(b->oldShots.put(b,((Turret.TurretBuild)b).totalShots));
  for(int n=0;n<20;n++){
   var type=n<10?newhorizon.content.NHUnitTypes.hurricane:newhorizon.content.NHUnitTypes.longinus;
   var u=type.spawn(Team.sharded,enemy.x+(n%10-4.5f)*8*tilesize,enemy.y-72*tilesize);
   u.controller(type.aiController.get());attackers.add(u);starts.put(u.id,new arc.math.geom.Vec2(u.x,u.y));
  }
  var movedIds=new arc.struct.IntSet();float totalDamage=0;for(int k=0;k<180;k++){ticks(60);if(command!=null&&k%10==0){var gun=(ItemTurret.ItemTurretBuild)command;log("COMMAND BATTLE sec="+k+" alive="+gun.isValid()+" health="+gun.health+" ammo="+gun.totalAmmo+" coolant="+gun.liquids.get(newhorizon.content.NHLiquids.antiMatter)+" power="+gun.power.status+" reload="+gun.reloadCounter+" rotation="+gun.rotation+" target="+gun.target+" shots="+gun.totalShots);}
  for(var u:attackers)if(u.isAdded()&&u.dst(starts.get(u.id))>tilesize*2)movedIds.add(u.id);}
  int moved=movedIds.size;
  var fire=new java.util.TreeMap<String,Integer>();var alive=new java.util.TreeMap<String,Integer>();for(var b:guns){fire.merge(b.block.name,((Turret.TurretBuild)b).totalShots-oldShots.get(b,0),Integer::sum);if(b.isValid())alive.merge(b.block.name,1,Integer::sum);}
  log("FORTRESS FIRE shots="+fire+" guns surviving="+alive);for(var u:attackers)log("ASSAULT UNIT "+u.type+" alive="+u.isAdded()+" range="+u.type.maxRange/tilesize+" health="+u.health+" at="+u.x/tilesize+","+u.y/tilesize);
  int lost=0;for(var u:attackers){if(!u.isAdded()||u.dead)lost++;else totalDamage+=u.maxHealth-u.health;}
  int destroyed=buildings-Groups.build.count(b->b.team==Team.blue);
  check(moved>0,"assault units actually move toward enemy defenses");
  check(lost>0||totalDamage>1000,"enemy fortress engages armed moving NH attackers");
  check(Team.blue.cores().size>0,"late fortress withstands a concentrated advanced-unit assault");
  if(command!=null)check(((Turret.TurretBuild)command).totalShots>0,"command artillery actually fires during a mobile assault");
  log("PASS DYNAMIC ASSAULT: 180 seconds, ten NH hurricane + ten NH longinus, armed native unit AI; observed moving="+moved+" lost="+lost+" survivor damage="+totalDamage+" enemy structures lost="+destroyed+" cores remaining="+Team.blue.cores().size+". One raid fixture, not a full campaign balance verdict.");
 }
 static void openingBattle(int c){
  for(int wave:new int[]{1,30,60,90}){
   var p=AfterglowCampaign.sectors[c];p.sector.clearInfo();logic.reset();world.loadSector(p.sector,new WorldParams(){{saveInfo=false;}});logic.play();state.rules.waveTimer=false;state.rules.waves=true;
   ticks(120*60);Groups.unit.copy().each(u->u.remove());
   var before=new arc.struct.ObjectFloatMap<Building>();for(var t:world.tiles)if(t.isCenter()&&t.build!=null&&t.team()==Team.sharded)before.put(t.build,t.build.health);
   var core=Team.sharded.core();state.wave=wave;logic.runWave();ticks(240);
   int count=Groups.unit.count(u->u.team==Team.blue);var roster=new java.util.TreeMap<String,Integer>();for(var u:Groups.unit)if(u.team==Team.blue)roster.merge(u.type.name,1,Integer::sum);
   float damage=0;int lost=0;for(int sec=0;sec<300&&!state.gameOver;sec++){ticks(60);for(var e:before){float health=e.key.isValid()?e.key.health:0;damage+=Math.max(0,e.value-health);before.put(e.key,health);}}
   for(var e:before)if(!e.key.isValid())lost++;
   log("ACTUAL WAVE "+wave+": spawned="+count+" roster="+roster+"; 300s no construction, accumulated player structure damage="+damage+" structures lost="+lost+" core="+(core.isValid()?core.health:0)+" enemies remaining="+Groups.unit.count(u->u.team==Team.blue)+" gameOver="+state.gameOver+". Later waves intentionally tested against unchanged starting base, not a developed player economy.");
   if(wave==1)check(core.isValid()&&!state.gameOver,"opening permits development instead of unavoidable immediate defeat");
  }
 }
 static void yardProduction(int c){
  var p=AfterglowCampaign.sectors[c];p.sector.clearInfo();logic.reset();world.loadSector(p.sector,new WorldParams(){{saveInfo=false;}});logic.play();state.rules.waveTimer=false;state.rules.waves=true;
  var yard=(newhorizon.expand.block.special.JumpGate.JumpGateBuild)world.build(state.rules.tags.getInt("workshop.yard.0",-1));
  var depot=world.build(state.rules.tags.getInt("workshop.yard-stock.0",-1));
  check(yard!=null&&depot!=null&&!yard.usesCoreItems(),"enemy NH yard uses local inventory");
  int recipe=yard.isCalling()?yard.spawnID:yard.buildQueue.first()[0];var type=yard.getRecipe(recipe).unitType;
  check(type.name.startsWith("new-horizon-"),"native NH production recipe");
  int before=depot.items.total();var produced=new arc.struct.IntSet();
  for(int second=0;second<480;second++){ticks(60);Groups.unit.copy().each(u->{if(u.team==Team.blue&&u.type==type&&u.within(yard,((newhorizon.expand.block.special.JumpGate)yard.block).maxRadius+((newhorizon.expand.block.special.JumpGate)yard.block).spawnRange))produced.add(u.id);u.remove();});}
  log("YARD OBSERVED: "+type+" produced="+produced.size+" stock="+before+" -> "+depot.items.total()+" progress="+yard.buildProgress+" gate items="+yard.items+" queue="+yard.buildQueue.size+" power="+yard.power.status);
  check(produced.size>0,"enemy yard produces actual NH units");check(depot.items.total()<before,"yard physically consumes finite military reserves");
  yard.configureAny(arc.struct.IntSeq.with(1,0));yard.startBuild(-1,0);yard.items.clear();depot.items.clear();
  world.tile(depot.tileX(),depot.tileY()+2).remove();
  for(var b:Groups.build)if(b.team==Team.blue&&b instanceof mindustry.world.blocks.distribution.Conveyor.ConveyorBuild cb&&Math.abs(b.tileX()-yard.tileX())<2&&b.tileY()<yard.tileY()&&b.tileY()>depot.tileY())cb.items.clear();
  ticks(3600);yard.items.clear();yard.cooling=false;yard.configureAny(arc.struct.IntSeq.with(0,recipe,1,1));ticks(600);
  check(!yard.isCalling(),"empty disconnected yard cannot start another unit even on wave team");
  log("PASS ENEMY YARD: native "+yard.block+" recipe="+type+" produced="+produced.size+"; local warehouse -> unloader -> belts -> native recipe; empty supply blocks new production. Reserves are finite, not a complete renewable unit-material industry.");
 }
 static void physicalFeed(int index){
  var gun=(ItemTurret.ItemTurretBuild)world.build(state.rules.tags.getInt("frontier.test-gun."+index,-1));
  check(gun!=null,"authored physical ammo gun");gun.ammo.clear();gun.totalAmmo=0;ticks(1200);check(gun.totalAmmo>0,"conveyors deliver ammunition without script");
  int depot=state.rules.tags.getInt("frontier.depot."+index,-1);var d=world.build(depot);int stock=d.items.get(Items.titanium);
  gun.ammo.clear();gun.totalAmmo=0;ticks(1200);check(d.items.get(Items.titanium)<stock,"physical loading consumes warehouse inventory");
  world.tile(d.tileX()+2,d.tileY()).remove();
  for(int k=0;k<50;k++){gun.ammo.clear();gun.totalAmmo=0;ticks(60);}
  gun.ammo.clear();gun.totalAmmo=0;ticks(600);check(gun.totalAmmo==0,"cutting unloader exhausts in-transit ammunition and stops feed");
 }
}
