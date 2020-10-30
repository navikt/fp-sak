package no.nav.foreldrepenger.økonomi.økonomistøtte.es;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingEndring;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.økonomi.økonomistøtte.kontantytelse.es.OppdragskontrollEngangsstønad;
import no.nav.foreldrepenger.økonomi.økonomistøtte.kontantytelse.es.OppdragskontrollManagerFactoryKontantytelse;

public class OppdragskontrollManagerFactoryKontantytelseTest {
    private OppdragskontrollManagerFactoryKontantytelse oppdragskontrollManagerFactory;
    private RevurderingEndring revurderingEndring = mock(RevurderingEndring.class);
    private OppdragskontrollEngangsstønad oppdragskontrollEngangsstønad = mock(OppdragskontrollEngangsstønad.class);

    @BeforeEach
    public void oppsett() {
        oppdragskontrollManagerFactory = new OppdragskontrollManagerFactoryKontantytelse(revurderingEndring, oppdragskontrollEngangsstønad);
    }

    private Behandling initBehandling(BehandlingType behandlingType) {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medBehandlingType(behandlingType);
        return scenario.lagMocked();
    }

    @Test
    public void testUtbetalingBehandlingResultatInnvilget() {
        // Arrange
        Behandling behandling = initBehandling(BehandlingType.FØRSTEGANGSSØKNAD);
        Behandlingsresultat.builder()
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET)
            .buildFor(behandling);

        // Act
        var managerOpt = oppdragskontrollManagerFactory.getManager(behandling, false);

        // Assert
        assertThat(managerOpt).hasValueSatisfying(manager ->
            assertThat(manager).isSameAs(oppdragskontrollEngangsstønad)
        );
    }

    @Test
    public void testUtbetalingBehandlingResultatAvslått() {
        // Arrange
        Behandling behandling = initBehandling(BehandlingType.FØRSTEGANGSSØKNAD);
        Behandlingsresultat.builder()
            .medBehandlingResultatType(BehandlingResultatType.AVSLÅTT)
            .medAvslagsårsak(Avslagsårsak.FAR_HAR_IKKE_OMSORG_FOR_BARNET)
            .buildFor(behandling);

        // Act
        var managerOpt = oppdragskontrollManagerFactory.getManager(behandling, false);

        // Assert
        assertThat(managerOpt).isEmpty();
    }

    @Test
    public void revurderingMedVedtakSomErBeslutningSkalIkkeSendeOppdrag() {
        // Arrange
        Behandling behandling = initBehandling(BehandlingType.REVURDERING);
        when(revurderingEndring.erRevurderingMedUendretUtfall(eq(behandling))).thenReturn(true);

        // Act
        var managerOpt = oppdragskontrollManagerFactory.getManager(behandling, false);

        // Assert
        assertThat(managerOpt).isEmpty();
    }

    @Test
    public void revurderingMedVedtakSomIkkeErBeslutningSkalSendeOppdrag() {
        // Arrange
        Behandling behandling = initBehandling(BehandlingType.REVURDERING);
        when(revurderingEndring.erRevurderingMedUendretUtfall(eq(behandling))).thenReturn(false);

        // Act
        var managerOpt = oppdragskontrollManagerFactory.getManager(behandling, false);

        // Assert
        assertThat(managerOpt).hasValueSatisfying(manager ->
            assertThat(manager).isSameAs(oppdragskontrollEngangsstønad)
        );
    }

    @Test
    public void avslagPåGrunnAvTidligereUtbetaltSkalIkkeSendeOppdrag() {
        // Arrange
        Behandling behandling = initBehandling(BehandlingType.FØRSTEGANGSSØKNAD);

        Behandlingsresultat.builder()
            .medBehandlingResultatType(BehandlingResultatType.AVSLÅTT)
            .medAvslagsårsak(Avslagsårsak.ENGANGSSTØNAD_ALLEREDE_UTBETALT_TIL_MOR)
            .buildFor(behandling);

        // Act
        var managerOpt = oppdragskontrollManagerFactory.getManager(behandling, false);

        // Assert
        assertThat(managerOpt).isEmpty();
    }
}
