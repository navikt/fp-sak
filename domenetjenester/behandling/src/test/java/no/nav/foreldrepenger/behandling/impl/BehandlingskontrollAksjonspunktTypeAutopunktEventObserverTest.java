package no.nav.foreldrepenger.behandling.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;

@ExtendWith(MockitoExtension.class)
class BehandlingskontrollAksjonspunktTypeAutopunktEventObserverTest {

    private HistorikkInnslagForAksjonspunktEventObserver observer; // objectet vi tester

    @Mock
    private BehandlingskontrollKontekst behandlingskontrollKontekst;
    @Mock
    private Aksjonspunkt autopunkt;
    @Mock
    private Aksjonspunkt manuellpunkt;
    @Mock
    private Historikkinnslag2Repository historikkinnslagRepository;
    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private Fagsak fagsak;

    private Long behandlingId = 1L;
    private String PERIODE = "P2W";
    private LocalDate localDate = LocalDate.now().plus(Period.parse(PERIODE));

    @BeforeEach
    public void setup() {
        lenient().when(manuellpunkt.getAksjonspunktDefinisjon()).thenReturn(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_OMSORGSVILKÅRET);

        lenient().when(autopunkt.getAksjonspunktDefinisjon()).thenReturn(AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT);
        lenient().when(autopunkt.getFristTid()).thenReturn(LocalDateTime.of(localDate, LocalDateTime.now().toLocalTime()));
        lenient().when(autopunkt.erOpprettet()).thenReturn(true);

        lenient().when(behandlingskontrollKontekst.getBehandlingId()).thenReturn(behandlingId);

        observer = new HistorikkInnslagForAksjonspunktEventObserver(historikkinnslagRepository, behandlingRepository);
    }

    @Test
    void skalMåleTidForFørsteAksjonspunktUtførtFødsel() {

        var event = new AksjonspunktStatusEvent(behandlingskontrollKontekst, List.of(autopunkt), null);

        observer.oppretteHistorikkForBehandlingPåVent(event);

        verify(historikkinnslagRepository).lagre(any(Historikkinnslag2.class));
    }

    @Test
    void skalIkkeOppretteHistorikkForManuellPunkt() {

        var event = new AksjonspunktStatusEvent(behandlingskontrollKontekst, List.of(manuellpunkt), null);

        observer.oppretteHistorikkForBehandlingPåVent(event);

        verify(historikkinnslagRepository, never()).lagre(any());
    }

    @Test
    void skalIkkeOppretteHistorikkForManuellTattAvVent() {
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandling.setAnsvarligSaksbehandler("IDENT");
        when(behandlingRepository.hentBehandlingReadOnly(anyLong())).thenReturn(behandling);
        when(autopunkt.erUtført()).thenReturn(true);

        var event = new AksjonspunktStatusEvent(behandlingskontrollKontekst, List.of(autopunkt), null);

        observer.oppretteHistorikkForGjenopptattBehandling(event);

        verify(historikkinnslagRepository, never()).lagre(any());
    }

    @Test
    void skalOppretteEnHistorikkForAutoPunktOgSjekkPåResultat() {

        var event = new AksjonspunktStatusEvent(behandlingskontrollKontekst, List.of(manuellpunkt, autopunkt), null);

        var captor = ArgumentCaptor.forClass(Historikkinnslag2.class);

        observer.oppretteHistorikkForBehandlingPåVent(event);

        verify(historikkinnslagRepository).lagre(captor.capture());
        var historikkinnslag = captor.getValue();

        assertThat(historikkinnslag.getBehandlingId()).isEqualTo(behandlingId);
        assertThat(historikkinnslag.getTittel()).isEqualTo("Behandling er satt på vent " + HistorikkinnslagLinjeBuilder.format(localDate));
    }

    @Test
    void skalOppretteToHistorikkForAutoPunkt() {

        var event = new AksjonspunktStatusEvent(behandlingskontrollKontekst, List.of(autopunkt, autopunkt), null);

        observer.oppretteHistorikkForBehandlingPåVent(event);

        verify(historikkinnslagRepository, times(2)).lagre(any());
    }

}
