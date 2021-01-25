package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.app;

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
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto.FeriepengegrunnlagDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;

class FeriepengegrunnlagMapperTest {
    private static final String ORGNR1 = KUNSTIG_ORG;
    private static final LocalDate FERIE_PERIODE_FOM = LocalDate.now();
    private static final LocalDate FERIE_PERIODE_TOM = FERIE_PERIODE_FOM.plusDays(60);

    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.namedRef("TEST-REF");
    private BeregningsresultatPeriode nyPeriode;
    private BeregningsresultatFeriepenger beregningsresultatFeriepenger = BeregningsresultatFeriepenger.builder().medFeriepengerRegelInput("")
        .medFeriepengerRegelSporing("").medFeriepengerPeriodeFom(FERIE_PERIODE_FOM).medFeriepengerPeriodeTom(FERIE_PERIODE_TOM).build();
    private BeregningsresultatEntitet bgres = BeregningsresultatEntitet.builder().medRegelInput("").medRegelSporing("").medBeregningsresultatFeriepenger(beregningsresultatFeriepenger).build();

    @BeforeEach
    void setUp() {
        BeregningsresultatEntitet beregningsresultatRevurdering = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();
        LocalDate fom = LocalDate.now();
        LocalDate tom = LocalDate.now().plusWeeks(1);
        nyPeriode = opprettBeregningsresultatPeriode(beregningsresultatRevurdering, fom, tom);
    }

    @Test
    public void tester_at_feriepenger_mappes() {
        // Arrange : nyPeriode
        BeregningsresultatAndel nyAndel = opprettBeregningsresultatAndel(nyPeriode, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER, ORGNR1, 1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 1000,
            OpptjeningAktivitetType.FORELDREPENGER);
        opprettFeriepenger(2020, 40000, nyAndel);
        // Act
        Optional<FeriepengegrunnlagDto> dtoOpt = FeriepengegrunnlagMapper.map(bgres);

        // Assert
        assertThat(dtoOpt).isPresent();
        FeriepengegrunnlagDto dto = dtoOpt.get();
        assertThat(dto.getFeriepengeperiodeFom()).isEqualTo(FERIE_PERIODE_FOM);
        assertThat(dto.getFeriepengeperiodeTom()).isEqualTo(FERIE_PERIODE_TOM);
        assertThat(dto.getAndeler()).hasSize(1);
        assertThat(dto.getAndeler().get(0).getOpptjeningsår()).isEqualTo(2020);
        assertThat(dto.getAndeler().get(0).getÅrsbeløp()).isEqualByComparingTo(BigDecimal.valueOf(40000));
    }

    @Test
    public void skal_ikke_mappe_når_ingen_feriepengeandeler() {
        // Arrange : nyPeriode
        opprettBeregningsresultatAndel(nyPeriode, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER, ORGNR1, 1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 1000,
            OpptjeningAktivitetType.FORELDREPENGER);
        // Act
        Optional<FeriepengegrunnlagDto> dtoOpt = FeriepengegrunnlagMapper.map(bgres);

        // Assert
        assertThat(dtoOpt).isEmpty();
    }

    private BeregningsresultatPeriode opprettBeregningsresultatPeriode(BeregningsresultatEntitet beregningsresultat, LocalDate fom, LocalDate tom) {
        return BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(fom, tom)
            .build(beregningsresultat);
    }

    private BeregningsresultatAndel opprettBeregningsresultatAndel(BeregningsresultatPeriode beregningsresultatPeriode, boolean erBrukerMottaker,
                                                                   InternArbeidsforholdRef arbeidsforholdId, AktivitetStatus aktivitetStatus,
                                                                   Inntektskategori inntektskategori, String orgNr, int dagsats,
                                                                   BigDecimal stillingsprosent, BigDecimal utbetalingsgrad, int dagsatsFraBg,
                                                                   OpptjeningAktivitetType opptjeningAktivitetType) {
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(orgNr);
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
