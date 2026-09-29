package newhorizon.content.campaign;

/** Authored optional objectives, shared by map generation and campaign checks. */
public final class FrontierSites {
    // Different cycles align relief windows with each mission's major phase changes.
    public static final int[] cycles={15,0,12,0,16,0,18,0,0,20};
    private static final int[] fast={7,0,4,0,5,0,6,0,0,7},siege={12,0,9,0,12,0,14,0,0,16};
    public static int waveRole(int mission,int wave){
        int cycle=cycles[mission];
        if(cycle==0||wave>=FrontierMissions.all[mission].waves)return 0;
        int at=wave%cycle;return at==0?3:at==fast[mission]?1:at==siege[mission]?2:0;
    }
    public static final int[][] positions={{128,158},{185,165},{224,220},{208,224},{208,332},{228,230},{240,245},{256,300},{240,220},{256,245}};
    public static final int[] kinds={0,1,3,1,2,0,2,0,2,3};
    public static final String[] names={"物资库","航空信标","变电站","防空哨所"};
    public static final String[] rewards={"修复后获得一批工业补给。","修复后获得 8 架空军增援。","修复后接管附近太阳能板、蓄电池和电网节点，可接入自己的电网。","修复后接管两座有弹药的防空炮，并获得 6 辆地面援军。"};
}
