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
import no.nav.foreldrepenger.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.dokumentbestiller.autopunkt.SendBrevForAutopunkt;

@ExtendWith(MockitoExtension.class)
public class SendBrevForAutopunktEventObserverTest {

    @Mock
    private Aksjonspunkt autopunktIngenSøknad;
    @Mock
    private Aksjonspunkt autopunktVentFødsel;
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

    private SendBrevForAutopunktEventObserver observer; // objektet vi tester

    private BehandlingskontrollKontekst behandlingskontrollKontekst;

    private Long behandlingId = 1L;

    @BeforeEach
    public void setUp() {

        var autopunktDefinisjonIngenSøknad = AksjonspunktDefinisjon.VENT_PÅ_SØKNAD;
        var autopunktDefinisjonTidligSøknad = AksjonspunktDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD;
        var autopunktDefinisjonVentFødsel = AksjonspunktDefinisjon.VENT_PÅ_FØDSEL;
        var autopunktDefinisjonEtterkontroll = AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_REVURDERING;

        var manuellpunktDefinisjon = AksjonspunktDefinisjon.MANUELL_VURDERING_AV_OMSORGSVILKÅRET;

        lenient().when(manuellpunkt.getAksjonspunktDefinisjon()).thenReturn(manuellpunktDefinisjon);

        lenient().when(autopunktIngenSøknad.getAksjonspunktDefinisjon()).thenReturn(autopunktDefinisjonIngenSøknad);
        lenient().when(autopunktVentFødsel.getAksjonspunktDefinisjon()).thenReturn(autopunktDefinisjonVentFødsel);
        lenient().when(autopunktTidligSøknad.getAksjonspunktDefinisjon()).thenReturn(autopunktDefinisjonTidligSøknad);
        lenient().when(autopunktEtterkontroll.getAksjonspunktDefinisjon()).thenReturn(autopunktDefinisjonEtterkontroll);

        lenient().when(manuellpunkt.erOpprettet()).thenReturn(true);
        lenient().when(autopunktIngenSøknad.erOpprettet()).thenReturn(true);
        lenient().when(autopunktVentFødsel.erOpprettet()).thenReturn(true);
        lenient().when(autopunktTidligSøknad.erOpprettet()).thenReturn(true);
        lenient().when(autopunktOpptjening.erOpprettet()).thenReturn(true);
        lenient().when(autopunktEtterkontroll.erOpprettet()).thenReturn(true);

        behandlingskontrollKontekst = mock(BehandlingskontrollKontekst.class);
        when(behandlingskontrollKontekst.getBehandlingId()).thenReturn(behandlingId);

        observer = new SendBrevForAutopunktEventObserver(behandlingRepository, sendBrevForAutopunkt);
    }

    @Test
    public void skalIkkeSendeBrevForAndreAksjonspunkter() {
        var event = new AksjonspunktStatusEvent(behandlingskontrollKontekst, List.of(manuellpunkt), null);

        observer.sendBrevForAutopunkt(event);

        verify(sendBrevForAutopunkt, times(0)).sendBrevForSøknadIkkeMottatt(any(), any());
        verify(sendBrevForAutopunkt, times(0)).sendBrevForVenterPåFødsel(any(), any());
        verify(sendBrevForAutopunkt, times(0)).sendBrevForTidligSøknad(any(), any());
        verify(sendBrevForAutopunkt, times(0)).oppdaterBehandlingsfristForVenterPåOpptjening(any(), any());
    }

    @Test
    public void skalSendeBrevForSøknadIkkeMottatt() {
        var event = new AksjonspunktStatusEvent(behandlingskontrollKontekst, List.of(autopunktIngenSøknad), null);
        observer.sendBrevForAutopunkt(event);
        verify(sendBrevForAutopunkt, times(1)).sendBrevForSøknadIkkeMottatt(any(), any());
    }

    @Test
    public void skalSendeBrevForTidligSøknad() {
        var event = new AksjonspunktStatusEvent(behandlingskontrollKontekst, List.of(autopunktTidligSøknad), null);
        observer.sendBrevForAutopunkt(event);
        verify(sendBrevForAutopunkt, times(1)).sendBrevForTidligSøknad(any(), any());
    }

    @Test
    public void skalSendeBrevForVenterFødsel() {
        var event = new AksjonspunktStatusEvent(behandlingskontrollKontekst, List.of(autopunktVentFødsel), null);
        observer.sendBrevForAutopunkt(event);
        verify(sendBrevForAutopunkt, times(1)).sendBrevForVenterPåFødsel(any(), any());
    }

    @Test
    public void skalSendeBrevForEtterkontroll() {
        var event = new AksjonspunktStatusEvent(behandlingskontrollKontekst, List.of(autopunktEtterkontroll), null);
        observer.sendBrevForAutopunkt(event);
        verify(sendBrevForAutopunkt, times(1)).sendBrevForEtterkontroll(any());
    }

}
