package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.BekreftTerminbekreftelseAksjonspunktDto;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

public class BekreftTerminbekreftelseAksjonspunktDtoTest {

    @Test
    public void test_av_json_mapping() {
        var terminbekreftelseAksjonspunktDto = bekreftFødselAksjonspunktDto();

        var json = DefaultJsonMapper.toJson(terminbekreftelseAksjonspunktDto);

        var objektFraJson =  DefaultJsonMapper.fromJson(json, BekreftTerminbekreftelseAksjonspunktDto.class);

        assertThat(objektFraJson.getAntallBarn()).isEqualTo(terminbekreftelseAksjonspunktDto.getAntallBarn());
        assertThat(objektFraJson.getTermindato()).isEqualTo(terminbekreftelseAksjonspunktDto.getTermindato());
        assertThat(objektFraJson.getUtstedtdato()).isEqualTo(terminbekreftelseAksjonspunktDto.getUtstedtdato());
        assertThat(objektFraJson.getBegrunnelse()).isEqualTo(terminbekreftelseAksjonspunktDto.getBegrunnelse());
    }

    private BekreftTerminbekreftelseAksjonspunktDto bekreftFødselAksjonspunktDto() {
        return new BekreftTerminbekreftelseAksjonspunktDto("Test", LocalDate.now().plusDays(30), LocalDate.now().minusDays(10), 1);
    }

}
