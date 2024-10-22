package no.nav.foreldrepenger.datavarehus.metrikker;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;

class BehandlingStatistikkRepositoryTest {


    @Test
    void sjekkMappingFraFlereBehandlingTyperTilEtSubsett() {
        var test = List.of(
            new BehandlingStatistikkRepository.BehandlingÅrsakQR(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER, 10L),
            new BehandlingStatistikkRepository.BehandlingÅrsakQR(BehandlingÅrsakType.RE_FEIL_I_LOVANDVENDELSE, 10L),
            new BehandlingStatistikkRepository.BehandlingÅrsakQR(BehandlingÅrsakType.RE_FEIL_REGELVERKSFORSTÅELSE, 10L),
            new BehandlingStatistikkRepository.BehandlingÅrsakQR(BehandlingÅrsakType.RE_FEIL_REGELVERKSFORSTÅELSE, 10L),
            new BehandlingStatistikkRepository.BehandlingÅrsakQR(BehandlingÅrsakType.RE_FEIL_REGELVERKSFORSTÅELSE, 10L),
            new BehandlingStatistikkRepository.BehandlingÅrsakQR(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_FØDSEL, 10L),
            new BehandlingStatistikkRepository.BehandlingÅrsakQR(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_SØKERS_REL, 10L),
            new BehandlingStatistikkRepository.BehandlingÅrsakQR(BehandlingÅrsakType.UDEFINERT, 10L),
            new BehandlingStatistikkRepository.BehandlingÅrsakQR(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER, 10000000L),
            new BehandlingStatistikkRepository.BehandlingÅrsakQR(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_FØDSEL, 10L)
        );

        var mapped = BehandlingStatistikkRepository.mapTilBehandlingStatistikk(test);
        assertThat(mapped)
            .containsExactlyInAnyOrder(
                new BehandlingStatistikkRepository.BehandlingStatistikk(BehandlingStatistikkRepository.Behandlingsårsak.MANUELL, 70L),
                new BehandlingStatistikkRepository.BehandlingStatistikk(BehandlingStatistikkRepository.Behandlingsårsak.SØKNAD, 10000010L),
                new BehandlingStatistikkRepository.BehandlingStatistikk(BehandlingStatistikkRepository.Behandlingsårsak.ANNET, 10L)
            );

    }
}
