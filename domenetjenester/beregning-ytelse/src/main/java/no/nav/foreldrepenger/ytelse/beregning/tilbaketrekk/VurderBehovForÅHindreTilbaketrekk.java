package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import static no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk.TilbaketrekkVedTilkommetArbeidsforholdTjeneste.finnStørsteTilbaketrekkForTilkomneArbeidsforhold;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetOgArbeidsgiverNøkkel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

/**
 * Tar inn en tidslinje over allerede utbetalt ytelse og ny beregnet ytelse
 * Sammenligner andelene og ser om det er blitt utbetalt for mye til bruker som må må tilbakekreves.
 * Man trenger kun bry seg om arbeidstakerandeler. Det er kun disse som kan ha utbetaling til arbeidsgiver.
 */
public class VurderBehovForÅHindreTilbaketrekk {

    private VurderBehovForÅHindreTilbaketrekk() {
        // Skjuler default konstruktør
    }

    static boolean skalVurdereTilbaketrekk(LocalDateTimeline<BRAndelSammenligning> brAndelTidslinje, Collection<Yrkesaktivitet> yrkesaktiviteter, LocalDate skjæringstidspunkt) {

        // hvis endring i totalDagsats
        for (var segment : brAndelTidslinje.toSegments()) {
            var sammenligning = segment.getValue();
            var originaleAndeler = sammenligning.getForrigeAndeler();
            var revurderingAndeler = sammenligning.getBgAndeler();

            var originaleAndelerSortertPåNøkkel = MapAndelerSortertPåNøkkel.map(originaleAndeler);
            var revurderingAndelerSortertPåNøkkel = MapAndelerSortertPåNøkkel.map(revurderingAndeler);

            for(var originalAndel : originaleAndelerSortertPåNøkkel) {
                if (kanAndelerPåNøkkelOmfordeles(revurderingAndelerSortertPåNøkkel, originalAndel))  {
                    return true;
                }
            }

            if (skalVurdereTilbaketrekkForTilkomneAndeler(yrkesaktiviteter, skjæringstidspunkt, originaleAndelerSortertPåNøkkel, revurderingAndelerSortertPåNøkkel)) {
                return true;
            }
        }
        return false;
    }

    private static boolean skalVurdereTilbaketrekkForTilkomneAndeler(Collection<Yrkesaktivitet> yrkesaktiviteter, LocalDate skjæringstidspunkt, List<BRNøkkelMedAndeler> originaleAndelerSortertPåNøkkel, List<BRNøkkelMedAndeler> revurderingAndelerSortertPåNøkkel) {
        var tilbaketrekkEntry = finnStørsteTilbaketrekkForTilkomneArbeidsforhold(revurderingAndelerSortertPåNøkkel, originaleAndelerSortertPåNøkkel, yrkesaktiviteter, skjæringstidspunkt);
        return tilbaketrekkEntry.isPresent();
    }

    private static boolean kanAndelerPåNøkkelOmfordeles(List<BRNøkkelMedAndeler> revurderingAndelerSortertPåNøkkel, BRNøkkelMedAndeler originalAndel) {
        if (originalAndel.erArbeidstaker()) {
            var andelerIRevurderingGrunnlagMedSammeNøkkel = finnSammenligningsandelMedSammeNøkkel(originalAndel.getNøkkel(), revurderingAndelerSortertPåNøkkel);
            if (andelerIRevurderingGrunnlagMedSammeNøkkel.isPresent()) {
                // Nøkkelen eksisterer fremdeles, må matche hver andel som tilhører nøkelen i gammelt og nytt grunnlag
                return nøkkelInneholderAndelerSomMåVurderes(originalAndel, andelerIRevurderingGrunnlagMedSammeNøkkel.get());
            }
            // Nøkkelen har blitt borte mellom forrige behandling og denne
            return måAndelerSomHarBortfaltOmfordeles(originalAndel);
        }
        return false;
    }

    private static boolean nøkkelInneholderAndelerSomMåVurderes(BRNøkkelMedAndeler originalBRNøkkelMedAndeler, BRNøkkelMedAndeler revurderingBRNøkkelMedAndeler) {

        // Hvis det ligger andeler i revurderingen som ikke matcher mot noen andel i originalbehandlingen (ulik arbeidsforholdreferanse) må disse spesialhåndteres, det gjøres her.
        if (andelerIRevurderingUtenMatchIOriginalbehandlingMåVurderes(originalBRNøkkelMedAndeler, revurderingBRNøkkelMedAndeler)) {
            return true;
        }

        var alleBrukersAndelerForNøkkelIOriginalBehandling = originalBRNøkkelMedAndeler.getBrukersAndelerTilknyttetNøkkel();
        for (var originalBrukersAndel : alleBrukersAndelerForNøkkelIOriginalBehandling) {
            if (andelMåVurderes(revurderingBRNøkkelMedAndeler, originalBrukersAndel)) {
                return true;
            }
        }
        return false;
    }

    private static boolean andelMåVurderes(BRNøkkelMedAndeler revurderingBRNøkkelMedAndeler, BeregningsresultatAndel originalBrukersAndel) {
        var brukersAndelIRevurdering = revurderingBRNøkkelMedAndeler.getAlleBrukersAndelerMedReferanse(originalBrukersAndel.getArbeidsforholdRef());
        var arbeidsgiversAndelIRevurdering = revurderingBRNøkkelMedAndeler.getAlleArbeidsgiversAndelerMedReferanse(originalBrukersAndel.getArbeidsforholdRef());
        var revurderingDagsatsBruker = dagsats(brukersAndelIRevurdering);
        var revurderingDagsatsArbeidsgiver = dagsats(arbeidsgiversAndelIRevurdering);
        var endringIDagsatsBruker = revurderingDagsatsBruker - originalBrukersAndel.getDagsats();

        return KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingDagsatsArbeidsgiver
        );
    }

    private static boolean andelerIRevurderingUtenMatchIOriginalbehandlingMåVurderes(BRNøkkelMedAndeler originalBRNøkkelMedAndeler, BRNøkkelMedAndeler revurderingBRNøkkelMedAndeler) {
        // Disse andelene har ikke matchende andel i det gamle grunnlaget
        var andelerIRevurderingSomIkkeSvarerTilNoenIOriginalt = finnAndelerIRevurderingSomIkkeMatcherSpesifikkeArbeidsforholdIOriginaltResultat(originalBRNøkkelMedAndeler, revurderingBRNøkkelMedAndeler);

        if (!andelerIRevurderingSomIkkeSvarerTilNoenIOriginalt.isEmpty()) {
            var brukersAndelPåOriginaltResultatUtenReferanse = originalBRNøkkelMedAndeler.getAlleBrukersAndelerUtenReferanse();
            return andelerMåVurderes(andelerIRevurderingSomIkkeSvarerTilNoenIOriginalt, brukersAndelPåOriginaltResultatUtenReferanse);
        }
        return false;
    }

    private static boolean andelerMåVurderes(List<BeregningsresultatAndel> andelerINyttResultatSomIkkeSvarerTilNoenIGammelt,
                                             List<BeregningsresultatAndel> brukersAndelPåGammeltResultatUtenReferanse) {

        // Hvis vi har en andel på gammelt grunnlag for denne nøkkelen uten referanse kan vi sjekke mot den.
        // Om vi ikke har denne må vi opprette aksjonspunkt så saksbehandler kan avgjøre hva som skal gjøres
        if (!brukersAndelPåGammeltResultatUtenReferanse.isEmpty()) {
            return måVurdereTilkomneAndeler(andelerINyttResultatSomIkkeSvarerTilNoenIGammelt, brukersAndelPåGammeltResultatUtenReferanse);
        }
        return true;
    }

    private static boolean måVurdereTilkomneAndeler(List<BeregningsresultatAndel> andelerIRevurderingSomIkkeSvarerTilNoenIOriginal,
                                                    List<BeregningsresultatAndel> brukersAndelPåOriginaltResultat) {
        var aggregertArbeidsgiversDagsats = andelerIRevurderingSomIkkeSvarerTilNoenIOriginal.stream()
            .filter(a -> !a.erBrukerMottaker())
            .mapToInt(BeregningsresultatAndel::getDagsats)
            .sum();

        var aggregertBrukersDagsats = andelerIRevurderingSomIkkeSvarerTilNoenIOriginal.stream()
            .filter(BeregningsresultatAndel::erBrukerMottaker)
            .mapToInt(BeregningsresultatAndel::getDagsats)
            .sum();

        var endringIDagsatsBruker = aggregertBrukersDagsats - brukersAndelPåOriginaltResultat.stream().mapToInt(BeregningsresultatAndel::getDagsats).sum();

        return KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            aggregertArbeidsgiversDagsats
        );
    }

    private static List<BeregningsresultatAndel> finnAndelerIRevurderingSomIkkeMatcherSpesifikkeArbeidsforholdIOriginaltResultat(BRNøkkelMedAndeler originalBRNøkkelMedAndeler, BRNøkkelMedAndeler revurderingBRNøkkelMedAndeler) {
        var alleReferanserIOriginalbehandlingForNøkkel = originalBRNøkkelMedAndeler.getAlleReferanserForDenneNøkkelen();
        return revurderingBRNøkkelMedAndeler.getAlleAndelerMedRefSomIkkeFinnesIListe(alleReferanserIOriginalbehandlingForNøkkel);
    }

    private static boolean måAndelerSomHarBortfaltOmfordeles(BRNøkkelMedAndeler bortfaltAndel) {
        var originaleBrukersAndeler = bortfaltAndel.getBrukersAndelerTilknyttetNøkkel();
        for (var originalBrukersAndel : originaleBrukersAndeler) {
            var revurderingDagsatsBruker = 0;
            var revurderingDagsatsArbeidsgiver = 0;
            var endringIDagsatsBruker = revurderingDagsatsBruker - originalBrukersAndel.getDagsats();

            var skalStoppes = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingDagsatsArbeidsgiver
            );
            if (skalStoppes) {
                return true;
            }
        }
        return false;
    }

    private static Optional<BRNøkkelMedAndeler> finnSammenligningsandelMedSammeNøkkel(AktivitetOgArbeidsgiverNøkkel nøkkel, List<BRNøkkelMedAndeler> liste) {
        var matchendeNøkler = liste.stream()
            .filter(a -> Objects.equals(a.getNøkkel(), nøkkel))
            .collect(Collectors.toList());
        if (matchendeNøkler.size() > 1) {
            throw new IllegalStateException("Forventet å ikke finne mer enn en matchende nøkkel i liste for nøkkel " + nøkkel + " men fant " + matchendeNøkler.size());
        }
        return matchendeNøkler.stream().findFirst();
    }

    private static int dagsats(List<BeregningsresultatAndel> andel) {
        return andel.stream().mapToInt(BeregningsresultatAndel::getDagsats).sum();
    }
}

