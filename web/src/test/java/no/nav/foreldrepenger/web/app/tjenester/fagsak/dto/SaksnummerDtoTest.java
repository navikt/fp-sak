package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;

public class SaksnummerDtoTest {

    @Test
    public void skal_ha_med_saksnummer_til_abac() throws Exception {
        assertThat(new SaksnummerDto("1234").abacAttributter()).isEqualTo(AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.SAKSNUMMER, "1234"));
    }
}
