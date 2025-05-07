package no.nav.foreldrepenger.web.app.tjenester.formidling.rest.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.web.app.tjenester.formidling.rest.kodeverk.AvslagÅrsakDto;
import no.nav.foreldrepenger.web.app.tjenester.formidling.rest.kodeverk.BehandlingResultatTypeDto;
import no.nav.foreldrepenger.web.app.tjenester.formidling.rest.kodeverk.KonsekvensForYtelsenDto;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

class BehandlingsresultatDtoTest {

    @Test
    void serdesMaksTest() {
        var now = LocalDate.now();
        var skjæringstidspunkt = new SkjæringstidspunktDto(now, true);
        var resultatType = BehandlingResultatTypeDto.INNVILGET;
        var avslagÅrsak = AvslagÅrsakDto.BARN_IKKE_UNDER_15_ÅR;
        var konsekvenser = List.of(KonsekvensForYtelsenDto.ENDRING_I_BEREGNING, KonsekvensForYtelsenDto.ENDRING_I_FORDELING_AV_YTELSEN);
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
        assertThat(deserialized.getKonsekvenserForYtelsen()).containsExactlyInAnyOrder(konsekvenser.toArray(new KonsekvensForYtelsenDto[0]));
        assertThat(deserialized.getAvslagsarsakFritekst()).isEqualTo(fritekstÅrsakTekst);
        assertThat(deserialized.getOverskrift()).isEqualTo(overskriftFritekst);
        assertThat(deserialized.getFritekstbrev()).isEqualTo(fritekst);
        assertThat(deserialized.getOpphørsdato()).isEqualTo(now);
    }

    @Test
    void serdesMinTest() {
        var now = LocalDate.now();
        var skjæringstidspunkt = new SkjæringstidspunktDto(now, true);
        var resultatType = BehandlingResultatTypeDto.INNVILGET;

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
