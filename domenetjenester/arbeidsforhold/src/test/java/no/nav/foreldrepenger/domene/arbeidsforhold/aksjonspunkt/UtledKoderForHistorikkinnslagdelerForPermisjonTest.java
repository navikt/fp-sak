package no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.historikk.VurderArbeidsforholdHistorikkinnslag;

public class UtledKoderForHistorikkinnslagdelerForPermisjonTest {

    @Test
    public void skal_utlede_at_søker_er_i_permisjon_når_bruk_permisjon_er_true() {
        // Arrange
        var arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setBrukPermisjon(true);
        // Act
        var kodeOpt = UtledKoderForHistorikkinnslagdelerForArbeidsforholdMedPermisjon
                .utled(arbeidsforholdDto);
        // Assert
        assertThat(kodeOpt).hasValueSatisfying(kode -> assertThat(kode).isEqualTo(VurderArbeidsforholdHistorikkinnslag.SØKER_ER_I_PERMISJON));

    }

    @Test
    public void skal_utlede_at_søker_ikke_er_i_permisjon_når_bruk_permisjon_er_false() {
        // Arrange
        var arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setBrukPermisjon(false);
        // Act
        var kodeOpt = UtledKoderForHistorikkinnslagdelerForArbeidsforholdMedPermisjon
                .utled(arbeidsforholdDto);
        // Assert
        assertThat(kodeOpt).hasValueSatisfying(kode -> assertThat(kode).isEqualTo(VurderArbeidsforholdHistorikkinnslag.SØKER_ER_IKKE_I_PERMISJON));
    }

    @Test
    public void skal_utlede_optional_empty_når_bruk_permisjon_er_null() {
        // Arrange
        var arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setBrukPermisjon(null);
        // Act
        var kodeOpt = UtledKoderForHistorikkinnslagdelerForArbeidsforholdMedPermisjon
                .utled(arbeidsforholdDto);
        // Assert
        assertThat(kodeOpt).isEmpty();
    }

}
