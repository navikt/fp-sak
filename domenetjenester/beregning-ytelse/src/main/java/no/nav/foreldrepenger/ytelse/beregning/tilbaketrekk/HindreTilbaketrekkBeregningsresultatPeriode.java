package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.Kopimaskin;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.domene.arbeidsforhold.VurderArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.fpsak.tidsserie.LocalDateSegment;

class HindreTilbaketrekkBeregningsresultatPeriode {
    private static final Logger LOGGER = LoggerFactory.getLogger(VurderArbeidsforholdTjeneste.class);

    private HindreTilbaketrekkBeregningsresultatPeriode() {
        // skjul public constructor
    }

    static BeregningsresultatPeriode omfordelPeriodeVedBehov(BeregningsresultatEntitet utbetaltTY, LocalDateSegment<BRAndelSammenligning> segment, Collection<Yrkesaktivitet> yrkesaktiviteter, LocalDate skjæringstidspunkt) {
        int bgDagsats = segment.getValue().getBgAndeler().stream()
            .mapToInt(BeregningsresultatAndel::getDagsats)
            .sum();

        BeregningsresultatPeriode beregningsresultatPeriode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(segment.getFom(), segment.getTom())
            .build(utbetaltTY);
        BRAndelSammenligning wrapper = segment.getValue();
        List<BeregningsresultatAndel> forrigeAndeler = wrapper.getForrigeAndeler();
        List<BeregningsresultatAndel> bgAndeler = wrapper.getBgAndeler();
        if (forrigeAndeler.isEmpty() || kunUtbetalingTilArbeidsgiver(forrigeAndeler)) {
            // ikke utbetalt tidligere: kopier bg-andeler
            bgAndeler.forEach(andel ->
                BeregningsresultatAndel.builder(Kopimaskin.deepCopy(andel))
                    .medDagsats(andel.getDagsats())
                    .medDagsatsFraBg(andel.getDagsatsFraBg())
                    .build(beregningsresultatPeriode)
            );
        } else {
            List<EndringIBeregningsresultat> alleEndringer = new ArrayList<>();
            alleEndringer.addAll(FinnEndringerIUtbetaltYtelse.finnEndringer(forrigeAndeler, bgAndeler));
            alleEndringer.addAll(FinnEndringerIResultatForTilkommetArbeidsforhold.finnEndringer(forrigeAndeler, bgAndeler, yrkesaktiviteter, skjæringstidspunkt));

            bgAndeler.stream()
                .map(Kopimaskin::deepCopy)
                .filter(resultatAndel -> erBrukersAndelEllerArbeidsgiversAndelUlikNull(alleEndringer, resultatAndel))
                .map(resultatAndel -> lagResultatBuilder(alleEndringer, resultatAndel))
                .forEach(builder -> builder.build(beregningsresultatPeriode));

            postcondition(bgDagsats, beregningsresultatPeriode);
        }
        return beregningsresultatPeriode;
    }

    private static boolean erBrukersAndelEllerArbeidsgiversAndelUlikNull(List<EndringIBeregningsresultat> alleEndringer, BeregningsresultatAndel resultatAndel) {
        return resultatAndel.erBrukerMottaker() || finnEndringForResultatAndel(alleEndringer, resultatAndel).map(e -> e.getDagsats() > 0).orElse(true);
    }

    private static BeregningsresultatAndel.Builder lagResultatBuilder(List<EndringIBeregningsresultat> alleEndringer, BeregningsresultatAndel resultatAndel) {
        Optional<EndringIBeregningsresultat> endring = finnEndringForResultatAndel(alleEndringer, resultatAndel);
        return endring.map(e -> lagBuilderForEndring(resultatAndel, e))
            .orElse(new BeregningsresultatAndel.Builder(resultatAndel));
    }

    private static BeregningsresultatAndel.Builder lagBuilderForEndring(BeregningsresultatAndel resultatAndel, EndringIBeregningsresultat e) {
        return new BeregningsresultatAndel.Builder(resultatAndel).medDagsats(e.getDagsats()).medDagsatsFraBg(e.getDagsatsFraBg());
    }

    private static Optional<EndringIBeregningsresultat> finnEndringForResultatAndel(List<EndringIBeregningsresultat> alleEndringer, BeregningsresultatAndel resultatAndel) {
        return alleEndringer.stream().filter(e -> e.gjelderResultatAndel(resultatAndel)).findFirst();
    }

    private static void postcondition(int bgDagsats, BeregningsresultatPeriode beregningsresultatPeriode) {
        int utbetDagsats = beregningsresultatPeriode.getBeregningsresultatAndelList().stream()
            .mapToInt(BeregningsresultatAndel::getDagsats)
            .sum();

        if (bgDagsats != utbetDagsats) {
            LOGGER.info("Dagsats som er feil: " + bgDagsats + " Tilhørende periode: " + beregningsresultatPeriode);
            throw new IllegalStateException("Utviklerfeil: utbetDagsats er ulik bgDagsats");
        }
    }

    private static boolean kunUtbetalingTilArbeidsgiver(List<BeregningsresultatAndel> andeler) {
        return andeler.stream()
            .filter(andel -> andel.getDagsats() > 0)
            .noneMatch(BeregningsresultatAndel::erBrukerMottaker);
    }
}
