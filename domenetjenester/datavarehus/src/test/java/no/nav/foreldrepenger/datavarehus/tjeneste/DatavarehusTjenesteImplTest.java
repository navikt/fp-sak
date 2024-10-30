package no.nav.foreldrepenger.datavarehus.tjeneste;

import static no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling.forceOppdaterBehandlingSteg;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_NFP;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.AKSJONSPUNKT_DEF;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.ANNEN_PART_AKTØR_ID;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.ANSVARLIG_BESLUTTER;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.ANSVARLIG_SAKSBEHANDLER;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.BEHANDLENDE_ENHET;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.BEHANDLING_STEG_TYPE;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.BRUKER_AKTØR_ID;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.IVERKSETTING_STATUS;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.SAKSNUMMER;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.VEDTAK_DATO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageFormkravEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageHjemmel;
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
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioKlageEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.datavarehus.domene.BehandlingDvh;
import no.nav.foreldrepenger.datavarehus.domene.DatavarehusRepository;
import no.nav.foreldrepenger.datavarehus.domene.KlageFormkravDvh;
import no.nav.foreldrepenger.datavarehus.domene.KlageVurderingResultatDvh;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.es.SkjæringstidspunktTjenesteImpl;

@ExtendWith(MockitoExtension.class)
@ExtendWith(JpaExtension.class)
class DatavarehusTjenesteImplTest {

    @Mock
    private DatavarehusRepository datavarehusRepository;
    @Mock
    private AnkeRepository ankeRepository;
    @Mock
    private KlageRepository klageRepository;
    @Mock
    private MottatteDokumentRepository mottatteDokumentRepository;
    @Mock
    private MottattDokument mottattDokument;

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    public void setUp(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider);
    }

    private DatavarehusTjenesteImpl nyDatavarehusTjeneste(BehandlingRepositoryProvider repositoryProvider) {
        return new DatavarehusTjenesteImpl(repositoryProvider, datavarehusRepository, repositoryProvider.getBehandlingsresultatRepository(),
            mock(FagsakEgenskapRepository.class), ankeRepository, klageRepository, mottatteDokumentRepository,
            skjæringstidspunktTjeneste, mock(SvangerskapspengerRepository.class));
    }

    @Test
    void lagreNedBehandling() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_TERMINBEKREFTELSE, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.medBehandlendeEnhet(BEHANDLENDE_ENHET);
        var behandling = scenario.lagMocked();
        forceOppdaterBehandlingSteg(behandling, BEHANDLING_STEG_TYPE);
        behandling.setAnsvarligBeslutter(ANSVARLIG_BESLUTTER);
        behandling.setAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER);

        var captor = ArgumentCaptor.forClass(BehandlingDvh.class);
        DatavarehusTjeneste datavarehusTjeneste = nyDatavarehusTjeneste(scenario.mockBehandlingRepositoryProvider());
        datavarehusTjeneste.lagreNedBehandling(behandling.getId());
        // Act
        verify(datavarehusRepository).lagre(captor.capture());

        assertThat(captor.getValue().getBehandlingId()).isEqualTo(behandling.getId());
    }

    @Test
    void lagreNedBehandlingMedMottattSøknadDokument() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_TERMINBEKREFTELSE, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.medBehandlendeEnhet(BEHANDLENDE_ENHET);
        var behandling = scenario.lagMocked();
        forceOppdaterBehandlingSteg(behandling, BEHANDLING_STEG_TYPE);
        behandling.setAnsvarligBeslutter(ANSVARLIG_BESLUTTER);
        behandling.setAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER);
        var behandlingRepositoryProvider = scenario.mockBehandlingRepositoryProvider();

        // Simuler mottatt dokument
        when(mottattDokument.getDokumentType()).thenReturn(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL);
        when(mottattDokument.getMottattTidspunkt()).thenReturn(LocalDateTime.now().minusDays(3));
        when(mottattDokument.getOpprettetTidspunkt()).thenReturn(LocalDateTime.now());
        when(mottattDokument.getJournalpostId()).thenReturn(new JournalpostId("123"));
        List<MottattDokument> mottatteDokumenter = new ArrayList<>();
        mottatteDokumenter.add(mottattDokument);
        when(mottatteDokumentRepository.hentMottatteDokument(behandling.getId())).thenReturn(mottatteDokumenter);

        var captor = ArgumentCaptor.forClass(BehandlingDvh.class);
        DatavarehusTjeneste datavarehusTjeneste = nyDatavarehusTjeneste(behandlingRepositoryProvider);
        datavarehusTjeneste.lagreNedBehandling(behandling.getId());
        // Act
        verify(datavarehusRepository).lagre(captor.capture());

        assertThat(captor.getValue().getBehandlingId()).isEqualTo(behandling.getId());
        assertThat(captor.getValue().getMottattTid()).isEqualTo(mottattDokument.getMottattTidspunkt());
    }

    @Test
    void lagreNedBehandlingMedId() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_TERMINBEKREFTELSE, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.medBehandlendeEnhet(BEHANDLENDE_ENHET);

        var behandling = scenario.lagMocked();
        forceOppdaterBehandlingSteg(behandling, BEHANDLING_STEG_TYPE);
        behandling.setAnsvarligBeslutter(ANSVARLIG_BESLUTTER);
        behandling.setAnsvarligSaksbehandler(ANSVARLIG_SAKSBEHANDLER);

        var captor = ArgumentCaptor.forClass(BehandlingDvh.class);
        DatavarehusTjeneste datavarehusTjeneste = nyDatavarehusTjeneste(scenario.mockBehandlingRepositoryProvider());
        // Act
        datavarehusTjeneste.lagreNedBehandling(behandling.getId());

        verify(datavarehusRepository).lagre(captor.capture());
        assertThat(captor.getValue().getBehandlingId()).isEqualTo(behandling.getId());
    }

    @Test
    void lagreKlageFormkrav() {
        var scenarioMorSøkerEngangsstønad = opprettFørstegangssøknad();
        var påklagdBehandling = scenarioMorSøkerEngangsstønad.lagMocked();
        var scenarioKlageEngangsstønad = ScenarioKlageEngangsstønad.forFormKrav(scenarioMorSøkerEngangsstønad);
        var klageBehandling = scenarioKlageEngangsstønad.lagMocked();
        AksjonspunktTestSupport.setTilUtført(klageBehandling.getAksjonspunktFor(VURDERING_AV_FORMKRAV_KLAGE_NFP), "Begrunnelse");
        var klageResultat = KlageResultatEntitet.builder().medKlageBehandlingId(klageBehandling.getId()).medPåKlagdBehandlingId(påklagdBehandling.getId()).build();

        var captor = ArgumentCaptor.forClass(KlageFormkravDvh.class);
        var behandlingRepositoryProvider = scenarioKlageEngangsstønad.mockBehandlingRepositoryProvider();
        DatavarehusTjeneste datavarehusTjeneste = nyDatavarehusTjeneste(behandlingRepositoryProvider);

        var formkravBuilder = KlageFormkravEntitet.builder()
            .medKlageResultat(klageResultat)
            .medErFristOverholdt(true)
            .medErKlagerPart(true)
            .medErKonkret(true)
            .medErSignert(true)
            .medGjelderVedtak(true)
            .medKlageVurdertAv(KlageVurdertAv.NFP)
            .medBegrunnelse("dette er en begrunnelse.");
        var klageFormkrav = formkravBuilder.build();

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
    void lagreKlageVurderingResultat() {

        var scenarioMorSøkerEngangsstønad = opprettFørstegangssøknad();
        var påklagdBehandling = scenarioMorSøkerEngangsstønad.lagMocked();
        var scenarioKlageEngangsstønad = opprettKlageScenario(scenarioMorSøkerEngangsstønad, KlageMedholdÅrsak.NYE_OPPLYSNINGER,
            KlageVurderingOmgjør.GUNST_MEDHOLD_I_KLAGE);
        scenarioKlageEngangsstønad.medAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP, BehandlingStegType.KLAGE_NFP);
        var klageBehandling = scenarioKlageEngangsstønad.lagMocked();
        AksjonspunktTestSupport.setTilUtført(klageBehandling.getAksjonspunktFor(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP), "Blah");
        var klageResultat = KlageResultatEntitet.builder().medKlageBehandlingId(klageBehandling.getId()).medPåKlagdBehandlingId(påklagdBehandling.getId()).build();

        var klageVurderingResultatBuilder = KlageVurderingResultat.builder();
        klageVurderingResultatBuilder
            .medKlageResultat(klageResultat)
            .medKlageMedholdÅrsak(KlageMedholdÅrsak.NYE_OPPLYSNINGER)
            .medKlageVurdering(KlageVurdering.MEDHOLD_I_KLAGE)
            .medKlageVurderingOmgjør(KlageVurderingOmgjør.GUNST_MEDHOLD_I_KLAGE)
            .medKlageHjemmel(KlageHjemmel.UTSETTELSE)
            .medKlageVurdertAv(KlageVurdertAv.NFP);

        var klageVurderingResultat = klageVurderingResultatBuilder.build();

        var captor = ArgumentCaptor.forClass(KlageVurderingResultatDvh.class);

        var behandlingRepositoryProvider = scenarioKlageEngangsstønad.mockBehandlingRepositoryProvider();

        when(klageRepository.hentGjeldendeKlageVurderingResultat(any())).thenReturn(Optional.of(klageVurderingResultat));

        DatavarehusTjeneste datavarehusTjeneste = nyDatavarehusTjeneste(behandlingRepositoryProvider);
        datavarehusTjeneste.oppdaterHvisKlageEllerAnke(klageBehandling.getId(), klageBehandling.getAksjonspunkter());

        verify(datavarehusRepository).lagre(captor.capture());
        assertThat(captor.getValue().getOpprettetTidspunkt()).isEqualTo(klageVurderingResultat.getOpprettetTidspunkt());
        assertThat(captor.getValue().getKlageMedholdÅrsak()).isEqualTo(klageVurderingResultat.getKlageMedholdÅrsak().getKode());
        assertThat(captor.getValue().getKlageVurdering()).isEqualTo(klageVurderingResultat.getKlageVurdering().getKode());
        assertThat(captor.getValue().getKlageVurderingOmgjør()).isEqualTo(klageVurderingResultat.getKlageVurderingOmgjør().getKode());
        assertThat(captor.getValue().getKlageHjemmel()).isEqualTo(klageVurderingResultat.getKlageHjemmel().getKode());
        assertThat(captor.getValue().getKlageVurdertAv()).isEqualTo(klageVurderingResultat.getKlageVurdertAv().getKode());
    }

    private ScenarioKlageEngangsstønad opprettKlageScenario(AbstractTestScenario<?> abstractTestScenario,
                                                            KlageMedholdÅrsak klageMedholdÅrsak, KlageVurderingOmgjør klageVurderingOmgjør) {
        var scenario = ScenarioKlageEngangsstønad.forMedholdNFP(abstractTestScenario);
        return scenario.medKlageMedholdÅrsak(klageMedholdÅrsak).medKlageVurderingOmgjør(klageVurderingOmgjør);
    }

    private BehandlingVedtak byggBehandlingVedtak() {
        var scenario = opprettFørstegangssøknad();

        var behandling = scenario.lagre(repositoryProvider);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var vedtak = BehandlingVedtak.builder()
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
        var terminDato = LocalDate.now().plusDays(10);

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel()
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
