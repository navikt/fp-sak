package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.fpsak.tidsserie.LocalDateSegment;

class HindreTilbaketrekkBeregningsresultatPeriode {
    private static final Logger LOG = LoggerFactory.getLogger(HindreTilbaketrekkBeregningsresultatPeriode.class);

    private HindreTilbaketrekkBeregningsresultatPeriode() {
        // skjul public constructor
    }

    static BeregningsresultatPeriode omfordelPeriodeVedBehov(BeregningsresultatEntitet utbetaltTY, LocalDateSegment<BRAndelSammenligning> segment, Collection<Yrkesaktivitet> yrkesaktiviteter, LocalDate skjæringstidspunkt) {
        var bgDagsats = segment.getValue().getBgAndeler().stream()
            .mapToInt(BeregningsresultatAndel::getDagsats)
            .sum();

        var beregningsresultatPeriode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(segment.getFom(), segment.getTom())
            .build(utbetaltTY);
        var wrapper = segment.getValue();
        var forrigeAndeler = wrapper.getForrigeAndeler();
        var bgAndeler = wrapper.getBgAndeler();
        if (forrigeAndeler.isEmpty() || kunUtbetalingTilArbeidsgiver(forrigeAndeler)) {
            // ikke utbetalt tidligere: kopier bg-andeler
            bgAndeler.forEach(andel -> lagBuilderFra(andel).build(beregningsresultatPeriode));
        } else {
            List<EndringIBeregningsresultat> alleEndringer = new ArrayList<>();
            alleEndringer.addAll(FinnEndringerIUtbetaltYtelse.finnEndringer(forrigeAndeler, bgAndeler));
            alleEndringer.addAll(FinnEndringerIResultatForTilkommetArbeidsforhold.finnEndringer(forrigeAndeler, bgAndeler, yrkesaktiviteter, skjæringstidspunkt));

            bgAndeler.stream()
                .map(resultatAndel -> lagResultatBuilder(alleEndringer, resultatAndel))
                .forEach(builder -> builder.build(beregningsresultatPeriode));

            if(utbetaltDagsatsAvvikerFraBeregningsgrunnlag(bgDagsats, beregningsresultatPeriode)){
                LOG.info("Dagsats som er feil: {} Tilhørende periode: {} Forrige andeler: {} BgAndeler: {} ", bgDagsats, beregningsresultatPeriode, forrigeAndeler, bgAndeler);
                throw new IllegalStateException("Utviklerfeil: utbetDagsats er ulik bgDagsats");
            }
        }
        return beregningsresultatPeriode;
    }

    private static BeregningsresultatAndel.Builder lagResultatBuilder(List<EndringIBeregningsresultat> alleEndringer, BeregningsresultatAndel resultatAndel) {
        var endring = finnEndringForResultatAndel(alleEndringer, resultatAndel);
        return endring.map(e -> lagBuilderForEndring(resultatAndel, e))
            .orElseGet(() -> lagBuilderFra(resultatAndel));
    }

    private static BeregningsresultatAndel.Builder lagBuilderForEndring(BeregningsresultatAndel resultatAndel, EndringIBeregningsresultat e) {
        return lagBuilderFra(resultatAndel).medDagsats(e.getDagsats()).medDagsatsFraBg(e.getDagsatsFraBg());
    }

    private static Optional<EndringIBeregningsresultat> finnEndringForResultatAndel(List<EndringIBeregningsresultat> alleEndringer, BeregningsresultatAndel resultatAndel) {
        return alleEndringer.stream().filter(e -> e.gjelderResultatAndel(resultatAndel)).findFirst();
    }

    private static boolean utbetaltDagsatsAvvikerFraBeregningsgrunnlag(int bgDagsats, BeregningsresultatPeriode beregningsresultatPeriode) {
        var utbetDagsats = beregningsresultatPeriode.getBeregningsresultatAndelList().stream()
            .mapToInt(BeregningsresultatAndel::getDagsats)
            .sum();

        return bgDagsats != utbetDagsats;
    }

    private static boolean kunUtbetalingTilArbeidsgiver(List<BeregningsresultatAndel> andeler) {
        return andeler.stream()
            .filter(andel -> andel.getDagsats() > 0)
            .noneMatch(BeregningsresultatAndel::erBrukerMottaker);
    }

    private static BeregningsresultatAndel.Builder lagBuilderFra(BeregningsresultatAndel andel) {
        return BeregningsresultatAndel.builder()
            .medDagsats(andel.getDagsats())
            .medDagsatsFraBg(andel.getDagsatsFraBg())
            .medUtbetalingsgrad(andel.getUtbetalingsgrad())
            .medStillingsprosent(andel.getStillingsprosent())
            .medBrukerErMottaker(andel.erBrukerMottaker())
            .medArbeidsgiver(andel.getArbeidsgiver().orElse(null))
            .medArbeidsforholdRef(andel.getArbeidsforholdRef())
            .medArbeidsforholdType(andel.getArbeidsforholdType())
            .medAktivitetStatus(andel.getAktivitetStatus())
            .medInntektskategori(andel.getInntektskategori());
    }
}
