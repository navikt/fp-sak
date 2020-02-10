package no.nav.foreldrepenger.dokumentbestiller.observers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.dokumentbestiller.autopunkt.SendBrevForAutopunkt;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

public class SendBrevForAutopunktEventObserverTest {

    @Rule
    public RepositoryRule repositoryRule = new UnittestRepositoryRule();

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

    @Before
    public void setUp() {
        initMocks(this);

        AksjonspunktDefinisjon autopunktDefinisjonIngenSøknad = AksjonspunktDefinisjon.VENT_PÅ_SØKNAD;
        AksjonspunktDefinisjon autopunktDefinisjonTidligSøknad = AksjonspunktDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD;
        AksjonspunktDefinisjon autopunktDefinisjonVentFødsel = AksjonspunktDefinisjon.VENT_PÅ_FØDSEL;
        AksjonspunktDefinisjon autopunktDefinisjonOpptjening = AksjonspunktDefinisjon.AUTO_VENT_PÅ_OPPTJENINGSOPPLYSNINGER;
        AksjonspunktDefinisjon autopunktDefinisjonEtterkontroll = AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_REVURDERING;

        AksjonspunktDefinisjon manuellpunktDefinisjon = AksjonspunktDefinisjon.MANUELL_VURDERING_AV_MEDLEMSKAP;

        when(manuellpunkt.getAksjonspunktDefinisjon()).thenReturn(manuellpunktDefinisjon);

        when(autopunktIngenSøknad.getAksjonspunktDefinisjon()).thenReturn(autopunktDefinisjonIngenSøknad);
        when(autopunktVentFødsel.getAksjonspunktDefinisjon()).thenReturn(autopunktDefinisjonVentFødsel);
        when(autopunktTidligSøknad.getAksjonspunktDefinisjon()).thenReturn(autopunktDefinisjonTidligSøknad);
        when(autopunktOpptjening.getAksjonspunktDefinisjon()).thenReturn(autopunktDefinisjonOpptjening);
        when(autopunktEtterkontroll.getAksjonspunktDefinisjon()).thenReturn(autopunktDefinisjonEtterkontroll);

        when(manuellpunkt.erOpprettet()).thenReturn(true);
        when(autopunktIngenSøknad.erOpprettet()).thenReturn(true);
        when(autopunktVentFødsel.erOpprettet()).thenReturn(true);
        when(autopunktTidligSøknad.erOpprettet()).thenReturn(true);
        when(autopunktOpptjening.erOpprettet()).thenReturn(true);
        when(autopunktEtterkontroll.erOpprettet()).thenReturn(true);

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
        verify(sendBrevForAutopunkt, times(0)).sendBrevForVenterPåOpptjening(any(), any());
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
    public void skalSendeBrevForVenterOpptjening() {
        var event = new AksjonspunktStatusEvent(behandlingskontrollKontekst, List.of(autopunktOpptjening), null);
        observer.sendBrevForAutopunkt(event);
        verify(sendBrevForAutopunkt, times(1)).sendBrevForVenterPåOpptjening(any(), any());
    }

    @Test
    public void skalSendeBrevForEtterkontroll() {
        var event = new AksjonspunktStatusEvent(behandlingskontrollKontekst, List.of(autopunktEtterkontroll), null);
        observer.sendBrevForAutopunkt(event);
        verify(sendBrevForAutopunkt, times(1)).sendBrevForEtterkontroll(any());
    }

}
