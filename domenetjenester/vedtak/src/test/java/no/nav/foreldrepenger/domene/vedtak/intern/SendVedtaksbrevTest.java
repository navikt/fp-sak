package no.nav.foreldrepenger.domene.vedtak.intern;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class SendVedtaksbrevTest {

    @Mock
    private DokumentBestillerTjeneste dokumentBestillerTjeneste;
    @Mock
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    @Mock
    private KlageRepository klageRepository;
    @Mock
    private Behandlingsresultat behandlingsresultat;
    @Mock
    private Behandling behandlingMock;
    @Mock
    private Fagsak fagsakMock;

    private SendVedtaksbrev sendVedtaksbrev;

    private Behandling behandling;
    private BehandlingVedtak behandlingVedtak;

    @Mock
    private KlageVurderingResultat klageVurderingResultat;

    private BehandlingRepository behandlingRepository;

    @BeforeEach
    public void oppsett() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        behandling = scenario.lagMocked();
        behandlingRepository = scenario.mockBehandlingRepository();
        var repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        behandlingVedtak = scenario.mockBehandlingVedtak();
        sendVedtaksbrev = new SendVedtaksbrev(behandlingRepository, repositoryProvider.getBehandlingVedtakRepository(),
            null, dokumentBestillerTjeneste, dokumentBehandlingTjeneste, klageRepository);
        lenient().when(behandlingsresultat.getVedtaksbrev()).thenReturn(Vedtaksbrev.AUTOMATISK);
        lenient().when(behandlingVedtak.getBehandlingsresultat()).thenReturn(behandlingsresultat);
        lenient().when(behandlingVedtak.getVedtakResultatType()).thenReturn(VedtakResultatType.INNVILGET);

    }

    @Test
    public void testSendVedtaksbrevVedtakInnvilget() {
        // Act
        sendVedtaksbrev.sendVedtaksbrev(behandling.getId());

        // Assert
        verify(dokumentBestillerTjeneste).produserVedtaksbrev(behandlingVedtak);
    }

    @Test
    public void testSendVedtaksbrevVedtakAvslag() {
        // Arrange
        lenient().when(behandlingVedtak.getVedtakResultatType()).thenReturn(VedtakResultatType.AVSLAG);

        // Act
        sendVedtaksbrev.sendVedtaksbrev(behandling.getId());

        // Assert
        verify(dokumentBestillerTjeneste).produserVedtaksbrev(behandlingVedtak);
    }

    @Test
    public void testSendVedtaksbrevEtterKlagebehandlingAvvistNFP() {
        testSendVedtaksbrevEtterKlagebehandling(BehandlingResultatType.KLAGE_AVVIST, KlageVurdering.AVVIS_KLAGE,
            KlageVurdertAv.NFP);
    }

    @Test
    public void testSendVedtaksbrevEtterKlagebehandlingAvvistNK() {
        testSendVedtaksbrevEtterKlagebehandling(BehandlingResultatType.KLAGE_AVVIST, KlageVurdering.AVVIS_KLAGE,
            KlageVurdertAv.NK);
    }

    @Test
    public void testSendVedtaksbrevEtterKlagebehandlingMedholdNFP() {
        testSendVedtaksbrevEtterKlagebehandling(BehandlingResultatType.KLAGE_MEDHOLD, KlageVurdering.MEDHOLD_I_KLAGE,
            KlageVurdertAv.NFP, true);
    }

    @Test
    public void testSendVedtaksbrevEtterKlagebehandlingMedholdNK() {
        testSendVedtaksbrevEtterKlagebehandling(BehandlingResultatType.KLAGE_MEDHOLD, KlageVurdering.MEDHOLD_I_KLAGE,
            KlageVurdertAv.NK, true);
    }

    @Test
    public void testIkkeSendVedtaksbrevEtterKlagebehandlingMedholdNFP() {
        lenient().when(behandlingMock.harBehandlingÅrsak(BehandlingÅrsakType.ETTER_KLAGE)).thenReturn(true);
        lenient().when(behandlingRepository.finnSisteIkkeHenlagteBehandlingavAvBehandlingTypeFor(fagsakMock.getId(),
            BehandlingType.KLAGE)).thenReturn(Optional.of(behandlingMock));
        testSendVedtaksbrevEtterKlagebehandling(BehandlingResultatType.KLAGE_MEDHOLD, KlageVurdering.MEDHOLD_I_KLAGE,
            KlageVurdertAv.NFP, false);
    }

    @Test
    public void testSendVedtaksbrevEtterKlagebehandlingOpphevet() {
        testSendVedtaksbrevEtterKlagebehandling(BehandlingResultatType.KLAGE_YTELSESVEDTAK_OPPHEVET,
            KlageVurdering.OPPHEVE_YTELSESVEDTAK, KlageVurdertAv.NK);
    }

    @Test
    public void testSendVedtaksbrevEtterKlagebehandlingStadfestet() {
        testSendVedtaksbrevEtterKlagebehandling(BehandlingResultatType.KLAGE_YTELSESVEDTAK_STADFESTET,
            KlageVurdering.STADFESTE_YTELSESVEDTAK, KlageVurdertAv.NK);
    }

    private void testSendVedtaksbrevEtterKlagebehandling(BehandlingResultatType behandlingResultat,
                                                         KlageVurdering klageVurdering,
                                                         KlageVurdertAv klageVurdertAv) {
        testSendVedtaksbrevEtterKlagebehandling(behandlingResultat, klageVurdering, klageVurdertAv, true);
    }

    private void testSendVedtaksbrevEtterKlagebehandling(BehandlingResultatType behandlingResultat,
                                                         KlageVurdering klageVurdering,
                                                         KlageVurdertAv klageVurdertAv,
                                                         boolean skalSende) {
        // Arrange
        // TODO (ONYX) erstatt med scenario
        lenient().when(klageRepository.hentGjeldendeKlageVurderingResultat(behandlingMock)).thenReturn(
            Optional.of(klageVurderingResultat));
        lenient().when(fagsakMock.getYtelseType()).thenReturn(FagsakYtelseType.ENGANGSTØNAD);
        lenient().when(behandlingMock.getFagsak()).thenReturn(fagsakMock);
        lenient().when(behandlingVedtak.getVedtakResultatType()).thenReturn(VedtakResultatType.VEDTAK_I_KLAGEBEHANDLING);
        lenient().when(behandlingRepository.hentBehandling(behandling.getId())).thenReturn(behandlingMock);
        lenient().when(klageVurderingResultat.getKlageVurdering()).thenReturn(klageVurdering);
        lenient().when(klageVurderingResultat.getKlageVurdertAv()).thenReturn(klageVurdertAv);
        lenient().when(behandlingsresultat.getBehandlingResultatType()).thenReturn(behandlingResultat);

        // Act
        sendVedtaksbrev.sendVedtaksbrev(behandling.getId());

        // Assert
        if (skalSende) {
            verify(dokumentBestillerTjeneste).produserVedtaksbrev(behandlingVedtak);
        } else {
            verify(dokumentBestillerTjeneste, Mockito.never()).produserVedtaksbrev(behandlingVedtak);
        }
    }

    @Test
    public void senderBrevOmUendretUtfallVedRevurdering() {
        lenient().when(behandlingVedtak.isBeslutningsvedtak()).thenReturn(true);
        lenient().when(dokumentBehandlingTjeneste.erDokumentBestilt(behandling.getId(),
            DokumentMalType.VARSEL_OM_REVURDERING)).thenReturn(true);

        sendVedtaksbrev.sendVedtaksbrev(behandling.getId());

        verify(dokumentBestillerTjeneste).produserVedtaksbrev(behandlingVedtak);
    }

    @Test
    public void senderIkkeBrevOmUendretUtfallHvisIkkeSendtVarselbrevOmRevurdering() {
        lenient().when(behandlingVedtak.isBeslutningsvedtak()).thenReturn(true);
        lenient().when(dokumentBehandlingTjeneste.erDokumentBestilt(behandling.getId(),
            DokumentMalType.VARSEL_OM_REVURDERING)).thenReturn(false);

        sendVedtaksbrev.sendVedtaksbrev(behandling.getId());

        verify(dokumentBestillerTjeneste, never()).produserVedtaksbrev(behandlingVedtak);
    }

    @Test
    public void sender_ikke_brev_dersom_førstegangsøknad_som_er_migrert_fra_infotrygd() {
        behandling.setMigrertKilde(Fagsystem.INFOTRYGD);

        sendVedtaksbrev.sendVedtaksbrev(behandling.getId());

        verify(dokumentBestillerTjeneste, never()).produserVedtaksbrev(behandlingVedtak);
    }

    @Test
    public void sender_brev_dersom_førstegangsøknad_som_er_migrert_fra_infotrygd_men_overstyrt_til_fritekstbrev() {
        behandling.setMigrertKilde(Fagsystem.INFOTRYGD);
        lenient().when(behandlingsresultat.getVedtaksbrev()).thenReturn(Vedtaksbrev.FRITEKST);

        sendVedtaksbrev.sendVedtaksbrev(behandling.getId());

        verify(dokumentBestillerTjeneste, times(1)).produserVedtaksbrev(behandlingVedtak);
    }

}
