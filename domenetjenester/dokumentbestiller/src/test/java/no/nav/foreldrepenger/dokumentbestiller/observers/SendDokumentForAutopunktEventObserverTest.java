package no.nav.foreldrepenger.dokumentbestiller.observers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.events.AutopunktStatusEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.dokumentbestiller.autopunkt.SendBrevForAutopunkt;

@ExtendWith(MockitoExtension.class)
class SendDokumentForAutopunktEventObserverTest {

    @Mock
    private Aksjonspunkt autopunktIngenSøknad;
    @Mock
    private Aksjonspunkt autopunktOpptjening;
    @Mock
    private Aksjonspunkt autopunktTidligSøknad;
    @Mock
    private Aksjonspunkt autopunktEtterkontroll;
    @Mock
    private Aksjonspunkt manuellpunkt;
    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private SendBrevForAutopunkt sendBrevForAutopunkt;

    private SendDokumentForAutopunktEventObserver observer; // objektet vi tester

    private BehandlingskontrollKontekst behandlingskontrollKontekst;

    private Long behandlingId = 1L;

    @BeforeEach
    void setUp() {

        var autopunktDefinisjonIngenSøknad = AksjonspunktDefinisjon.VENT_PÅ_SØKNAD;
        var autopunktDefinisjonTidligSøknad = AksjonspunktDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD;
        var autopunktDefinisjonEtterkontroll = AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_REVURDERING;

        var manuellpunktDefinisjon = AksjonspunktDefinisjon.MANUELL_VURDERING_AV_OMSORGSVILKÅRET;

        lenient().when(manuellpunkt.getAksjonspunktDefinisjon()).thenReturn(manuellpunktDefinisjon);

        lenient().when(autopunktIngenSøknad.getAksjonspunktDefinisjon()).thenReturn(autopunktDefinisjonIngenSøknad);
        lenient().when(autopunktTidligSøknad.getAksjonspunktDefinisjon()).thenReturn(autopunktDefinisjonTidligSøknad);
        lenient().when(autopunktEtterkontroll.getAksjonspunktDefinisjon()).thenReturn(autopunktDefinisjonEtterkontroll);

        lenient().when(manuellpunkt.erOpprettet()).thenReturn(true);
        lenient().when(autopunktIngenSøknad.erOpprettet()).thenReturn(true);
        lenient().when(autopunktTidligSøknad.erOpprettet()).thenReturn(true);
        lenient().when(autopunktOpptjening.erOpprettet()).thenReturn(true);
        lenient().when(autopunktEtterkontroll.erOpprettet()).thenReturn(true);

        behandlingskontrollKontekst = mock(BehandlingskontrollKontekst.class);
        when(behandlingskontrollKontekst.getBehandlingId()).thenReturn(behandlingId);

        observer = new SendDokumentForAutopunktEventObserver(behandlingRepository, sendBrevForAutopunkt);
    }

    @Test
    void skalIkkeSendeBrevForAndreAksjonspunkter() {
        var event = new AutopunktStatusEvent(behandlingskontrollKontekst, List.of(manuellpunkt));

        observer.sendBrevForAutopunkt(event);

        verify(sendBrevForAutopunkt, times(0)).sendBrevForSøknadIkkeMottatt(any());
        verify(sendBrevForAutopunkt, times(0)).sendBrevForTidligSøknad(any());
    }

    @Test
    void skalSendeBrevForSøknadIkkeMottatt() {
        var event = new AutopunktStatusEvent(behandlingskontrollKontekst, List.of(autopunktIngenSøknad));
        observer.sendBrevForAutopunkt(event);
        verify(sendBrevForAutopunkt, times(1)).sendBrevForSøknadIkkeMottatt(any());
    }

    @Test
    void skalSendeBrevForTidligSøknad() {
        var event = new AutopunktStatusEvent(behandlingskontrollKontekst, List.of(autopunktTidligSøknad));
        observer.sendBrevForAutopunkt(event);
        verify(sendBrevForAutopunkt, times(1)).sendBrevForTidligSøknad(any());
    }

    @Test
    void skalSendeBrevForEtterkontroll() {
        var event = new AutopunktStatusEvent(behandlingskontrollKontekst, List.of(autopunktEtterkontroll));
        observer.sendBrevForAutopunkt(event);
        verify(sendBrevForAutopunkt, times(1)).sendBrevForEtterkontroll(any());
    }

}
