package no.nav.foreldrepenger.ytelse.beregning;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.ytelse.beregning.adapter.MapBeregningsresultatFeriepengerFraRegelTilVL;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatAndel;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatFeriepengerResultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.FastsattFeriepengeresultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Inntektskategori;
import no.nav.fpsak.tidsserie.LocalDateInterval;

class MapBeregningsresultatFeriepengerFraRegelTilVLTest {

    private static final LocalDate STP = LocalDate.now();
    private static final LocalDateInterval PERIODE = LocalDateInterval.withPeriodAfterDate(STP, Period.ofMonths(10));
    public static final String ORGNR = KUNSTIG_ORG;
    private static final Arbeidsforhold ARBEIDSFORHOLD = Arbeidsforhold.nyttArbeidsforholdHosVirksomhet(ORGNR, null);
    private static final long DAGSATS = 500L;
    private static final long DAGSATS_FRA_BG = 500L;
    private static final BigDecimal UTBETALINGSGRAD = BigDecimal.valueOf(100);

    @Test
    void skal_ikkje_lage_feriepengeresultat_om_årsbeløp_avrundes_til_0() {
        // Arrange
        var periode = lagPeriodeMedAndel(BigDecimal.valueOf(0.1));
        var beregningsresultat = lagVlBeregningsresultat();
        var regelresultat = new BeregningsresultatFeriepengerResultat(List.of(periode), new LocalDateInterval(STP, STP.plusMonths(10)));
        var resultat = new FastsattFeriepengeresultat(regelresultat, null, "input", "sporing", null);

        // Act
        MapBeregningsresultatFeriepengerFraRegelTilVL.mapFra(beregningsresultat, resultat);

        // Assert
        var beregningsresultatFeriepengerPrÅrListe = beregningsresultat.getBeregningsresultatFeriepenger().get()
                .getBeregningsresultatFeriepengerPrÅrListe();
        assertThat(beregningsresultatFeriepengerPrÅrListe).isEmpty();
    }

    @Test
    void skal_lage_feriepengeresultat_om_årsbeløp_ikkje_avrundes_til_0() {
        // Arrange
        var periode = lagPeriodeMedAndel(BigDecimal.valueOf(1.5));
        var beregningsresultat = lagVlBeregningsresultat();
        var regelresultat = new BeregningsresultatFeriepengerResultat(List.of(periode), new LocalDateInterval(STP, STP.plusMonths(10)));
        var resultat = new FastsattFeriepengeresultat(regelresultat, null, "input", "sporing", null);

        // Act
        MapBeregningsresultatFeriepengerFraRegelTilVL.mapFra(beregningsresultat, resultat);


        // Assert
        var beregningsresultatFeriepengerPrÅrListe = beregningsresultat.getBeregningsresultatFeriepenger().get()
                .getBeregningsresultatFeriepengerPrÅrListe();
        assertThat(beregningsresultatFeriepengerPrÅrListe).hasSize(1);
    }

    private BeregningsresultatEntitet lagVlBeregningsresultat() {
        var beregningsresultat = BeregningsresultatEntitet
                .builder()
                .medRegelInput("Regelinput")
                .medRegelSporing("Regelsporing")
                .build();
        var vlBeregningsresultatPeriode = no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode
                .builder()
                .medBeregningsresultatPeriodeFomOgTom(PERIODE.getFomDato(), PERIODE.getTomDato())
                .build(beregningsresultat);

        no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel
                .builder()
                .medBrukerErMottaker(true)
                .medDagsats((int) DAGSATS)
                .medDagsatsFraBg((int) DAGSATS_FRA_BG)
                .medInntektskategori(no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori.ARBEIDSTAKER)
                .medUtbetalingsgrad(UTBETALINGSGRAD)
                .medAktivitetStatus(no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus.ARBEIDSTAKER)
                .medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR))
                .medStillingsprosent(BigDecimal.valueOf(100)).build(vlBeregningsresultatPeriode);
        return beregningsresultat;
    }

    private BeregningsresultatPeriode lagPeriodeMedAndel(BigDecimal årsbeløp) {
        var andel = BeregningsresultatAndel.builder().medAktivitetStatus(AktivitetStatus.ATFL)
                .medBrukerErMottaker(true)
                .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
                .medArbeidsforhold(ARBEIDSFORHOLD)
                .medDagsats(DAGSATS)
                .medDagsatsFraBg(DAGSATS_FRA_BG)
                .medUtbetalingssgrad(UTBETALINGSGRAD)
                .build();
        andel.addBeregningsresultatFeriepengerPrÅr(BeregningsresultatFeriepengerPrÅr.builder().medÅrsbeløp(årsbeløp)
                .medOpptjeningÅr(LocalDate.now())
                .medBrukerErMottaker(andel.erBrukerMottaker())
                .medArbeidsforhold(andel.getArbeidsforhold())
                .build());
        var periode = new BeregningsresultatPeriode(PERIODE);
        periode.addBeregningsresultatAndel(andel);
        return periode;
    }
}
