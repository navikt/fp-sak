package no.nav.foreldrepenger.web.app.tjenester.infotrygd;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.Grunnlag;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

class InfotrygdMappingTest {

    @Test
    void test_mapForeldrepenger() throws IOException {
        var respons = Optional.ofNullable(getResponse()).orElseThrow();
        var dto = InfotrygdOppslagRestTjeneste.mapTilVedtakDto(respons.stream().distinct().toList());
        assertThat(dto.vedtakKjeder()).hasSize(2);
        assertThat(dto.vedtakKjeder().get(0).vedtak()).hasSize(1);
        assertThat(dto.vedtakKjeder().get(1).vedtak()).hasSize(3);
        System.out.println(DefaultJsonMapper.toPrettyJson(dto));
    }

    private List<Grunnlag> getResponse() {
        var file = new File(getClass().getClassLoader().getResource("infotrygdresponse.json").getFile());
        try {
            var svar = DefaultJsonMapper.getObjectMapper().readValue(file, Grunnlag[].class);
            return svar != null && svar.length > 0 ? Arrays.asList(svar) : List.of();
        } catch (Exception e) {
            //
        }
        return null;

    }

}
