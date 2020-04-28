package no.nav.foreldrepenger.datavarehus.observer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.behandling.FagsakStatusEvent;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegTilstandSnapshot;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStegTilstandEndringEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.datavarehus.tjeneste.DatavarehusTjeneste;

public class DatavarehusEventObserverTest {

    private DatavarehusTjeneste datavarehusTjeneste;
    private DatavarehusEventObserver datavarehusEventObserver;

    @Before
    public void setUp() throws Exception {
        datavarehusTjeneste = mock(DatavarehusTjeneste.class);
        datavarehusEventObserver = new DatavarehusEventObserver(datavarehusTjeneste);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void observerAksjonspunktUtførtEvent() throws Exception {
        Behandling behandling = byggBehandling();
        Long behandlingId = behandling.getId();
        List<Aksjonspunkt> aksjonspunktListe = new ArrayList<>(behandling.getAksjonspunkter());

        var event = new AksjonspunktStatusEvent(byggKontekst(behandling), aksjonspunktListe, BehandlingStegType.BEREGN_YTELSE);
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);

        datavarehusEventObserver.observerAksjonspunktStatusEvent(event);

        verify(datavarehusTjeneste).lagreNedAksjonspunkter(captor.capture(), eq(behandlingId), eq(BehandlingStegType.BEREGN_YTELSE));
        List resultList = captor.getValue();
        assertThat(resultList.get(0)).isEqualTo(aksjonspunktListe.get(0));
        assertThat(resultList.get(1)).isEqualTo(aksjonspunktListe.get(1));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void observerAksjonspunkterFunnetEvent() throws Exception {
        Behandling behandling = byggBehandling();
        Long behandlingId = behandling.getId();
        List<Aksjonspunkt> aksjonspunktListe = new ArrayList<>(behandling.getAksjonspunkter());

        var event = new AksjonspunktStatusEvent(byggKontekst(behandling), aksjonspunktListe, BehandlingStegType.BEREGN_YTELSE);
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);

        datavarehusEventObserver.observerAksjonspunktStatusEvent(event);

        verify(datavarehusTjeneste).lagreNedAksjonspunkter(captor.capture(), eq(behandlingId), eq(BehandlingStegType.BEREGN_YTELSE));
        List resultList = captor.getValue();
        assertThat(resultList.get(0)).isEqualTo(aksjonspunktListe.get(0));
        assertThat(resultList.get(1)).isEqualTo(aksjonspunktListe.get(1));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void observerAksjonspunkterAvbruttEvent() throws Exception {
        Behandling behandling = byggBehandling();
        Long behandlingId = behandling.getId();
        List<Aksjonspunkt> aksjonspunktListe = new ArrayList<>(behandling.getAksjonspunkter());

        var event = new AksjonspunktStatusEvent(byggKontekst(behandling), aksjonspunktListe, BehandlingStegType.BEREGN_YTELSE);
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);

        datavarehusEventObserver.observerAksjonspunktStatusEvent(event);

        verify(datavarehusTjeneste).lagreNedAksjonspunkter(captor.capture(), eq(behandlingId), eq(BehandlingStegType.BEREGN_YTELSE));
        List resultList = captor.getValue();
        assertThat(resultList.get(0)).isEqualTo(aksjonspunktListe.get(0));
        assertThat(resultList.get(1)).isEqualTo(aksjonspunktListe.get(1));
    }


    @Test
    public void observerFagsakStatus() throws Exception {
        Behandling behandling = byggBehandling();
        Fagsak fagsak = behandling.getFagsak();

        FagsakStatusEvent event = new FagsakStatusEvent(fagsak.getId(), fagsak.getAktørId(), FagsakStatus.OPPRETTET, fagsak.getStatus());

        datavarehusEventObserver.observerFagsakStatus(event);

        verify(datavarehusTjeneste).lagreNedFagsak(eq(fagsak.getId()));
    }

    @Test
    public void observerBehandlingStegTilstandEndringEvent() throws Exception {
        Behandling behandling = byggBehandling();

        BehandlingskontrollKontekst kontekst = byggKontekst(behandling);
        BehandlingStegTilstandSnapshot fraTilstand = new BehandlingStegTilstandSnapshot(1L, BehandlingStegType.BEREGN_YTELSE, BehandlingStegStatus.UTFØRT);
        BehandlingStegTilstandSnapshot tilTilstand = new BehandlingStegTilstandSnapshot(2L, BehandlingStegType.FATTE_VEDTAK, BehandlingStegStatus.INNGANG);
        BehandlingStegTilstandEndringEvent event = new BehandlingStegTilstandEndringEvent(kontekst,
            fraTilstand,
            tilTilstand);
        datavarehusEventObserver.observerBehandlingStegTilstandEndringEvent(event);

        ArgumentCaptor<BehandlingStegTilstandSnapshot> captor = ArgumentCaptor.forClass(BehandlingStegTilstandSnapshot.class);

        verify(datavarehusTjeneste, times(2)).lagreNedBehandlingStegTilstand(any(Long.class), captor.capture());
        List<BehandlingStegTilstandSnapshot> tilstandListe = captor.getAllValues();
        assertThat(tilstandListe.get(0)).isEqualTo(fraTilstand);
        assertThat(tilstandListe.get(1)).isEqualTo(tilTilstand);
    }

    @Test
    public void observerBehandlingStegTilstandEndringEvent_regsøk_til_insøk() throws Exception {
        Behandling behandling = byggBehandling();

        BehandlingskontrollKontekst kontekst = byggKontekst(behandling);
        BehandlingStegTilstandSnapshot fraTilstand = new BehandlingStegTilstandSnapshot(1L, BehandlingStegType.REGISTRER_SØKNAD, BehandlingStegStatus.UTFØRT);
        BehandlingStegTilstandSnapshot tilTilstand = new BehandlingStegTilstandSnapshot(2L, BehandlingStegType.INNHENT_SØKNADOPP, BehandlingStegStatus.INNGANG);
        BehandlingStegTilstandEndringEvent event = new BehandlingStegTilstandEndringEvent(kontekst,
            fraTilstand,
            tilTilstand);
        datavarehusEventObserver.observerBehandlingStegTilstandEndringEvent(event);

        ArgumentCaptor<BehandlingStegTilstandSnapshot> captor = ArgumentCaptor.forClass(BehandlingStegTilstandSnapshot.class);

        verify(datavarehusTjeneste, times(2)).lagreNedBehandlingStegTilstand(any(Long.class), captor.capture());
        List<BehandlingStegTilstandSnapshot> tilstandListe = captor.getAllValues();
        assertThat(tilstandListe.get(0)).isEqualTo(fraTilstand);
        assertThat(tilstandListe.get(1)).isEqualTo(tilTilstand);
    }

    @Test
    public void observerBehandlingOpprettetEvent() throws Exception {
        Behandling behandling = byggBehandling();
        BehandlingStatusEvent.BehandlingOpprettetEvent event = BehandlingStatusEvent.nyEvent(byggKontekst(behandling), BehandlingStatus.OPPRETTET);

        datavarehusEventObserver.observerBehandlingStatusEvent(event);
        verify(datavarehusTjeneste).lagreNedBehandling(behandling.getId());
    }

    @Test
    public void observerBehandlingAvsluttetEvent() throws Exception {
        Behandling behandling = byggBehandling();
        BehandlingStatusEvent.BehandlingAvsluttetEvent event = BehandlingStatusEvent.nyEvent(byggKontekst(behandling), BehandlingStatus.AVSLUTTET);

        datavarehusEventObserver.observerBehandlingStatusEvent(event);
        verify(datavarehusTjeneste).lagreNedBehandling(behandling.getId());
    }

    @Test
    public void observerBehandlingVedtakEvent() throws Exception {
        BehandlingVedtak vedtak = byggVedtak();
        Behandling behandling = byggBehandling();
        BehandlingVedtakEvent event = new BehandlingVedtakEvent(vedtak, behandling);

        datavarehusEventObserver.observerBehandlingVedtakEvent(event);
        verify(datavarehusTjeneste).lagreNedVedtak(eq(vedtak), eq(behandling.getId()));
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
        BehandlingLås behandlingLås = new BehandlingLås(behandling.getId()) {
        };
        Fagsak fagsak = behandling.getFagsak();
        return new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingLås);
    }

    private Behandling byggBehandling() {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT, BehandlingStegType.VURDER_UTTAK);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_FAKTA_FOR_PERSONSTATUS, BehandlingStegType.VURDER_UTTAK);

        Behandling behandling = scenario.lagMocked();
        return behandling;
    }

}
