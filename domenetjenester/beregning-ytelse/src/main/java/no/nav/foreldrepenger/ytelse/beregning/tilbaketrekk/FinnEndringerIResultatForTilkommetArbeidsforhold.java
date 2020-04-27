package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import static no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk.TilbaketrekkVedTilkommetArbeidsforholdTjeneste.finnStørsteTilbaketrekkForTilkomneArbeidsforhold;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

class FinnEndringerIResultatForTilkommetArbeidsforhold {

    public static List<EndringIBeregningsresultat> finnEndringer(List<BeregningsresultatAndel> originaleAndeler,
                                                                 List<BeregningsresultatAndel> revurderingAndeler,
                                                                 Collection<Yrkesaktivitet> yrkesaktiviteter,
                                                                 LocalDate skjæringstidspunkt) {
        List<EndringIBeregningsresultat> endringer = new ArrayList<>();
        List<BRNøkkelMedAndeler> originaleAndelerSortertPåNøkkel = MapAndelerSortertPåNøkkel.map(originaleAndeler);
        List<BRNøkkelMedAndeler> revurderingAndelerSortertPåNøkkel = MapAndelerSortertPåNøkkel.map(revurderingAndeler);
        Optional<TilbaketrekkForTilkommetArbeidEntry> tilbaketrekkOptional = finnStørsteTilbaketrekkForTilkomneArbeidsforhold(revurderingAndelerSortertPåNøkkel, originaleAndelerSortertPåNøkkel, yrkesaktiviteter, skjæringstidspunkt);
        tilbaketrekkOptional.ifPresent(tilbaketrekkForTilkommetArbeidEntry -> {
            List<EndringIBeregningsresultat> endringITilkommetArbeidsgiversAndel = initEndringerForTilkomneAndeler(tilbaketrekkForTilkommetArbeidEntry);
            for (BRNøkkelMedAndeler nøkkelForAvsluttetArbeid : tilbaketrekkForTilkommetArbeidEntry.getAndelerIRevurderingMedSluttFørDatoSortertPåDato()) {
                BRNøkkelMedAndeler originalNøkkel = tilbaketrekkForTilkommetArbeidEntry.finnOriginalForNøkkel(nøkkelForAvsluttetArbeid.getNøkkel());
                endringer.addAll(flyttForMatchendeReferanser(endringITilkommetArbeidsgiversAndel, nøkkelForAvsluttetArbeid, originalNøkkel));
                flyttForManglendeReferanser(endringITilkommetArbeidsgiversAndel, nøkkelForAvsluttetArbeid, originalNøkkel).ifPresent(endringer::add);
            }
            endringer.addAll(endringITilkommetArbeidsgiversAndel);
        });
        return endringer;
    }

    private static Optional<EndringIBeregningsresultat> flyttForManglendeReferanser(List<EndringIBeregningsresultat> endringITilkommetArbeidsgiversAndel, BRNøkkelMedAndeler nøkkelForAvsluttetArbeid, BRNøkkelMedAndeler originalNøkkel) {
        List<InternArbeidsforholdRef> alleReferanserForDenneNøkkelen = nøkkelForAvsluttetArbeid.getAlleReferanserForDenneNøkkelen();
        List<BeregningsresultatAndel> originaleAndelerUtenMatch = originalNøkkel.getAlleAndelerMedRefSomIkkeFinnesIListe(alleReferanserForDenneNøkkelen);
        Optional<BeregningsresultatAndel> originalBrukersAndelUtenReferanse = originalNøkkel.getBrukersAndelUtenreferanse();
        int totalDagsatsFraOriginaleAndelerUtenMatch = originaleAndelerUtenMatch.stream().map(BeregningsresultatAndel::getDagsats).reduce(Integer::sum).orElse(0) +
            originalBrukersAndelUtenReferanse.map(BeregningsresultatAndel::getDagsats).orElse(0);
        Optional<EndringIBeregningsresultat> endringForBrukersAndelUtenReferanseOpt = nøkkelForAvsluttetArbeid.getBrukersAndelUtenreferanse()
            .map(brukersAndelUtenRef -> EndringIBeregningsresultat.forEndringMedOriginalDagsats(brukersAndelUtenRef, totalDagsatsFraOriginaleAndelerUtenMatch));
        if (endringForBrukersAndelUtenReferanseOpt.isPresent()) {
            EndringIBeregningsresultat endringIBeregningsresultat = endringForBrukersAndelUtenReferanseOpt.get();
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
        List<EndringIBeregningsresultat> endringerForMatchMellomOriginalOgRevurdering = initEndringerForMatchendeReferanser(nøkkelForAvsluttetArbeid, originalNøkkel);
        for (EndringIBeregningsresultat endring : endringerForMatchMellomOriginalOgRevurdering) {
            flyttDagsatsForAndel(endringITilkommetArbeidsgiversAndel, endring);
        }
        return endringerForMatchMellomOriginalOgRevurdering;
    }

    private static void flyttDagsatsForAndel(List<EndringIBeregningsresultat> endringITilkommetArbeidsgiversAndel, EndringIBeregningsresultat endring) {
        Iterator<EndringIBeregningsresultat> iterator = endringITilkommetArbeidsgiversAndel.iterator();
        while (endring.finnResterendeTilbaketrekk() > 0 && iterator.hasNext()) {
            EndringIBeregningsresultat endringForTilkommet = iterator.next();
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

    private static List<EndringIBeregningsresultat> initEndringerForMatchendeReferanser(BRNøkkelMedAndeler nøkkelForAvsluttetArbeid, BRNøkkelMedAndeler originalNøkkel) {
        return originalNøkkel.getBrukersAndelerTilknyttetNøkkel()
            .stream().filter(a -> a.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold() && nøkkelForAvsluttetArbeid.getBrukersAndelMedReferanse(a.getArbeidsforholdRef()).isPresent())
            .map(a -> {
                BeregningsresultatAndel revurderingBrukersAndelMedReferanse = nøkkelForAvsluttetArbeid.getBrukersAndelMedReferanse(a.getArbeidsforholdRef()).get();
                return new EndringIBeregningsresultat(revurderingBrukersAndelMedReferanse, a);
            }).collect(Collectors.toList());
    }

    private static List<EndringIBeregningsresultat> initEndringerForTilkomneAndeler(TilbaketrekkForTilkommetArbeidEntry tilbaketrekk) {
        return tilbaketrekk.getTilkomneNøklerMedStartEtterDato().stream()
                    .flatMap(n -> n.getArbeidsgiversAndelerTilknyttetNøkkel().stream())
                    .map(EndringIBeregningsresultat::new)
                    .collect(Collectors.toList());
    }

}
