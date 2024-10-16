package no.nav.foreldrepenger.skjæringstidspunkt.es;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.FamilieHendelseDato;

class BotidCore2024FamilieHendelseDatoTest {

    private static final LocalDate IKRAFT = LocalDate.of(2024, Month.OCTOBER, 1);
    private static final Period OVERGANG = Period.parse("P18W3D");

    @Test
    void skal_returnere_uten_botidskrav_hvis_gjeldende_termin_før_ikrafttredelsedato() {
        var bekreftettermindato = IKRAFT.plusWeeks(10);
        var fhd = FamilieHendelseDato.forFødsel(bekreftettermindato, null);
        assertThat(new BotidCore2024(null, null).ikkeBotidskrav(fhd)).isTrue();
    }

    @Test
    void skal_returnere_uten_botidskrav_hvis_gjeldende_termin_og_fødsel_før_ikrafttredelsedato() {
        var bekreftettermindato = IKRAFT.plusWeeks(10);
        var fødselsdato = IKRAFT.minusWeeks(1);
        var fhd = FamilieHendelseDato.forFødsel(bekreftettermindato, fødselsdato);
        assertThat(new BotidCore2024(null, null).ikkeBotidskrav(fhd)).isTrue();
    }

    @Test
    void skal_returnere_uten_botidskrav_hvis_gjeldende_fødsel_før_ikrafttredelsedato_uten_termin() {
        // Arrange
        var fødselsdato = IKRAFT.minusWeeks(1);
        var fhd = FamilieHendelseDato.forFødsel(null, fødselsdato);
        assertThat(new BotidCore2024(null, null).ikkeBotidskrav(fhd)).isTrue();
    }

    @Test
    void skal_returnere_uten_botidskrav_hvis_gjeldende_adopsjon_før_ikrafttredelsedato() {
        var omsorgsdato = IKRAFT.minusWeeks(1);
        var fhd = FamilieHendelseDato.forAdopsjonOmsorg(omsorgsdato);
        assertThat(new BotidCore2024(null, null).ikkeBotidskrav(fhd)).isTrue();
    }

    @Test
    void skal_returnere_botidskrav_hvis_gjeldende_termin_etter_ikrafttredelsedato() {
        var bekreftettermindato = IKRAFT.plus(OVERGANG).plusWeeks(1);
        var fhd = FamilieHendelseDato.forFødsel(bekreftettermindato, null);
        assertThat(new BotidCore2024(null, null).ikkeBotidskrav(fhd)).isFalse();
    }

    @Test
    void skal_returnere_botidskrav_hvis_gjeldende_termin_og_fødsel_etter_ikrafttredelsedato() {
        var bekreftettermindato = IKRAFT.plus(OVERGANG).plusWeeks(1);
        var fødselsdato = IKRAFT.plus(OVERGANG);
        var fhd = FamilieHendelseDato.forFødsel(bekreftettermindato, fødselsdato);
        assertThat(new BotidCore2024(null, null).ikkeBotidskrav(fhd)).isFalse();
    }

    @Test
    void skal_returnere_botidskrav_hvis_gjeldende_termin_etter_ikrafttredelsedato_fødsel_før() {
        var bekreftettermindato = IKRAFT.plus(OVERGANG).plusWeeks(1);
        var fødselsdato = IKRAFT.minusDays(2);
        var fhd = FamilieHendelseDato.forFødsel(bekreftettermindato, fødselsdato);
        assertThat(new BotidCore2024(null, null).ikkeBotidskrav(fhd)).isFalse();
    }

    @Test
    void skal_returnere_botidskrav_hvis_gjeldende_fødsel_etter_ikrafttredelsedato_uten_termin() {
        var fødselsdato = IKRAFT.plusWeeks(2);
        var fhd = FamilieHendelseDato.forFødsel(null, fødselsdato);
        assertThat(new BotidCore2024(null, null).ikkeBotidskrav(fhd)).isFalse();
    }

    @Test
    void skal_returnere_botidskrav_hvis_gjeldende_adopsjon_etter_ikrafttredelsedato() {
        var omsorgsdato = IKRAFT.plusWeeks(1);
        var fhd = FamilieHendelseDato.forAdopsjonOmsorg(omsorgsdato);
        assertThat(new BotidCore2024(null, null).ikkeBotidskrav(fhd)).isFalse();
    }


}
