package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.SjekkTerminbekreftelseAksjonspunktDto;

class SjekkTerminbekreftelseAksjonspunktDtoTest {

    @Test
    void test_av_json_mapping() {
        var terminbekreftelseAksjonspunktDto = bekreftFødselAksjonspunktDto();

        var json = StandardJsonConfig.toJson(terminbekreftelseAksjonspunktDto);

        var objektFraJson =  StandardJsonConfig.fromJson(json, SjekkTerminbekreftelseAksjonspunktDto.class);

        assertThat(objektFraJson.getAntallBarn()).isEqualTo(terminbekreftelseAksjonspunktDto.getAntallBarn());
        assertThat(objektFraJson.getTermindato()).isEqualTo(terminbekreftelseAksjonspunktDto.getTermindato());
        assertThat(objektFraJson.getUtstedtdato()).isEqualTo(terminbekreftelseAksjonspunktDto.getUtstedtdato());
        assertThat(objektFraJson.getBegrunnelse()).isEqualTo(terminbekreftelseAksjonspunktDto.getBegrunnelse());
    }

    private SjekkTerminbekreftelseAksjonspunktDto bekreftFødselAksjonspunktDto() {
        return new SjekkTerminbekreftelseAksjonspunktDto("Test", LocalDate.now().plusDays(30), LocalDate.now().minusDays(10), 1);
    }

}
