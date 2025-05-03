package no.nav.foreldrepenger.domene.fpinntektsmelding;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;

@ExtendWith(MockitoExtension.class)
class LukkForespørselObserverTest {
    @Mock
    private FpInntektsmeldingTjeneste fpInntektsmeldingTjeneste;
    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;
    @Mock
    private FagsakRepository fagsakRepository;

    private LukkForespørselObserver lukkForespørselObserver;

    @BeforeEach
    void setUp() {
        lukkForespørselObserver = new LukkForespørselObserver(fpInntektsmeldingTjeneste, behandlingRepository, behandlingsresultatRepository);
    }

    @Test
    void observer_mottattImEvent() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.SVANGERSKAPSPENGER, null);
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        var orgnr = new OrgNummer(KUNSTIG_ORG);

        var event = new LukkForespørselForMottattImEvent(behandling, orgnr);

        lukkForespørselObserver.observerLukkForespørselForMotattImEvent(event);

        verify(fpInntektsmeldingTjeneste, times(1)).lagLukkForespørselTask(behandling, orgnr, ForespørselStatus.UTFØRT);

    }
    @Test
    void observer_behandling_avsluttet_event_skal_sette_forespørsel_utgått() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, null);
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();

        var behandlingsres = new Behandlingsresultat.Builder().medBehandlingResultatType(BehandlingResultatType.MERGET_OG_HENLAGT).build();
        BehandlingStatusEvent.BehandlingAvsluttetEvent event = BehandlingStatusEvent.nyEvent(byggKontekst(behandling), BehandlingStatus.AVSLUTTET);

        behandling.setBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        when(behandlingRepository.hentBehandling(behandling.getId())).thenReturn(behandling);
        when(behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())).thenReturn(Optional.of(behandlingsres));

        lukkForespørselObserver.observerBehandlingAvsluttetEvent(event);

        verify(fpInntektsmeldingTjeneste, times(1)).lagLukkForespørselTask(behandling, null, ForespørselStatus.UTGÅTT);
    }

    @Test
    void observer_behandling_avsluttet_event_skal_ikke_sette_forespørsel_utgått() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, null);
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();

        var behandlingsres = new Behandlingsresultat.Builder().medBehandlingResultatType(BehandlingResultatType.MERGET_OG_HENLAGT).build();
        BehandlingStatusEvent.BehandlingAvsluttetEvent event = BehandlingStatusEvent.nyEvent(byggKontekst(behandling), BehandlingStatus.AVSLUTTET);

        behandling.setBehandlingType(BehandlingType.REVURDERING);
        when(behandlingRepository.hentBehandling(behandling.getId())).thenReturn(behandling);
        when(behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())).thenReturn(Optional.of(behandlingsres));

        lukkForespørselObserver.observerBehandlingAvsluttetEvent(event);

        verify(fpInntektsmeldingTjeneste, times(0)).lagLukkForespørselTask(behandling, null, ForespørselStatus.UTGÅTT);
    }

    private BehandlingskontrollKontekst byggKontekst(Behandling behandling) {
        var behandlingLås = new BehandlingLås(behandling.getId());
        return new BehandlingskontrollKontekst(behandling, behandlingLås);
    }
}
