package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.fødsel;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.fødsel.dto.SjekkTerminbekreftelseAksjonspunktDto;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

class SjekkTerminbekreftelseAksjonspunktDtoTest {

    @Test
    void test_av_json_mapping() {
        var terminbekreftelseAksjonspunktDto = bekreftFødselAksjonspunktDto();

        var json = DefaultJsonMapper.toJson(terminbekreftelseAksjonspunktDto);

        var objektFraJson =  DefaultJsonMapper.fromJson(json, SjekkTerminbekreftelseAksjonspunktDto.class);

        assertThat(objektFraJson.getAntallBarn()).isEqualTo(terminbekreftelseAksjonspunktDto.getAntallBarn());
        assertThat(objektFraJson.getTermindato()).isEqualTo(terminbekreftelseAksjonspunktDto.getTermindato());
        assertThat(objektFraJson.getUtstedtdato()).isEqualTo(terminbekreftelseAksjonspunktDto.getUtstedtdato());
        assertThat(objektFraJson.getBegrunnelse()).isEqualTo(terminbekreftelseAksjonspunktDto.getBegrunnelse());
    }

    private SjekkTerminbekreftelseAksjonspunktDto bekreftFødselAksjonspunktDto() {
        return new SjekkTerminbekreftelseAksjonspunktDto("Test", LocalDate.now().plusDays(30), LocalDate.now().minusDays(10), 1);
    }

}
