package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.adapter.MapBehandlingVedtak;

public class MapBehandlingVedtakTest {

    private MapBehandlingVedtak mapBehandlingVedtakFP;
    private BehandlingVedtakRepository behandlingVedtakRepository = mock(BehandlingVedtakRepository.class);

    @Before
    public void setup() {
        mapBehandlingVedtakFP = new MapBehandlingVedtak(behandlingVedtakRepository);
    }

    @Test
    public void mapUtenBehandlingVedtak() {
        // Arrange
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        behandling.setAnsvarligBeslutter("behandlingBeslutter");
        Behandlingsresultat.builder()
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET)
            .buildFor(behandling);

        LocalDate vedtaksdato = LocalDate.now();

        // Act
        var behandlingVedtakFP = mapBehandlingVedtakFP.map(behandling);

        // Assert
        assertThat(behandlingVedtakFP.getAnsvarligSaksbehandler()).isEqualTo("behandlingBeslutter");
        assertThat(behandlingVedtakFP.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.INNVILGET);
        assertThat(behandlingVedtakFP.getVedtaksdato()).isEqualTo(vedtaksdato);
    }

    @Test
    public void mapMedBehandlingVedtak() {
        // Arrange
        Behandling behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        Behandlingsresultat behandlingsresultat = Behandlingsresultat.builder()
            .medBehandlingResultatType(BehandlingResultatType.OPPHØR)
            .buildFor(behandling);
        LocalDateTime vedtakstidspunkt = LocalDateTime.now().minusDays(1);
        BehandlingVedtak behandlingVedtak = BehandlingVedtak.builder()
            .medAnsvarligSaksbehandler("saksbehandlerVedtak")
            .medBehandlingsresultat(behandlingsresultat)
            .medVedtakResultatType(VedtakResultatType.OPPHØR)
            .medVedtakstidspunkt(vedtakstidspunkt)
            .build();
        when(behandlingVedtakRepository.hentBehandlingvedtakForBehandlingId(any())).thenReturn(Optional.of(behandlingVedtak));

        // Act
        var behandlingVedtakFP = mapBehandlingVedtakFP.map(behandling);

        // Assert
        assertThat(behandlingVedtakFP.getAnsvarligSaksbehandler()).isEqualTo("saksbehandlerVedtak");
        assertThat(behandlingVedtakFP.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.OPPHØR);
        assertThat(behandlingVedtakFP.getVedtaksdato()).isEqualTo(vedtakstidspunkt.toLocalDate());
    }
}
