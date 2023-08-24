package no.nav.foreldrepenger.behandlingslager.geografisk;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.konfig.Tid;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class MapRegionLandkoder {

    private static final LocalDate BREXIT_DATO = LocalDate.of(2021,1,1);

    private static final Map<Landkoder, Region> LANDKODER_REGION_MAP = Map.ofEntries(
        Map.entry(Landkoder.NOR, Region.NORDEN),
        Map.entry(Landkoder.SWE, Region.NORDEN),
        Map.entry(Landkoder.DNK, Region.NORDEN),
        Map.entry(Landkoder.FIN, Region.NORDEN),
        Map.entry(Landkoder.ISL, Region.NORDEN),
        Map.entry(Landkoder.ALA, Region.NORDEN),
        Map.entry(Landkoder.FRO, Region.NORDEN),
        Map.entry(Landkoder.GRL, Region.NORDEN),
        Map.entry(Landkoder.AUT, Region.EOS),
        Map.entry(Landkoder.BEL, Region.EOS),
        Map.entry(Landkoder.BGR, Region.EOS),
        Map.entry(Landkoder.CYP, Region.EOS),
        Map.entry(Landkoder.CZE, Region.EOS),
        Map.entry(Landkoder.DEU, Region.EOS),
        Map.entry(Landkoder.ESP, Region.EOS),
        Map.entry(Landkoder.EST, Region.EOS),
        Map.entry(Landkoder.FRA, Region.EOS),
        Map.entry(Landkoder.GRC, Region.EOS),
        Map.entry(Landkoder.HRV, Region.EOS),
        Map.entry(Landkoder.HUN, Region.EOS),
        Map.entry(Landkoder.IRL, Region.EOS),
        Map.entry(Landkoder.ITA, Region.EOS),
        Map.entry(Landkoder.LIE, Region.EOS),
        Map.entry(Landkoder.LTU, Region.EOS),
        Map.entry(Landkoder.LUX, Region.EOS),
        Map.entry(Landkoder.LVA, Region.EOS),
        Map.entry(Landkoder.MLT, Region.EOS),
        Map.entry(Landkoder.NLD, Region.EOS),
        Map.entry(Landkoder.POL, Region.EOS),
        Map.entry(Landkoder.PRT, Region.EOS),
        Map.entry(Landkoder.ROU, Region.EOS),
        Map.entry(Landkoder.SVK, Region.EOS),
        Map.entry(Landkoder.SVN, Region.EOS)
    );

    private static final Map<Landkoder, List<RegionsTilhørighet>> LANDKODER_OVERGANG_MAP = Map.ofEntries(
        Map.entry(Landkoder.GBR, List.of(
            new RegionsTilhørighet(Region.EOS,
                new LocalDateInterval(Tid.TIDENES_BEGYNNELSE, BREXIT_DATO.minusDays(1)),
                new LocalDateInterval(Tid.TIDENES_BEGYNNELSE, BREXIT_DATO.plusYears(1).minusDays(1)))))
    );

    private static final Map<Landkoder, Set<LocalDate>> LANDKODER_ENDRING_MAP = Map.ofEntries(
        Map.entry(Landkoder.GBR, Set.of(BREXIT_DATO, BREXIT_DATO.plusYears(1)))
    );

    public static Region mapLandkode(Landkoder landkode) {
        if (landkode == null)
            return Region.UDEFINERT;
        return LANDKODER_REGION_MAP.getOrDefault(landkode, Region.TREDJELANDS_BORGER);
    }

    public static Region mapLandkodeForDatoMedSkjæringsdato(Landkoder landkode, LocalDate mapForDato, LocalDate skjæringstidspunkt) {
        if (landkode == null)
            return Region.UDEFINERT;
        if (LANDKODER_OVERGANG_MAP.getOrDefault(landkode, List.of()).isEmpty()) {
            return LANDKODER_REGION_MAP.getOrDefault(landkode, Region.TREDJELANDS_BORGER);
        }
        return LANDKODER_OVERGANG_MAP.get(landkode).stream()
            .filter(t -> t.intervall.encloses(skjæringstidspunkt) && t.utvidetIntervall.encloses(mapForDato))
            .map(RegionsTilhørighet::region)
            .findFirst().orElse(Region.TREDJELANDS_BORGER);
    }

    public static Landkoder finnRangertLandkode(List<Landkoder> landkoder) {
        if (landkoder == null || landkoder.isEmpty())
            return Landkoder.UOPPGITT;
        var sortering = landkoder.stream()
            .collect(Collectors.groupingBy(l -> LANDKODER_REGION_MAP.getOrDefault(l, Region.TREDJELANDS_BORGER)));
        return sortering.entrySet().stream()
            .filter(e -> !e.getValue().isEmpty())
            .min(Comparator.comparing(e -> e.getKey().getRank()))
            .map(Map.Entry::getValue).orElse(Collections.emptyList()).stream()
            .findFirst().orElse(Landkoder.UOPPGITT);
    }

    public static Set<LocalDate> utledRegionsEndringsDatoer(List<Landkoder> landkoder) {
        if (landkoder == null)
            return Set.of();
        return landkoder.stream()
            .flatMap(l -> LANDKODER_ENDRING_MAP.getOrDefault(l, Set.of()).stream())
            .collect(Collectors.toSet());
    }

    private record RegionsTilhørighet(Region region, LocalDateInterval intervall, LocalDateInterval utvidetIntervall) { }

}
