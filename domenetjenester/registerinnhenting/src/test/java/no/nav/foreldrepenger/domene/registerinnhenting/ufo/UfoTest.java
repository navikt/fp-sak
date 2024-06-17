package no.nav.foreldrepenger.domene.registerinnhenting.ufo;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.domene.json.StandardJsonConfig;

class UfoTest {

    private static final String UFO_PERIODE = """
        {
            "harUforegrad": true,
            "datoUfor": "2017-04-20",
            "virkDato": "2017-04-20"
        }
        """;

    @Test
    void ufotest() {
        var dto = StandardJsonConfig.fromJson(UFO_PERIODE, HarUføreGrad.class);
        var uføreperiode = new Uføreperiode(dto);
        assertThat(uføreperiode.virkningsdato()).isEqualTo(LocalDate.of(2017, 4, 20));
        System.out.println(uføreperiode);
    }
}
