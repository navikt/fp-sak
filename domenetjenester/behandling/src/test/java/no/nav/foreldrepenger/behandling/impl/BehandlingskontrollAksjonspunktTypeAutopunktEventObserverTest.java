package no.nav.foreldrepenger.behandling.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;

class BehandlingskontrollAksjonspunktTypeAutopunktEventObserverTest {

    private HistorikkInnslagForAksjonspunktEventObserver observer; // objectet vi tester

    private BehandlingskontrollKontekst behandlingskontrollKontekst;
    private Aksjonspunkt autopunkt;
    private Aksjonspunkt manuellpunkt;
    private HistorikkRepository historikkRepository;
    private Long behandlingId = 1L;
    private String PERIODE = "P2W";
    private LocalDate localDate = LocalDate.now().plus(Period.parse(PERIODE));

    @BeforeEach
    public void setup() {
        manuellpunkt = Mockito.mock(Aksjonspunkt.class);
        when(manuellpunkt.getAksjonspunktDefinisjon()).thenReturn(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_OMSORGSVILKÅRET);

        autopunkt = Mockito.mock(Aksjonspunkt.class);
        when(autopunkt.getAksjonspunktDefinisjon()).thenReturn(AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT);
        when(autopunkt.getFristTid()).thenReturn(LocalDateTime.of(localDate, LocalDateTime.now().toLocalTime()));
        when(autopunkt.erOpprettet()).thenReturn(true);

        behandlingskontrollKontekst = mock(BehandlingskontrollKontekst.class);
        when(behandlingskontrollKontekst.getBehandlingId()).thenReturn(behandlingId);

        historikkRepository = mock(HistorikkRepository.class);
        observer = new HistorikkInnslagForAksjonspunktEventObserver(historikkRepository, "srvengangsstonad");
    }

    @Test
    void skalMåleTidForFørsteAksjonspunktUtførtFødsel() {

        var event = new AksjonspunktStatusEvent(behandlingskontrollKontekst, List.of(autopunkt), null);

        observer.oppretteHistorikkForBehandlingPåVent(event);

        verify(historikkRepository).lagre(any(Historikkinnslag.class));
    }

    @Test
    void skalIkkeOppretteHistorikkForManuellPunkt() {

        var event = new AksjonspunktStatusEvent(behandlingskontrollKontekst, List.of(manuellpunkt), null);

        observer.oppretteHistorikkForBehandlingPåVent(event);

        verify(historikkRepository, never()).lagre(any());
    }

    @Test
    void skalOppretteEnHistorikkForAutoPunktOgSjekkPåResultat() {

        var event = new AksjonspunktStatusEvent(behandlingskontrollKontekst, List.of(manuellpunkt, autopunkt), null);

        var captor = ArgumentCaptor.forClass(Historikkinnslag.class);

        observer.oppretteHistorikkForBehandlingPåVent(event);

        verify(historikkRepository).lagre(captor.capture());
        var historikkinnslag = captor.getValue();
        var historikkinnslagDel = historikkinnslag.getHistorikkinnslagDeler().get(0);

        assertThat(historikkinnslag.getBehandlingId()).isEqualTo(behandlingId);
        assertThat(historikkinnslagDel.getHendelse()).hasValueSatisfying(hendelse -> {
            assertThat(hendelse.getNavn()).as("navn").isEqualTo(HistorikkinnslagType.BEH_VENT.getKode());
            assertThat(hendelse.getTilVerdi()).as("tilVerdi").isEqualTo(localDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        });
        assertThat(historikkinnslag.getType()).isEqualTo(HistorikkinnslagType.BEH_VENT);
    }

    @Test
    void skalOppretteToHistorikkForAutoPunkt() {

        var event = new AksjonspunktStatusEvent(behandlingskontrollKontekst, List.of(autopunkt, autopunkt), null);

        observer.oppretteHistorikkForBehandlingPåVent(event);

        verify(historikkRepository, times(2)).lagre(any());
    }

}
