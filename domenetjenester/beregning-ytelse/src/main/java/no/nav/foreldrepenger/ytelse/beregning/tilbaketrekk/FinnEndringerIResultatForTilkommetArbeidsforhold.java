package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import static no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk.TilbaketrekkVedTilkommetArbeidsforholdTjeneste.finnStørsteTilbaketrekkForTilkomneArbeidsforhold;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;

class FinnEndringerIResultatForTilkommetArbeidsforhold {

    private FinnEndringerIResultatForTilkommetArbeidsforhold() {
    }

    public static List<EndringIBeregningsresultat> finnEndringer(List<BeregningsresultatAndel> originaleAndeler,
                                                                 List<BeregningsresultatAndel> revurderingAndeler,
                                                                 Collection<Yrkesaktivitet> yrkesaktiviteter,
                                                                 LocalDate skjæringstidspunkt) {
        List<EndringIBeregningsresultat> endringer = new ArrayList<>();
        var originaleAndelerSortertPåNøkkel = MapAndelerSortertPåNøkkel.map(originaleAndeler);
        var revurderingAndelerSortertPåNøkkel = MapAndelerSortertPåNøkkel.map(revurderingAndeler);
        var tilbaketrekkOptional = finnStørsteTilbaketrekkForTilkomneArbeidsforhold(revurderingAndelerSortertPåNøkkel,
            originaleAndelerSortertPåNøkkel, yrkesaktiviteter, skjæringstidspunkt);
        tilbaketrekkOptional.ifPresent(tilbaketrekkForTilkommetArbeidEntry -> {
            var endringITilkommetArbeidsgiversAndel = initEndringerForTilkomneAndeler(tilbaketrekkForTilkommetArbeidEntry);
            for (var nøkkelForAvsluttetArbeid : tilbaketrekkForTilkommetArbeidEntry.getAndelerIRevurderingMedSluttFørDatoSortertPåDato()) {
                var originalNøkkel = tilbaketrekkForTilkommetArbeidEntry.finnOriginalForNøkkel(nøkkelForAvsluttetArbeid.getNøkkel());
                endringer.addAll(flyttForMatchendeReferanser(endringITilkommetArbeidsgiversAndel, nøkkelForAvsluttetArbeid, originalNøkkel));
                flyttForManglendeReferanser(endringITilkommetArbeidsgiversAndel, nøkkelForAvsluttetArbeid, originalNøkkel).ifPresent(endringer::add);
            }
            endringer.addAll(endringITilkommetArbeidsgiversAndel);
        });
        return endringer;
    }

    private static Optional<EndringIBeregningsresultat> flyttForManglendeReferanser(List<EndringIBeregningsresultat> endringITilkommetArbeidsgiversAndel,
                                                                                    BRNøkkelMedAndeler nøkkelForAvsluttetArbeid,
                                                                                    BRNøkkelMedAndeler originalNøkkel) {
        var alleReferanserForDenneNøkkelen = nøkkelForAvsluttetArbeid.getAlleReferanserForDenneNøkkelen();
        var originaleAndelerUtenMatch = originalNøkkel.getAlleAndelerMedRefSomIkkeFinnesIListe(alleReferanserForDenneNøkkelen);
        var originalBrukersAndelUtenReferanse = originalNøkkel.getBrukersAndelUtenreferanse();
        var totalDagsatsFraOriginaleAndelerUtenMatch =
            originaleAndelerUtenMatch.stream().map(BeregningsresultatAndel::getDagsats).reduce(Integer::sum).orElse(0)
                + originalBrukersAndelUtenReferanse.map(BeregningsresultatAndel::getDagsats).orElse(0);
        var endringForBrukersAndelUtenReferanseOpt = nøkkelForAvsluttetArbeid.getBrukersAndelUtenreferanse()
            .map(brukersAndelUtenRef -> EndringIBeregningsresultat.forEndringMedOriginalDagsats(brukersAndelUtenRef,
                totalDagsatsFraOriginaleAndelerUtenMatch));
        if (endringForBrukersAndelUtenReferanseOpt.isPresent()) {
            var endringIBeregningsresultat = endringForBrukersAndelUtenReferanseOpt.get();
            flyttDagsatsForAndel(endringITilkommetArbeidsgiversAndel, endringIBeregningsresultat);
            if (endringIBeregningsresultat.getDagsats() != endringIBeregningsresultat.getDagsatsFraBg()) {
                return Optional.of(endringIBeregningsresultat);
            }
        }
        return Optional.empty();
    }

    private static List<EndringIBeregningsresultat> flyttForMatchendeReferanser(List<EndringIBeregningsresultat> endringITilkommetArbeidsgiversAndel,
                                                                                BRNøkkelMedAndeler nøkkelForAvsluttetArbeid,
                                                                                BRNøkkelMedAndeler originalNøkkel) {
        var endringerForMatchMellomOriginalOgRevurdering = initEndringerForMatchendeReferanser(nøkkelForAvsluttetArbeid, originalNøkkel);
        for (var endring : endringerForMatchMellomOriginalOgRevurdering) {
            flyttDagsatsForAndel(endringITilkommetArbeidsgiversAndel, endring);
        }
        return endringerForMatchMellomOriginalOgRevurdering;
    }

    private static void flyttDagsatsForAndel(List<EndringIBeregningsresultat> endringITilkommetArbeidsgiversAndel,
                                             EndringIBeregningsresultat endring) {
        var iterator = endringITilkommetArbeidsgiversAndel.iterator();
        while (endring.finnResterendeTilbaketrekk() > 0 && iterator.hasNext()) {
            var endringForTilkommet = iterator.next();
            if (endringForTilkommet.getDagsats() > 0) {
                if (endringForTilkommet.getDagsats() > endring.finnResterendeTilbaketrekk()) {
                    flyttDelerAvArbeidstakerDagsatsTilBruker(endring, endring.finnResterendeTilbaketrekk(), endringForTilkommet);
                } else {
                    flyttHeleDagsatsFraArbeidsgiverTilBruker(endring, endringForTilkommet);
                }
            }
        }
    }

    private static void flyttHeleDagsatsFraArbeidsgiverTilBruker(EndringIBeregningsresultat endringEksisterendeBrukersAndel,
                                                                 EndringIBeregningsresultat endringTilkommetArbeidsgiversAndel) {
        endringEksisterendeBrukersAndel.setDagsats(endringEksisterendeBrukersAndel.getDagsats() + endringTilkommetArbeidsgiversAndel.getDagsats());
        endringTilkommetArbeidsgiversAndel.setDagsats(0);
    }

    private static void flyttDelerAvArbeidstakerDagsatsTilBruker(EndringIBeregningsresultat endringEksisterendeBrukersAndel,
                                                                 int måFlyttes,
                                                                 EndringIBeregningsresultat endringTilkommetArbeidsgiversAndel) {
        endringEksisterendeBrukersAndel.setDagsats(endringEksisterendeBrukersAndel.getDagsats() + måFlyttes);
        endringTilkommetArbeidsgiversAndel.setDagsats(endringTilkommetArbeidsgiversAndel.getDagsats() - måFlyttes);
    }

    private static List<EndringIBeregningsresultat> initEndringerForMatchendeReferanser(BRNøkkelMedAndeler nøkkelForAvsluttetArbeid,
                                                                                        BRNøkkelMedAndeler originalNøkkel) {
        return originalNøkkel.getBrukersAndelerTilknyttetNøkkel()
            .stream()
            .filter(a -> a.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold() && nøkkelForAvsluttetArbeid.getBrukersAndelMedReferanse(
                a.getArbeidsforholdRef()).isPresent())
            .map(a -> {
                var revurderingBrukersAndelMedReferanse = nøkkelForAvsluttetArbeid.getBrukersAndelMedReferanse(a.getArbeidsforholdRef()).get();
                return new EndringIBeregningsresultat(revurderingBrukersAndelMedReferanse, a);
            })
            .toList();
    }

    private static List<EndringIBeregningsresultat> initEndringerForTilkomneAndeler(TilbaketrekkForTilkommetArbeidEntry tilbaketrekk) {
        return tilbaketrekk.getTilkomneNøklerMedStartEtterDato()
            .stream()
            .flatMap(n -> n.getArbeidsgiversAndelerTilknyttetNøkkel().stream())
            .map(EndringIBeregningsresultat::new)
            .toList();
    }

}
