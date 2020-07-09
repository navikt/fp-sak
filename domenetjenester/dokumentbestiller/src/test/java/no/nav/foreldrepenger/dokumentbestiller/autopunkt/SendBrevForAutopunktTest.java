package no.nav.foreldrepenger.dokumentbestiller.autopunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.MockitoAnnotations.initMocks;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.Spy;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerApplikasjonTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class SendBrevForAutopunktTest {

    @Spy
    DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste;
    @Spy
    DokumentBehandlingTjeneste dokumentBehandlingTjeneste;

    Aksjonspunkt aksjonspunkt;

    private SendBrevForAutopunkt sendBrevForAutopunkt;

    private Behandling behandling;
    private ScenarioMorSøkerForeldrepenger scenario;
    private BehandlingRepositoryProvider repositoryProvider;
    private AksjonspunktTestSupport aksjonspunktRepository = new AksjonspunktTestSupport();

    @Before
    public void setUp() {
        initMocks(this);
        scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medDefaultBekreftetTerminbekreftelse();
        repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VENT_PÅ_FØDSEL, BehandlingStegType.KONTROLLER_FAKTA);
        behandling = scenario.lagMocked();
        aksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VENT_PÅ_FØDSEL).get();

        aksjonspunktRepository.setFrist(aksjonspunkt, LocalDateTime.now().plusWeeks(4), Venteårsak.AVV_FODSEL);

        sendBrevForAutopunkt = new SendBrevForAutopunkt(dokumentBestillerApplikasjonTjeneste,
            dokumentBehandlingTjeneste,
            repositoryProvider);

        doReturn(false).when(dokumentBehandlingTjeneste).erDokumentBestilt(Mockito.eq(behandling.getId()), Mockito.any());
        doNothing().when(dokumentBestillerApplikasjonTjeneste).bestillDokument(Mockito.any(), Mockito.eq(HistorikkAktør.VEDTAKSLØSNINGEN), Mockito.anyBoolean());
    }

    @Test
    public void sendBrevForSøknadIkkeMottattFørsteGang() {
        sendBrevForAutopunkt.sendBrevForSøknadIkkeMottatt(behandling, aksjonspunkt);
        Mockito.verify(dokumentBestillerApplikasjonTjeneste, times(1)).bestillDokument(Mockito.any(), Mockito.eq(HistorikkAktør.VEDTAKSLØSNINGEN), Mockito.anyBoolean());
    }

    @Test
    public void sendBrevForSøknadIkkeMottattFørsteGangInfoBrev() {
        ScenarioMorSøkerForeldrepenger scenarioMorSøkerForeldrepenger = ScenarioMorSøkerForeldrepenger.forFødsel().medDefaultBekreftetTerminbekreftelse();
        scenarioMorSøkerForeldrepenger.leggTilAksjonspunkt(AksjonspunktDefinisjon.VENT_PÅ_SØKNAD, BehandlingStegType.REGISTRER_SØKNAD);
        Behandling behandling = scenarioMorSøkerForeldrepenger.lagMocked();
        BehandlingÅrsakType oppholdÅrsak = BehandlingÅrsakType.INFOBREV_BEHANDLING;
        new BehandlingÅrsak.Builder(List.of(oppholdÅrsak)).buildFor(behandling);
        Aksjonspunkt autopunkt = behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VENT_PÅ_SØKNAD).get();
        aksjonspunktRepository.setFrist(autopunkt, LocalDate.now().plusWeeks(3).atStartOfDay(), Venteårsak.VENT_SØKNAD_SENDT_INFORMASJONSBREV);
        doReturn(false).when(dokumentBehandlingTjeneste).erDokumentBestilt(Mockito.eq(behandling.getId()), Mockito.any());
        doNothing().when(dokumentBestillerApplikasjonTjeneste).bestillDokument(Mockito.any(), Mockito.eq(HistorikkAktør.VEDTAKSLØSNINGEN), Mockito.anyBoolean());

        sendBrevForAutopunkt.sendBrevForSøknadIkkeMottatt(behandling, autopunkt);
        Mockito.verify(dokumentBestillerApplikasjonTjeneste, times(1)).bestillDokument(Mockito.any(), Mockito.eq(HistorikkAktør.VEDTAKSLØSNINGEN), Mockito.anyBoolean());
    }
    @Test
    public void sendBrevForSøknadIkkeMottattFørsteGangInfoBrevOpphold() {
        ScenarioMorSøkerForeldrepenger scenarioMorSøkerForeldrepenger = ScenarioMorSøkerForeldrepenger.forFødsel().medDefaultBekreftetTerminbekreftelse();
        scenarioMorSøkerForeldrepenger.leggTilAksjonspunkt(AksjonspunktDefinisjon.VENT_PÅ_SØKNAD, BehandlingStegType.REGISTRER_SØKNAD);
        Behandling behandling = scenarioMorSøkerForeldrepenger.lagMocked();
        BehandlingÅrsakType oppholdÅrsak = BehandlingÅrsakType.INFOBREV_OPPHOLD;
        new BehandlingÅrsak.Builder(List.of(oppholdÅrsak)).buildFor(behandling);
        Aksjonspunkt autopunkt = behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VENT_PÅ_SØKNAD).get();
        aksjonspunktRepository.setFrist(autopunkt, LocalDate.now().plusWeeks(3).atStartOfDay(), Venteårsak.VENT_SØKNAD_SENDT_INFORMASJONSBREV);
        doReturn(false).when(dokumentBehandlingTjeneste).erDokumentBestilt(Mockito.eq(behandling.getId()), Mockito.any());
        doNothing().when(dokumentBestillerApplikasjonTjeneste).bestillDokument(Mockito.any(), Mockito.eq(HistorikkAktør.VEDTAKSLØSNINGEN), Mockito.anyBoolean());

        sendBrevForAutopunkt.sendBrevForSøknadIkkeMottatt(behandling, autopunkt);
        Mockito.verify(dokumentBestillerApplikasjonTjeneste, times(1)).bestillDokument(Mockito.any(), Mockito.eq(HistorikkAktør.VEDTAKSLØSNINGEN), Mockito.anyBoolean());
    }
    @Test

    public void skalBareSendeBrevForSøknadIkkeMottattFørsteGang() {
        ScenarioMorSøkerForeldrepenger scenarioMorSøkerForeldrepenger = ScenarioMorSøkerForeldrepenger.forFødsel().medDefaultBekreftetTerminbekreftelse();
        scenarioMorSøkerForeldrepenger.leggTilAksjonspunkt(AksjonspunktDefinisjon.VENT_PÅ_SØKNAD, BehandlingStegType.REGISTRER_SØKNAD);
        Behandling behandling = scenarioMorSøkerForeldrepenger.lagMocked();
        Aksjonspunkt autopunkt = behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VENT_PÅ_SØKNAD).get();
        aksjonspunktRepository.setFrist(autopunkt, LocalDate.now().plusWeeks(3).atStartOfDay(), Venteårsak.AVV_DOK);
        doReturn(true).when(dokumentBehandlingTjeneste).erDokumentBestilt(behandling.getId(), DokumentMalType.INNTEKTSMELDING_FOR_TIDLIG_DOK);
        doNothing().when(dokumentBestillerApplikasjonTjeneste).bestillDokument(Mockito.any(), Mockito.eq(HistorikkAktør.VEDTAKSLØSNINGEN), Mockito.anyBoolean());
        sendBrevForAutopunkt.sendBrevForSøknadIkkeMottatt(behandling, autopunkt);
        Mockito.verify(dokumentBestillerApplikasjonTjeneste, times(0)).bestillDokument(Mockito.any(), Mockito.eq(HistorikkAktør.VEDTAKSLØSNINGEN), Mockito.anyBoolean());
    }

    @Test
    public void sendBrevForTidligSøknadFørsteGang() {
        sendBrevForAutopunkt.sendBrevForTidligSøknad(behandling, aksjonspunkt);
        Mockito.verify(dokumentBestillerApplikasjonTjeneste, times(1)).bestillDokument(Mockito.any(), Mockito.eq(HistorikkAktør.VEDTAKSLØSNINGEN), Mockito.anyBoolean());
        assertThat(behandling.getBehandlingstidFrist()).isEqualTo(LocalDate.from(aksjonspunkt.getFristTid().toLocalDate().plusWeeks(behandling.getType().getBehandlingstidFristUker())));
    }

    @Test
    public void sendBrevForTidligSøknadBareEnGang() {
        doReturn(true).when(dokumentBehandlingTjeneste).erDokumentBestilt(behandling.getId(), DokumentMalType.FORLENGET_TIDLIG_SOK);
        sendBrevForAutopunkt.sendBrevForTidligSøknad(behandling, aksjonspunkt);
        Mockito.verify(dokumentBestillerApplikasjonTjeneste, times(0)).bestillDokument(Mockito.any(), Mockito.eq(HistorikkAktør.VEDTAKSLØSNINGEN), Mockito.anyBoolean());
    }

    @Test
    public void sendBrevForVenterPåFødsel() {
        Aksjonspunkt spyAp = Mockito.spy(aksjonspunkt);
        sendBrevForAutopunkt.sendBrevForVenterPåFødsel(behandling, spyAp);
        Mockito.verify(dokumentBestillerApplikasjonTjeneste, times(1)).bestillDokument(Mockito.any(), Mockito.eq(HistorikkAktør.VEDTAKSLØSNINGEN), Mockito.anyBoolean());
        assertThat(behandling.getBehandlingstidFrist()).isAfter(LocalDate.now());
    }

    @Test
    public void sendBrevForVenterFødselBareEnGang() {
        doReturn(true).when(dokumentBehandlingTjeneste).erDokumentBestilt(behandling.getId(), DokumentMalType.FORLENGET_MEDL_DOK);
        Aksjonspunkt spyAp = Mockito.spy(aksjonspunkt);
        sendBrevForAutopunkt.sendBrevForVenterPåFødsel(behandling, spyAp);
        Mockito.verify(dokumentBestillerApplikasjonTjeneste, times(0)).bestillDokument(Mockito.any(), Mockito.eq(HistorikkAktør.VEDTAKSLØSNINGEN), Mockito.anyBoolean());
    }

}
