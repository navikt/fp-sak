package no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt;

import static no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING;
import static no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType.BRUK;
import static no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE;
import static no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType.BRUK_UTEN_INNTEKTSMELDING;
import static no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType.IKKE_BRUK;
import static no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType.INNTEKT_IKKE_MED_I_BG;
import static no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER;
import static no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType.NYTT_ARBEIDSFORHOLD;
import static no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType.SLÅTT_SAMMEN_MED_ANNET;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

public class ArbeidsforholdHandlingTypeUtlederTest {

    @Test
    public void skal_utlede_INNTEKT_IKKE_MED_I_BG() {
        var arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setInntektMedTilBeregningsgrunnlag(false);
        arbeidsforholdDto.setBrukArbeidsforholdet(true);

        // Act
        var resultat = ArbeidsforholdHandlingTypeUtleder.utledHandling(arbeidsforholdDto);

        // Assert
        assertThat(resultat).isEqualTo(INNTEKT_IKKE_MED_I_BG);
    }

    @Test
    public void skal_utlede_LAGT_TIL_AV_SAKSBEHANDLER() {
        var arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setLagtTilAvSaksbehandler(true);
        arbeidsforholdDto.setBrukArbeidsforholdet(true);

        // Act
        var resultat = ArbeidsforholdHandlingTypeUtleder.utledHandling(arbeidsforholdDto);

        // Assert
        assertThat(resultat).isEqualTo(LAGT_TIL_AV_SAKSBEHANDLER);
    }

    @Test
    public void skal_utlede_BASERT_PÅ_INNTEKTSMELDING() {
        var arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setBasertPaInntektsmelding(true);
        arbeidsforholdDto.setBrukArbeidsforholdet(true);

        // Act
        var resultat = ArbeidsforholdHandlingTypeUtleder.utledHandling(arbeidsforholdDto);

        // Assert
        assertThat(resultat).isEqualTo(BASERT_PÅ_INNTEKTSMELDING);
    }

    @Test
    public void skal_utlede_BRUK_MED_OVERSTYRT_PERIODE() {
        var arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setOverstyrtTom(LocalDate.now());
        arbeidsforholdDto.setBrukArbeidsforholdet(true);

        // Act
        var resultat = ArbeidsforholdHandlingTypeUtleder.utledHandling(arbeidsforholdDto);

        // Assert
        assertThat(resultat).isEqualTo(BRUK_MED_OVERSTYRT_PERIODE);
    }

    @Test
    public void skal_utlede_BRUK_UTEN_INNTEKTSMELDING() {
        var arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setFortsettBehandlingUtenInntektsmelding(true);
        arbeidsforholdDto.setBrukArbeidsforholdet(true);

        // Act
        var resultat = ArbeidsforholdHandlingTypeUtleder.utledHandling(arbeidsforholdDto);

        // Assert
        assertThat(resultat).isEqualTo(BRUK_UTEN_INNTEKTSMELDING);
    }

    @Test
    public void skal_utlede_SLÅTT_SAMMEN_MED_ANNET() {
        var arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setBrukArbeidsforholdet(true);
        arbeidsforholdDto.setErstatterArbeidsforholdId("1");

        // Act
        var resultat = ArbeidsforholdHandlingTypeUtleder.utledHandling(arbeidsforholdDto);

        // Assert
        assertThat(resultat).isEqualTo(SLÅTT_SAMMEN_MED_ANNET);
    }

    @Test
    public void skal_utlede_NYTT_ARBEIDSFORHOLD() {
        var arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setBrukArbeidsforholdet(true);
        arbeidsforholdDto.setErNyttArbeidsforhold(true);

        // Act
        var resultat = ArbeidsforholdHandlingTypeUtleder.utledHandling(arbeidsforholdDto);

        // Assert
        assertThat(resultat).isEqualTo(NYTT_ARBEIDSFORHOLD);
    }

    @Test
    public void skal_utlede_BRUK() {
        var arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setBrukArbeidsforholdet(true);

        // Act
        var resultat = ArbeidsforholdHandlingTypeUtleder.utledHandling(arbeidsforholdDto);

        // Assert
        assertThat(resultat).isEqualTo(BRUK);
    }

    @Test
    public void skal_utlede_IKKE_BRUK_hvis_false() {
        var arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setBrukArbeidsforholdet(false);

        // Act
        var resultat = ArbeidsforholdHandlingTypeUtleder.utledHandling(arbeidsforholdDto);

        // Assert
        assertThat(resultat).isEqualTo(IKKE_BRUK);
    }

    @Test
    public void skal_utlede_IKKE_BRUK_hvis_null() {
        var arbeidsforholdDto = new ArbeidsforholdDto();

        // Act
        var resultat = ArbeidsforholdHandlingTypeUtleder.utledHandling(arbeidsforholdDto);

        // Assert
        assertThat(resultat).isEqualTo(IKKE_BRUK);
    }
}
