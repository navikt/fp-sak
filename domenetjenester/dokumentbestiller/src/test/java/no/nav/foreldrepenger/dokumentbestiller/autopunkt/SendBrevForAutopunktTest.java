package no.nav.foreldrepenger.dokumentbestiller.autopunkt;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.DokumentMalType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestilling;

@ExtendWith(MockitoExtension.class)
class SendBrevForAutopunktTest {

    @Spy
    DokumentBestillerTjeneste dokumentBestillerTjeneste;
    @Spy
    DokumentBehandlingTjeneste dokumentBehandlingTjeneste;

    Aksjonspunkt aksjonspunkt;

    private SendBrevForAutopunkt sendBrevForAutopunkt;

    private Behandling behandling;

    @BeforeEach
    void setUp() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medDefaultBekreftetTerminbekreftelse();
        var repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VENT_PÅ_SØKNAD, BehandlingStegType.REGISTRER_SØKNAD);
        behandling = scenario.lagMocked();
        aksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VENT_PÅ_SØKNAD).get();

        AksjonspunktTestSupport.setFrist(aksjonspunkt, LocalDateTime.now().plusWeeks(4), Venteårsak.AVV_FODSEL);

        sendBrevForAutopunkt = new SendBrevForAutopunkt(dokumentBestillerTjeneste,
                dokumentBehandlingTjeneste,
                repositoryProvider);

        lenient().doReturn(false).when(dokumentBehandlingTjeneste).erDokumentBestilt(Mockito.eq(behandling.getId()), Mockito.any());
        lenient().doNothing().when(dokumentBestillerTjeneste).bestillDokument(Mockito.any(DokumentBestilling.class));
    }

    @Test
    void sendBrevForSøknadIkkeMottattFørsteGang() {
        sendBrevForAutopunkt.sendBrevForSøknadIkkeMottatt(behandling);
        Mockito.verify(dokumentBestillerTjeneste, times(1)).bestillDokument(Mockito.any(DokumentBestilling.class));
    }

    @Test
    void sendBrevForSøknadIkkeMottattFørsteGangInfoBrev() {
        var scenarioMorSøkerForeldrepenger = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medDefaultBekreftetTerminbekreftelse();
        scenarioMorSøkerForeldrepenger.leggTilAksjonspunkt(AksjonspunktDefinisjon.VENT_PÅ_SØKNAD, BehandlingStegType.REGISTRER_SØKNAD);
        var behandling = scenarioMorSøkerForeldrepenger.lagMocked();
        var oppholdÅrsak = BehandlingÅrsakType.INFOBREV_BEHANDLING;
        BehandlingÅrsak.builder(List.of(oppholdÅrsak)).buildFor(behandling);
        var autopunkt = behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VENT_PÅ_SØKNAD).get();
        AksjonspunktTestSupport.setFrist(autopunkt, LocalDate.now().plusWeeks(3).atStartOfDay(), Venteårsak.VENT_SØKNAD_SENDT_INFORMASJONSBREV);
        doReturn(false).when(dokumentBehandlingTjeneste).erDokumentBestilt(Mockito.eq(behandling.getId()), Mockito.any());
        doNothing().when(dokumentBestillerTjeneste).bestillDokument(Mockito.any(DokumentBestilling.class));

        sendBrevForAutopunkt.sendBrevForSøknadIkkeMottatt(behandling);
        Mockito.verify(dokumentBestillerTjeneste, times(1)).bestillDokument(Mockito.any(DokumentBestilling.class));
    }

    @Test
    void sendBrevForSøknadIkkeMottattFørsteGangInfoBrevOpphold() {
        var scenarioMorSøkerForeldrepenger = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medDefaultBekreftetTerminbekreftelse();
        scenarioMorSøkerForeldrepenger.leggTilAksjonspunkt(AksjonspunktDefinisjon.VENT_PÅ_SØKNAD, BehandlingStegType.REGISTRER_SØKNAD);
        var behandling = scenarioMorSøkerForeldrepenger.lagMocked();
        var oppholdÅrsak = BehandlingÅrsakType.INFOBREV_OPPHOLD;
        BehandlingÅrsak.builder(List.of(oppholdÅrsak)).buildFor(behandling);
        var autopunkt = behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VENT_PÅ_SØKNAD).get();
        AksjonspunktTestSupport.setFrist(autopunkt, LocalDate.now().plusWeeks(3).atStartOfDay(), Venteårsak.VENT_SØKNAD_SENDT_INFORMASJONSBREV);
        doReturn(false).when(dokumentBehandlingTjeneste).erDokumentBestilt(Mockito.eq(behandling.getId()), Mockito.any());
        doNothing().when(dokumentBestillerTjeneste).bestillDokument(Mockito.any(DokumentBestilling.class));

        sendBrevForAutopunkt.sendBrevForSøknadIkkeMottatt(behandling);
        Mockito.verify(dokumentBestillerTjeneste, times(1)).bestillDokument(Mockito.any(DokumentBestilling.class));
    }

    @Test
    void skalBareSendeBrevForSøknadIkkeMottattFørsteGang() {
        var scenarioMorSøkerForeldrepenger = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medDefaultBekreftetTerminbekreftelse();
        scenarioMorSøkerForeldrepenger.leggTilAksjonspunkt(AksjonspunktDefinisjon.VENT_PÅ_SØKNAD, BehandlingStegType.REGISTRER_SØKNAD);
        var behandling = scenarioMorSøkerForeldrepenger.lagMocked();
        var autopunkt = behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VENT_PÅ_SØKNAD).get();
        AksjonspunktTestSupport.setFrist(autopunkt, LocalDate.now().plusWeeks(3).atStartOfDay(), Venteårsak.AVV_DOK);
        doReturn(true).when(dokumentBehandlingTjeneste).erDokumentBestilt(behandling.getId(), DokumentMalType.IKKE_SØKT);
        sendBrevForAutopunkt.sendBrevForSøknadIkkeMottatt(behandling);
        Mockito.verify(dokumentBestillerTjeneste, times(0)).bestillDokument(Mockito.any(DokumentBestilling.class));
    }

    @Test
    void sendBrevForTidligSøknadFørsteGang() {
        sendBrevForAutopunkt.sendBrevForTidligSøknad(behandling);
        Mockito.verify(dokumentBestillerTjeneste, times(1)).bestillDokument(Mockito.any(DokumentBestilling.class));
    }

    @Test
    void sendBrevForTidligSøknadBareEnGang() {
        doReturn(true).when(dokumentBehandlingTjeneste).erDokumentBestilt(behandling.getId(), DokumentMalType.FORLENGET_SAKSBEHANDLINGSTID_TIDLIG);
        sendBrevForAutopunkt.sendBrevForTidligSøknad(behandling);
        Mockito.verify(dokumentBestillerTjeneste, times(0)).bestillDokument(Mockito.any(DokumentBestilling.class));
    }

}
