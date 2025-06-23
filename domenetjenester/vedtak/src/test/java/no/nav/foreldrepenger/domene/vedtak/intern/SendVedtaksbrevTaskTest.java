package no.nav.foreldrepenger.domene.vedtak.intern;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

@ExtendWith(MockitoExtension.class)
class SendVedtaksbrevTaskTest {

    @Mock
    private DokumentBestillerTjeneste dokumentBestillerTjeneste;
    @Mock
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    @Mock
    private RevurderingTjeneste revurderingTjeneste;
    @Mock
    private KlageRepository klageRepository;


    private BehandlingRepositoryProvider repositoryProvider;
    private SendVedtaksbrevTask sendVedtaksbrevTask;

    @BeforeEach
    void setUp() {
        repositoryProvider = ScenarioMorSøkerForeldrepenger.forFødsel().mockBehandlingRepositoryProvider();
        var skalSendeVedtaksbrevUtleder = new VedtaksbrevStatusUtleder(
            repositoryProvider.getBehandlingRepository(),
            repositoryProvider.getBehandlingsresultatRepository(),
            dokumentBehandlingTjeneste,
            klageRepository,
            new UnitTestLookupInstanceImpl<>(revurderingTjeneste));
        sendVedtaksbrevTask = new SendVedtaksbrevTask(repositoryProvider, skalSendeVedtaksbrevUtleder, dokumentBestillerTjeneste);
    }

    @Test
    void send_vedtaksbrev_når_ingen_endring_men_det_foreligger_fritekstbrev() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET).medBeslutning(true);
        scenario.medBehandlingsresultat(Behandlingsresultat.builder().medVedtaksbrev(Vedtaksbrev.FRITEKST));
        var behandling = scenario.lagre(repositoryProvider);
        when(revurderingTjeneste.erRevurderingMedUendretUtfall(behandling)).thenReturn(true);

        // Act
        sendVedtaksbrevTask.prosesser(null, behandling.getId());

        // Assert
        verify(dokumentBestillerTjeneste, times(1)).produserVedtaksbrev(any());
    }

    @Test
    void send_vedtaksbrev_vedtak_innvilget() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET).medBeslutning(true);
        scenario.medBehandlingsresultat(Behandlingsresultat.builder().medVedtaksbrev(Vedtaksbrev.AUTOMATISK));
        var behandling = scenario.lagre(repositoryProvider);

        // Act
        sendVedtaksbrevTask.prosesser(null, behandling.getId());

        // Assert
        verify(dokumentBestillerTjeneste, times(1)).produserVedtaksbrev(any());
    }

    @Test
    void test_ingen_brev_om_vedtaksbrev_INGEN() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingsresultat(Behandlingsresultat.builder().medVedtaksbrev(Vedtaksbrev.INGEN));
        var behandling = scenario.lagre(repositoryProvider);

        // Act
        sendVedtaksbrevTask.prosesser(null, behandling.getId());

        // Assert
        verify(dokumentBestillerTjeneste, never()).produserVedtaksbrev(any());
    }

    @Test
    void send_vedtaksbrev_ved_vedtak_avslag() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.AVSLAG);
        var behandling = scenario.lagre(repositoryProvider);

        // Act
        sendVedtaksbrevTask.prosesser(null, behandling.getId());

        // Assert
        verify(dokumentBestillerTjeneste, times(1)).produserVedtaksbrev(any());
    }

    @Test
    void send_vedtaksbrev_etter_klagebehandling_avvist_NFP() {
        testSendVedtaksbrevEtterKlagebehandling(BehandlingResultatType.KLAGE_AVVIST, KlageVurdering.AVVIS_KLAGE, KlageVurdertAv.NFP);
    }

    @Test
    void send_vedtaksbrev_etter_klagebehandling_avvist_NK() {
        testSendVedtaksbrevEtterKlagebehandling(BehandlingResultatType.KLAGE_AVVIST, KlageVurdering.AVVIS_KLAGE, KlageVurdertAv.NK);
    }

    @Test
    void send_vedtaksbrev_etter_klagebehandling_medhold_NFP() {
        testSendVedtaksbrevEtterKlagebehandling(BehandlingResultatType.KLAGE_MEDHOLD, KlageVurdering.MEDHOLD_I_KLAGE, KlageVurdertAv.NFP);
    }

    @Test
    void send_vedtaksbrev_etter_klagebehandling_medhold_NK() {
        testSendVedtaksbrevEtterKlagebehandling(BehandlingResultatType.KLAGE_MEDHOLD, KlageVurdering.MEDHOLD_I_KLAGE, KlageVurdertAv.NK);
    }

    @Test
    void send_vedtaksbrev_ETTER_KLAGE_hvis_behandlignen_er_en_førstegangsbehandling() {
        var klagebehandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.KLAGE)
            .lagre(repositoryProvider);

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.KLAGE_MEDHOLD));
        scenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.VEDTAK_I_KLAGEBEHANDLING);
        scenario.medFagsakId(klagebehandling.getFagsakId());
        scenario.medOriginalBehandling(klagebehandling, BehandlingÅrsakType.ETTER_KLAGE);
        scenario.medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        var revurderingEtterKlage = scenario.lagre(repositoryProvider);

        when(klageRepository.hentGjeldendeKlageVurderingResultat(klagebehandling)).thenReturn(Optional.of(KlageVurderingResultat.builder()
            .medKlageVurdertAv(KlageVurdertAv.NFP)
            .medKlageVurdering(KlageVurdering.MEDHOLD_I_KLAGE)
            .medKlageResultat(new KlageResultatEntitet())
            .build()));

        // Act
        sendVedtaksbrevTask.prosesser(null, revurderingEtterKlage.getId());

        // Assert
        verify(dokumentBestillerTjeneste, times(1)).produserVedtaksbrev(any());
    }

    @Test
    void ikke_send_vedtaksbrev_ETTER_KLAGE_hvis_behandlignen_er_en_revurdering() {
        var klagebehandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.KLAGE)
            .lagre(repositoryProvider);

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.KLAGE_MEDHOLD));
        scenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.VEDTAK_I_KLAGEBEHANDLING);
        scenario.medFagsakId(klagebehandling.getFagsakId());
        scenario.medOriginalBehandling(klagebehandling, BehandlingÅrsakType.ETTER_KLAGE);
        var revurderingEtterKlage = scenario.lagre(repositoryProvider);

        when(klageRepository.hentGjeldendeKlageVurderingResultat(klagebehandling)).thenReturn(Optional.of(KlageVurderingResultat.builder()
            .medKlageVurdertAv(KlageVurdertAv.NFP)
            .medKlageVurdering(KlageVurdering.MEDHOLD_I_KLAGE)
            .medKlageResultat(new KlageResultatEntitet())
            .build()));

        // Act
        sendVedtaksbrevTask.prosesser(null, revurderingEtterKlage.getId());

        // Assert
        verify(dokumentBestillerTjeneste, never()).produserVedtaksbrev(any());
    }

    @Test
    void send_vedtaksbrev_etter_klagebehandling_opphevet() {
        testSendVedtaksbrevEtterKlagebehandling(BehandlingResultatType.KLAGE_YTELSESVEDTAK_OPPHEVET, KlageVurdering.OPPHEVE_YTELSESVEDTAK, KlageVurdertAv.NK);
    }

    @Test
    void send_vedtaksbrev_etter_klagebehandling_stadfestet() {
        testSendVedtaksbrevEtterKlagebehandling(BehandlingResultatType.KLAGE_YTELSESVEDTAK_STADFESTET, KlageVurdering.STADFESTE_YTELSESVEDTAK, KlageVurdertAv.NK);
    }

    private void testSendVedtaksbrevEtterKlagebehandling(BehandlingResultatType behandlingResultatType, KlageVurdering klageVurdering, KlageVurdertAv klageVurdertAv) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingType(BehandlingType.KLAGE);
        scenario.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(behandlingResultatType));
        scenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.VEDTAK_I_KLAGEBEHANDLING);
        var behandling = scenario.lagre(repositoryProvider);
        when(klageRepository.hentGjeldendeKlageVurderingResultat(behandling)).thenReturn(Optional.of(KlageVurderingResultat.builder()
            .medKlageVurdertAv(klageVurdertAv)
            .medKlageVurdering(klageVurdering)
            .medKlageResultat(new KlageResultatEntitet())
            .build()));

        // Act
        sendVedtaksbrevTask.prosesser(null, behandling.getId());

        // Assert
        verify(dokumentBestillerTjeneste, times(1)).produserVedtaksbrev(any());
    }

    @Test
    void anke_behandling_skal_IKKE_produsere_og_sende_ut_vedtaksbrev() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingType(BehandlingType.ANKE);
        var behandling = scenario.lagre(repositoryProvider);

        // Act
        sendVedtaksbrevTask.prosesser(null, behandling.getId());

        // Assert
        verify(dokumentBestillerTjeneste, never()).produserVedtaksbrev(any());
    }

    @Test
    void sender_brev_om_uendret_utfall_ved_revurdering() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET).medBeslutning(true);
        var behandling = scenario.lagre(repositoryProvider);
        when(revurderingTjeneste.erRevurderingMedUendretUtfall(behandling)).thenReturn(true);
        lenient().when(dokumentBehandlingTjeneste.erDokumentBestilt(behandling.getId(),
            DokumentMalType.VARSEL_OM_REVURDERING)).thenReturn(true);

        // Act
        sendVedtaksbrevTask.prosesser(null, behandling.getId());

        // Assert
        verify(dokumentBestillerTjeneste, times(1)).produserVedtaksbrev(any());
    }

    @Test
    void sender_IKKE_brev_om_uendret_utfall_hvis_det_ikke_er_sendt_varselbrev_om_revurdering() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET).medBeslutning(true);
        var behandling = scenario.lagre(repositoryProvider);
        when(revurderingTjeneste.erRevurderingMedUendretUtfall(behandling)).thenReturn(true);
        lenient().when(dokumentBehandlingTjeneste.erDokumentBestilt(behandling.getId(),
            DokumentMalType.VARSEL_OM_REVURDERING)).thenReturn(false);

        // Act
        sendVedtaksbrevTask.prosesser(null, behandling.getId());

        // Assert
        verify(dokumentBestillerTjeneste, never()).produserVedtaksbrev(any());
    }

    @Test
    void jusering_av_feriepenger_skal_ikke_produsere_vedtaksbrev() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.KLAGE_MEDHOLD));
        scenario.medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.VEDTAK_I_KLAGEBEHANDLING);
        scenario.medOriginalBehandling(ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider), BehandlingÅrsakType.REBEREGN_FERIEPENGER);
        var revurderingFeriepenger = scenario.lagre(repositoryProvider);

        // Act
        sendVedtaksbrevTask.prosesser(null, revurderingFeriepenger.getId());

        // Assert
        verify(dokumentBestillerTjeneste, never()).produserVedtaksbrev(any());
    }
}
