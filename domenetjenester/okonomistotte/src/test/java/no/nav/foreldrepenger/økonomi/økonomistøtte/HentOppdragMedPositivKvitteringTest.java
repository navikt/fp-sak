package no.nav.foreldrepenger.økonomi.økonomistøtte;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomi.økonomistøtte.HentOppdragMedPositivKvittering;
import no.nav.foreldrepenger.økonomi.økonomistøtte.ØkonomioppdragRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;

public class HentOppdragMedPositivKvitteringTest {

    private HentOppdragMedPositivKvittering hentOppdragMedPositivKvittering;
    private ØkonomioppdragRepository økonomioppdragRepository;

    private Behandling behandling;
    private Oppdragskontroll oppdragskontroll;
    private Saksnummer saksnummer;

    @Before
    public void setup() {
        behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        saksnummer = behandling.getFagsak().getSaksnummer();
        oppdragskontroll = OppdragTestDataHelper.buildOppdragskontroll();
        OppdragTestDataHelper.buildOppdrag110ES(oppdragskontroll, 1L);
        mockRepository(oppdragskontroll);
    }

    private void mockRepository(Oppdragskontroll oppdragskontroll) {
        økonomioppdragRepository = mock(ØkonomioppdragRepository.class);
        when(økonomioppdragRepository.finnOppdragForBehandling(eq(behandling.getId()))).thenReturn(Optional.of(oppdragskontroll));
        when(økonomioppdragRepository.finnAlleOppdragForSak(eq(saksnummer))).thenReturn(Collections.singletonList(oppdragskontroll));
        hentOppdragMedPositivKvittering = new HentOppdragMedPositivKvittering(økonomioppdragRepository);
    }

    @Test
    public void skalHenteOppdrag110MedPositivKvitteringForBehandling() {
        // Arrange
        OppdragKvitteringTestUtil.lagPositiveKvitteringer(oppdragskontroll);

        // Act
        var resultater = hentOppdragMedPositivKvittering.hentOppdragMedPositivKvittering(behandling);

        // Assert
        assertThat(resultater).hasSize(1);
    }

    @Test
    public void skalHenteOppdrag110MedNegativOgPositivKvitteringForBehandling() {
        // Arrange
        lagToOppdrag110MedPositivOgNegativKvittering();

        // Act
        var resultater = hentOppdragMedPositivKvittering.hentOppdragMedPositivKvittering(behandling);

        // Assert
        assertThat(resultater).hasSize(1);
    }

    @Test
    public void skalIkkeHenteOppdrag110MedNegativKvitteringForBehandling() {
        // Arrange
        OppdragKvitteringTestUtil.lagNegativeKvitteringer(oppdragskontroll);

        // Act
        var resultater = hentOppdragMedPositivKvittering.hentOppdragMedPositivKvittering(behandling);

        // Assert
        assertThat(resultater).isEmpty();
    }

    @Test
    public void skalIkkeHenteOppdrag110UtenKvitteringForBehandling() {
        // Act
        var resultater = hentOppdragMedPositivKvittering.hentOppdragMedPositivKvittering(behandling);

        // Assert
        assertThat(resultater).isEmpty();
    }

    @Test
    public void skalHenteOppdrag110MedPositivKvitteringForSaksnummer() {
        // Arrange
        OppdragKvitteringTestUtil.lagPositiveKvitteringer(oppdragskontroll);

        // Act
        var resultater = hentOppdragMedPositivKvittering.hentOppdragMedPositivKvittering(saksnummer);

        // Assert
        assertThat(resultater).hasSize(1);
    }

    @Test
    public void skalHenteOppdrag110MedNegativOgPositivKvitteringForSaksnummer() {
        // Arrange
        lagToOppdrag110MedPositivOgNegativKvittering();

        // Act
        var resultater = hentOppdragMedPositivKvittering.hentOppdragMedPositivKvittering(saksnummer);

        // Assert
        assertThat(resultater).hasSize(1);
    }

    @Test
    public void skalIkkeHenteOppdrag110MedNegativKvitteringForSaksnummer() {
        // Arrange
        OppdragKvitteringTestUtil.lagNegativeKvitteringer(oppdragskontroll);

        // Act
        var resultater = hentOppdragMedPositivKvittering.hentOppdragMedPositivKvittering(saksnummer);

        // Assert
        assertThat(resultater).isEmpty();
    }

    @Test
    public void skalIkkeHenteOppdrag110UtenKvitteringForSaksnummer() {
        // Act
        var resultater = hentOppdragMedPositivKvittering.hentOppdragMedPositivKvittering(saksnummer);

        // Assert
        assertThat(resultater).isEmpty();
    }

    /**
     * Førstegangsbehandling:
     * <ul>
     * <li>2 mottakere (bruker og arbeidsgiver)</li>
     * <li>3 oppdrag110:
     * <ul>
     * <li>Oppdrag110-1: Positiv</li>
     * <li>Oppdrag110-2: Negativ</li>
     * <li>Oppdrag110-3: Positiv</li>
     * </ul>
     * </li>
     * </ul>
     * Revurdering: Skal kun bruke Oppdrag110 med positiv kvittering. Altså 1 og 3.
     */
    @Test
    public void skalKunHenteOppdrag110MedPositivKvitteringForSaksnummer() {
        // Arrange
        Oppdragskontroll oppdragskontroll = OppdragTestDataHelper.buildOppdragskontroll();
        var oppdrag1 = OppdragTestDataHelper.buildOppdrag110ES(oppdragskontroll, 1L);
        OppdragKvitteringTestUtil.lagPositivKvitting(oppdrag1);
        var oppdrag2 = OppdragTestDataHelper.buildOppdrag110ES(oppdragskontroll, 2L);
        OppdragKvitteringTestUtil.lagNegativKvitting(oppdrag2);
        var oppdrag3 = OppdragTestDataHelper.buildOppdrag110ES(oppdragskontroll, 3L);
        OppdragKvitteringTestUtil.lagPositivKvitting(oppdrag3);
        mockRepository(oppdragskontroll);

        // Act
        var resultater = hentOppdragMedPositivKvittering.hentOppdragMedPositivKvittering(saksnummer);

        // Assert
        assertThat(resultater).hasSize(2);
        assertThat(resultater).anySatisfy(oppdrag110 ->
            assertThat(oppdrag110.getFagsystemId()).isEqualTo(1L));
        assertThat(resultater).anySatisfy(oppdrag110 ->
            assertThat(oppdrag110.getFagsystemId()).isEqualTo(3L));
    }

    private void lagToOppdrag110MedPositivOgNegativKvittering() {
        Oppdrag110 oppdrag110_2 = OppdragTestDataHelper.buildOppdrag110ES(oppdragskontroll, 1L);
        OppdragKvitteringTestUtil.lagPositivKvitting(oppdragskontroll.getOppdrag110Liste().get(0));
        OppdragKvitteringTestUtil.lagNegativKvitting(oppdrag110_2);
    }
}
