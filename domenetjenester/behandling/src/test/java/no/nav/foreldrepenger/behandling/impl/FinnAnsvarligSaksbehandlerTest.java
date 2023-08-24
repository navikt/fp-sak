package no.nav.foreldrepenger.behandling.impl;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FinnAnsvarligSaksbehandlerTest {

    private static final String BESLUTTER = "Beslutter";
    private static final String SAKSBEHANDLER = "Saksbehandler";

    private Behandling behandling;

    @BeforeEach
    public void setup() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        behandling = scenario.lagMocked();
    }

    @Test
    void ansvarligSaksbehandlerSettesTilAnsvarligBeslutterNårSatt() {
        // Arrange
        behandling.setAnsvarligSaksbehandler(SAKSBEHANDLER);
        behandling.setAnsvarligBeslutter(BESLUTTER);

        // Act
        var ansvarligSaksbehandler = FinnAnsvarligSaksbehandler.finn(behandling);

        // Assert
        assertThat(ansvarligSaksbehandler).isEqualTo(BESLUTTER);
    }

    @Test
    void ansvarligSaksbehandlerSettesTilAnsvarligSaksbehandlerNårAnsvarligBeslutterIkkeErSatt() {
        // Arrange
        behandling.setAnsvarligSaksbehandler(SAKSBEHANDLER);

        // Act
        var ansvarligSaksbehandler = FinnAnsvarligSaksbehandler.finn(behandling);

        // Assert
        assertThat(ansvarligSaksbehandler).isEqualTo(SAKSBEHANDLER);
    }

    @Test
    void ansvarligSaksbehandlerSettesTilVLNårBeslutterOgSaksbehandlerMangler() {
        // Act
        var ansvarligSaksbehandler = FinnAnsvarligSaksbehandler.finn(behandling);

        // Assert
        assertThat(ansvarligSaksbehandler).isEqualTo("VL");

    }
}
