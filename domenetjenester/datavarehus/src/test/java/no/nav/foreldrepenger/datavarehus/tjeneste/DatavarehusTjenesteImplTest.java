package no.nav.foreldrepenger.datavarehus.tjeneste;

import static no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling.forceOppdaterBehandlingSteg;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_NFP;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.AKSJONSPUNKT_DEF;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.ANNEN_PART_AKTØR_ID;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.ANSVARLIG_BESLUTTER;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.ANSVARLIG_SAKSBEHANDLER;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.BEHANDLENDE_ENHET;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.BEHANDLING_STEG_STATUS;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.BEHANDLING_STEG_TYPE;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.BRUKER_AKTØR_ID;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.IVERKSETTING_STATUS;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.SAKSNUMMER;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.VEDTAK_DATO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegTilstandSnapshot;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageFormkravEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageMedholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingOmgjør;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioKlageEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.datavarehus.domene.AksjonspunktDvh;
import no.nav.foreldrepenger.datavarehus.domene.BehandlingDvh;
import no.nav.foreldrepenger.datavarehus.domene.BehandlingStegDvh;
import no.nav.foreldrepenger.datavarehus.domene.BehandlingVedtakDvh;
import no.nav.foreldrepenger.datavarehus.domene.DatavarehusRepository;
import no.nav.foreldrepenger.datavarehus.domene.FagsakDvh;
import no.nav.foreldrepenger.datavarehus.domene.KlageFormkravDvh;
import no.nav.foreldrepenger.datavarehus.domene.KlageVurderingResultatDvh;
import no.nav.foreldrepenger.datavarehus.domene.VedtakUtbetalingDvh;
import no.nav.foreldrepenger.datavarehus.xml.DvhVedtakXmlTjeneste;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.TotrinnRepository;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.es.RegisterInnhentingIntervall;
import no.nav.foreldrepenger.skjæringstidspunkt.es.SkjæringstidspunktTjenesteImpl;

@ExtendWith(MockitoExtension.class)
@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class DatavarehusTjenesteImplTest {

    @Mock
    private DatavarehusRepository datavarehusRepository;
    @Mock
    private DvhVedtakXmlTjeneste dvhVedtakTjenesteEngangsstønad;
    @Mock
    private TotrinnRepository totrinnRepository;
    @Mock
    private AnkeRepository ankeRepository;
    @Mock
    private KlageRepository klageRepository;
    @Mock
    private MottatteDokumentRepository mottatteDokumentRepository;
    @Mock
    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;
    @Mock
    private MottattDokument mottattDokument;

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    public void setUp(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider,
            new RegisterInnhentingIntervall(Period.of(1, 0, 0), Period.of(0, 6, 0)));
    }

    @Test
    public void lagreNedFagsak() {
        ScenarioMorSøkerEngangsstønad scenario = opprettFørstegangssøknad();
        Behandling behandling = scenario.lagre(repositoryProvider);
        Fagsak fagsak = behandling.getFagsak();

        ArgumentCaptor<FagsakDvh> captor = ArgumentCaptor.forClass(FagsakDvh.class);

        DatavarehusTjeneste datavarehusTjeneste = nyDatavarehusTjeneste(repositoryProvider);
        datavarehusTjeneste.lagreNedFagsak(fagsak.getId());

        verify(datavarehusRepository).lagre(captor.capture());
        FagsakDvh fagsakDvh = captor.getValue();
        assertThat(fagsakDvh.getFagsakId()).isEqualTo(fagsak.getId());
    }

    private DatavarehusTjenesteImpl nyDatavarehusTjeneste(BehandlingRepositoryProvider repositoryProvider) {
        return new DatavarehusTjenesteImpl(repositoryProvider, datavarehusRepository, repositoryProvider.getBehandlingsresultatRepository(),
            totrinnRepository, ankeRepository, klageRepository, mottatteDokumentRepository,
            dvhVedtakTjenesteEngangsstønad, foreldrepengerUttakTjeneste, skjæringstidspunktTjeneste);
    }

    @Test
    public void lagreNedAksjonspunkter() {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_TERMINBEKREFTELSE, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.medBehandlingStegStart(BEHANDLING_STEG_TYPE);
        scenario.medBehandlendeEnhet(BEHANDLENDE_ENHET);
        Behandling behandling = scenario.lagre(repositoryProvider);
        behandling.setAnsvarligBeslutter(ANSVARLIG_BESLUTTER);
        behandling.setAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER);

        List<Aksjonspunkt> aksjonspunkter = new ArrayList<>(behandling.getAksjonspunkter());
        ArgumentCaptor<AksjonspunktDvh> captor = ArgumentCaptor.forClass(AksjonspunktDvh.class);
        DatavarehusTjeneste datavarehusTjeneste = nyDatavarehusTjeneste(repositoryProvider);

        // Act
        datavarehusTjeneste.lagreNedAksjonspunkter(aksjonspunkter, behandling.getId(), BEHANDLING_STEG_TYPE);

        verify(datavarehusRepository, times(2)).lagre(captor.capture());
        List<AksjonspunktDvh> aksjonspunktDvhList = captor.getAllValues();
        assertThat(aksjonspunktDvhList.get(0).getAksjonspunktId()).isEqualTo(aksjonspunkter.get(0).getId());
        assertThat(aksjonspunktDvhList.get(0).getBehandlingId()).isEqualTo(behandling.getId());
        assertThat(aksjonspunktDvhList.get(0).getBehandlingStegId())
            .isEqualTo(behandling.getBehandlingStegTilstand(BEHANDLING_STEG_TYPE).get().getId());
        assertThat(aksjonspunktDvhList.get(1).getAksjonspunktId()).isEqualTo(aksjonspunkter.get(1).getId());
    }

    @Test
    public void lagreNedBehandlingStegTilstand() {
        Behandling behandling = ScenarioMorSøkerEngangsstønad.forFødsel().lagMocked();
        BehandlingStegTilstandSnapshot behandlingStegTilstand = new BehandlingStegTilstandSnapshot(behandling.getId(),
            BEHANDLING_STEG_TYPE, BEHANDLING_STEG_STATUS);

        ArgumentCaptor<BehandlingStegDvh> captor = ArgumentCaptor.forClass(BehandlingStegDvh.class);

        DatavarehusTjeneste datavarehusTjeneste = nyDatavarehusTjeneste(repositoryProvider);
        datavarehusTjeneste.lagreNedBehandlingStegTilstand(behandling.getId(), behandlingStegTilstand);

        verify(datavarehusRepository).lagre(captor.capture());

        BehandlingStegDvh behandlingStegDvh = captor.getValue();
        assertThat(behandlingStegDvh.getBehandlingStegId()).isEqualTo(behandlingStegTilstand.getId());
        assertThat(behandlingStegDvh.getBehandlingId()).isEqualTo(behandling.getId());
    }

    @Test
    public void lagreNedBehandling() {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_TERMINBEKREFTELSE, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.medBehandlendeEnhet(BEHANDLENDE_ENHET);
        Behandling behandling = scenario.lagMocked();
        forceOppdaterBehandlingSteg(behandling, BEHANDLING_STEG_TYPE);
        behandling.setAnsvarligBeslutter(ANSVARLIG_BESLUTTER);
        behandling.setAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER);

        ArgumentCaptor<BehandlingDvh> captor = ArgumentCaptor.forClass(BehandlingDvh.class);
        DatavarehusTjeneste datavarehusTjeneste = nyDatavarehusTjeneste(scenario.mockBehandlingRepositoryProvider());
        datavarehusTjeneste.lagreNedBehandling(behandling.getId());
        // Act
        verify(datavarehusRepository).lagre(captor.capture());

        assertThat(captor.getValue().getBehandlingId()).isEqualTo(behandling.getId());
    }

    @Test
    public void lagreNedBehandlingMedMottattSøknadDokument() {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_TERMINBEKREFTELSE, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.medBehandlendeEnhet(BEHANDLENDE_ENHET);
        Behandling behandling = scenario.lagMocked();
        forceOppdaterBehandlingSteg(behandling, BEHANDLING_STEG_TYPE);
        behandling.setAnsvarligBeslutter(ANSVARLIG_BESLUTTER);
        behandling.setAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER);
        BehandlingRepositoryProvider behandlingRepositoryProvider = scenario.mockBehandlingRepositoryProvider();

        // Simuler mottatt dokument
        when(mottattDokument.getDokumentType()).thenReturn(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL);
        when(mottattDokument.getMottattTidspunkt()).thenReturn(LocalDateTime.now().minusDays(3));
        List<MottattDokument> mottatteDokumenter = new ArrayList<>();
        mottatteDokumenter.add(mottattDokument);
        when(mottatteDokumentRepository.hentMottatteDokument(behandling.getId())).thenReturn(mottatteDokumenter);

        ArgumentCaptor<BehandlingDvh> captor = ArgumentCaptor.forClass(BehandlingDvh.class);
        DatavarehusTjeneste datavarehusTjeneste = nyDatavarehusTjeneste(behandlingRepositoryProvider);
        datavarehusTjeneste.lagreNedBehandling(behandling.getId());
        // Act
        verify(datavarehusRepository).lagre(captor.capture());

        assertThat(captor.getValue().getBehandlingId()).isEqualTo(behandling.getId());
        assertThat(captor.getValue().getMottattTidspunkt()).isEqualTo(mottattDokument.getMottattTidspunkt());
    }

    @Test
    public void lagreNedBehandlingMedId() {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_TERMINBEKREFTELSE, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.medBehandlendeEnhet(BEHANDLENDE_ENHET);

        Behandling behandling = scenario.lagMocked();
        forceOppdaterBehandlingSteg(behandling, BEHANDLING_STEG_TYPE);
        behandling.setAnsvarligBeslutter(ANSVARLIG_BESLUTTER);
        behandling.setAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER);

        ArgumentCaptor<BehandlingDvh> captor = ArgumentCaptor.forClass(BehandlingDvh.class);
        DatavarehusTjeneste datavarehusTjeneste = nyDatavarehusTjeneste(scenario.mockBehandlingRepositoryProvider());
        // Act
        datavarehusTjeneste.lagreNedBehandling(behandling.getId());

        verify(datavarehusRepository).lagre(captor.capture());
        assertThat(captor.getValue().getBehandlingId()).isEqualTo(behandling.getId());
    }

    @Test
    public void lagreNedVedtak() {
        var vedtak = byggBehandlingVedtak();
        var behandling = behandlingRepository.hentBehandling(vedtak.getBehandlingsresultat().getBehandlingId());
        var captor = ArgumentCaptor.forClass(BehandlingVedtakDvh.class);
        var datavarehusTjeneste = nyDatavarehusTjeneste(repositoryProvider);
        // Act
        datavarehusTjeneste.lagreNedVedtak(vedtak, behandling);

        verify(datavarehusRepository).lagre(captor.capture());
        assertThat(captor.getValue().getBehandlingId()).isEqualTo(behandling.getId());
    }

    @Test
    public void skal_lagre_Ned_Vedtak_Xml() {
        BehandlingVedtak vedtak = byggBehandlingVedtak();
        Long behandlingId = vedtak.getBehandlingsresultat().getBehandlingId();
        ArgumentCaptor<VedtakUtbetalingDvh> captor = ArgumentCaptor.forClass(VedtakUtbetalingDvh.class);
        String xml = "<bob>bob</bob";
        when(dvhVedtakTjenesteEngangsstønad.opprettDvhVedtakXml(any())).thenReturn(xml);

        DatavarehusTjeneste datavarehusTjeneste = nyDatavarehusTjeneste(repositoryProvider);
        // Act
        datavarehusTjeneste.opprettOgLagreVedtakXml(behandlingId);
        verify(datavarehusRepository).lagre(captor.capture());
        assertThat(captor.getValue().getXmlClob()).isEqualTo(xml);
    }

    @Test
    public void lagreKlageFormkrav() {
        ScenarioMorSøkerEngangsstønad scenarioMorSøkerEngangsstønad = opprettFørstegangssøknad();
        Behandling påklagdBehandling = scenarioMorSøkerEngangsstønad.lagMocked();
        ScenarioKlageEngangsstønad scenarioKlageEngangsstønad = ScenarioKlageEngangsstønad.forFormKrav(scenarioMorSøkerEngangsstønad);
        Behandling klageBehandling = scenarioKlageEngangsstønad.lagMocked();
        AksjonspunktTestSupport.setTilUtført(klageBehandling.getAksjonspunktFor(VURDERING_AV_FORMKRAV_KLAGE_NFP), "Begrunnelse");
        var klageResultat = KlageResultatEntitet.builder().medKlageBehandlingId(klageBehandling.getId()).medPåKlagdBehandlingId(påklagdBehandling.getId()).build();

        ArgumentCaptor<KlageFormkravDvh> captor = ArgumentCaptor.forClass(KlageFormkravDvh.class);
        BehandlingRepositoryProvider behandlingRepositoryProvider = scenarioKlageEngangsstønad.mockBehandlingRepositoryProvider();
        DatavarehusTjeneste datavarehusTjeneste = nyDatavarehusTjeneste(behandlingRepositoryProvider);

        KlageFormkravEntitet.Builder formkravBuilder = KlageFormkravEntitet.builder()
            .medKlageResultat(klageResultat)
            .medErFristOverholdt(true)
            .medErKlagerPart(true)
            .medErKonkret(true)
            .medErSignert(true)
            .medGjelderVedtak(true)
            .medKlageVurdertAv(KlageVurdertAv.NFP)
            .medBegrunnelse("dette er en begrunnelse.");
        KlageFormkravEntitet klageFormkrav = formkravBuilder.build();

        when(klageRepository.hentGjeldendeKlageFormkrav(anyLong())).thenReturn(Optional.of(klageFormkrav));

        datavarehusTjeneste.oppdaterHvisKlageEllerAnke(klageBehandling.getId(), klageBehandling.getAksjonspunkter());

        verify(datavarehusRepository).lagre(captor.capture());
        assertThat(captor.getValue().getKlageVurdertAv()).isEqualTo(klageFormkrav.getKlageVurdertAv().getKode());
        assertThat(captor.getValue().getOpprettetTidspunkt()).isEqualTo(klageFormkrav.getOpprettetTidspunkt());
        assertThat(captor.getValue().erFristOverholdt()).isEqualTo(klageFormkrav.erFristOverholdt());
        assertThat(captor.getValue().erKlagerPart()).isEqualTo(klageFormkrav.erKlagerPart());
        assertThat(captor.getValue().erKonkret()).isEqualTo(klageFormkrav.erKonkret());
        assertThat(captor.getValue().erSignert()).isEqualTo(klageFormkrav.erSignert());
    }

    @Test
    public void lagreKlageVurderingResultat() {

        ScenarioMorSøkerEngangsstønad scenarioMorSøkerEngangsstønad = opprettFørstegangssøknad();
        Behandling påklagdBehandling = scenarioMorSøkerEngangsstønad.lagMocked();
        ScenarioKlageEngangsstønad scenarioKlageEngangsstønad = opprettKlageScenario(scenarioMorSøkerEngangsstønad, KlageMedholdÅrsak.NYE_OPPLYSNINGER,
            KlageVurderingOmgjør.GUNST_MEDHOLD_I_KLAGE);
        scenarioKlageEngangsstønad.medAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP, BehandlingStegType.KLAGE_NFP);
        Behandling klageBehandling = scenarioKlageEngangsstønad.lagMocked();
        AksjonspunktTestSupport.setTilUtført(klageBehandling.getAksjonspunktFor(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP), "Blah");
        var klageResultat = KlageResultatEntitet.builder().medKlageBehandlingId(klageBehandling.getId()).medPåKlagdBehandlingId(påklagdBehandling.getId()).build();

        KlageVurderingResultat.Builder klageVurderingResultatBuilder = KlageVurderingResultat.builder();
        klageVurderingResultatBuilder
            .medKlageResultat(klageResultat)
            .medKlageMedholdÅrsak(KlageMedholdÅrsak.NYE_OPPLYSNINGER)
            .medKlageVurdering(KlageVurdering.MEDHOLD_I_KLAGE)
            .medKlageVurderingOmgjør(KlageVurderingOmgjør.GUNST_MEDHOLD_I_KLAGE)
            .medKlageVurdertAv(KlageVurdertAv.NFP);

        KlageVurderingResultat klageVurderingResultat = klageVurderingResultatBuilder.build();

        ArgumentCaptor<KlageVurderingResultatDvh> captor = ArgumentCaptor.forClass(KlageVurderingResultatDvh.class);

        BehandlingRepositoryProvider behandlingRepositoryProvider = scenarioKlageEngangsstønad.mockBehandlingRepositoryProvider();

        when(klageRepository.hentGjeldendeKlageVurderingResultat(any())).thenReturn(Optional.of(klageVurderingResultat));

        DatavarehusTjeneste datavarehusTjeneste = nyDatavarehusTjeneste(behandlingRepositoryProvider);
        datavarehusTjeneste.oppdaterHvisKlageEllerAnke(klageBehandling.getId(), klageBehandling.getAksjonspunkter());

        verify(datavarehusRepository).lagre(captor.capture());
        assertThat(captor.getValue().getOpprettetTidspunkt()).isEqualTo(klageVurderingResultat.getOpprettetTidspunkt());
        assertThat(captor.getValue().getKlageMedholdÅrsak()).isEqualTo(klageVurderingResultat.getKlageMedholdÅrsak().getKode());
        assertThat(captor.getValue().getKlageVurdering()).isEqualTo(klageVurderingResultat.getKlageVurdering().getKode());
        assertThat(captor.getValue().getKlageVurderingOmgjør()).isEqualTo(klageVurderingResultat.getKlageVurderingOmgjør().getKode());
        assertThat(captor.getValue().getKlageVurdertAv()).isEqualTo(klageVurderingResultat.getKlageVurdertAv().getKode());
    }

    private ScenarioKlageEngangsstønad opprettKlageScenario(AbstractTestScenario<?> abstractTestScenario,
                                                            KlageMedholdÅrsak klageMedholdÅrsak, KlageVurderingOmgjør klageVurderingOmgjør) {
        ScenarioKlageEngangsstønad scenario = ScenarioKlageEngangsstønad.forMedholdNFP(abstractTestScenario);
        return scenario.medKlageMedholdÅrsak(klageMedholdÅrsak).medKlageVurderingOmgjør(klageVurderingOmgjør);
    }

    private BehandlingVedtak byggBehandlingVedtak() {
        ScenarioMorSøkerEngangsstønad scenario = opprettFørstegangssøknad();

        Behandling behandling = scenario.lagre(repositoryProvider);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        BehandlingVedtak vedtak = BehandlingVedtak.builder()
            .medAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER)
            .medIverksettingStatus(IVERKSETTING_STATUS)
            .medVedtakstidspunkt(VEDTAK_DATO)
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medBehandlingsresultat(repositoryProvider.getBehandlingsresultatRepository().hent(behandling.getId()))
            .build();
        repositoryProvider.getBehandlingVedtakRepository().lagre(vedtak, behandlingRepository.taSkriveLås(behandling));

        return vedtak;
    }

    private ScenarioMorSøkerEngangsstønad opprettFørstegangssøknad() {
        LocalDate terminDato = LocalDate.now().plusDays(10);

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medBruker(BRUKER_AKTØR_ID, NavBrukerKjønn.KVINNE)
            .medSaksnummer(SAKSNUMMER);
        scenario.medSøknadAnnenPart().medAktørId(ANNEN_PART_AKTØR_ID);

        scenario.medBekreftetHendelse()
            .medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
                .medNavnPå("Lege Legesen")
                .medTermindato(terminDato)
                .medUtstedtDato(terminDato.minusDays(40)))
            .medAntallBarn(1);

        return scenario;
    }

}
