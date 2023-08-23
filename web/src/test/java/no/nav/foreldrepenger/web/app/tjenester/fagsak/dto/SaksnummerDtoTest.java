package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SaksnummerDtoTest {

    @Test
    void skal_ha_med_saksnummer_til_abac()  {
        assertThat(new SaksnummerDto("1234").getVerdi()).isEqualTo("1234");
    }
}
