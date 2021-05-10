package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class SaksnummerDtoTest {

    @Test
    public void skal_ha_med_saksnummer_til_abac()  {
        assertThat(new SaksnummerDto("1234").getVerdi()).isEqualTo("1234");
    }
}
