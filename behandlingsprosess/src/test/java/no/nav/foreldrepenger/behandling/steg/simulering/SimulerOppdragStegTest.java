package no.nav.foreldrepenger.behandling.steg.simulering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.StegTransisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingValg;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.kontrakter.fpwsproxy.simulering.request.OppdragskontrollDto;
import no.nav.foreldrepenger.kontrakter.simulering.resultat.v1.SimuleringResultatDto;
import no.nav.foreldrepenger.produksjonsstyring.tilbakekreving.FptilbakeRestKlient;
import no.nav.foreldrepenger.økonomistøtte.SimulerOppdragTjeneste;
import no.nav.foreldrepenger.økonomistøtte.simulering.klient.FpOppdragRestKlient;
import no.nav.foreldrepenger.økonomistøtte.simulering.tjeneste.SimuleringIntegrasjonTjeneste;

@CdiDbAwareTest
class SimulerOppdragStegTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private TilbakekrevingRepository tilbakekrevingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;

    private SimulerOppdragSteg steg;
    private final SimulerOppdragTjeneste simulerOppdragTjenesteMock = mock(SimulerOppdragTjeneste.class);
    private final FpOppdragRestKlient fpOppdragRestKlientMock = mock(FpOppdragRestKlient.class);
    private final FptilbakeRestKlient fptilbakeRestKlientMock = mock(FptilbakeRestKlient.class);
    private SimuleringIntegrasjonTjeneste simuleringIntegrasjonTjeneste;

    private final BehandlingProsesseringTjeneste behandlingProsesseringTjeneste = mock(BehandlingProsesseringTjeneste.class);
    private EntityManager entityManager;

    private Behandling behandling;
    private BehandlingskontrollKontekst kontekst;

    @BeforeEach
    void setup(EntityManager entityManager) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        var behandlingRepository = repositoryProvider.getBehandlingRepository();
        simuleringIntegrasjonTjeneste = new SimuleringIntegrasjonTjeneste(fpOppdragRestKlientMock);
        tilbakekrevingRepository = new TilbakekrevingRepository(entityManager);
        beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.entityManager = entityManager;
        behandling = scenario.lagre(repositoryProvider);
        kontekst = new BehandlingskontrollKontekst(behandling, behandlingRepository.taSkriveLås(behandling));
    }

    @Test
    void skal_ha_aksjonspunkter_fra_aksjonspunktutleder_når_feature_er_enabled_men_skal_kalle_på_tjeneste_og_klient() {
        // Arrange
        var oppdragskontroll = lagOppdragKontrollMedPåkrevdeFelter(123L);
        when(simulerOppdragTjenesteMock.hentOppdragskontrollForBehandling(anyLong())).thenReturn(
            Optional.of(oppdragskontroll));

        when(fpOppdragRestKlientMock.hentResultat(anyLong(), any(), anyString())).thenReturn(
                Optional.of(new SimuleringResultatDto(-2354L, 0L,true)));
        steg = opprettSteg();

        // Act
        var resultat = steg.utførSteg(kontekst);

        // Assert
        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.VURDER_FEILUTBETALING);
        assertThat(resultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.UTFØRT);
        verify(simulerOppdragTjenesteMock).hentOppdragskontrollForBehandling(anyLong());
        verify(fpOppdragRestKlientMock).startSimulering(any(OppdragskontrollDto.class), any(), anyString());

        var tilbakekrevingInntrekk = tilbakekrevingRepository.hentTilbakekrevingInntrekk(
                behandling.getId());
        assertThat(tilbakekrevingInntrekk).isPresent();
        assertThat(tilbakekrevingInntrekk.get().isAvslåttInntrekk()).isTrue();
    }


    @Disabled
    @Test
    void stor_etterbetaling_til_søker_og_feilutbetaling_skal_føre_til_to_aksjonspunkter() {
        // Arrange
        var oppdragskontroll = lagOppdragKontrollMedPåkrevdeFelter(123L);
        when(simulerOppdragTjenesteMock.hentOppdragskontrollForBehandling(anyLong())).thenReturn(
            Optional.of(oppdragskontroll));

        when(fpOppdragRestKlientMock.hentResultat(anyLong(), any(), anyString())).thenReturn(
            Optional.of(new SimuleringResultatDto(-2354L, 0L,true)));
        steg = opprettSteg();

        // Act
        var resultat = steg.utførSteg(kontekst);

        // Assert
        assertThat(resultat.getAksjonspunktListe()).containsExactlyInAnyOrder(
            AksjonspunktDefinisjon.KONTROLLER_STOR_ETTERBETALING_SØKER,
            AksjonspunktDefinisjon.VURDER_FEILUTBETALING
        );
        assertThat(resultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.UTFØRT);
    }


    @Disabled
    @Test
    void skal_ikke_opprette_stor_etterbetaling_aksjonspunkt_hvor_etterbetaling_til_søker_er_under_grenseverdi() {
        // Arrange
        var oppdragskontroll = lagOppdragKontrollMedPåkrevdeFelter(123L);
        when(simulerOppdragTjenesteMock.hentOppdragskontrollForBehandling(anyLong())).thenReturn(
            Optional.of(oppdragskontroll));

        when(fpOppdragRestKlientMock.hentResultat(anyLong(), any(), anyString())).thenReturn(
            Optional.of(new SimuleringResultatDto(0L, -2354L,true)));
        steg = opprettSteg();

        // Act
        var resultat = steg.utførSteg(kontekst);

        // Assert
        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        assertThat(resultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.UTFØRT);
    }


    @Test
    void deaktiverer_eksisterende_tilbakekrevingValg_ved_hopp_over_bakover() {
        // Arrange
        tilbakekrevingRepository.lagre(behandling,
                TilbakekrevingValg.utenMulighetForInntrekk(TilbakekrevingVidereBehandling.OPPRETT_TILBAKEKREVING,
                        "varsel"));
        entityManager.flush();
        entityManager.clear();

        steg = opprettSteg();

        // Act
        steg.vedHoppOverBakover(kontekst, mock(BehandlingStegModell.class), BehandlingStegType.VURDER_UTTAK,
                BehandlingStegType.FATTE_VEDTAK);
        entityManager.flush();
        entityManager.clear();

        // Assert
        var tilbakekrevingValg = tilbakekrevingRepository.hent(behandling.getId());
        assertThat(tilbakekrevingValg).isNotPresent();
    }

    @Test
    void lagrer_automatisk_inntrekk_og_returnerer_ingen_aksjonspunkter_dersom_aksjonspunkt_for_inntrekk() {
        // Arrange
        when(fpOppdragRestKlientMock.hentResultat(anyLong(), any(), anyString())).thenReturn(
                Optional.of(new SimuleringResultatDto(0L, -2354L,false)));

        steg = opprettSteg();

        // Act
        var resultat = steg.utførSteg(kontekst);
        entityManager.flush();
        entityManager.clear();

        // Assert
        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        assertThat(resultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.UTFØRT);

        var tilbakekrevingValg = tilbakekrevingRepository.hent(behandling.getId());
        assertThat(tilbakekrevingValg).isPresent();
        assertThat(tilbakekrevingValg.get().getVidereBehandling()).isEqualTo(TilbakekrevingVidereBehandling.INNTREKK);

        var tilbakekrevingInntrekk = tilbakekrevingRepository.hentTilbakekrevingInntrekk(
                behandling.getId());
        assertThat(tilbakekrevingInntrekk).isPresent();
        assertThat(tilbakekrevingInntrekk.get().isAvslåttInntrekk()).isFalse();
    }

    @Test
    void skal_kalle_kanseller_oppdrag_ved_tilbakehopp() {
        // Arrange
        steg = new SimulerOppdragSteg(repositoryProvider, behandlingProsesseringTjeneste, simulerOppdragTjenesteMock,
                simuleringIntegrasjonTjeneste, tilbakekrevingRepository, fpOppdragRestKlientMock,
                fptilbakeRestKlientMock, beregningsresultatRepository);

        // Act
        steg.vedHoppOverBakover(kontekst, null, null, null);

        // Verify
        verify(fpOppdragRestKlientMock).kansellerSimulering(kontekst.getBehandlingId(), behandling.getUuid(), kontekst.getSaksnummer().getVerdi());
    }

    @Test
    void skal__ikke_kalle_kanseller_oppdrag_ved_tilbakehopp_tilSimulerOppdragSteget() {
        // Arrange
        steg = new SimulerOppdragSteg(repositoryProvider, behandlingProsesseringTjeneste, simulerOppdragTjenesteMock,
                simuleringIntegrasjonTjeneste, tilbakekrevingRepository, fpOppdragRestKlientMock,
                fptilbakeRestKlientMock, beregningsresultatRepository);

        // Act
        steg.vedHoppOverBakover(kontekst, null, BehandlingStegType.SIMULER_OPPDRAG, null);

        // Verify
        verify(fpOppdragRestKlientMock, never()).kansellerSimulering(kontekst.getBehandlingId(), null, kontekst.getSaksnummer().getVerdi());
    }

    @Test
    void utførSteg_lagrer_tilbakekrevingoppdater_hvis_det_er_en_åpen_tilbakekreving() {
        when(fptilbakeRestKlientMock.harÅpenTilbakekrevingsbehandling(any(Saksnummer.class))).thenReturn(true);
        when(fpOppdragRestKlientMock.hentResultat(anyLong(), any(), anyString())).thenReturn(
                Optional.of(new SimuleringResultatDto(-2354L, 0L,true)));

        steg = opprettSteg();

        // Act
        var resultat = steg.utførSteg(kontekst);
        entityManager.flush();
        entityManager.clear();

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        assertThat(resultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.UTFØRT);

        var tilbakekrevingValg = tilbakekrevingRepository.hent(behandling.getId());
        assertThat(tilbakekrevingValg).isPresent();
        assertThat(tilbakekrevingValg.get().getVidereBehandling()).isEqualTo(
                TilbakekrevingVidereBehandling.TILBAKEKR_OPPDATER);

    }

    @Test
    void utførSteg_lagrer_tilbakekrevingoppdater_hvis_det_er_en_åpen_tilbakekreving_men_simuleringresultat_ikke_påvirke_grunnlag() {
        when(fptilbakeRestKlientMock.harÅpenTilbakekrevingsbehandling(any(Saksnummer.class))).thenReturn(true);
        when(fpOppdragRestKlientMock.hentResultat(anyLong(), any(), anyString())).thenReturn(
                Optional.of(new SimuleringResultatDto(0L, 0L,true)));

        steg = opprettSteg();

        // Act
        var resultat = steg.utførSteg(kontekst);
        entityManager.flush();
        entityManager.clear();

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        assertThat(resultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.UTFØRT);

        var tilbakekrevingValg = tilbakekrevingRepository.hent(behandling.getId());
        assertThat(tilbakekrevingValg).isEmpty();
    }

    @Test
    void utførSteg_ikke_lagrer_tilbakekrevingoppdater_for_åpen_tilbakekreving_når_simuleringresultat_ikke_finnes() {
        when(fptilbakeRestKlientMock.harÅpenTilbakekrevingsbehandling(any(Saksnummer.class))).thenReturn(true);

        steg = opprettSteg();

        // Act
        var resultat = steg.utførSteg(kontekst);
        entityManager.flush();
        entityManager.clear();

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        assertThat(resultat.getTransisjon().stegTransisjon()).isEqualTo(StegTransisjon.UTFØRT);
        assertThat(tilbakekrevingRepository.hent(behandling.getId())).isEmpty();
    }

    private SimulerOppdragSteg opprettSteg() {
        return new SimulerOppdragSteg(repositoryProvider, behandlingProsesseringTjeneste, simulerOppdragTjenesteMock,
                simuleringIntegrasjonTjeneste, tilbakekrevingRepository, fpOppdragRestKlientMock,
                fptilbakeRestKlientMock, beregningsresultatRepository);
    }

    private static Oppdragskontroll lagOppdragKontrollMedPåkrevdeFelter(Long behandlingsId) {
        var oppdragskontroll = lagOppdragskontrollMedPaakrevdeFelter(behandlingsId);
        oppdragskontroll.getOppdrag110Liste().add(lagOppdrag110Påkrevd(behandlingsId));
        return oppdragskontroll;
    }
    public static Oppdrag110 lagOppdrag110Påkrevd(Long behandlingsId) {
        return Oppdrag110.builder()
            .medKodeEndring(KodeEndring.ENDR)
            .medKodeFagomrade(KodeFagområde.REFUTG)
            .medFagSystemId(250L)
            .medOppdragGjelderId("12345678910")
            .medSaksbehId("Z123456")
            .medAvstemming(Avstemming.ny())
            .medOppdragskontroll(lagOppdragskontrollMedPaakrevdeFelter(behandlingsId))
            .build();
    }

    private static Oppdragskontroll lagOppdragskontrollMedPaakrevdeFelter(Long behandlingsId) {
        return Oppdragskontroll.builder()
            .medBehandlingId(behandlingsId)
            .medSaksnummer(new Saksnummer("700"))
            .medVenterKvittering(true)
            .medProsessTaskId(52L)
            .build();
    }
}
