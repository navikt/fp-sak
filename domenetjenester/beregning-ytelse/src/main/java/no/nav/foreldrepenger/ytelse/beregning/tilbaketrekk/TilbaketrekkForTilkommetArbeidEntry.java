package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetOgArbeidsgiverNøkkel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;

class TilbaketrekkForTilkommetArbeidEntry {

    private List<BRNøkkelMedAndeler> tilkomneNøklerMedStartEtterDato;

    private List<BRNøkkelMedAndeler> andelerIRevurderingMedSluttFørDato;

    private List<BRNøkkelMedAndeler> andelerIOriginalMedSluttFørDato;
    private Map<LocalDate, List<BRNøkkelMedAndeler>> andelerIRevurderingSluttdatoTilNøkkelMap;
    private Map<LocalDate, List<BRNøkkelMedAndeler>> andelerIOriginalSluttdatoTilNøkkelMap;

    public void setTilkomneNøklerMedStartEtterDato(List<BRNøkkelMedAndeler> tilkomneNøklerMedStartEtterDato) {
        this.tilkomneNøklerMedStartEtterDato = tilkomneNøklerMedStartEtterDato;
    }

    public void setAndelerIRevurderingMedSluttFørDato(Map<LocalDate, List<BRNøkkelMedAndeler>> andelerIRevurderingMedSluttFørDato) {
        this.andelerIRevurderingSluttdatoTilNøkkelMap = andelerIRevurderingMedSluttFørDato;
        this.andelerIRevurderingMedSluttFørDato = andelerIRevurderingMedSluttFørDato.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    public void setAndelerIOriginalMedSluttFørDato(Map<LocalDate, List<BRNøkkelMedAndeler>> andelerIOriginalMedSluttFørDato) {
        this.andelerIOriginalSluttdatoTilNøkkelMap = andelerIOriginalMedSluttFørDato;
        this.andelerIOriginalMedSluttFørDato = andelerIOriginalMedSluttFørDato.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    public List<BRNøkkelMedAndeler> getTilkomneNøklerMedStartEtterDato() {
        return tilkomneNøklerMedStartEtterDato;
    }

    public List<BRNøkkelMedAndeler> getAndelerIRevurderingMedSluttFørDatoSortertPåDato() {

        List<BRNøkkelMedAndeler> sorterteAndeler = andelerIRevurderingSluttdatoTilNøkkelMap.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .flatMap(e -> e.getValue().stream())
            .collect(Collectors.toList());
        Collections.reverse(sorterteAndeler);
        return sorterteAndeler;
    }

    public Map<LocalDate, List<BRNøkkelMedAndeler>> getAndelerIRevurderingSluttdatoTilNøkkelMap() {
        return andelerIRevurderingSluttdatoTilNøkkelMap;
    }

    public Map<LocalDate, List<BRNøkkelMedAndeler>> getAndelerIOriginalSluttdatoTilNøkkelMap() {
        return andelerIOriginalSluttdatoTilNøkkelMap;
    }

    int finnTotalbeløpTilTilbaketrekkForAndelerMedAvsluttetArbeid() {
        Integer totalBrukersAndelOriginalt = finnTotalBrukersAndel(andelerIOriginalMedSluttFørDato);
        Integer totalBrukersAndelRevurdering = finnTotalBrukersAndel(andelerIRevurderingMedSluttFørDato);
        return totalBrukersAndelOriginalt - totalBrukersAndelRevurdering;
    }

    private Integer finnTotalBrukersAndel(List<BRNøkkelMedAndeler> nøkler) {
        return nøkler.stream().flatMap(a -> a.getBrukersAndelerTilknyttetNøkkel().stream())
            .map(BeregningsresultatAndel::getDagsats).reduce(Integer::sum).orElse(0);
    }

    int finnTotalRefusjonForTilkomneAndeler() {
        return tilkomneNøklerMedStartEtterDato.stream().flatMap(a -> a.getArbeidsgiversAndelerTilknyttetNøkkel().stream())
            .map(BeregningsresultatAndel::getDagsats).reduce(Integer::sum).orElse(0);
    }

    int finnHindretTilbaketrekk() {
        int totalrefusjon = finnTotalRefusjonForTilkomneAndeler();
        int totaltRedusertBeløp = finnTotalbeløpTilTilbaketrekkForAndelerMedAvsluttetArbeid();
        return Math.min(totalrefusjon, totaltRedusertBeløp);
    }

    BRNøkkelMedAndeler finnOriginalForNøkkel(AktivitetOgArbeidsgiverNøkkel nøkkel) {
        return andelerIOriginalMedSluttFørDato.stream().filter(n -> n.getNøkkel().equals(nøkkel)).findFirst().orElseThrow(() -> new IllegalArgumentException("Forventet å finne matchende nøkkel."));
    }

}
