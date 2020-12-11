package no.nav.foreldrepenger.ytelse.beregning;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.ytelse.beregning.endringsdato.SjekkOmPerioderHarEndringIAndeler;
import no.nav.foreldrepenger.ytelse.beregning.endringsdato.regelmodell.BeregningsresultatAndelEndringModell;
import no.nav.foreldrepenger.ytelse.beregning.endringsdato.regelmodell.BeregningsresultatPeriodeEndringModell;
import no.nav.vedtak.exception.TekniskException;

public class SjekkOmPerioderHarEndringIAndelerTest {

    private static final String ORGNR1 = KUNSTIG_ORG;
    private static final String ORGNR2 = "2";

    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.namedRef("TEST-REF");

    private SjekkOmPerioderHarEndringIAndeler sjekkOmPerioderHarEndringIAndeler;

    @BeforeEach
    void setUp() {
        sjekkOmPerioderHarEndringIAndeler = new SjekkOmPerioderHarEndringIAndeler();
    }

    @Test
    public void innneholder_samme_andeler_når_alt_er_likt() {
        // Arrange : nyPeriode
        var andel1 = opprettBeregningsresultatAndel(false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR1, 1000);
        var andel2 = opprettBeregningsresultatAndel(true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER,
            ORGNR2, 500);
        var nyPeriode = opprettBeregningsresultatPeriode(List.of(andel1, andel2));

        // Arrange : gammelPeriode
        var gammelAndel1 = opprettBeregningsresultatAndel(false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR1, 1000);
        var gammelAndel2 = opprettBeregningsresultatAndel(true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER,
            ORGNR2, 500);
        var gammelPeriode = opprettBeregningsresultatPeriode(List.of(gammelAndel1, gammelAndel2));

        // Act
        boolean erEndring = sjekkOmPerioderHarEndringIAndeler.sjekk(nyPeriode, gammelPeriode);
        // Assert
        assertThat(erEndring).isFalse();
    }

    @Test
    public void inneholder_samme_andeler_når_dagsatsFraBg_er_endret() {
        // Arrange : nyPeriode
        var andel1 = opprettBeregningsresultatAndel(false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR1, 1000);
        var andel2 = opprettBeregningsresultatAndel(true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER,
            ORGNR2, 500);
        var nyPeriode = opprettBeregningsresultatPeriode(List.of(andel1, andel2));

        // Arrange : gammelPeriode
        var gammelAndel1 = opprettBeregningsresultatAndel(false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR1, 1000);
        var gammelAndel2 = opprettBeregningsresultatAndel(true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER,
            ORGNR2, 500);
        var gammelPeriode = opprettBeregningsresultatPeriode(List.of(gammelAndel1, gammelAndel2));

        // Act
        boolean erEndring = sjekkOmPerioderHarEndringIAndeler.sjekk(nyPeriode, gammelPeriode);
        // Assert
        assertThat(erEndring).isFalse();
    }

    @Test
    public void inneholder_samme_andeler_når_OpptjeningAktivitetType_er_endret() {
        // Arrange : nyPeriode
        BeregningsresultatAndelEndringModell andel1 = opprettBeregningsresultatAndel(false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR1, 1000);
        var andel2 = opprettBeregningsresultatAndel(true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER,
            ORGNR2, 500);
        var nyPeriode = opprettBeregningsresultatPeriode(List.of(andel1, andel2));

        // Arrange : gammelPeriode
        var gammelAndel1 = opprettBeregningsresultatAndel(false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR1, 1000);
        var gammelAndel2 = opprettBeregningsresultatAndel(true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER,
            ORGNR2, 500);
        var gammelPeriode = opprettBeregningsresultatPeriode(List.of(gammelAndel1, gammelAndel2));

        // Act
        boolean erEndring = sjekkOmPerioderHarEndringIAndeler.sjekk(nyPeriode, gammelPeriode);
        // Assert
        assertThat(erEndring).isFalse();
    }

    @Test
    public void inneholder_samme_andeler_når_stillingsprosenten_er_endret() {
        // Arrange : nyPeriode
        var andel1 = opprettBeregningsresultatAndel(false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR1, 1000);
        var andel2 = opprettBeregningsresultatAndel(true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER,
            ORGNR2, 500);
        var nyPeriode = opprettBeregningsresultatPeriode(List.of(andel1, andel2));

        // Arrange : gammelPeriode
        var gammelAndel1 = opprettBeregningsresultatAndel(false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR1, 1000);
        var gammelAndel2 = opprettBeregningsresultatAndel(true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER,
            ORGNR2, 500);
        var gammelPeriode = opprettBeregningsresultatPeriode(List.of(gammelAndel1, gammelAndel2));

        // Act
        boolean erEndring = sjekkOmPerioderHarEndringIAndeler.sjekk(nyPeriode, gammelPeriode);
        // Assert
        assertThat(erEndring).isFalse();
    }

    @Test
    public void inneholder_samme_andeler_når_utbetalingsgrad_er_endret() {
        // Arrange : nyPeriode
        var andel1 = opprettBeregningsresultatAndel(false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR1, 1000);
        var andel2 = opprettBeregningsresultatAndel(true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER,
            ORGNR2, 500);
        var nyPeriode = opprettBeregningsresultatPeriode(List.of(andel1, andel2));

        // Arrange : gammelPeriode
        var gammelAndel1 = opprettBeregningsresultatAndel(false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR1, 1000);
        var gammelAndel2 = opprettBeregningsresultatAndel(true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER,
            ORGNR2, 500);
        var gammelPeriode = opprettBeregningsresultatPeriode(List.of(gammelAndel1, gammelAndel2));

        // Act
        boolean erEndring = sjekkOmPerioderHarEndringIAndeler.sjekk(nyPeriode, gammelPeriode);
        // Assert
        assertThat(erEndring).isFalse();
    }

    @Test
    public void innneholder_ikke_samme_andeler_når_dagsats_er_endret() {
        // Arrange : nyPeriode
        var andel1 = opprettBeregningsresultatAndel(false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER, ORGNR1, 1000);
        var andel2 = opprettBeregningsresultatAndel(true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER,
            Inntektskategori.FRILANSER, ORGNR2, 500);
        var nyPeriode = opprettBeregningsresultatPeriode(List.of(andel1, andel2));

        // Arrange : gammelPeriode
        var gammelAndel1 = opprettBeregningsresultatAndel(false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER, ORGNR1, 1000);
        var gammelAndel2 = opprettBeregningsresultatAndel(true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER,
            Inntektskategori.FRILANSER, ORGNR2, 1000);
        var gammelPeriode = opprettBeregningsresultatPeriode(List.of(gammelAndel1, gammelAndel2));

        // Act
        boolean erEndring = sjekkOmPerioderHarEndringIAndeler.sjekk(nyPeriode, gammelPeriode);
        // Assert
        assertThat(erEndring).isTrue();
    }

    @Test
    public void innneholder_ikke_samme_andeler_når_inntektskategori_er_endret() {
        // Arrange : nyPeriode
        var andel1 = opprettBeregningsresultatAndel(false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR1, 1000);
        var andel2 = opprettBeregningsresultatAndel(true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR2, 500);
        var nyPeriode = opprettBeregningsresultatPeriode(List.of(andel1, andel2));

        // Arrange : gammelPeriode
        var gammelAndel1 = opprettBeregningsresultatAndel(false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR1, 1000);
        var gammelAndel2 = opprettBeregningsresultatAndel(true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER,
            ORGNR2, 500);
        var gammelPeriode = opprettBeregningsresultatPeriode(List.of(gammelAndel1, gammelAndel2));

        // Act
        boolean erEndring = sjekkOmPerioderHarEndringIAndeler.sjekk(nyPeriode, gammelPeriode);
        // Assert
        assertThat(erEndring).isTrue();
    }

    @Test
    public void innneholder_ikke_samme_andeler_når_aktivtetstatus_er_endret() {
        // Arrange : nyPeriode
        var andel1 = opprettBeregningsresultatAndel(false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR1, 1000);
        var andel2 = opprettBeregningsresultatAndel(true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER,
            ORGNR2, 500);
        var nyPeriode = opprettBeregningsresultatPeriode(List.of(andel1, andel2));

        // Arrange : gammelPeriode
        var gammelAndel1 = opprettBeregningsresultatAndel(false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR1, 1000);
        var gammelAndel2 = opprettBeregningsresultatAndel(true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR2, 500);
        var gammelPeriode = opprettBeregningsresultatPeriode(List.of(gammelAndel1, gammelAndel2));

        // Act
        boolean erEndring = sjekkOmPerioderHarEndringIAndeler.sjekk(nyPeriode, gammelPeriode);
        // Assert
        assertThat(erEndring).isTrue();
    }

    @Test
    public void innneholder_ikke_samme_andeler_når_arbeidsforholdId_er_endret() {
        // Arrange : nyPeriode
        var andel1 = opprettBeregningsresultatAndel(false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR1, 1000);
        var andel2 = opprettBeregningsresultatAndel(true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER,
            ORGNR2, 500);
        var nyPeriode = opprettBeregningsresultatPeriode(List.of(andel1, andel2));

        // Arrange : gammelPeriode
        var gammelAndel1 = opprettBeregningsresultatAndel(false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER,
            ORGNR1, 1000);
        var gammelAndel2 = opprettBeregningsresultatAndel(true, InternArbeidsforholdRef.nyRef(), AktivitetStatus.FRILANSER, Inntektskategori.FRILANSER,
            ORGNR2, 500);
        var gammelPeriode = opprettBeregningsresultatPeriode(List.of(gammelAndel1, gammelAndel2));

        // Act
        boolean erEndring = sjekkOmPerioderHarEndringIAndeler.sjekk(nyPeriode, gammelPeriode);
        // Assert
        assertThat(erEndring).isTrue();
    }

    @Test
    public void innneholder_ikke_samme_andeler_når_virksomhet_er_endret() {
        // Arrange : nyPeriode
        var andel1 = opprettBeregningsresultatAndel(false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER, ORGNR1, 1000);
        var andel2 = opprettBeregningsresultatAndel(true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER,
            Inntektskategori.FRILANSER, "3", 500);
        var nyPeriode = opprettBeregningsresultatPeriode(List.of(andel1, andel2));

        // Arrange : gammelPeriode
        var gammelAndel1 = opprettBeregningsresultatAndel(false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER, ORGNR1, 1000);
        var gammelAndel2 = opprettBeregningsresultatAndel(true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER,
            Inntektskategori.FRILANSER, ORGNR2, 500);
        var gammelPeriode = opprettBeregningsresultatPeriode(List.of(gammelAndel1, gammelAndel2));

        // Act
        boolean erEndring = sjekkOmPerioderHarEndringIAndeler.sjekk(nyPeriode, gammelPeriode);
        // Assert
        assertThat(erEndring).isTrue();
    }

    @Test
    public void innneholder_ikke_samme_andeler_når_erBrukerMottaker_er_endret() {
        // Arrange : nyPeriode
        var andel1 = opprettBeregningsresultatAndel(true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER, ORGNR1, 1000);
        var andel2 = opprettBeregningsresultatAndel(true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER,
            Inntektskategori.FRILANSER, ORGNR2, 500);
        var nyPeriode = opprettBeregningsresultatPeriode(List.of(andel1, andel2));

        // Arrange : gammelPeriode
        var gammelAndel1 = opprettBeregningsresultatAndel(false, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER, ORGNR1, 1000);
        var gammelAndel2 = opprettBeregningsresultatAndel(true, ARBEIDSFORHOLD_ID, AktivitetStatus.FRILANSER,
            Inntektskategori.FRILANSER, ORGNR2, 500);
        var gammelPeriode = opprettBeregningsresultatPeriode(List.of(gammelAndel1, gammelAndel2));

        // Act
        boolean erEndring = sjekkOmPerioderHarEndringIAndeler.sjekk(nyPeriode, gammelPeriode);
        // Assert
        assertThat(erEndring).isTrue();
    }

    @Test
    public void innneholder_ikke_samme_andeler_når_ny_og_gammel_periode_har_forskjellig_antall_andeler() {
        // Arrange : nyPeriode
        var andel1 = opprettBeregningsresultatAndel(true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER, ORGNR1, 1000);
        var nyPeriode = opprettBeregningsresultatPeriode(List.of(andel1));

        // Arrange : gammelPeriode
        var gammelAndel1 = opprettBeregningsresultatAndel(true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER, ORGNR1, 1000);
        var gammelAndel2 = opprettBeregningsresultatAndel(true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER, ORGNR1, 1000);
        var gammelPeriode = opprettBeregningsresultatPeriode(List.of(gammelAndel1, gammelAndel2));

        // Act
        boolean erEndring = sjekkOmPerioderHarEndringIAndeler.sjekk(nyPeriode, gammelPeriode);
        // Assert
        assertThat(erEndring).isTrue();
    }

    @Test
    public void skal_kaste_exception_når_det_er_flere_korresponderende_andeler_for_en_andel() {
        // Arrange : nyPeriode
        var andel = opprettBeregningsresultatAndel(true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER, ORGNR1, 1000);
        var andel2 = opprettBeregningsresultatAndel(true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER, ORGNR1, 1000);
        var nyPeriode = opprettBeregningsresultatPeriode(List.of(andel, andel2));

        // Arrange : gammelPeriode
        var gammelAndel1 = opprettBeregningsresultatAndel(true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER, ORGNR1, 1000);
        var gammelAndel2 = opprettBeregningsresultatAndel(true, ARBEIDSFORHOLD_ID, AktivitetStatus.ARBEIDSTAKER,
            Inntektskategori.ARBEIDSTAKER, ORGNR1, 1000);
        var gammelPeriode = opprettBeregningsresultatPeriode(List.of(gammelAndel1, gammelAndel2));

        // Act
        assertThatThrownBy(() -> sjekkOmPerioderHarEndringIAndeler.sjekk(nyPeriode, gammelPeriode))
            .isInstanceOf(TekniskException.class)
            .hasMessageContaining(String.format("Fant flere korresponderende andeler for andel %s", andel.toString()));
    }

    private BeregningsresultatPeriodeEndringModell opprettBeregningsresultatPeriode(List<BeregningsresultatAndelEndringModell> andeler) {
        return new BeregningsresultatPeriodeEndringModell(LocalDate.now(), LocalDate.now().plusWeeks(1), andeler);
    }

    private BeregningsresultatAndelEndringModell opprettBeregningsresultatAndel(boolean erBrukerMottaker,
                                                                                InternArbeidsforholdRef arbeidsforholdId, AktivitetStatus aktivitetStatus,
                                                                                Inntektskategori inntektskategori, String orgNr, int dagsats) {
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(orgNr);
        return new BeregningsresultatAndelEndringModell(aktivitetStatus, inntektskategori, arbeidsgiver, arbeidsforholdId, erBrukerMottaker, dagsats);
    }

}
