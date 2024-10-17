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
            new BehandlingStatistikkRepository.BehandlingStatistikkEntitet(FagsakYtelseType.FORELDREPENGER, BehandlingType.FØRSTEGANGSSØKNAD, null, 10L), // Mappes til BehandlingÅrsakType.SØKNAD
            new BehandlingStatistikkRepository.BehandlingStatistikkEntitet(FagsakYtelseType.FORELDREPENGER, BehandlingType.FØRSTEGANGSSØKNAD, null, 10L), // Mappes til BehandlingÅrsakType.SØKNAD
            new BehandlingStatistikkRepository.BehandlingStatistikkEntitet(FagsakYtelseType.FORELDREPENGER, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER, 10L),
            new BehandlingStatistikkRepository.BehandlingStatistikkEntitet(FagsakYtelseType.FORELDREPENGER, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingÅrsakType.RE_FEIL_I_LOVANDVENDELSE, 10L),
            new BehandlingStatistikkRepository.BehandlingStatistikkEntitet(FagsakYtelseType.FORELDREPENGER, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingÅrsakType.RE_FEIL_REGELVERKSFORSTÅELSE, 10L),
            new BehandlingStatistikkRepository.BehandlingStatistikkEntitet(FagsakYtelseType.FORELDREPENGER, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingÅrsakType.RE_FEIL_REGELVERKSFORSTÅELSE, 10L),
            new BehandlingStatistikkRepository.BehandlingStatistikkEntitet(FagsakYtelseType.FORELDREPENGER, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingÅrsakType.RE_FEIL_REGELVERKSFORSTÅELSE, 10L),
            new BehandlingStatistikkRepository.BehandlingStatistikkEntitet(FagsakYtelseType.FORELDREPENGER, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_FØDSEL, 10L),
            new BehandlingStatistikkRepository.BehandlingStatistikkEntitet(FagsakYtelseType.ENGANGSTØNAD, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_SØKERS_REL, 10L),
            new BehandlingStatistikkRepository.BehandlingStatistikkEntitet(FagsakYtelseType.ENGANGSTØNAD, BehandlingType.REVURDERING, BehandlingÅrsakType.UDEFINERT, 10L),
            new BehandlingStatistikkRepository.BehandlingStatistikkEntitet(FagsakYtelseType.ENGANGSTØNAD, BehandlingType.REVURDERING, null, 10L),
            new BehandlingStatistikkRepository.BehandlingStatistikkEntitet(FagsakYtelseType.SVANGERSKAPSPENGER, BehandlingType.REVURDERING, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER, 10000000L),
            new BehandlingStatistikkRepository.BehandlingStatistikkEntitet(FagsakYtelseType.SVANGERSKAPSPENGER, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_FØDSEL, 10L)



        );

        var mapped = BehandlingStatistikkRepository.    mapTilBehandlingStatistikk(test);
        assertThat(mapped)
            .containsExactlyInAnyOrder(
                new BehandlingStatistikkRepository.BehandlingStatistikk(new BehandlingStatistikkRepository.BehandlingStatistikk.Type(FagsakYtelseType.FORELDREPENGER, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingStatistikkRepository.Behandlingsårsak.SØKNAD), 30L),
                new BehandlingStatistikkRepository.BehandlingStatistikk(new BehandlingStatistikkRepository.BehandlingStatistikk.Type(FagsakYtelseType.FORELDREPENGER, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingStatistikkRepository.Behandlingsårsak.MANUELL), 50L),
                new BehandlingStatistikkRepository.BehandlingStatistikk(new BehandlingStatistikkRepository.BehandlingStatistikk.Type(FagsakYtelseType.SVANGERSKAPSPENGER, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingStatistikkRepository.Behandlingsårsak.MANUELL), 10L),
                new BehandlingStatistikkRepository.BehandlingStatistikk(new BehandlingStatistikkRepository.BehandlingStatistikk.Type(FagsakYtelseType.SVANGERSKAPSPENGER, BehandlingType.REVURDERING, BehandlingStatistikkRepository.Behandlingsårsak.SØKNAD), 10000000L),
                new BehandlingStatistikkRepository.BehandlingStatistikk(new BehandlingStatistikkRepository.BehandlingStatistikk.Type(FagsakYtelseType.ENGANGSTØNAD, BehandlingType.FØRSTEGANGSSØKNAD, BehandlingStatistikkRepository.Behandlingsårsak.MANUELL), 10L),
                new BehandlingStatistikkRepository.BehandlingStatistikk(new BehandlingStatistikkRepository.BehandlingStatistikk.Type(FagsakYtelseType.ENGANGSTØNAD, BehandlingType.REVURDERING, BehandlingStatistikkRepository.Behandlingsårsak.ANNET), 20L)
            );

    }
}
