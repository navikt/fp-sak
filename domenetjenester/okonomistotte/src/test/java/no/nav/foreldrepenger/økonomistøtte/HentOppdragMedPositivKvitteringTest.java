package no.nav.foreldrepenger.økonomistøtte;

import static no.nav.foreldrepenger.økonomistøtte.OppdragTestDataHelper.oppdragskontrollMedOppdrag;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomioppdragRepository;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

class HentOppdragMedPositivKvitteringTest {

    private HentOppdragMedPositivKvittering hentOppdragMedPositivKvittering;

    private Behandling behandling;
    private Oppdragskontroll oppdragskontroll;
    private Saksnummer saksnummer;

    @BeforeEach
    void setup() {
        behandling = Behandling.nyBehandlingFor(
            Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, NavBruker.opprettNyNB(AktørId.dummy()), new Saksnummer("123456789")),
            BehandlingType.FØRSTEGANGSSØKNAD).build();
        behandling.setId(123L);
        saksnummer = behandling.getSaksnummer();

        oppdragskontroll = oppdragskontrollMedOppdrag(saksnummer, 1L);
        mockRepository(oppdragskontroll);
    }

    private void mockRepository(Oppdragskontroll oppdragskontroll) {
        var økonomioppdragRepository = mock(ØkonomioppdragRepository.class);
        when(økonomioppdragRepository.finnOppdragForBehandling(eq(behandling.getId()))).thenReturn(Optional.of(oppdragskontroll));
        when(økonomioppdragRepository.finnAlleOppdragForSak(eq(saksnummer))).thenReturn(Collections.singletonList(oppdragskontroll));
        hentOppdragMedPositivKvittering = new HentOppdragMedPositivKvittering(økonomioppdragRepository);
    }

    @Test
    void skalHenteOppdrag110MedPositivKvitteringForBehandling() {

        OppdragKvitteringTestUtil.lagPositiveKvitteringer(oppdragskontroll);

        var resultater = hentOppdragMedPositivKvittering.hentOppdragMedPositivKvittering(behandling);

        assertThat(resultater).hasSize(1);
    }

    @Test
    void skalHenteOppdrag110MedNegativOgPositivKvitteringForBehandling() {

        lagToOppdrag110MedPositivOgNegativKvittering();

        var resultater = hentOppdragMedPositivKvittering.hentOppdragMedPositivKvittering(behandling);

        assertThat(resultater).hasSize(1);
    }

    @Test
    void skalIkkeHenteOppdrag110MedNegativKvitteringForBehandling() {

        OppdragKvitteringTestUtil.lagNegativeKvitteringer(oppdragskontroll);

        var resultater = hentOppdragMedPositivKvittering.hentOppdragMedPositivKvittering(behandling);

        assertThat(resultater).isEmpty();
    }

    @Test
    void skalIkkeHenteOppdrag110UtenKvitteringForBehandling() {

        var resultater = hentOppdragMedPositivKvittering.hentOppdragMedPositivKvittering(behandling);

        assertThat(resultater).isEmpty();
    }

    @Test
    void skalHenteOppdrag110MedPositivKvitteringForBehandlingFeilHvisVenter() {

        OppdragKvitteringTestUtil.lagPositiveKvitteringer(oppdragskontroll);

        var resultater = hentOppdragMedPositivKvittering.hentOppdragMedPositivKvitteringFeilHvisVenter(behandling);

        assertThat(resultater).hasSize(1);
    }

    @Test
    void skalHenteOppdrag110MedNegativOgPositivKvitteringForBehandlingFeilHvisVenter() {

        lagToOppdrag110MedPositivOgNegativKvittering();

        var resultater = hentOppdragMedPositivKvittering.hentOppdragMedPositivKvitteringFeilHvisVenter(behandling);

        assertThat(resultater).hasSize(1);
    }

    @Test
    void skalIkkeHenteOppdrag110MedNegativKvitteringForBehandlingFeilHvisVenter() {

        OppdragKvitteringTestUtil.lagNegativeKvitteringer(oppdragskontroll);

        var resultater = hentOppdragMedPositivKvittering.hentOppdragMedPositivKvitteringFeilHvisVenter(behandling);

        assertThat(resultater).isEmpty();
    }

    @Test
    void skalIkkeHenteOppdrag110UtenKvitteringForBehandlingFeilHvisVenter() {

        assertThrows(IllegalStateException.class, () -> hentOppdragMedPositivKvittering.hentOppdragMedPositivKvitteringFeilHvisVenter(behandling));

    }

    @Test
    void skalHenteOppdrag110MedPositivKvitteringForSaksnummer() {

        OppdragKvitteringTestUtil.lagPositiveKvitteringer(oppdragskontroll);

        var resultater = hentOppdragMedPositivKvittering.hentOppdragMedPositivKvittering(saksnummer);

        assertThat(resultater).hasSize(1);
    }

    @Test
    void skalHenteOppdrag110MedNegativOgPositivKvitteringForSaksnummer() {

        lagToOppdrag110MedPositivOgNegativKvittering();

        var resultater = hentOppdragMedPositivKvittering.hentOppdragMedPositivKvittering(saksnummer);

        assertThat(resultater).hasSize(1);
    }

    @Test
    void skalIkkeHenteOppdrag110MedNegativKvitteringForSaksnummer() {

        OppdragKvitteringTestUtil.lagNegativeKvitteringer(oppdragskontroll);

        var resultater = hentOppdragMedPositivKvittering.hentOppdragMedPositivKvittering(saksnummer);

        assertThat(resultater).isEmpty();
    }

    @Test
    void skalIkkeHenteOppdrag110UtenKvitteringForSaksnummer() {

        var resultater = hentOppdragMedPositivKvittering.hentOppdragMedPositivKvittering(saksnummer);

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
    void skalKunHenteOppdrag110MedPositivKvitteringForSaksnummer() {

        var oppdragskontroll = OppdragTestDataHelper.oppdragskontrollUtenOppdrag();
        var oppdrag1 = OppdragTestDataHelper.lagOppdrag110ES(oppdragskontroll, 1L);
        OppdragKvitteringTestUtil.lagPositivKvitting(oppdrag1);
        var oppdrag2 = OppdragTestDataHelper.lagOppdrag110ES(oppdragskontroll, 2L);
        OppdragKvitteringTestUtil.lagNegativKvitting(oppdrag2);
        var oppdrag3 = OppdragTestDataHelper.lagOppdrag110ES(oppdragskontroll, 3L);
        OppdragKvitteringTestUtil.lagPositivKvitting(oppdrag3);
        mockRepository(oppdragskontroll);

        var resultater = hentOppdragMedPositivKvittering.hentOppdragMedPositivKvittering(saksnummer);

        assertThat(resultater)
            .hasSize(2)
                .anySatisfy(oppdrag110 -> assertThat(oppdrag110.getFagsystemId()).isEqualTo(1L))
                .anySatisfy(oppdrag110 -> assertThat(oppdrag110.getFagsystemId()).isEqualTo(3L));
    }

    private void lagToOppdrag110MedPositivOgNegativKvittering() {
        var oppdrag110_2 = OppdragTestDataHelper.lagOppdrag110ES(oppdragskontroll, 1L);
        OppdragKvitteringTestUtil.lagPositivKvitting(oppdragskontroll.getOppdrag110Liste().get(0));
        OppdragKvitteringTestUtil.lagNegativKvitting(oppdrag110_2);
    }
}
