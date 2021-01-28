package no.nav.foreldrepenger.økonomi.ny.mapper;

import java.time.LocalDate;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class OppdragMapperTest {

    @Test
    void skal_mappe_bruk_inntrekk_til_riktig_flag_og_dato() {
        LocalDate omposteringFom = LocalDate.now();
        String saksbehandler = "Saksbehandler";

        var ompostering116 = OppdragMapper.opprettOmpostering116(omposteringFom, true, saksbehandler);

        Assertions.assertThat(ompostering116.getOmPostering()).isEqualTo("J");
        Assertions.assertThat(ompostering116.getDatoOmposterFom()).isEqualTo(omposteringFom);
        Assertions.assertThat(ompostering116.getSaksbehId()).isEqualTo(saksbehandler);
    }

    @Test
    void skal_mappe_ikke_bruk_inntrekk_til_riktig_flag_og_uten_ompostering_fom() {
        LocalDate omposteringFom = LocalDate.now();
        String saksbehandler = "Saksbehandler";

        var ompostering116 = OppdragMapper.opprettOmpostering116(omposteringFom, false, saksbehandler);

        Assertions.assertThat(ompostering116.getOmPostering()).isEqualTo("N");
        Assertions.assertThat(ompostering116.getDatoOmposterFom()).isNull();
        Assertions.assertThat(ompostering116.getSaksbehId()).isEqualTo(saksbehandler);
    }
}
