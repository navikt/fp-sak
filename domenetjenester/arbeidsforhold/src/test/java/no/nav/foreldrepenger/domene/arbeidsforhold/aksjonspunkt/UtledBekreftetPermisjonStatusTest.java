package no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.domene.arbeidsforhold.dto.PermisjonDto;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;

public class UtledBekreftetPermisjonStatusTest {

    private static final LocalDate DAGENS_DATO = LocalDate.now();

    @Test
    public void skal_returnere_status_UGYLDIGE_PERIODER_når_dto_inneholder_flere_permisjoner() {
        // Arrange
        var permisjonDto1 = new PermisjonDto(DAGENS_DATO, DAGENS_DATO.plusWeeks(1), BigDecimal.valueOf(100),
                PermisjonsbeskrivelseType.PERMISJON);
        var permisjonDto2 = new PermisjonDto(DAGENS_DATO, null, BigDecimal.valueOf(100), PermisjonsbeskrivelseType.PERMISJON);
        var arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setBrukPermisjon(false);
        arbeidsforholdDto.setPermisjoner(List.of(permisjonDto1, permisjonDto2));
        // Act
        var status = UtledBekreftetPermisjonStatus.utled(arbeidsforholdDto);
        // Assert
        assertThat(status).isEqualTo(BekreftetPermisjonStatus.UGYLDIGE_PERIODER);
    }

    @Test
    public void skal_returnere_status_BRUK_PERMISJON_når_dto_inneholder_en_permisjon_og_bruk_permisjon_er_true() {
        // Arrange
        var permisjonDto = new PermisjonDto(DAGENS_DATO, DAGENS_DATO.plusWeeks(1), BigDecimal.valueOf(100),
                PermisjonsbeskrivelseType.PERMISJON);
        var arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setBrukPermisjon(true);
        arbeidsforholdDto.setPermisjoner(List.of(permisjonDto));
        // Act
        var status = UtledBekreftetPermisjonStatus.utled(arbeidsforholdDto);
        // Assert
        assertThat(status).isEqualTo(BekreftetPermisjonStatus.BRUK_PERMISJON);
    }

    @Test
    public void skal_returnere_status_IKKE_BRUK_PERMISJON_når_dto_inneholder_en_permisjon_og_bruk_permisjon_er_false() {
        // Arrange
        var permisjonDto = new PermisjonDto(DAGENS_DATO, DAGENS_DATO.plusWeeks(1), BigDecimal.valueOf(100),
                PermisjonsbeskrivelseType.PERMISJON);
        var arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setBrukPermisjon(false);
        arbeidsforholdDto.setPermisjoner(List.of(permisjonDto));
        // Act
        var status = UtledBekreftetPermisjonStatus.utled(arbeidsforholdDto);
        // Assert
        assertThat(status).isEqualTo(BekreftetPermisjonStatus.IKKE_BRUK_PERMISJON);
    }

    @Test
    public void skal_returnere_status_UDEFINERT_når_dto_ikke_inneholder_permisjoner_og_bruk_permisjon_er_null() {
        // Arrange
        var arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setBrukPermisjon(null);
        arbeidsforholdDto.setPermisjoner(List.of());
        // Act
        var status = UtledBekreftetPermisjonStatus.utled(arbeidsforholdDto);
        // Assert
        assertThat(status).isEqualTo(BekreftetPermisjonStatus.UDEFINERT);
    }

}
