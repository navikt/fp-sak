package no.nav.foreldrepenger.web.app.tjenester.infotrygd;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.Grunnlag;
import no.nav.vedtak.felles.integrasjon.infotrygd.saker.v1.respons.InfotrygdSak;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

class InfotrygdMappingTest {

    @Test
    void test_mapForeldrepenger() throws IOException {
        var dto = InfotrygdOppslagRestTjeneste.mapTilVedtakDto(getSakResponse(), getResponse().stream().distinct().toList());
        assertThat(dto.saker()).hasSize(1);
        assertThat(dto.saker().getFirst().sakId()).isEqualTo("B01");
        assertThat(dto.vedtakKjeder()).hasSize(2);
        assertThat(dto.vedtakKjeder().get(0).vedtak()).hasSize(1);
        assertThat(dto.vedtakKjeder().get(1).vedtak()).hasSize(3);
        System.out.println(DefaultJsonMapper.toPrettyJson(dto));
    }


    private List<Grunnlag> getResponse() throws IOException {
        var file = new File(getClass().getClassLoader().getResource("infotrygdresponse.json").getFile());
        var svar = DefaultJsonMapper.getObjectMapper().readValue(file, Grunnlag[].class);
        return svar != null && svar.length > 0 ? Arrays.asList(svar) : List.of();
    }

    private List<InfotrygdSak> getSakResponse() throws IOException {
        var file = new File(getClass().getClassLoader().getResource("infotrygdresponsesak.json").getFile());
        var svar = DefaultJsonMapper.getObjectMapper().readValue(file, InfotrygdSak[].class);
        return svar != null && svar.length > 0 ? Arrays.asList(svar) : List.of();
    }

}
