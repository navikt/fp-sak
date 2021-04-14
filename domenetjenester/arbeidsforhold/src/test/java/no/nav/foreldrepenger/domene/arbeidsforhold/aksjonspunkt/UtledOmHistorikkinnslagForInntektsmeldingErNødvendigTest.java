package no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

public class UtledOmHistorikkinnslagForInntektsmeldingErNødvendigTest {

    @Test
    public void skal_returne_false_når_inntektsmelding_er_mottatt() {
        // Arrange
        var arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setMottattDatoInntektsmelding(LocalDate.now());
        // Act
        var erNødvendig = UtledOmHistorikkinnslagForInntektsmeldingErNødvendig.utled(arbeidsforholdDto, Optional.empty());
        // Assert
        assertThat(erNødvendig).isFalse();
    }

    @Test
    public void skal_returne_false_når_permisjon_skal_brukes() {
        // Arrange
        var arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setBrukPermisjon(true);
        // Act
        var erNødvendig = UtledOmHistorikkinnslagForInntektsmeldingErNødvendig.utled(arbeidsforholdDto, Optional.empty());
        // Assert
        assertThat(erNødvendig).isFalse();
    }

    @Test
    public void skal_returne_false_når_arbeidsforhold_fom_dato_er_lik_stp() {
        // Arrange
        var arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setFomDato(LocalDate.now());
        // Act
        var erNødvendig = UtledOmHistorikkinnslagForInntektsmeldingErNødvendig.utled(arbeidsforholdDto, Optional.of(LocalDate.now()));
        // Assert
        assertThat(erNødvendig).isFalse();
    }

    @Test
    public void skal_returne_false_når_arbeidsforhold_fom_dato_er_etter_stp() {
        // Arrange
        var arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setFomDato(LocalDate.now().plusDays(1));
        // Act
        var erNødvendig = UtledOmHistorikkinnslagForInntektsmeldingErNødvendig.utled(arbeidsforholdDto, Optional.of(LocalDate.now()));
        // Assert
        assertThat(erNødvendig).isFalse();
    }

    @Test
    public void skal_returne_true_når_arbeidsforholdet_ikke_har_mottat_IM_og_arbeidsforhold_starter_før_stp_og_ingen_permisjon() {
        // Arrange
        var arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setFomDato(LocalDate.now().minusDays(1));
        arbeidsforholdDto.setMottattDatoInntektsmelding(null);
        arbeidsforholdDto.setBrukPermisjon(null);
        // Act
        var erNødvendig = UtledOmHistorikkinnslagForInntektsmeldingErNødvendig.utled(arbeidsforholdDto, Optional.of(LocalDate.now()));
        // Assert
        assertThat(erNødvendig).isTrue();
    }

    @Test
    public void skal_returne_true_når_IM_ikke_mottatt_og_ingen_permisjon_og_ingen_stp() {
        // Arrange
        var arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setFomDato(LocalDate.now().plusDays(1));
        arbeidsforholdDto.setMottattDatoInntektsmelding(null);
        arbeidsforholdDto.setBrukPermisjon(null);
        // Act
        var erNødvendig = UtledOmHistorikkinnslagForInntektsmeldingErNødvendig.utled(arbeidsforholdDto, Optional.empty());
        // Assert
        assertThat(erNødvendig).isTrue();
    }

}
