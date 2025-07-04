package no.nav.foreldrepenger.datavarehus.tjeneste;

import static no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling.forceOppdaterBehandlingSteg;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.AKSJONSPUNKT_DEF;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.ANSVARLIG_BESLUTTER;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.ANSVARLIG_SAKSBEHANDLER;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.BEHANDLENDE_ENHET;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.BEHANDLING_STEG_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.datavarehus.domene.BehandlingDvh;
import no.nav.foreldrepenger.datavarehus.domene.DatavarehusRepository;
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

    @BeforeEach
    void setUp(EntityManager entityManager) {
        skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(new BehandlingRepositoryProvider(entityManager));
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
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.SJEKK_TERMINBEKREFTELSE, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
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
        assertThat(captor.getValue().getBehandlingUuid()).isEqualTo(behandling.getUuid());
    }

    @Test
    void lagreNedBehandlingMedMottattSøknadDokument() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.SJEKK_TERMINBEKREFTELSE, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
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
        assertThat(captor.getValue().getBehandlingUuid()).isEqualTo(behandling.getUuid());
        assertThat(captor.getValue().getMottattTid()).isEqualTo(mottattDokument.getMottattTidspunkt());
    }

    @Test
    void lagreNedBehandlingMedId() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.SJEKK_TERMINBEKREFTELSE, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
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
        assertThat(captor.getValue().getBehandlingUuid()).isEqualTo(behandling.getUuid());
    }

}
