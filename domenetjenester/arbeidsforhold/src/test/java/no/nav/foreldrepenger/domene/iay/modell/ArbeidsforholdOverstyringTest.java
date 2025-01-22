package no.nav.foreldrepenger.domene.iay.modell;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

class ArbeidsforholdOverstyringTest {

    private static final String ORGNR = "000000000";
    private static final LocalDate DAGENS_DATO = LocalDate.now();

    @Test
    void skal_kopiere_over_verdier_fra_gammel_entitet_til_ny_entitet() {

        // Arrange
        var gammelEntitet = new ArbeidsforholdOverstyring();
        var bekreftetPermisjon = new BekreftetPermisjon(DAGENS_DATO.minusDays(1), DAGENS_DATO,
                BekreftetPermisjonStatus.BRUK_PERMISJON);

        gammelEntitet.setArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR));
        gammelEntitet.setArbeidsforholdRef(InternArbeidsforholdRef.nyRef());
        gammelEntitet.setHandling(ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE);
        gammelEntitet.setNyArbeidsforholdRef(InternArbeidsforholdRef.nyRef());
        gammelEntitet.leggTilOverstyrtPeriode(DAGENS_DATO.plusDays(1), DAGENS_DATO.plusDays(2));
        gammelEntitet.leggTilOverstyrtPeriode(DAGENS_DATO.plusDays(3), DAGENS_DATO.plusDays(4));
        gammelEntitet.leggTilOverstyrtPeriode(DAGENS_DATO.plusDays(5), DAGENS_DATO.plusDays(6));
        gammelEntitet.setBekreftetPermisjon(bekreftetPermisjon);

        // Act
        var nyEntitet = new ArbeidsforholdOverstyring(gammelEntitet);

        // Assert
        assertThat(nyEntitet.getArbeidsgiver()).isEqualTo(gammelEntitet.getArbeidsgiver());
        assertThat(nyEntitet.getArbeidsforholdRef()).isEqualTo(gammelEntitet.getArbeidsforholdRef());
        assertThat(nyEntitet.getHandling()).isEqualTo(gammelEntitet.getHandling());
        assertThat(nyEntitet.getNyArbeidsforholdRef()).isEqualTo(gammelEntitet.getNyArbeidsforholdRef());
        assertThat(nyEntitet.getBekreftetPermisjon()).isEqualTo(Optional.of(bekreftetPermisjon));
        var nyPerioder = nyEntitet.getArbeidsforholdOverstyrtePerioder();
        var gamlePerioder = gammelEntitet.getArbeidsforholdOverstyrtePerioder();

        assertThat(nyPerioder)
            .hasSameSizeAs(gamlePerioder)
            .anySatisfy(p -> {
                assertThat(p.getOverstyrtePeriode().getFomDato()).isEqualTo(DAGENS_DATO.plusDays(1));
                assertThat(p.getOverstyrtePeriode().getTomDato()).isEqualTo(DAGENS_DATO.plusDays(2));
            }).anySatisfy(p -> {
                assertThat(p.getOverstyrtePeriode().getFomDato()).isEqualTo(DAGENS_DATO.plusDays(3));
                assertThat(p.getOverstyrtePeriode().getTomDato()).isEqualTo(DAGENS_DATO.plusDays(4));
            }).anySatisfy(p -> {
                assertThat(p.getOverstyrtePeriode().getFomDato()).isEqualTo(DAGENS_DATO.plusDays(5));
                assertThat(p.getOverstyrtePeriode().getTomDato()).isEqualTo(DAGENS_DATO.plusDays(6));
            });
    }

    @Test
    void er_overstyrt_skal_returnere_true_hvis_handling_ikke_er_BRUK() {
        // Arrange
        var overstyring = new ArbeidsforholdOverstyring();
        overstyring.setHandling(ArbeidsforholdHandlingType.BRUK_UTEN_INNTEKTSMELDING);
        // Act
        var erOverstyrt = overstyring.erOverstyrt();
        // Assert
        assertThat(erOverstyrt).isTrue();
    }

    @Test
    void er_overstyrt_skal_returnere_true_hvis_handling_er_BRUK_og_med_bekreftet_permisjon_som_ikke_er_udefinert() {
        // Arrange
        var overstyring = new ArbeidsforholdOverstyring();
        overstyring.setHandling(ArbeidsforholdHandlingType.BRUK);
        overstyring.setBekreftetPermisjon(new BekreftetPermisjon(BekreftetPermisjonStatus.BRUK_PERMISJON));
        // Act
        var erOverstyrt = overstyring.erOverstyrt();
        // Assert
        assertThat(erOverstyrt).isTrue();
    }

    @Test
    void er_overstyrt_skal_returnere_false_hvis_handling_er_BRUK_og_med_bekreftet_permisjon_som_er_udefinert() {
        // Arrange
        var overstyring = new ArbeidsforholdOverstyring();
        overstyring.setHandling(ArbeidsforholdHandlingType.BRUK);
        overstyring.setBekreftetPermisjon(new BekreftetPermisjon());
        // Act
        var erOverstyrt = overstyring.erOverstyrt();
        // Assert
        assertThat(erOverstyrt).isFalse();
    }

    @Test
    void er_overstyrt_skal_returnere_true_hvis_handling_er_BRUK_MED_OVERSTYRT_PERIODE() {
        // Arrange
        var overstyring = new ArbeidsforholdOverstyring();
        overstyring.setHandling(ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE);
        // Act
        var erOverstyrt = overstyring.erOverstyrt();
        // Assert
        assertThat(erOverstyrt).isTrue();
    }

    @Test
    void er_overstyrt_skal_returnere_true_hvis_handling_er_INNTEKT_IKKE_MED_I_BG() {
        // Arrange
        var overstyring = new ArbeidsforholdOverstyring();
        overstyring.setHandling(ArbeidsforholdHandlingType.INNTEKT_IKKE_MED_I_BG);
        // Act
        var erOverstyrt = overstyring.erOverstyrt();
        // Assert
        assertThat(erOverstyrt).isTrue();
    }

    @Test
    void krever_ikke_inntektsmelding_skal_returne_false_hvis_handling_er_lik_BRUK() {
        // Arrange
        var overstyring = new ArbeidsforholdOverstyring();
        overstyring.setHandling(ArbeidsforholdHandlingType.BRUK);
        // Act
        var kreverIkkeInntektsmelding = overstyring.kreverIkkeInntektsmelding();
        // Assert
        assertThat(kreverIkkeInntektsmelding).isFalse();
    }

    @Test
    void krever_ikke_inntektsmelding_skal_returne_false_hvis_handling_er_lik_SLÅTT_SAMMEN_MED_ANNET() {
        // Arrange
        var overstyring = new ArbeidsforholdOverstyring();
        overstyring.setHandling(ArbeidsforholdHandlingType.SLÅTT_SAMMEN_MED_ANNET);
        // Act
        var kreverIkkeInntektsmelding = overstyring.kreverIkkeInntektsmelding();
        // Assert
        assertThat(kreverIkkeInntektsmelding).isFalse();
    }

    @Test
    void krever_ikke_inntektsmelding_skal_returne_false_hvis_handling_er_lik_IKKE_BRUK() {
        // Arrange
        var overstyring = new ArbeidsforholdOverstyring();
        overstyring.setHandling(ArbeidsforholdHandlingType.IKKE_BRUK);
        // Act
        var kreverIkkeInntektsmelding = overstyring.kreverIkkeInntektsmelding();
        // Assert
        assertThat(kreverIkkeInntektsmelding).isFalse();
    }

    @Test
    void krever_ikke_inntektsmelding_skal_returne_false_hvis_handling_er_lik_NYTT_ARBEIDSFORHOLD() {
        // Arrange
        var overstyring = new ArbeidsforholdOverstyring();
        overstyring.setHandling(ArbeidsforholdHandlingType.NYTT_ARBEIDSFORHOLD);
        // Act
        var kreverIkkeInntektsmelding = overstyring.kreverIkkeInntektsmelding();
        // Assert
        assertThat(kreverIkkeInntektsmelding).isFalse();
    }

    @Test
    void krever_ikke_inntektsmelding_skal_returne_false_hvis_handling_er_lik_UDEFINERT() {
        // Arrange
        var overstyring = new ArbeidsforholdOverstyring();
        overstyring.setHandling(ArbeidsforholdHandlingType.UDEFINERT);
        // Act
        var kreverIkkeInntektsmelding = overstyring.kreverIkkeInntektsmelding();
        // Assert
        assertThat(kreverIkkeInntektsmelding).isFalse();
    }

    @Test
    void krever_ikke_inntektsmelding_skal_returne_true_hvis_handling_er_lik_BRUK_UTEN_INNTEKTSMELDING() {
        // Arrange
        var overstyring = new ArbeidsforholdOverstyring();
        overstyring.setHandling(ArbeidsforholdHandlingType.BRUK_UTEN_INNTEKTSMELDING);
        // Act
        var kreverIkkeInntektsmelding = overstyring.kreverIkkeInntektsmelding();
        // Assert
        assertThat(kreverIkkeInntektsmelding).isTrue();
    }

    @Test
    void krever_ikke_inntektsmelding_skal_returne_true_hvis_handling_er_lik_LAGT_TIL_AV_SAKSBEHANDLER() {
        // Arrange
        var overstyring = new ArbeidsforholdOverstyring();
        overstyring.setHandling(ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER);
        // Act
        var kreverIkkeInntektsmelding = overstyring.kreverIkkeInntektsmelding();
        // Assert
        assertThat(kreverIkkeInntektsmelding).isTrue();
    }

    @Test
    void krever_ikke_inntektsmelding_skal_returne_true_hvis_handling_er_lik_BRUK_MED_OVERSTYRT_PERIODE() {
        // Arrange
        var overstyring = new ArbeidsforholdOverstyring();
        overstyring.setHandling(ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE);
        // Act
        var kreverIkkeInntektsmelding = overstyring.kreverIkkeInntektsmelding();
        // Assert
        assertThat(kreverIkkeInntektsmelding).isTrue();
    }

    @Test
    void krever_ikke_inntektsmelding_skal_returne_true_hvis_handling_er_lik_INNTEKT_IKKE_MED_I_BG() {
        // Arrange
        var overstyring = new ArbeidsforholdOverstyring();
        overstyring.setHandling(ArbeidsforholdHandlingType.INNTEKT_IKKE_MED_I_BG);
        // Act
        var kreverIkkeInntektsmelding = overstyring.kreverIkkeInntektsmelding();
        // Assert
        assertThat(kreverIkkeInntektsmelding).isTrue();
    }

}
