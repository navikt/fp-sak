package no.nav.foreldrepenger.domene.vedtak.intern;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerApplikasjonTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;

public class SendVedtaksbrevTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste;
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

    private BehandlingRepositoryProvider repositoryProvider;

    @Before
    public void oppsett() {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        behandling = scenario.lagMocked();
        behandlingRepository = scenario.mockBehandlingRepository();
        repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        behandlingVedtak = scenario.mockBehandlingVedtak();
        sendVedtaksbrev = new SendVedtaksbrev(behandlingRepository, repositoryProvider.getBehandlingVedtakRepository(), null, dokumentBestillerApplikasjonTjeneste, dokumentBehandlingTjeneste, klageRepository, null);
        when(behandlingsresultat.getVedtaksbrev()).thenReturn(Vedtaksbrev.AUTOMATISK);
        when(behandlingVedtak.getBehandlingsresultat()).thenReturn(behandlingsresultat);
        when(behandlingVedtak.getVedtakResultatType()).thenReturn(VedtakResultatType.INNVILGET);

    }

    @Test
    public void testSendVedtaksbrevVedtakInnvilget() {
        // Act
        sendVedtaksbrev.sendVedtaksbrev(behandling.getId());

        // Assert
        verify(dokumentBestillerApplikasjonTjeneste).produserVedtaksbrev(behandlingVedtak);
    }

    @Test
    public void testSendVedtaksbrevVedtakAvslag() {
        // Arrange
        when(behandlingVedtak.getVedtakResultatType()).thenReturn(VedtakResultatType.AVSLAG);

        // Act
        sendVedtaksbrev.sendVedtaksbrev(behandling.getId());

        // Assert
        verify(dokumentBestillerApplikasjonTjeneste).produserVedtaksbrev(behandlingVedtak);
    }

    @Test
    public void testSendVedtaksbrevEtterKlagebehandlingAvvistNFP() {
        testSendVedtaksbrevEtterKlagebehandling(BehandlingResultatType.KLAGE_AVVIST, KlageVurdering.AVVIS_KLAGE, KlageVurdertAv.NFP);
    }

    @Test
    public void testSendVedtaksbrevEtterKlagebehandlingAvvistNK() {
        testSendVedtaksbrevEtterKlagebehandling(BehandlingResultatType.KLAGE_AVVIST, KlageVurdering.AVVIS_KLAGE, KlageVurdertAv.NK);
    }

    @Test
    public void testSendVedtaksbrevEtterKlagebehandlingMedholdNFP() {
        testSendVedtaksbrevEtterKlagebehandling(BehandlingResultatType.KLAGE_MEDHOLD, KlageVurdering.MEDHOLD_I_KLAGE, KlageVurdertAv.NFP,
            true);
    }

    @Test
    public void testSendVedtaksbrevEtterKlagebehandlingMedholdNK() {
        testSendVedtaksbrevEtterKlagebehandling(BehandlingResultatType.KLAGE_MEDHOLD, KlageVurdering.MEDHOLD_I_KLAGE, KlageVurdertAv.NK,
            true);
    }

    @Test
    public void testIkkeSendVedtaksbrevEtterKlagebehandlingMedholdNFP() {
        when(behandlingMock.harBehandlingÅrsak(BehandlingÅrsakType.ETTER_KLAGE)).thenReturn(true);
        when(behandlingRepository.finnSisteIkkeHenlagteBehandlingavAvBehandlingTypeForFagsakId(fagsakMock.getId(), BehandlingType.KLAGE)).thenReturn(Optional.of(behandlingMock));
        testSendVedtaksbrevEtterKlagebehandling(BehandlingResultatType.KLAGE_MEDHOLD, KlageVurdering.MEDHOLD_I_KLAGE, KlageVurdertAv.NFP, false);
    }

    @Test
    public void testSendVedtaksbrevEtterKlagebehandlingOpphevet() {
        testSendVedtaksbrevEtterKlagebehandling(BehandlingResultatType.KLAGE_YTELSESVEDTAK_OPPHEVET, KlageVurdering.OPPHEVE_YTELSESVEDTAK,
            KlageVurdertAv.NK);
    }

    @Test
    public void testSendVedtaksbrevEtterKlagebehandlingStadfestet() {
        testSendVedtaksbrevEtterKlagebehandling(BehandlingResultatType.KLAGE_YTELSESVEDTAK_STADFESTET,
            KlageVurdering.STADFESTE_YTELSESVEDTAK, KlageVurdertAv.NK);
    }

    private void testSendVedtaksbrevEtterKlagebehandling(BehandlingResultatType behandlingResultat, KlageVurdering klageVurdering,
                                                         KlageVurdertAv klageVurdertAv) {
        testSendVedtaksbrevEtterKlagebehandling(behandlingResultat, klageVurdering, klageVurdertAv, true);
    }

    private void testSendVedtaksbrevEtterKlagebehandling(BehandlingResultatType behandlingResultat, KlageVurdering klageVurdering,
                                                         KlageVurdertAv klageVurdertAv, boolean skalSende) {
        // Arrange
        // TODO (ONYX) erstatt med scenario
        when(klageRepository.hentGjeldendeKlageVurderingResultat(behandlingMock)).thenReturn(Optional.of(klageVurderingResultat));
        when(fagsakMock.getYtelseType()).thenReturn(FagsakYtelseType.ENGANGSTØNAD);
        when(behandlingMock.getFagsak()).thenReturn(fagsakMock);
        when(behandlingVedtak.getVedtakResultatType()).thenReturn(VedtakResultatType.VEDTAK_I_KLAGEBEHANDLING);
        when(behandlingRepository.hentBehandling(behandling.getId())).thenReturn(behandlingMock);
        when(klageVurderingResultat.getKlageVurdering()).thenReturn(klageVurdering);
        when(klageVurderingResultat.getKlageVurdertAv()).thenReturn(klageVurdertAv);
        when(behandlingsresultat.getBehandlingResultatType()).thenReturn(behandlingResultat);

        // Act
        sendVedtaksbrev.sendVedtaksbrev(behandling.getId());

        // Assert
        if (skalSende) {
            verify(dokumentBestillerApplikasjonTjeneste).produserVedtaksbrev(behandlingVedtak);
        } else {
            verify(dokumentBestillerApplikasjonTjeneste, Mockito.never()).produserVedtaksbrev(behandlingVedtak);
        }
    }

    @Test
    public void senderBrevOmUendretUtfallVedRevurdering() {
        when(behandlingVedtak.isBeslutningsvedtak()).thenReturn(true);
        when(dokumentBehandlingTjeneste.erDokumentProdusert(behandling.getId(), DokumentMalType.REVURDERING_DOK))
            .thenReturn(true);

        sendVedtaksbrev.sendVedtaksbrev(behandling.getId());

        verify(dokumentBestillerApplikasjonTjeneste).produserVedtaksbrev(behandlingVedtak);
    }

    @Test
    public void senderIkkeBrevOmUendretUtfallHvisIkkeSendtVarselbrevOmRevurdering() {
        when(behandlingVedtak.isBeslutningsvedtak()).thenReturn(true);
        when(dokumentBehandlingTjeneste.erDokumentProdusert(behandling.getId(), DokumentMalType.REVURDERING_DOK))
            .thenReturn(false);

        sendVedtaksbrev.sendVedtaksbrev(behandling.getId());

        verify(dokumentBestillerApplikasjonTjeneste, never()).produserVedtaksbrev(behandlingVedtak);
    }

    @Test
    public void sender_ikke_brev_dersom_førstegangsøknad_som_er_migrert_fra_infotrygd() {
        behandling.setMigrertKilde(Fagsystem.INFOTRYGD);

        sendVedtaksbrev.sendVedtaksbrev(behandling.getId());

        verify(dokumentBestillerApplikasjonTjeneste, never()).produserVedtaksbrev(behandlingVedtak);
    }

    @Test
    public void sender_brev_dersom_førstegangsøknad_som_er_migrert_fra_infotrygd_men_overstyrt_til_fritekstbrev() {
        behandling.setMigrertKilde(Fagsystem.INFOTRYGD);
        when(behandlingsresultat.getVedtaksbrev()).thenReturn(Vedtaksbrev.FRITEKST);

        sendVedtaksbrev.sendVedtaksbrev(behandling.getId());

        verify(dokumentBestillerApplikasjonTjeneste, times(1)).produserVedtaksbrev(behandlingVedtak);
    }


}
