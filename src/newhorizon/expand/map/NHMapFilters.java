package newhorizon.expand.map;

import arc.func.Prov;
import arc.struct.Seq;
import arc.util.Strings;
import mindustry.io.JsonIO;
import mindustry.maps.Maps;
import newhorizon.expand.map.filters.RuinGenerateFilter;

public class NHMapFilters{
    public static void load(){
        Maps.allFilterTypes = Seq.with(Maps.allFilterTypes).add(RuinGenerateFilter::new).toArray(Prov.class);
        JsonIO.classTag(Strings.camelize("RuinGenerate"), RuinGenerateFilter.class);
    }
}
