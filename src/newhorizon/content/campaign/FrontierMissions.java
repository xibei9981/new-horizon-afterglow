package newhorizon.content.campaign;

/** Authored coordinates and pacing shared by the map generator, runtime and checks. */
public final class FrontierMissions {
    public static final class Mission {
        public final String id, name, brief;
        public final int width, height, x, y, waves, spacing, grace, difficulty;
        public final int[][] cores, spawns;
        public Mission(String id, String name, String brief, int w, int h, int x, int y,
                       int waves, int spacing, int grace, int difficulty, int[][] cores, int[][] spawns) {
            this.id=id; this.name=name; this.brief=brief; width=w; height=h; this.x=x; this.y=y;
            this.waves=waves; this.spacing=spacing; this.grace=grace; this.difficulty=difficulty;
            this.cores=cores; this.spawns=spawns;
        }
        public boolean attack(){return waves==0;}
    }
    public static final Mission[] all = {
        new Mission("forge","熔炉盆地","先经营，后爆发。前 20 波轻装试探，第 21 波获得工业物资与标准跃迁门科技，第 41 波装甲增援。守住 60 波。优先把宽阔后方变成兵工厂。",420,420,210,70,60,50,480,6,
            new int[][]{},new int[][]{{210,376}}),
        new Mission("isles","断桥群岛","夺取三座岛屿核心。曲折桥梁可通地面部队，空军可跨海绕后。摧毁每岛标记的补给枢纽，会停止该岛定期空军增援，并切断周围炮塔补弹。",448,384,96,70,0,85,300,7,
            new int[][]{{100,265},{334,166},{330,315}},new int[][]{{100,328},{386,230},{384,348}}),
        new Mission("trident","三叉前线","72 波，三条战线。前 24 波仅中路；第 25 波东线开启；第 49 波三线齐进。提前建设分支防区，第 25、49 波有物资与友军增援。",448,448,224,72,72,48,360,7,
            new int[][]{},new int[][]{{224,401},{387,300},{61,300}}),
        new Mission("hunt","猎网突袭","两座核心、两处补给枢纽。第 20 波前摧毁全部枢纽，可获得一支突击队；错过窗口则每 6 波有重装追猎队入场，直到枢纽被摧毁。主动出击比死守更有利。",416,416,208,66,0,55,180,8,
            new int[][]{{92,303},{324,303}},new int[][]{{54,368},{362,368}}),
        new Mission("storm","风暴穹顶","80 波，以空袭为主。第 21–30、51–60 波太阳能降至 25%，提前准备燃料发电与蓄电池。第 31、61 波电力恢复并获得空军增援。矿区也需要防空。",416,416,208,208,80,45,420,8,
            new int[][]{},new int[][]{{56,353},{360,353},{208,52}}),
        new Mission("march","逆流远征","沿曲折河谷逐段北上，摧毁四座核心。前三处据点失守后，会建成我方前进核心并发放补给。新核心附近可部署工厂与防御，缩短前线补给距离。",448,576,112,68,0,90,300,8,
            new int[][]{{310,182},{116,303},{316,409},{160,508}},new int[][]{{372,227},{62,350},{375,458},{100,545}}),
        new Mission("bastion","孤城双塔","保卫东西两座指定核心，缺一即失败。90 波，两路交替施压，第 61 波开始同时来袭。中央通道可以调兵，两个核心共享物资；不能只守一边。",480,448,112,76,90,48,420,9,
            new int[][]{},new int[][]{{112,397},{368,397}}),
        new Mission("arsenal","破晓兵工厂","开局即有装甲纵队、标准跃迁门和充足高级物资。攻破五座纵深据点，每摧毁一座核心都会获得续战补给。保持推进，别让后方波次积压。",512,512,256,70,0,80,180,8,
            new int[][]{{108,222},{402,222},{130,369},{382,369},{256,449}},new int[][]{{54,279},{457,279},{75,435},{439,435},{256,482}}),
        new Mission("blackout","静默物流","敌军三处补给枢纽分别维持三路波次与炮塔供弹。优先突袭枢纽：摧毁一处就永久关闭对应出兵点。摧毁三座核心结束战斗，正面强攻和切断后勤都可行。",480,448,240,68,0,58,300,9,
            new int[][]{{82,282},{240,355},{398,282}},new int[][]{{58,357},{240,405},{424,357}}),
        new Mission("daybreak","长夜终焉","先守 100 波，再反攻三座核心。第 31、61 波工业空投，81 波装甲友军抵达。百波结束并清场后，我方重装纵队投入反攻，敌方停止出波。也可提前削弱其据点。",512,544,256,72,100,45,480,10,
            new int[][]{{94,388},{418,388},{256,478}},new int[][]{{72,304},{256,315},{440,304}})
    };
}
