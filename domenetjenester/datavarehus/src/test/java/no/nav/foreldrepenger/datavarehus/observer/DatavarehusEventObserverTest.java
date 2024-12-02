package no.nav.foreldrepenger.datavarehus.observer;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingVedtakEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.datavarehus.tjeneste.DatavarehusTjeneste;

@ExtendWith(MockitoExtension.class)
class DatavarehusEventObserverTest {

    @Mock
    private DatavarehusTjeneste datavarehusTjeneste;
    private DatavarehusEventObserver datavarehusEventObserver;

    @BeforeEach
    public void setUp() {
        datavarehusEventObserver = new DatavarehusEventObserver(datavarehusTjeneste);
    }

    @Test
    void observerBehandlingOpprettetEvent() {
        var behandling = byggBehandling();
        BehandlingStatusEvent.BehandlingOpprettetEvent event = BehandlingStatusEvent.nyEvent(byggKontekst(behandling), BehandlingStatus.OPPRETTET);

        datavarehusEventObserver.observerBehandlingStatusEvent(event);
        verify(datavarehusTjeneste).lagreNedBehandling(behandling.getId());
    }

    @Test
    void observerBehandlingAvsluttetEvent() {
        var behandling = byggBehandling();
        BehandlingStatusEvent.BehandlingAvsluttetEvent event = BehandlingStatusEvent.nyEvent(byggKontekst(behandling), BehandlingStatus.AVSLUTTET);

        datavarehusEventObserver.observerBehandlingStatusEvent(event);
        verify(datavarehusTjeneste).lagreNedBehandling(behandling.getId());
    }

    @Test
    void observerBehandlingVedtakEvent() {
        var vedtak = byggVedtak();
        var behandling = byggBehandling();
        var event = new BehandlingVedtakEvent(vedtak, behandling);

        datavarehusEventObserver.observerBehandlingVedtakEvent(event);
        verify(datavarehusTjeneste).lagreNedBehandling(eq(behandling), eq(vedtak));
    }

    private BehandlingVedtak byggVedtak() {
        return BehandlingVedtak.builder()
                .medAnsvarligSaksbehandler("s142443")
                .medIverksettingStatus(IverksettingStatus.IVERKSATT)
                .medVedtakstidspunkt(LocalDateTime.now())
                .medVedtakResultatType(VedtakResultatType.INNVILGET)
                .build();
    }

    private BehandlingskontrollKontekst byggKontekst(Behandling behandling) {
        var behandlingLås = new BehandlingLås(behandling.getId()) {
        };
        var fagsak = behandling.getFagsak();
        return new BehandlingskontrollKontekst(fagsak.getSaksnummer(), fagsak.getId(), behandlingLås);
    }

    private Behandling byggBehandling() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, BehandlingStegType.VURDER_UTTAK);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_MEDLEMSKAPSVILKÅRET, BehandlingStegType.VURDER_UTTAK);

        return scenario.lagMocked();
    }

}
