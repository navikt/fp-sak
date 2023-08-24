package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetOgArbeidsgiverNøkkel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        this.andelerIRevurderingMedSluttFørDato = andelerIRevurderingMedSluttFørDato.values().stream().flatMap(Collection::stream).toList();
    }

    public void setAndelerIOriginalMedSluttFørDato(Map<LocalDate, List<BRNøkkelMedAndeler>> andelerIOriginalMedSluttFørDato) {
        this.andelerIOriginalSluttdatoTilNøkkelMap = andelerIOriginalMedSluttFørDato;
        this.andelerIOriginalMedSluttFørDato = andelerIOriginalMedSluttFørDato.values().stream().flatMap(Collection::stream).toList();
    }

    public List<BRNøkkelMedAndeler> getTilkomneNøklerMedStartEtterDato() {
        return tilkomneNøklerMedStartEtterDato;
    }

    public List<BRNøkkelMedAndeler> getAndelerIRevurderingMedSluttFørDatoSortertPåDato() {

        var sorterteAndeler = andelerIRevurderingSluttdatoTilNøkkelMap.entrySet().stream()
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
        var totalBrukersAndelOriginalt = finnTotalBrukersAndel(andelerIOriginalMedSluttFørDato);
        var totalBrukersAndelRevurdering = finnTotalBrukersAndel(andelerIRevurderingMedSluttFørDato);
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
        var totalrefusjon = finnTotalRefusjonForTilkomneAndeler();
        var totaltRedusertBeløp = finnTotalbeløpTilTilbaketrekkForAndelerMedAvsluttetArbeid();
        return Math.min(totalrefusjon, totaltRedusertBeløp);
    }

    BRNøkkelMedAndeler finnOriginalForNøkkel(AktivitetOgArbeidsgiverNøkkel nøkkel) {
        return andelerIOriginalMedSluttFørDato.stream().filter(n -> n.getNøkkel().equals(nøkkel)).findFirst().orElseThrow(() -> new IllegalArgumentException("Forventet å finne matchende nøkkel."));
    }

}
