package no.nav.foreldrepenger.dokumentbestiller.autopunkt;

import static org.assertj.core.api.Assertions.assertThat;
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
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;

@ExtendWith(MockitoExtension.class)
public class SendBrevForAutopunktTest {

    @Spy
    DokumentBestillerTjeneste dokumentBestillerTjeneste;
    @Spy
    DokumentBehandlingTjeneste dokumentBehandlingTjeneste;

    Aksjonspunkt aksjonspunkt;

    private SendBrevForAutopunkt sendBrevForAutopunkt;

    private Behandling behandling;

    @BeforeEach
    public void setUp() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medDefaultBekreftetTerminbekreftelse();
        var repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VENT_PÅ_FØDSEL, BehandlingStegType.KONTROLLER_FAKTA);
        behandling = scenario.lagMocked();
        aksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VENT_PÅ_FØDSEL).get();

        AksjonspunktTestSupport.setFrist(aksjonspunkt, LocalDateTime.now().plusWeeks(4), Venteårsak.AVV_FODSEL);

        sendBrevForAutopunkt = new SendBrevForAutopunkt(dokumentBestillerTjeneste,
                dokumentBehandlingTjeneste,
                repositoryProvider);

        lenient().doReturn(false).when(dokumentBehandlingTjeneste).erDokumentBestilt(Mockito.eq(behandling.getId()), Mockito.any());
        lenient().doNothing().when(dokumentBestillerTjeneste).bestillDokument(Mockito.any(), Mockito.eq(HistorikkAktør.VEDTAKSLØSNINGEN));
    }

    @Test
    public void sendBrevForSøknadIkkeMottattFørsteGang() {
        sendBrevForAutopunkt.sendBrevForSøknadIkkeMottatt(behandling, aksjonspunkt);
        Mockito.verify(dokumentBestillerTjeneste, times(1)).bestillDokument(Mockito.any(), Mockito.eq(HistorikkAktør.VEDTAKSLØSNINGEN));
    }

    @Test
    public void sendBrevForSøknadIkkeMottattFørsteGangInfoBrev() {
        var scenarioMorSøkerForeldrepenger = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medDefaultBekreftetTerminbekreftelse();
        scenarioMorSøkerForeldrepenger.leggTilAksjonspunkt(AksjonspunktDefinisjon.VENT_PÅ_SØKNAD, BehandlingStegType.REGISTRER_SØKNAD);
        var behandling = scenarioMorSøkerForeldrepenger.lagMocked();
        var oppholdÅrsak = BehandlingÅrsakType.INFOBREV_BEHANDLING;
        BehandlingÅrsak.builder(List.of(oppholdÅrsak)).buildFor(behandling);
        var autopunkt = behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VENT_PÅ_SØKNAD).get();
        AksjonspunktTestSupport.setFrist(autopunkt, LocalDate.now().plusWeeks(3).atStartOfDay(), Venteårsak.VENT_SØKNAD_SENDT_INFORMASJONSBREV);
        doReturn(false).when(dokumentBehandlingTjeneste).erDokumentBestilt(Mockito.eq(behandling.getId()), Mockito.any());
        doNothing().when(dokumentBestillerTjeneste).bestillDokument(Mockito.any(), Mockito.eq(HistorikkAktør.VEDTAKSLØSNINGEN));

        sendBrevForAutopunkt.sendBrevForSøknadIkkeMottatt(behandling, autopunkt);
        Mockito.verify(dokumentBestillerTjeneste, times(1)).bestillDokument(Mockito.any(), Mockito.eq(HistorikkAktør.VEDTAKSLØSNINGEN));
    }

    @Test
    public void sendBrevForSøknadIkkeMottattFørsteGangInfoBrevOpphold() {
        var scenarioMorSøkerForeldrepenger = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medDefaultBekreftetTerminbekreftelse();
        scenarioMorSøkerForeldrepenger.leggTilAksjonspunkt(AksjonspunktDefinisjon.VENT_PÅ_SØKNAD, BehandlingStegType.REGISTRER_SØKNAD);
        var behandling = scenarioMorSøkerForeldrepenger.lagMocked();
        var oppholdÅrsak = BehandlingÅrsakType.INFOBREV_OPPHOLD;
        BehandlingÅrsak.builder(List.of(oppholdÅrsak)).buildFor(behandling);
        var autopunkt = behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VENT_PÅ_SØKNAD).get();
        AksjonspunktTestSupport.setFrist(autopunkt, LocalDate.now().plusWeeks(3).atStartOfDay(), Venteårsak.VENT_SØKNAD_SENDT_INFORMASJONSBREV);
        doReturn(false).when(dokumentBehandlingTjeneste).erDokumentBestilt(Mockito.eq(behandling.getId()), Mockito.any());
        doNothing().when(dokumentBestillerTjeneste).bestillDokument(Mockito.any(), Mockito.eq(HistorikkAktør.VEDTAKSLØSNINGEN));

        sendBrevForAutopunkt.sendBrevForSøknadIkkeMottatt(behandling, autopunkt);
        Mockito.verify(dokumentBestillerTjeneste, times(1)).bestillDokument(Mockito.any(), Mockito.eq(HistorikkAktør.VEDTAKSLØSNINGEN));
    }

    @Test

    public void skalBareSendeBrevForSøknadIkkeMottattFørsteGang() {
        var scenarioMorSøkerForeldrepenger = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medDefaultBekreftetTerminbekreftelse();
        scenarioMorSøkerForeldrepenger.leggTilAksjonspunkt(AksjonspunktDefinisjon.VENT_PÅ_SØKNAD, BehandlingStegType.REGISTRER_SØKNAD);
        var behandling = scenarioMorSøkerForeldrepenger.lagMocked();
        var autopunkt = behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VENT_PÅ_SØKNAD).get();
        AksjonspunktTestSupport.setFrist(autopunkt, LocalDate.now().plusWeeks(3).atStartOfDay(), Venteårsak.AVV_DOK);
        doReturn(true).when(dokumentBehandlingTjeneste).erDokumentBestilt(behandling.getId(), DokumentMalType.IKKE_SØKT);
        sendBrevForAutopunkt.sendBrevForSøknadIkkeMottatt(behandling, autopunkt);
        Mockito.verify(dokumentBestillerTjeneste, times(0)).bestillDokument(Mockito.any(), Mockito.eq(HistorikkAktør.VEDTAKSLØSNINGEN));
    }

    @Test
    public void sendBrevForTidligSøknadFørsteGang() {
        sendBrevForAutopunkt.sendBrevForTidligSøknad(behandling, aksjonspunkt);
        Mockito.verify(dokumentBestillerTjeneste, times(1)).bestillDokument(Mockito.any(), Mockito.eq(HistorikkAktør.VEDTAKSLØSNINGEN));
        assertThat(behandling.getBehandlingstidFrist())
                .isEqualTo(LocalDate.from(aksjonspunkt.getFristTid().toLocalDate().plusWeeks(behandling.getType().getBehandlingstidFristUker())));
    }

    @Test
    public void sendBrevForTidligSøknadBareEnGang() {
        doReturn(true).when(dokumentBehandlingTjeneste).erDokumentBestilt(behandling.getId(), DokumentMalType.FORLENGET_SAKSBEHANDLINGSTID_TIDLIG);
        sendBrevForAutopunkt.sendBrevForTidligSøknad(behandling, aksjonspunkt);
        Mockito.verify(dokumentBestillerTjeneste, times(0)).bestillDokument(Mockito.any(), Mockito.eq(HistorikkAktør.VEDTAKSLØSNINGEN));
    }

    @Test
    public void sendBrevForVenterPåFødsel() {
        var spyAp = Mockito.spy(aksjonspunkt);
        sendBrevForAutopunkt.sendBrevForVenterPåFødsel(behandling, spyAp);
        Mockito.verify(dokumentBestillerTjeneste, times(1)).bestillDokument(Mockito.any(), Mockito.eq(HistorikkAktør.VEDTAKSLØSNINGEN));
        assertThat(behandling.getBehandlingstidFrist()).isAfter(LocalDate.now());
    }

    @Test
    public void sendBrevForVenterFødselBareEnGang() {
        doReturn(true).when(dokumentBehandlingTjeneste).erDokumentBestilt(behandling.getId(), DokumentMalType.FORLENGET_SAKSBEHANDLINGSTID_MEDL);
        var spyAp = Mockito.spy(aksjonspunkt);
        sendBrevForAutopunkt.sendBrevForVenterPåFødsel(behandling, spyAp);
        Mockito.verify(dokumentBestillerTjeneste, times(0)).bestillDokument(Mockito.any(), Mockito.eq(HistorikkAktør.VEDTAKSLØSNINGEN));
    }

}
