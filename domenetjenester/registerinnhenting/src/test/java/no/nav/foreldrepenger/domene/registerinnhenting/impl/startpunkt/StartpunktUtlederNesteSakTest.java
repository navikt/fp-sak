package no.nav.foreldrepenger.domene.registerinnhenting.impl.startpunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakRepository;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ExtendWith(MockitoExtension.class)
class StartpunktUtlederNesteSakTest {

    @Mock
    private NesteSakRepository nesteSakRepository;

    @Test
    void skal_returnere_startpunkt_udefinert_dersom_ingen_neste_sak() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .lagMocked();
        when(nesteSakRepository.hentGrunnlagPåId(anyLong())).thenReturn(null);
        // Act/Assert
        var utleder = new StartpunktUtlederNesteSak(nesteSakRepository);
        assertThat(utleder.utledStartpunkt(BehandlingReferanse.fra(behandling), null, 1L, 2L)).isEqualTo(StartpunktType.UDEFINERT);
    }

    @Test
    void skal_returnere_startpunkt_udefinert_dersom_lik_startdato_neste_sak() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .lagMocked();
        var nestesak = NesteSakGrunnlagEntitet.Builder.oppdatere(Optional.empty()).medBehandlingId(123L)
            .medHendelsedato(LocalDate.now().plusWeeks(3))
            .medStartdato(LocalDate.now().plusDays(2))
            .medSaksnummer(new Saksnummer("987"))
            .build();
        var nestesak2 = NesteSakGrunnlagEntitet.Builder.oppdatere(Optional.of(nestesak)).medBehandlingId(234L).build();
        when(nesteSakRepository.hentGrunnlagPåId(1L)).thenReturn(nestesak);
        when(nesteSakRepository.hentGrunnlagPåId(2L)).thenReturn(nestesak2);
        // Act/Assert
        var utleder = new StartpunktUtlederNesteSak(nesteSakRepository);
        assertThat(utleder.utledStartpunkt(BehandlingReferanse.fra(behandling), null, 1L, 2L)).isEqualTo(StartpunktType.UDEFINERT);
    }

    @Test
    void skal_returnere_startpunkt_uttak_dersom_oppstått_neste_sak() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .lagMocked();
        var nestesak = NesteSakGrunnlagEntitet.Builder.oppdatere(Optional.empty()).medBehandlingId(123L)
            .medHendelsedato(LocalDate.now().plusWeeks(3))
            .medStartdato(LocalDate.now().plusDays(2))
            .medSaksnummer(new Saksnummer("987"))
            .build();
        when(nesteSakRepository.hentGrunnlagPåId(1L)).thenReturn(nestesak);
        when(nesteSakRepository.hentGrunnlagPåId(2L)).thenReturn(null);
        // Act/Assert
        var utleder = new StartpunktUtlederNesteSak(nesteSakRepository);
        assertThat(utleder.utledStartpunkt(BehandlingReferanse.fra(behandling), null, 1L, 2L)).isEqualTo(StartpunktType.UTTAKSVILKÅR);
    }

    @Test
    void skal_returnere_startpunkt_uttak_dersom_ulik_startdato_neste_sak() {
        // Arrange
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .lagMocked();
        var nestesak = NesteSakGrunnlagEntitet.Builder.oppdatere(Optional.empty()).medBehandlingId(123L)
            .medHendelsedato(LocalDate.now().plusWeeks(3))
            .medStartdato(LocalDate.now().plusDays(2))
            .medSaksnummer(new Saksnummer("987"))
            .build();
        var nestesak2 = NesteSakGrunnlagEntitet.Builder.oppdatere(Optional.of(nestesak))
            .medBehandlingId(234L)
            .medStartdato(LocalDate.now().plusDays(1))
            .build();
        when(nesteSakRepository.hentGrunnlagPåId(1L)).thenReturn(nestesak);
        when(nesteSakRepository.hentGrunnlagPåId(2L)).thenReturn(nestesak2);
        // Act/Assert
        var utleder = new StartpunktUtlederNesteSak(nesteSakRepository);
        assertThat(utleder.utledStartpunkt(BehandlingReferanse.fra(behandling), null, 1L, 2L)).isEqualTo(StartpunktType.UTTAKSVILKÅR);
    }


}
