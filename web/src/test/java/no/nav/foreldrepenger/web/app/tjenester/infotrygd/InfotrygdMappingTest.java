package no.nav.foreldrepenger.web.app.tjenester.infotrygd;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.Grunnlag;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

class InfotrygdMappingTest {

    private static final String RESPONS = """
        [
          {
            "status": {
              "kode": "A",
              "termnavn": "Avsluttet"
            },
            "tema": {
              "kode": "FA",
              "termnavn": "Foreldrepenger"
            },
            "dekningsgrad": 100,
            "fødselsdatoBarn": "2015-08-20",
            "kategori": {
              "kode": "01",
              "termnavn": "Arbeidstaker"
            },
            "arbeidsforhold": [],
            "periode": {
              "fom": "2015-08-20",
              "tom": "2015-10-08"
            },
            "behandlingstema": {
              "kode": "FØ",
              "termnavn": "Foreldrepenger m/ fødsel"
            },
            "identdato": "2015-08-20",
            "iverksatt": "2015-08-20",
            "opphørFom": "2015-10-09",
            "gradering": null,
            "opprinneligIdentdato": "2015-08-20",
            "registrert": "2015-09-15",
            "saksbehandlerId": "SAKSBEHANDLER",
            "vedtak": []
          },
          {
            "status": {
              "kode": "A",
              "termnavn": "Avsluttet"
            },
            "tema": {
              "kode": "FA",
              "termnavn": "Foreldrepenger"
            },
            "dekningsgrad": 100,
            "fødselsdatoBarn": "2015-08-20",
            "kategori": {
              "kode": "01",
              "termnavn": "Arbeidstaker"
            },
            "arbeidsforhold": [],
            "periode": {
              "fom": "2016-01-07",
              "tom": "2016-07-06"
            },
            "behandlingstema": {
              "kode": "FØ",
              "termnavn": "Foreldrepenger m/ fødsel"
            },
            "identdato": "2016-01-07",
            "iverksatt": "2016-01-07",
            "opphørFom": "2016-07-07",
            "gradering": null,
            "opprinneligIdentdato": "2015-08-20",
            "registrert": "2015-09-21",
            "saksbehandlerId": "SAKSBEHANDLER",
            "vedtak": []
          },
          {
            "status": {
              "kode": "A",
              "termnavn": "Avsluttet"
            },
            "tema": {
              "kode": "FA",
              "termnavn": "Foreldrepenger"
            },
            "dekningsgrad": 100,
            "fødselsdatoBarn": "2015-08-20",
            "kategori": {
              "kode": "01",
              "termnavn": "Arbeidstaker"
            },
            "arbeidsforhold": [],
            "periode": {
              "fom": "2015-12-18",
              "tom": "2016-01-06"
            },
            "behandlingstema": {
              "kode": "FØ",
              "termnavn": "Foreldrepenger m/ fødsel"
            },
            "identdato": "2015-12-18",
            "iverksatt": "2015-12-18",
            "opphørFom": "2016-01-07",
            "gradering": null,
            "opprinneligIdentdato": "2015-08-20",
            "registrert": "2015-09-21",
            "saksbehandlerId": "SAKSBEHANDLER",
            "vedtak": []
          },
          {
            "status": {
              "kode": "A",
              "termnavn": "Avsluttet"
            },
            "tema": {
              "kode": "FA",
              "termnavn": "Foreldrepenger"
            },
            "dekningsgrad": 100,
            "fødselsdatoBarn": "2018-08-20",
            "kategori": {
              "kode": "01",
              "termnavn": "Arbeidstaker"
            },
            "arbeidsforhold": [],
            "periode": {
              "fom": "2018-08-01",
              "tom": "2018-11-06"
            },
            "behandlingstema": {
              "kode": "FØ",
              "termnavn": "Foreldrepenger m/ fødsel"
            },
            "identdato": "2018-08-20",
            "iverksatt": "2018-08-24",
            "opphørFom": "2018-11-07",
            "gradering": null,
            "opprinneligIdentdato": "2018-08-20",
            "registrert": "2018-08-10",
            "saksbehandlerId": "SAKSBEHANDLER",
            "vedtak": []
          }
        ]
        """;


    @Test
    void test_mapForeldrepenger() {
        var respons = DefaultJsonMapper.listFromJson(RESPONS, Grunnlag.class);
        var dto = InfotrygdOppslagRestTjeneste.mapTilGrunnlagDto(respons);
        assertThat(dto.grunnlagPerIdentdato()).hasSize(2);
        assertThat(dto.grunnlagPerIdentdato().get(LocalDate.of(2015, 8, 20))).hasSize(3);
    }

}
