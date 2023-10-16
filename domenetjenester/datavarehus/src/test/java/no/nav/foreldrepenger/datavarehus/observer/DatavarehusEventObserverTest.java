package no.nav.foreldrepenger.datavarehus.observer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.FagsakStatusEvent;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.events.BehandlingVedtakEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    void observerAksjonspunktUtførtEvent() {
        var behandling = byggBehandling();
        var behandlingId = behandling.getId();
        List<Aksjonspunkt> aksjonspunktListe = new ArrayList<>(behandling.getAksjonspunkter());

        var event = new AksjonspunktStatusEvent(byggKontekst(behandling), aksjonspunktListe, BehandlingStegType.BEREGN_YTELSE);
        var captor = ArgumentCaptor.forClass(List.class);

        datavarehusEventObserver.observerAksjonspunktStatusEvent(event);

        verify(datavarehusTjeneste).lagreNedAksjonspunkter(captor.capture(), eq(behandlingId), eq(BehandlingStegType.BEREGN_YTELSE));
        var resultList = captor.getValue();
        assertThat(resultList.get(0)).isEqualTo(aksjonspunktListe.get(0));
        assertThat(resultList.get(1)).isEqualTo(aksjonspunktListe.get(1));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    void observerAksjonspunkterFunnetEvent() {
        var behandling = byggBehandling();
        var behandlingId = behandling.getId();
        List<Aksjonspunkt> aksjonspunktListe = new ArrayList<>(behandling.getAksjonspunkter());

        var event = new AksjonspunktStatusEvent(byggKontekst(behandling), aksjonspunktListe, BehandlingStegType.BEREGN_YTELSE);
        var captor = ArgumentCaptor.forClass(List.class);

        datavarehusEventObserver.observerAksjonspunktStatusEvent(event);

        verify(datavarehusTjeneste).lagreNedAksjonspunkter(captor.capture(), eq(behandlingId), eq(BehandlingStegType.BEREGN_YTELSE));
        var resultList = captor.getValue();
        assertThat(resultList.get(0)).isEqualTo(aksjonspunktListe.get(0));
        assertThat(resultList.get(1)).isEqualTo(aksjonspunktListe.get(1));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    void observerAksjonspunkterAvbruttEvent() {
        var behandling = byggBehandling();
        var behandlingId = behandling.getId();
        List<Aksjonspunkt> aksjonspunktListe = new ArrayList<>(behandling.getAksjonspunkter());

        var event = new AksjonspunktStatusEvent(byggKontekst(behandling), aksjonspunktListe, BehandlingStegType.BEREGN_YTELSE);
        var captor = ArgumentCaptor.forClass(List.class);

        datavarehusEventObserver.observerAksjonspunktStatusEvent(event);

        verify(datavarehusTjeneste).lagreNedAksjonspunkter(captor.capture(), eq(behandlingId), eq(BehandlingStegType.BEREGN_YTELSE));
        var resultList = captor.getValue();
        assertThat(resultList.get(0)).isEqualTo(aksjonspunktListe.get(0));
        assertThat(resultList.get(1)).isEqualTo(aksjonspunktListe.get(1));
    }

    @Test
    void observerFagsakStatus() {
        var behandling = byggBehandling();
        var fagsak = behandling.getFagsak();

        var event = new FagsakStatusEvent(fagsak.getId(), behandling.getId(), fagsak.getAktørId(), FagsakStatus.OPPRETTET, fagsak.getStatus());

        datavarehusEventObserver.observerFagsakStatus(event);

        verify(datavarehusTjeneste).lagreNedFagsak(eq(fagsak.getId()));
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
        verify(datavarehusTjeneste).lagreNedVedtak(eq(vedtak), eq(behandling));
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
        return new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingLås);
    }

    private Behandling byggBehandling() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, BehandlingStegType.VURDER_UTTAK);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_LOVLIG_OPPHOLD, BehandlingStegType.VURDER_UTTAK);

        return scenario.lagMocked();
    }

}
