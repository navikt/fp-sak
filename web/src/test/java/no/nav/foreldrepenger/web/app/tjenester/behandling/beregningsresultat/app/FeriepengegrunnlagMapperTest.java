package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.app;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

class FeriepengegrunnlagMapperTest {
    private static final String ORGNR1 = KUNSTIG_ORG;
    private static final LocalDate FERIE_PERIODE_FOM = LocalDate.now();
    private static final LocalDate FERIE_PERIODE_TOM = FERIE_PERIODE_FOM.plusDays(60);

    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.namedRef("TEST-REF");
    private BeregningsresultatPeriode nyPeriode;
    private BeregningsresultatEntitet bgres = BeregningsresultatEntitet.builder().medRegelInput("").medRegelSporing("").build();
    private BeregningsresultatFeriepenger beregningsresultatFeriepenger = BeregningsresultatFeriepenger.builder()
        .medFeriepengerRegelInput("")
        .medFeriepengerRegelSporing("")
        .medFeriepengerPeriodeFom(FERIE_PERIODE_FOM)
        .medFeriepengerPeriodeTom(FERIE_PERIODE_TOM)
        .build(bgres);

    @BeforeEach
    void setUp() {
        var beregningsresultatRevurdering = BeregningsresultatEntitet.builder().medRegelInput("clob1").medRegelSporing("clob2").build();
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusWeeks(1);
        nyPeriode = opprettBeregningsresultatPeriode(beregningsresultatRevurdering, fom, tom);
    }

    @Test
    void tester_at_feriepenger_mappes() {
        // Arrange : nyPeriode
        var nyAndel = opprettBeregningsresultatAndel(nyPeriode, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR1, 1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 1000, OpptjeningAktivitetType.FORELDREPENGER);
        opprettFeriepenger(2020, 40000, nyAndel);
        // Act
        var dtoOpt = FeriepengegrunnlagMapper.map(bgres);

        // Assert
        assertThat(dtoOpt).isPresent();
        var dto = dtoOpt.get();
        assertThat(dto.andeler()).hasSize(1);
        assertThat(dto.andeler().getFirst().getOpptjeningsår()).isEqualTo(2020);
        assertThat(dto.andeler().getFirst().getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(40000));
    }

    @Test
    void skal_ikke_mappe_når_ingen_feriepengeandeler() {
        // Arrange : nyPeriode
        opprettBeregningsresultatAndel(nyPeriode, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER, ORGNR1, 1000,
            BigDecimal.valueOf(100), BigDecimal.valueOf(100), 1000, OpptjeningAktivitetType.FORELDREPENGER);
        // Act
        var dtoOpt = FeriepengegrunnlagMapper.map(bgres);

        // Assert
        assertThat(dtoOpt).isEmpty();
    }

    private BeregningsresultatPeriode opprettBeregningsresultatPeriode(BeregningsresultatEntitet beregningsresultat, LocalDate fom, LocalDate tom) {
        return BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(fom, tom).build(beregningsresultat);
    }

    private BeregningsresultatAndel opprettBeregningsresultatAndel(BeregningsresultatPeriode beregningsresultatPeriode,
                                                                   boolean erBrukerMottaker,
                                                                   InternArbeidsforholdRef arbeidsforholdId,
                                                                   AktivitetStatus aktivitetStatus,
                                                                   Inntektskategori inntektskategori,
                                                                   String orgNr,
                                                                   int dagsats,
                                                                   BigDecimal stillingsprosent,
                                                                   BigDecimal utbetalingsgrad,
                                                                   int dagsatsFraBg,
                                                                   OpptjeningAktivitetType opptjeningAktivitetType) {
        var arbeidsgiver = Arbeidsgiver.virksomhet(orgNr);
        return BeregningsresultatAndel.builder()
            .medBrukerErMottaker(erBrukerMottaker)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdRef(arbeidsforholdId)
            .medAktivitetStatus(aktivitetStatus)
            .medInntektskategori(inntektskategori)
            .medStillingsprosent(stillingsprosent)
            .medUtbetalingsgrad(utbetalingsgrad)
            .medDagsats(dagsats)
            .medDagsatsFraBg(dagsatsFraBg)
            .medArbeidsforholdType(opptjeningAktivitetType)
            .build(beregningsresultatPeriode);
    }

    private void opprettFeriepenger(int opptjeningsår, int årsbeløp, BeregningsresultatAndel andel) {
        BeregningsresultatFeriepengerPrÅr.builder().medOpptjeningsår(opptjeningsår).medÅrsbeløp(årsbeløp).build(beregningsresultatFeriepenger, andel);
    }

}
