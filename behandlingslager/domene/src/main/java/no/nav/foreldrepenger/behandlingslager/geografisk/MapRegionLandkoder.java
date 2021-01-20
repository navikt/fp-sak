package no.nav.foreldrepenger.behandlingslager.geografisk;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class MapRegionLandkoder {

    private static final Map<String, Region> LANDKODER_REGION_MAP = Map.ofEntries(
        Map.entry(Landkoder.NOR.getKode(), Region.NORDEN),
        Map.entry(Landkoder.SWE.getKode(), Region.NORDEN),
        Map.entry(Landkoder.DNK.getKode(), Region.NORDEN),
        Map.entry(Landkoder.FIN.getKode(), Region.NORDEN),
        Map.entry(Landkoder.ISL.getKode(), Region.NORDEN),
        Map.entry(Landkoder.ALA.getKode(), Region.NORDEN),
        Map.entry(Landkoder.FRO.getKode(), Region.NORDEN),
        Map.entry(Landkoder.GRL.getKode(), Region.NORDEN),
        Map.entry(Landkoder.AUT.getKode(), Region.EOS),
        Map.entry(Landkoder.BEL.getKode(), Region.EOS),
        Map.entry(Landkoder.BGR.getKode(), Region.EOS),
        Map.entry(Landkoder.CYP.getKode(), Region.EOS),
        Map.entry(Landkoder.CZE.getKode(), Region.EOS),
        Map.entry(Landkoder.DEU.getKode(), Region.EOS),
        Map.entry(Landkoder.ESP.getKode(), Region.EOS),
        Map.entry(Landkoder.EST.getKode(), Region.EOS),
        Map.entry(Landkoder.FRA.getKode(), Region.EOS),
        Map.entry(Landkoder.GBR.getKode(), Region.EOS),  // TODO: BREXIT. Overgangsperiode i 2021. Avklar Sveits
        Map.entry(Landkoder.GRC.getKode(), Region.EOS),
        Map.entry(Landkoder.HRV.getKode(), Region.EOS),
        Map.entry(Landkoder.HUN.getKode(), Region.EOS),
        Map.entry(Landkoder.IRL.getKode(), Region.EOS),
        Map.entry(Landkoder.ITA.getKode(), Region.EOS),
        Map.entry(Landkoder.LIE.getKode(), Region.EOS),
        Map.entry(Landkoder.LTU.getKode(), Region.EOS),
        Map.entry(Landkoder.LUX.getKode(), Region.EOS),
        Map.entry(Landkoder.LVA.getKode(), Region.EOS),
        Map.entry(Landkoder.MLT.getKode(), Region.EOS),
        Map.entry(Landkoder.NLD.getKode(), Region.EOS),
        Map.entry(Landkoder.POL.getKode(), Region.EOS),
        Map.entry(Landkoder.PRT.getKode(), Region.EOS),
        Map.entry(Landkoder.ROU.getKode(), Region.EOS),
        Map.entry(Landkoder.SVK.getKode(), Region.EOS),
        Map.entry(Landkoder.SVN.getKode(), Region.EOS)
    );

    public static Region mapLandkode(String landkode) {
        if (landkode == null)
            return Region.UDEFINERT;
        return LANDKODER_REGION_MAP.getOrDefault(landkode, Region.TREDJELANDS_BORGER);
    }

    public static Region mapRangerLandkoder(List<Landkoder> landkoder) {
        if (landkoder == null || landkoder.isEmpty())
            return Region.UDEFINERT;
        return landkoder.stream().map(Landkoder::getKode).map(LANDKODER_REGION_MAP::get).filter(Objects::nonNull).min(Comparator.comparing(Region::getRank)).orElse(Region.TREDJELANDS_BORGER);
    }

    public static Landkoder finnRangertLandkode(List<Landkoder> landkoder) {
        if (landkoder == null || landkoder.isEmpty())
            return Landkoder.UOPPGITT;
        var sortering = landkoder.stream()
            .collect(Collectors.groupingBy(l -> LANDKODER_REGION_MAP.getOrDefault(l.getKode(), Region.TREDJELANDS_BORGER)));
        return sortering.entrySet().stream()
            .filter(e -> !e.getValue().isEmpty())
            .min(Comparator.comparing(e -> e.getKey().getRank()))
            .map(Map.Entry::getValue).orElse(Collections.emptyList()).stream()
            .findFirst().orElse(Landkoder.UOPPGITT);
    }

}
