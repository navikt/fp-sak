package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.fødsel;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelKjønn;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelSøkerRolle;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FødselsVilkårDocTest {


    private static final String gammelJson = """
        {
            "soekersKjonn" : "KVINNE",
            "bekreftetFoedselsdato" : null,
            "antallBarn" : 1,
            "bekreftetTermindato" : "2021-05-20",
            "soekerRolle" : null,
            "dagensdato" : "2021-04-22",
            "erMorForSykVedFødsel" : false,
            "erSøktOmTermin" : true,
            "erTerminBekreftelseUtstedtEtterXUker" : true
        }
        """;

    @Test
    void kanDeserialisereGammeltFormat() throws JsonProcessingException {
        var gsource = new FødselsvilkårGrunnlag(RegelKjønn.KVINNE, null, LocalDate.of(2021,4,22),
            null, LocalDate.of(2021,5,20), 1,
            false, false, true,
            false, true, false);
        var grunnlag = deserialiser(gammelJson);
        assertThat(grunnlag).isEqualTo(gsource);
    }

    @Test
    void kanSerialisereDeserialisereNyttFormat() throws JsonProcessingException {
        var gsource = new FødselsvilkårGrunnlag(RegelKjønn.MANN, RegelSøkerRolle.FARA, LocalDate.now().minusWeeks(1),
            null, LocalDate.now().plusMonths(1), 1,
            false, false, true,
            true, true, false);
        var serialisert = DefaultJsonMapper.toJson(gsource);
        var grunnlag = deserialiser(serialisert);
        assertThat(grunnlag).isEqualTo(gsource);
    }

    private FødselsvilkårGrunnlag deserialiser(String s) throws JsonProcessingException {
        return DefaultJsonMapper.getObjectMapper().readValue(s, FødselsvilkårGrunnlag.class);
    }

    private static String eldgammelJson = """
        {
          "soekersKjonn" : "KVINNE",
          "bekreftetFoedselsdato" : {
            "year" : 2017,
            "month" : "AUGUST",
            "chronology" : {
              "calendarType" : "iso8601",
              "id" : "ISO"
            },
            "dayOfMonth" : 5,
            "dayOfWeek" : "SATURDAY",
            "era" : "CE",
            "dayOfYear" : 217,
            "leapYear" : false,
            "monthValue" : 8
          },
          "antallBarn" : 1,
          "bekreftetTermindato" : null,
          "soekerRolle" : "MORA",
          "soeknadsdato" : {
            "year" : 2018,
            "month" : "FEBRUARY",
            "chronology" : {
              "calendarType" : "iso8601",
              "id" : "ISO"
            },
            "dayOfMonth" : 5,
            "dayOfWeek" : "MONDAY",
            "era" : "CE",
            "dayOfYear" : 36,
            "leapYear" : false,
            "monthValue" : 2
          }
        }
        """;

    @Test
    void kanSerialisereDeserialisereEldgammeltFormat() throws JsonProcessingException {
        var grunnlag = DefaultJsonMapper.getObjectMapper().readValue(eldgammelJson, FødselsvilkårGrunnlagLegacy.class);
        assertThat(grunnlag.behandlingsdato()).isEqualTo(LocalDate.of(2018,2,5));
        assertThat(grunnlag.bekreftetFødselsdato()).isEqualTo(LocalDate.of(2017,8,5));
    }

}
