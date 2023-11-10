package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetOgArbeidsgiverNøkkel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;

class MapAndelerSortertPåNøkkel {

    private MapAndelerSortertPåNøkkel() {
    }

    static List<BRNøkkelMedAndeler> map(List<BeregningsresultatAndel> resultatandeler) {
        var nøkkelMap = lagMapSorertPåNøkkel(resultatandeler);
        return lagListeMedSammenligningsandeler(nøkkelMap);
    }

    private static List<BRNøkkelMedAndeler> lagListeMedSammenligningsandeler(Map<AktivitetOgArbeidsgiverNøkkel, List<BeregningsresultatAndel>> nøkkelMap) {
        List<BRNøkkelMedAndeler> listeSortertPåNøkkel = new ArrayList<>();
        nøkkelMap.forEach((key, value) -> {
            var sammenligningAndel = new BRNøkkelMedAndeler(key);
            value.forEach(sammenligningAndel::leggTilAndel);
            listeSortertPåNøkkel.add(sammenligningAndel);
        });
        return listeSortertPåNøkkel;
    }

    private static Map<AktivitetOgArbeidsgiverNøkkel, List<BeregningsresultatAndel>> lagMapSorertPåNøkkel(List<BeregningsresultatAndel> resultatandeler) {
        Map<AktivitetOgArbeidsgiverNøkkel, List<BeregningsresultatAndel>> nøkkelMap = new HashMap<>();
        resultatandeler.forEach(andel -> {
            var nøkkel = andel.getAktivitetOgArbeidsgiverNøkkel();
            var andelsliste = nøkkelMap.getOrDefault(nøkkel, new ArrayList<>());
            andelsliste.add(andel);
            nøkkelMap.put(nøkkel, andelsliste);
        });
        return nøkkelMap;
    }
}
