package no.nav.foreldrepenger.datavarehus.metrikker;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

class BehandlingStatistikkRepositoryTest {


    @Test
    void sjekkMappingFraFlereBehandlingTyperTilEtSubsett() {
        var test = List.of(
            new BehandlingStatistikkRepository.BehandlingStatistikkEntitet(FagsakYtelseType.FORELDREPENGER, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER, 10L),
            new BehandlingStatistikkRepository.BehandlingStatistikkEntitet(FagsakYtelseType.FORELDREPENGER, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingÅrsakType.RE_FEIL_I_LOVANDVENDELSE, 10L),
            new BehandlingStatistikkRepository.BehandlingStatistikkEntitet(FagsakYtelseType.FORELDREPENGER, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingÅrsakType.RE_FEIL_REGELVERKSFORSTÅELSE, 10L),
            new BehandlingStatistikkRepository.BehandlingStatistikkEntitet(FagsakYtelseType.FORELDREPENGER, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_FØDSEL, 10L),
            new BehandlingStatistikkRepository.BehandlingStatistikkEntitet(FagsakYtelseType.SVANGERSKAPSPENGER, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_FØDSEL, 10L),
            new BehandlingStatistikkRepository.BehandlingStatistikkEntitet(FagsakYtelseType.ENGANGSTØNAD, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_SØKERS_REL, 10L)
        );

        var mapped = BehandlingStatistikkRepository.mapTilBehandlingStatistikk(test);
        assertThat(mapped).hasSize(4)
            .containsExactlyInAnyOrder(
                new BehandlingStatistikkRepository.BehandlingStatistikk(FagsakYtelseType.FORELDREPENGER, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingStatistikkRepository.Behandlingsårsak.SØKNAD, 10L),
                new BehandlingStatistikkRepository.BehandlingStatistikk(FagsakYtelseType.FORELDREPENGER, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingStatistikkRepository.Behandlingsårsak.MANUELL, 30L),
                new BehandlingStatistikkRepository.BehandlingStatistikk(FagsakYtelseType.SVANGERSKAPSPENGER, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingStatistikkRepository.Behandlingsårsak.MANUELL, 10L),
                new BehandlingStatistikkRepository.BehandlingStatistikk(FagsakYtelseType.ENGANGSTØNAD, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingStatistikkRepository.Behandlingsårsak.MANUELL, 10L)
            );

    }
}
