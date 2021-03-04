package no.nav.foreldrepenger.økonomistøtte.ny.mapper;

import java.time.LocalDate;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class OppdragMapperTest {

    @Test
    void skal_mappe_bruk_inntrekk_til_riktig_flag_og_dato() {
        LocalDate omposteringFom = LocalDate.now();

        var ompostering116 = OppdragMapper.opprettOmpostering116(omposteringFom, true);

        Assertions.assertThat(ompostering116.getOmPostering()).isTrue();
        Assertions.assertThat(ompostering116.getDatoOmposterFom()).isEqualTo(omposteringFom);
    }

    @Test
    void skal_mappe_ikke_bruk_inntrekk_til_riktig_flag_og_uten_ompostering_fom() {
        LocalDate omposteringFom = LocalDate.now();

        var ompostering116 = OppdragMapper.opprettOmpostering116(omposteringFom, false);

        Assertions.assertThat(ompostering116.getOmPostering()).isFalse();
        Assertions.assertThat(ompostering116.getDatoOmposterFom()).isNull();
    }
}
