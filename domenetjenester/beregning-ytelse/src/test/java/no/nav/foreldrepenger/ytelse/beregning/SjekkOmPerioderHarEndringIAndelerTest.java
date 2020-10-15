package no.nav.foreldrepenger.ytelse.beregning;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Java6Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.exception.TekniskException;

public class SjekkOmPerioderHarEndringIAndelerTest {

    private static final String ORGNR1 = KUNSTIG_ORG;
    private static final String ORGNR2 = "2";

    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.namedRef("TEST-REF");

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private SjekkOmPerioderHarEndringIAndeler sjekkOmPerioderHarEndringIAndeler;
    private BeregningsresultatPeriode nyPeriode;
    private BeregningsresultatPeriode gammelPeriode;

    @Before
    public void oppsett(){
        BeregningsresultatEntitet beregningsresultatFørstegangsbehandling = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();
        BeregningsresultatEntitet beregningsresultatRevurdering = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();
        LocalDate fom = LocalDate.now();
        LocalDate tom = LocalDate.now().plusWeeks(1);
        nyPeriode = opprettBeregningsresultatPeriode(beregningsresultatRevurdering, fom, tom);
        gammelPeriode = opprettBeregningsresultatPeriode(beregningsresultatFørstegangsbehandling, fom, tom);
        sjekkOmPerioderHarEndringIAndeler = new SjekkOmPerioderHarEndringIAndeler();
    }

    @Test
    public void innneholder_samme_andeler_når_alt_er_likt(){
        // Arrange : nyPeriode
        opprettBeregningsresultatAndel(nyPeriode, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR1,1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 1000, OpptjeningAktivitetType.FORELDREPENGER);
        opprettBeregningsresultatAndel(nyPeriode, true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER,
            ORGNR2,500, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 500, OpptjeningAktivitetType.FORELDREPENGER);
        // Arrange : gammelPeriode
        opprettBeregningsresultatAndel(gammelPeriode, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR1,1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 1000, OpptjeningAktivitetType.FORELDREPENGER);
        opprettBeregningsresultatAndel(gammelPeriode, true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER,
            ORGNR2,500, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 500, OpptjeningAktivitetType.FORELDREPENGER);
        // Act
        boolean erEndring = sjekkOmPerioderHarEndringIAndeler.sjekk(nyPeriode, gammelPeriode);
        // Assert
        assertThat(erEndring).isFalse();
    }

    @Test
    public void inneholder_samme_andeler_når_dagsatsFraBg_er_endret(){
        // Arrange : nyPeriode
        opprettBeregningsresultatAndel(nyPeriode, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR1,1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 1000, OpptjeningAktivitetType.FORELDREPENGER);
        opprettBeregningsresultatAndel(nyPeriode, true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER,
            ORGNR2,500, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 500, OpptjeningAktivitetType.FORELDREPENGER);
        // Arrange : gammelPeriode
        opprettBeregningsresultatAndel(gammelPeriode, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR1,1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 1000, OpptjeningAktivitetType.FORELDREPENGER);
        opprettBeregningsresultatAndel(gammelPeriode, true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER,
            ORGNR2,500, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 1500, OpptjeningAktivitetType.FORELDREPENGER);
        // Act
        boolean erEndring = sjekkOmPerioderHarEndringIAndeler.sjekk(nyPeriode, gammelPeriode);
        // Assert
        assertThat(erEndring).isFalse();
    }

    @Test
    public void inneholder_samme_andeler_når_OpptjeningAktivitetType_er_endret(){
        // Arrange : nyPeriode
        opprettBeregningsresultatAndel(nyPeriode, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR1,1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 1000, OpptjeningAktivitetType.FORELDREPENGER);
        opprettBeregningsresultatAndel(nyPeriode, true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER,
            ORGNR2,500, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 500, OpptjeningAktivitetType.FORELDREPENGER);
        // Arrange : gammelPeriode
        opprettBeregningsresultatAndel(gammelPeriode, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR1,1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 1000, OpptjeningAktivitetType.FORELDREPENGER);
        opprettBeregningsresultatAndel(gammelPeriode, true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER,
            ORGNR2,500, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 500, OpptjeningAktivitetType.ARBEID);
        // Act
        boolean erEndring = sjekkOmPerioderHarEndringIAndeler.sjekk(nyPeriode, gammelPeriode);
        // Assert
        assertThat(erEndring).isFalse();
    }

    @Test
    public void inneholder_samme_andeler_når_stillingsprosenten_er_endret(){
        // Arrange : nyPeriode
        opprettBeregningsresultatAndel(nyPeriode, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR1,1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 1000, OpptjeningAktivitetType.FORELDREPENGER);
        opprettBeregningsresultatAndel(nyPeriode, true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER,
            ORGNR2,500, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 500, OpptjeningAktivitetType.FORELDREPENGER);
        // Arrange : gammelPeriode
        opprettBeregningsresultatAndel(gammelPeriode, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR1,1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 1000, OpptjeningAktivitetType.FORELDREPENGER);
        opprettBeregningsresultatAndel(gammelPeriode, true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER,
            ORGNR2,500, BigDecimal.valueOf(50), BigDecimal.valueOf(100), 500, OpptjeningAktivitetType.FORELDREPENGER);
        // Act
        boolean erEndring = sjekkOmPerioderHarEndringIAndeler.sjekk(nyPeriode, gammelPeriode);
        // Assert
        assertThat(erEndring).isFalse();
    }

    @Test
    public void inneholder_samme_andeler_når_utbetalingsgrad_er_endret(){
        // Arrange : nyPeriode
        opprettBeregningsresultatAndel(nyPeriode, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR1,1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 1000, OpptjeningAktivitetType.FORELDREPENGER);
        opprettBeregningsresultatAndel(nyPeriode, true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER,
            ORGNR2,500, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 500, OpptjeningAktivitetType.FORELDREPENGER);
        // Arrange : gammelPeriode
        opprettBeregningsresultatAndel(gammelPeriode, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR1,1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 1000, OpptjeningAktivitetType.FORELDREPENGER);
        opprettBeregningsresultatAndel(gammelPeriode, true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER,
            ORGNR2,500, BigDecimal.valueOf(100), BigDecimal.valueOf(80), 500, OpptjeningAktivitetType.FORELDREPENGER);
        // Act
        boolean erEndring = sjekkOmPerioderHarEndringIAndeler.sjekk(nyPeriode, gammelPeriode);
        // Assert
        assertThat(erEndring).isFalse();
    }

    @Test
    public void innneholder_ikke_samme_andeler_når_dagsats_er_endret(){
        // Arrange : nyPeriode
        opprettBeregningsresultatAndel(nyPeriode, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER, ORGNR1,1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 1000, OpptjeningAktivitetType.FORELDREPENGER);
        opprettBeregningsresultatAndel(nyPeriode, true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER,
            Inntektskategori.FRILANSER, ORGNR2,500, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 500, OpptjeningAktivitetType.FORELDREPENGER);
        // Arrange : gammelPeriode
        opprettBeregningsresultatAndel(gammelPeriode, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER, ORGNR1,1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 1000, OpptjeningAktivitetType.FORELDREPENGER);
        opprettBeregningsresultatAndel(gammelPeriode, true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER,
            Inntektskategori.FRILANSER, ORGNR2,1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 500, OpptjeningAktivitetType.FORELDREPENGER);
        // Act
        boolean erEndring = sjekkOmPerioderHarEndringIAndeler.sjekk(nyPeriode, gammelPeriode);
        // Assert
        assertThat(erEndring).isTrue();
    }

    @Test
    public void innneholder_ikke_samme_andeler_når_inntektskategori_er_endret(){
        // Arrange : nyPeriode
        opprettBeregningsresultatAndel(nyPeriode, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR1,1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 1000, OpptjeningAktivitetType.FORELDREPENGER);
        opprettBeregningsresultatAndel(nyPeriode, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR2,500, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 500, OpptjeningAktivitetType.FORELDREPENGER);
        // Arrange : gammelPeriode
        opprettBeregningsresultatAndel(gammelPeriode, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR1,1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 1000, OpptjeningAktivitetType.FORELDREPENGER);
        opprettBeregningsresultatAndel(gammelPeriode, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER,
            ORGNR2,500, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 500, OpptjeningAktivitetType.FORELDREPENGER);
        // Act
        boolean erEndring = sjekkOmPerioderHarEndringIAndeler.sjekk(nyPeriode, gammelPeriode);
        // Assert
        assertThat(erEndring).isTrue();
    }

    @Test
    public void innneholder_ikke_samme_andeler_når_aktivtetstatus_er_endret(){
        // Arrange : nyPeriode
        opprettBeregningsresultatAndel(nyPeriode, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR1,1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 1000, OpptjeningAktivitetType.FORELDREPENGER);
        opprettBeregningsresultatAndel(nyPeriode, true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER,
            ORGNR2,500, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 500, OpptjeningAktivitetType.FORELDREPENGER);
        // Arrange : gammelPeriode
        opprettBeregningsresultatAndel(gammelPeriode, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR1,1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 1000, OpptjeningAktivitetType.FORELDREPENGER);
        opprettBeregningsresultatAndel(gammelPeriode, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR2,500, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 500, OpptjeningAktivitetType.FORELDREPENGER);
        // Act
        boolean erEndring = sjekkOmPerioderHarEndringIAndeler.sjekk(nyPeriode, gammelPeriode);
        // Assert
        assertThat(erEndring).isTrue();
    }

    @Test
    public void innneholder_ikke_samme_andeler_når_arbeidsforholdId_er_endret(){
        // Arrange : nyPeriode
        opprettBeregningsresultatAndel(nyPeriode, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR1,1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 1000, OpptjeningAktivitetType.FORELDREPENGER);
        opprettBeregningsresultatAndel(nyPeriode, true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER,
            ORGNR2,500, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 500, OpptjeningAktivitetType.FORELDREPENGER);
        // Arrange : gammelPeriode
        opprettBeregningsresultatAndel(gammelPeriode, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR1,1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 1000, OpptjeningAktivitetType.FORELDREPENGER);
        opprettBeregningsresultatAndel(gammelPeriode, true, InternArbeidsforholdRef.nyRef(), AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER,
            ORGNR2,500, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 500, OpptjeningAktivitetType.FORELDREPENGER);
        // Act
        boolean erEndring = sjekkOmPerioderHarEndringIAndeler.sjekk(nyPeriode, gammelPeriode);
        // Assert
        assertThat(erEndring).isTrue();
    }

    @Test
    public void innneholder_ikke_samme_andeler_når_virksomhet_er_endret(){
        // Arrange : nyPeriode
        opprettBeregningsresultatAndel(nyPeriode, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER, ORGNR1,1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 1000, OpptjeningAktivitetType.FORELDREPENGER);
        opprettBeregningsresultatAndel(nyPeriode, true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER,
            Inntektskategori.FRILANSER, "3",500, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 500, OpptjeningAktivitetType.FORELDREPENGER);
        // Arrange : gammelPeriode
        opprettBeregningsresultatAndel(gammelPeriode, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER, ORGNR1,1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 1000, OpptjeningAktivitetType.FORELDREPENGER);
        opprettBeregningsresultatAndel(gammelPeriode, true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER,
            Inntektskategori.FRILANSER, ORGNR2,500, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 500, OpptjeningAktivitetType.FORELDREPENGER);
        // Act
        boolean erEndring = sjekkOmPerioderHarEndringIAndeler.sjekk(nyPeriode, gammelPeriode);
        // Assert
        assertThat(erEndring).isTrue();
    }

    @Test
    public void innneholder_ikke_samme_andeler_når_erBrukerMottaker_er_endret(){
        // Arrange : nyPeriode
        opprettBeregningsresultatAndel(nyPeriode, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER, ORGNR1,1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 1000, OpptjeningAktivitetType.FORELDREPENGER);
        opprettBeregningsresultatAndel(nyPeriode, true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER,
            Inntektskategori.FRILANSER, ORGNR2,500, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 500, OpptjeningAktivitetType.FORELDREPENGER);
        // Arrange : gammelPeriode
        opprettBeregningsresultatAndel(gammelPeriode, false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER, ORGNR1,1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 1000, OpptjeningAktivitetType.FORELDREPENGER);
        opprettBeregningsresultatAndel(gammelPeriode, true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER,
            Inntektskategori.FRILANSER, ORGNR2,500, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 500, OpptjeningAktivitetType.FORELDREPENGER);
        // Act
        boolean erEndring = sjekkOmPerioderHarEndringIAndeler.sjekk(nyPeriode, gammelPeriode);
        // Assert
        assertThat(erEndring).isTrue();
    }

    @Test
    public void innneholder_ikke_samme_andeler_når_ny_og_gammel_periode_har_forskjellig_antall_andeler(){
        // Arrange : nyPeriode
        opprettBeregningsresultatAndel(nyPeriode, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER, ORGNR1,1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 1000, OpptjeningAktivitetType.FORELDREPENGER);
        // Arrange : gammelPeriode
        opprettBeregningsresultatAndel(gammelPeriode, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER, ORGNR1,1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 1000, OpptjeningAktivitetType.FORELDREPENGER);
        opprettBeregningsresultatAndel(gammelPeriode, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER, ORGNR1,1000, BigDecimal.valueOf(80), BigDecimal.valueOf(80), 1000, OpptjeningAktivitetType.FORELDREPENGER);
        // Act
        boolean erEndring = sjekkOmPerioderHarEndringIAndeler.sjekk(nyPeriode, gammelPeriode);
        // Assert
        assertThat(erEndring).isTrue();
    }

    @Test
    public void skal_kaste_exception_når_det_er_flere_korresponderende_andeler_for_en_andel(){
        // Arrange : nyPeriode
        BeregningsresultatAndel andel = opprettBeregningsresultatAndel(nyPeriode, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER, ORGNR1, 1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 1000, OpptjeningAktivitetType.FORELDREPENGER);
        opprettBeregningsresultatAndel(nyPeriode, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER, ORGNR1,1000, BigDecimal.valueOf(80), BigDecimal.valueOf(80), 1000, OpptjeningAktivitetType.FORELDREPENGER);
        // Arrange : gammelPeriode
        opprettBeregningsresultatAndel(gammelPeriode, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER, ORGNR1,1000, BigDecimal.valueOf(100), BigDecimal.valueOf(100), 1000, OpptjeningAktivitetType.FORELDREPENGER);
        opprettBeregningsresultatAndel(gammelPeriode, true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER, ORGNR1,1000, BigDecimal.valueOf(80), BigDecimal.valueOf(80), 1000, OpptjeningAktivitetType.FORELDREPENGER);
        // Expect
        expectedException.expect(TekniskException.class);
        expectedException.expectMessage(String.format("Fant flere korresponderende andeler for andel med id %s", andel.getId()));
        // Act
        sjekkOmPerioderHarEndringIAndeler.sjekk(nyPeriode, gammelPeriode);
    }

    private BeregningsresultatPeriode opprettBeregningsresultatPeriode(BeregningsresultatEntitet beregningsresultat, LocalDate fom, LocalDate tom){
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

}
