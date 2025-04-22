package no.nav.foreldrepenger.web.app.tjenester.formidling.rest.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.web.app.tjenester.formidling.rest.kodeverk.AvslagÅrsak;
import no.nav.foreldrepenger.web.app.tjenester.formidling.rest.kodeverk.BehandlingResultatType;
import no.nav.foreldrepenger.web.app.tjenester.formidling.rest.kodeverk.KonsekvensForYtelsen;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

class BehandlingsresultatDtoTest {

    @Test
    void serdesMaksTest() {
        var now = LocalDate.now();
        var skjæringstidspunkt = new SkjæringstidspunktDto(now, true);
        var resultatType = BehandlingResultatType.INNVILGET;
        var avslagÅrsak = AvslagÅrsak.BARN_IKKE_UNDER_15_ÅR;
        var konsekvenser = List.of(KonsekvensForYtelsen.ENDRING_I_BEREGNING, KonsekvensForYtelsen.ENDRING_I_FORDELING_AV_YTELSEN);
        var fritekstÅrsakTekst = "Fritekst årsak";
        var overskriftFritekst = "Overskrift";
        var fritekst = "Fritekst";

        BehandlingsresultatDto dto = new BehandlingsresultatDto();
        dto.setType(resultatType);
        dto.setAvslagsarsak(avslagÅrsak);
        dto.setEndretDekningsgrad(true);
        dto.setSkjæringstidspunkt(skjæringstidspunkt);
        dto.setKonsekvenserForYtelsen(konsekvenser);
        dto.setAvslagsarsakFritekst(fritekstÅrsakTekst);
        dto.setOverskrift(overskriftFritekst);
        dto.setFritekstbrev(fritekst);
        dto.setOpphørsdato(now);

        var json = DefaultJsonMapper.toJson(dto);

        var deserialized = DefaultJsonMapper.fromJson(json, BehandlingsresultatDto.class);

        assertThat(deserialized).isNotNull();
        assertThat(deserialized.getType()).isEqualTo(resultatType);
        assertThat(deserialized.getAvslagsarsak()).isEqualTo(avslagÅrsak);
        assertThat(deserialized.isEndretDekningsgrad()).isTrue();
        assertThat(deserialized.getSkjæringstidspunkt()).isEqualTo(skjæringstidspunkt);
        assertThat(deserialized.getKonsekvenserForYtelsen()).containsExactlyInAnyOrder(konsekvenser.toArray(new KonsekvensForYtelsen[0]));
        assertThat(deserialized.getAvslagsarsakFritekst()).isEqualTo(fritekstÅrsakTekst);
        assertThat(deserialized.getOverskrift()).isEqualTo(overskriftFritekst);
        assertThat(deserialized.getFritekstbrev()).isEqualTo(fritekst);
        assertThat(deserialized.getOpphørsdato()).isEqualTo(now);
    }

    @Test
    void serdesMinTest() {
        var now = LocalDate.now();
        var skjæringstidspunkt = new SkjæringstidspunktDto(now, true);
        var resultatType = BehandlingResultatType.INNVILGET;

        BehandlingsresultatDto dto = new BehandlingsresultatDto();
        dto.setType(resultatType);
        dto.setSkjæringstidspunkt(skjæringstidspunkt);
        dto.setOpphørsdato(now);

        var json = DefaultJsonMapper.toJson(dto);

        var deserialized = DefaultJsonMapper.fromJson(json, BehandlingsresultatDto.class);

        assertThat(deserialized).isNotNull();
        assertThat(deserialized.getType()).isEqualTo(resultatType);
        assertThat(deserialized.isEndretDekningsgrad()).isNull();
        assertThat(deserialized.getSkjæringstidspunkt()).isEqualTo(skjæringstidspunkt);
        assertThat(deserialized.getOpphørsdato()).isEqualTo(now);
    }
}
