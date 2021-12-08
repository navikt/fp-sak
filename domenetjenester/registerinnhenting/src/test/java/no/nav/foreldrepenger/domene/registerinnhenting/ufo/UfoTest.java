package no.nav.foreldrepenger.domene.registerinnhenting.ufo;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.domene.json.StandardJsonConfig;

public class UfoTest {

    private static final String UFO_PERIODE = """
        {
            "uforehistorikk": {
                "reaktiviseringFomDato": null,
                "reaktiviseringTomDato": null,
                "uforeperiodeListe": [ {
                    "uforegrad": 100,
                    "uforetidspunkt": "1359673200000",
                    "virk": "1514761200000",
                    "uforetype": {
                        "code": "UFORE",
                        "decode": "Uføre"
                    },
                    "uforetidspunktTom": null,
                    "ufgFom": "1359673200000",
                    "ufgTom": null
                 } ]
              }
        }
        """;

    @Test
    public void ufotest() {
        var dto = StandardJsonConfig.fromJson(UFO_PERIODE, HentUforehistorikkResponseDto.class);
        Uføreperiode uføreperiode = new Uføreperiode(dto.uforehistorikk().uforeperiodeListe().get(0));
        assertThat(uføreperiode.virkningsdato()).isEqualTo(LocalDate.of(2018,1,1));
        System.out.println(uføreperiode);
    }
}
