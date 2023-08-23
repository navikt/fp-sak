package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.BekreftTerminbekreftelseAksjonspunktDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BekreftTerminbekreftelseAksjonspunktDtoTest {

    @Test
    void test_av_json_mapping() {
        var terminbekreftelseAksjonspunktDto = bekreftFødselAksjonspunktDto();

        var json = StandardJsonConfig.toJson(terminbekreftelseAksjonspunktDto);

        var objektFraJson =  StandardJsonConfig.fromJson(json, BekreftTerminbekreftelseAksjonspunktDto.class);

        assertThat(objektFraJson.getAntallBarn()).isEqualTo(terminbekreftelseAksjonspunktDto.getAntallBarn());
        assertThat(objektFraJson.getTermindato()).isEqualTo(terminbekreftelseAksjonspunktDto.getTermindato());
        assertThat(objektFraJson.getUtstedtdato()).isEqualTo(terminbekreftelseAksjonspunktDto.getUtstedtdato());
        assertThat(objektFraJson.getBegrunnelse()).isEqualTo(terminbekreftelseAksjonspunktDto.getBegrunnelse());
    }

    private BekreftTerminbekreftelseAksjonspunktDto bekreftFødselAksjonspunktDto() {
        return new BekreftTerminbekreftelseAksjonspunktDto("Test", LocalDate.now().plusDays(30), LocalDate.now().minusDays(10), 1);
    }

}
