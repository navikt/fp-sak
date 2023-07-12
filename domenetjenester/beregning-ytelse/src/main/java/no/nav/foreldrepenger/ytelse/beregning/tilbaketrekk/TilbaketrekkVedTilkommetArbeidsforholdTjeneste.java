package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.konfig.Tid;

class TilbaketrekkVedTilkommetArbeidsforholdTjeneste {

    private TilbaketrekkVedTilkommetArbeidsforholdTjeneste() {
    }

    /**
     * Finner største tilbaketrekk i form av et entry for informasjon om tilbaketrekk for eit sett med tilkomne arbeidsforhold. Ein TilbaketrekkForTilkommetArbeidEntry inneheld informasjon om kva nøkler fra revurderingen som er tilkommet,
     * og hvilke nøkler fra revurderingen og originalbehandlingen som tilsvarer arbeidsforhold som er avsluttet og det kan gjøres tilbaketrekk fra (eventuelt hindre tilbaketrekk).
     *
     * Hver entry korresponderer til en startdato for et tilkommet arbeidsforhold.
     *
     * Hver entry inneholder ett sett med tilkomne andeler. Dette settet lages ved å gå igjennom hver startdato for arbeidsforhold som tilkommer etter skjæringstidspunktet.
     * For en gitt startdato vil settet av tilkomne andeler bestå av andeler som tilkommer ved eller etter denne datoen.
     *
     * En entry inneholder også informasjon om avsluttede arbeidsforhold. Dette settet består av arbeidsforhold som avsluttet før datoen nevnt over.
     *
     * @param revurderingAndelerSortertPåNøkkel Andeler fra revurdering sortert på nøkkel
     * @param originaleAndelerSortertPåNøkkel Andeler fra original behandling sortert på nøkkel
     * @param yrkesaktiviteter Yrkesaktiviteter som er gjeldende
     * @param skjæringstidspunkt Skjæringstidspunkt for beregning.
     * @return Det største mulige tilbaketrekket for tilkomne arbeidsforhold som er større enn 0
     */
    static Optional<TilbaketrekkForTilkommetArbeidEntry> finnStørsteTilbaketrekkForTilkomneArbeidsforhold(List<BRNøkkelMedAndeler> revurderingAndelerSortertPåNøkkel,
                                                                                                              List<BRNøkkelMedAndeler> originaleAndelerSortertPåNøkkel,
                                                                                                              Collection<Yrkesaktivitet> yrkesaktiviteter,
                                                                                                              LocalDate skjæringstidspunkt) {
        return finnTilbaketrekkForTilkomneArbeidsforholdOptimeringEntry(revurderingAndelerSortertPåNøkkel, originaleAndelerSortertPåNøkkel, yrkesaktiviteter, skjæringstidspunkt)
            .stream()
            .filter(entry -> entry.finnHindretTilbaketrekk() > 0)
            .max(Comparator.comparing(TilbaketrekkForTilkommetArbeidEntry::finnHindretTilbaketrekk));
    }

    private static List<TilbaketrekkForTilkommetArbeidEntry> finnTilbaketrekkForTilkomneArbeidsforholdOptimeringEntry(List<BRNøkkelMedAndeler> revurderingAndelerSortertPåNøkkel,
                                                                                                                      List<BRNøkkelMedAndeler> originaleAndelerSortertPåNøkkel,
                                                                                                                      Collection<Yrkesaktivitet> yrkesaktiviteter,
                                                                                                                      LocalDate skjæringstidspunkt) {

        var tilkomneAndeler = revurderingAndelerSortertPåNøkkel.stream()
            .filter(brNøkkelMedAndeler -> originaleAndelerSortertPåNøkkel.stream().map(BRNøkkelMedAndeler::getNøkkel).noneMatch(originalNøkkel -> originalNøkkel.equals(brNøkkelMedAndeler.getNøkkel())))
            .filter(n -> finnTidligsteStartdatoForArbeidsforholdPåNøkkel(n, yrkesaktiviteter, skjæringstidspunkt).isPresent())
            .toList();
        return tilkomneAndeler
            .stream()
            .map(n -> finnTidligsteStartdatoForArbeidsforholdPåNøkkel(n, yrkesaktiviteter, skjæringstidspunkt).get())
            .distinct()
            .map(lagTilbaketrekkEntry(revurderingAndelerSortertPåNøkkel, originaleAndelerSortertPåNøkkel, yrkesaktiviteter, skjæringstidspunkt, tilkomneAndeler))
            .toList();
    }

    private static Function<LocalDate, TilbaketrekkForTilkommetArbeidEntry> lagTilbaketrekkEntry(List<BRNøkkelMedAndeler> revurderingAndelerSortertPåNøkkel, List<BRNøkkelMedAndeler> originaleAndelerSortertPåNøkkel, Collection<Yrkesaktivitet> yrkesaktiviteter, LocalDate skjæringstidspunkt, List<BRNøkkelMedAndeler> tilkomneAndeler) {
        return d -> {
            var entry = new TilbaketrekkForTilkommetArbeidEntry();
            entry.setTilkomneNøklerMedStartEtterDato(finnNøklerMedStartLikEllerEtter(yrkesaktiviteter, skjæringstidspunkt, tilkomneAndeler).apply(d));
            entry.setAndelerIRevurderingMedSluttFørDato(finnBrukersAndelerSomAvslutterFørStartdatoForTilkommet(d, skjæringstidspunkt, revurderingAndelerSortertPåNøkkel, originaleAndelerSortertPåNøkkel, yrkesaktiviteter));
            entry.setAndelerIOriginalMedSluttFørDato(finnBrukersAndelerSomAvslutterFørStartdatoForTilkommet(d, skjæringstidspunkt, originaleAndelerSortertPåNøkkel, originaleAndelerSortertPåNøkkel, yrkesaktiviteter));
            return entry;
        };
    }

    private static Function<LocalDate, List<BRNøkkelMedAndeler>> finnNøklerMedStartLikEllerEtter(Collection<Yrkesaktivitet> yrkesaktiviteter, LocalDate skjæringstidspunkt, List<BRNøkkelMedAndeler> tilkomneAndelerStream) {
        return d -> tilkomneAndelerStream.stream().filter(n -> !finnTidligsteStartdatoForArbeidsforholdPåNøkkel(n, yrkesaktiviteter, skjæringstidspunkt).get().isBefore(d))
            .toList();
    }

    private static Map<LocalDate, List<BRNøkkelMedAndeler>> finnBrukersAndelerSomAvslutterFørStartdatoForTilkommet(LocalDate startdatoArbeid, LocalDate skjæringstidspunkt,
                                                                                                                   List<BRNøkkelMedAndeler> nøkler,
                                                                                                                   List<BRNøkkelMedAndeler> originaleAndelerSortertPåNøkkel, Collection<Yrkesaktivitet> yrkesaktiviteter) {
        var nøklerSomSlutterFørDatoIkkeErTilkommet = nøkler.stream()
            .filter(harNøkkelAvsluttetArbeidsforhold(startdatoArbeid, skjæringstidspunkt, yrkesaktiviteter))
            .filter(erIkkeTilkommetIRevurdering(originaleAndelerSortertPåNøkkel))
            .toList();
        return nøklerSomSlutterFørDatoIkkeErTilkommet.stream()
            .collect(Collectors.toMap(n -> finnSluttdatoFørTilkommetForNøkkel(n, startdatoArbeid, yrkesaktiviteter, skjæringstidspunkt), List::of, (l1, l2) -> {
                var newList = new ArrayList<BRNøkkelMedAndeler>();
                newList.addAll(l1);
                newList.addAll(l2);
                return newList;
            }));
    }

    private static Predicate<BRNøkkelMedAndeler> erIkkeTilkommetIRevurdering(List<BRNøkkelMedAndeler> originaleAndelerSortertPåNøkkel) {
        return n -> originaleAndelerSortertPåNøkkel.stream().anyMatch(originalNøkkel -> originalNøkkel.getNøkkel().equals(n.getNøkkel()));
    }

    private static LocalDate finnSluttdatoFørTilkommetForNøkkel(BRNøkkelMedAndeler n, LocalDate startdatoArbeid, Collection<Yrkesaktivitet> yrkesaktiviteter, LocalDate skjæringstidspunkt) {
        return n.getBrukersAndelerTilknyttetNøkkel()
            .stream()
            .filter(beregningsresultatAndel -> beregningsresultatAndel.getArbeidsgiver().isPresent())
            .flatMap(beregningsresultatAndel -> yrkesaktiviteter.stream()
                .filter(matchMedAndel(beregningsresultatAndel.getArbeidsgiver().get(), beregningsresultatAndel.getArbeidsforholdRef()))
                .flatMap(y -> y.getAlleAktivitetsAvtaler().stream()).filter(AktivitetsAvtale::erAnsettelsesPeriode))
            .filter(periodeLiggerMellomDatoer(skjæringstidspunkt, startdatoArbeid))
            .map(AktivitetsAvtale::getPeriode)
            .map(DatoIntervallEntitet::getTomDato)
            .max(Comparator.naturalOrder())
            .orElse(skjæringstidspunkt);
    }

    private static Predicate<BRNøkkelMedAndeler> harNøkkelAvsluttetArbeidsforhold(LocalDate startdatoArbeid, LocalDate skjæringstidspunkt, Collection<Yrkesaktivitet> yrkesaktiviteter) {
        return nøkkel -> nøkkel.getBrukersAndelerTilknyttetNøkkel().size() > 0 &&
            nøkkel.getBrukersAndelerTilknyttetNøkkel()
                .stream()
                .filter(beregningsresultatAndel -> beregningsresultatAndel.getArbeidsgiver().isPresent())
                .allMatch(korrespondererTilAvsluttetArbeidsforhold(startdatoArbeid, skjæringstidspunkt, yrkesaktiviteter));
    }

    private static Predicate<BeregningsresultatAndel> korrespondererTilAvsluttetArbeidsforhold(LocalDate startdatoArbeid, LocalDate skjæringstidspunkt, Collection<Yrkesaktivitet> yrkesaktiviteter) {
        return beregningsresultatAndel ->
        {
            var matchendeAktiviteter = yrkesaktiviteter.stream()
                .filter(matchMedAndel(beregningsresultatAndel.getArbeidsgiver().get(), beregningsresultatAndel.getArbeidsforholdRef()))
                .toList();
            var harPeriodeSomSlutterFørStartdato = matchendeAktiviteter.stream()
                .anyMatch(harAnsettelsesperiodeSomSlutterMellomDatoer(skjæringstidspunkt, startdatoArbeid));
            var harIkkePeriodeSomSlutterEtterStartdato = matchendeAktiviteter.stream()
                .noneMatch(harAnsettelsesperiodeSomSlutterMellomDatoer(startdatoArbeid, Tid.TIDENES_ENDE));
            return harPeriodeSomSlutterFørStartdato && harIkkePeriodeSomSlutterEtterStartdato;
        };
    }

    private static Predicate<Yrkesaktivitet> harAnsettelsesperiodeSomSlutterMellomDatoer(LocalDate dato1, LocalDate dato2) {
        return ya -> ya.getAlleAktivitetsAvtaler().stream().filter(AktivitetsAvtale::erAnsettelsesPeriode).anyMatch(periodeLiggerMellomDatoer(dato1, dato2));
    }

    private static Predicate<AktivitetsAvtale> periodeLiggerMellomDatoer(LocalDate skjæringstidspunkt, LocalDate startdatoArbeid) {
        return aa -> aa.getPeriode().getTomDato().isAfter(skjæringstidspunkt) && aa.getPeriode().getTomDato().isBefore(startdatoArbeid);
    }

    private static Optional<LocalDate> finnTidligsteStartdatoForArbeidsforholdPåNøkkel(BRNøkkelMedAndeler revurderingAndel, Collection<Yrkesaktivitet> yrkesaktiviteter, LocalDate skjæringstidspunkt) {
        return yrkesaktiviteter.stream()
            .filter(matchMedAndel(revurderingAndel.getNøkkel().getArbeidsgiver(), InternArbeidsforholdRef.nullRef())) // Matcher på InternArbeidsforholdRef.nullRef() fordi vi er interessert i alle ansettelseperioder fra denne arbeidsgiveren
            .flatMap(yr -> yr.getAlleAktivitetsAvtaler().stream())
            .filter(AktivitetsAvtale::erAnsettelsesPeriode)
            .filter(a -> a.getPeriode().getFomDato().isAfter(skjæringstidspunkt))
            .filter(harIkkeAktivAnsettelsesPeriodeDagenFørStart(revurderingAndel, yrkesaktiviteter))
            .map(AktivitetsAvtale::getPeriode)
            .map(DatoIntervallEntitet::getFomDato)
            .min(Comparator.naturalOrder());
    }

    private static Predicate<AktivitetsAvtale> harIkkeAktivAnsettelsesPeriodeDagenFørStart(BRNøkkelMedAndeler revurderingAndel, Collection<Yrkesaktivitet> yrkesaktiviteter) {
        return a -> yrkesaktiviteter.stream().filter(matchMedAndel(revurderingAndel.getNøkkel().getArbeidsgiver(), InternArbeidsforholdRef.nullRef()))// Matcher på InternArbeidsforholdRef.nullRef() fordi vi er interessert i alle ansettelseperioder fra denne arbeidsgiveren
                .flatMap(yr -> yr.getAlleAktivitetsAvtaler().stream())
            .filter(AktivitetsAvtale::erAnsettelsesPeriode)
            .map(AktivitetsAvtale::getPeriode)
            .noneMatch(p -> p.inkluderer(a.getPeriode().getFomDato().minusDays(1)));
    }

    private static Predicate<Yrkesaktivitet> matchMedAndel(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef internArbeidsforholdRef) {
        return yr -> yr.gjelderFor(arbeidsgiver, internArbeidsforholdRef);
    }

}
