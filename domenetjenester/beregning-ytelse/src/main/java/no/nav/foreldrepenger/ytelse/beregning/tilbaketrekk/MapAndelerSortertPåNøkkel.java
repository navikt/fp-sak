package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetOgArbeidsgiverNøkkel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;

public class MapAndelerSortertPåNøkkel {
    public static List<BRNøkkelMedAndeler> map(List<BeregningsresultatAndel> resultatandeler) {
        Map<AktivitetOgArbeidsgiverNøkkel, List<BeregningsresultatAndel>> nøkkelMap = lagMapSorertPåNøkkel(resultatandeler);
        return lagListeMedSammenligningsandeler(nøkkelMap);
    }

    private static List<BRNøkkelMedAndeler> lagListeMedSammenligningsandeler(Map<AktivitetOgArbeidsgiverNøkkel, List<BeregningsresultatAndel>> nøkkelMap) {
        List<BRNøkkelMedAndeler> listeSortertPåNøkkel = new ArrayList<>();
        nøkkelMap.forEach((key, value) -> {
            BRNøkkelMedAndeler sammenligningAndel = new BRNøkkelMedAndeler(key);
            value.forEach(sammenligningAndel::leggTilAndel);
            listeSortertPåNøkkel.add(sammenligningAndel);
        });
        return listeSortertPåNøkkel;
    }

    private static Map<AktivitetOgArbeidsgiverNøkkel, List<BeregningsresultatAndel>> lagMapSorertPåNøkkel(List<BeregningsresultatAndel> resultatandeler) {
        Map<AktivitetOgArbeidsgiverNøkkel, List<BeregningsresultatAndel>> nøkkelMap = new HashMap<>();
        resultatandeler.forEach(andel -> {
            AktivitetOgArbeidsgiverNøkkel nøkkel = andel.getAktivitetOgArbeidsgiverNøkkel();
            List<BeregningsresultatAndel> andelsliste = nøkkelMap.getOrDefault(nøkkel, new ArrayList<>());
            andelsliste.add(andel);
            nøkkelMap.put(nøkkel, andelsliste);
        });
        return nøkkelMap;
    }
}
