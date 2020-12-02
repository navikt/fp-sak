package no.nav.foreldrepenger.behandling.steg.simulering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingInntrekkEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingValg;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomi.simulering.klient.FpOppdragRestKlient;
import no.nav.foreldrepenger.økonomi.simulering.klient.FpoppdragSystembrukerRestKlient;
import no.nav.foreldrepenger.økonomi.simulering.kontrakt.SimulerOppdragDto;
import no.nav.foreldrepenger.økonomi.simulering.kontrakt.SimuleringResultatDto;
import no.nav.foreldrepenger.økonomi.simulering.tjeneste.SimuleringIntegrasjonTjeneste;
import no.nav.foreldrepenger.økonomi.tilbakekreving.klient.FptilbakeRestKlient;
import no.nav.foreldrepenger.økonomi.økonomistøtte.SimulerOppdragTjeneste;
import no.nav.vedtak.felles.testutilities.db.Repository;

@CdiDbAwareTest
public class SimulerOppdragStegTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private TilbakekrevingRepository tilbakekrevingRepository;

    private SimulerOppdragSteg steg;
    private final SimulerOppdragTjeneste simulerOppdragTjenesteMock = mock(
            SimulerOppdragTjeneste.class);
    private final FpOppdragRestKlient fpOppdragRestKlientMock = mock(FpOppdragRestKlient.class);
    private final FpoppdragSystembrukerRestKlient fpoppdragSystembrukerRestKlientMock = mock(
            FpoppdragSystembrukerRestKlient.class);
    private final FptilbakeRestKlient fptilbakeRestKlientMock = mock(FptilbakeRestKlient.class);
    private SimuleringIntegrasjonTjeneste simuleringIntegrasjonTjeneste;

    private final BehandlingProsesseringTjeneste behandlingProsesseringTjeneste = mock(BehandlingProsesseringTjeneste.class);
    private Repository repository;

    private Behandling behandling;
    private BehandlingskontrollKontekst kontekst;

    @BeforeEach
    public void setup(EntityManager entityManager) {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
        simuleringIntegrasjonTjeneste = new SimuleringIntegrasjonTjeneste(fpOppdragRestKlientMock);
        tilbakekrevingRepository = new TilbakekrevingRepository(entityManager);
        repository = new Repository(entityManager);
        behandling = scenario.lagre(repositoryProvider);
        kontekst = new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(),
                behandlingRepository.taSkriveLås(behandling));
    }

    @Test
    public void skal_ha_aksjonspunkter_fra_aksjonspunktutleder_når_feature_er_enabled_men_skal_kalle_på_tjeneste_og_klient() {
        // Arrange
        when(simulerOppdragTjenesteMock.simulerOppdrag(anyLong(), anyLong())).thenReturn(
                Collections.singletonList("test"));

        when(fpOppdragRestKlientMock.hentResultat(anyLong())).thenReturn(
                Optional.of(new SimuleringResultatDto(-2354L, 0L, true)));
        steg = opprettSteg();

        // Act
        BehandleStegResultat resultat = steg.utførSteg(kontekst);

        // Assert
        assertThat(resultat.getAksjonspunktListe()).containsOnly(AksjonspunktDefinisjon.VURDER_FEILUTBETALING);
        assertThat(resultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        verify(simulerOppdragTjenesteMock).simulerOppdrag(anyLong(), anyLong());
        verify(fpOppdragRestKlientMock).startSimulering(any(SimulerOppdragDto.class));

        Optional<TilbakekrevingInntrekkEntitet> tilbakekrevingInntrekk = tilbakekrevingRepository.hentTilbakekrevingInntrekk(
                behandling.getId());
        assertThat(tilbakekrevingInntrekk).isPresent();
        assertThat(tilbakekrevingInntrekk.get().isAvslåttInntrekk()).isTrue();
    }

    @Test
    public void deaktiverer_eksisterende_tilbakekrevingValg_ved_hopp_over_bakover() {
        // Arrange
        tilbakekrevingRepository.lagre(behandling,
                TilbakekrevingValg.utenMulighetForInntrekk(TilbakekrevingVidereBehandling.TILBAKEKREV_I_INFOTRYGD,
                        "varsel"));
        repository.flushAndClear();

        steg = opprettSteg();

        // Act
        steg.vedHoppOverBakover(kontekst, mock(BehandlingStegModell.class), BehandlingStegType.VURDER_UTTAK,
                BehandlingStegType.FATTE_VEDTAK);
        repository.flushAndClear();

        // Assert
        Optional<TilbakekrevingValg> tilbakekrevingValg = tilbakekrevingRepository.hent(behandling.getId());
        assertThat(tilbakekrevingValg).isNotPresent();
    }

    @Test
    public void lagrer_automatisk_inntrekk_og_returnerer_ingen_aksjonspunkter_dersom_aksjonspunkt_for_inntrekk() {
        // Arrange
        when(fpOppdragRestKlientMock.hentResultat(anyLong())).thenReturn(
                Optional.of(new SimuleringResultatDto(0L, -2354L, false)));

        steg = opprettSteg();

        // Act
        BehandleStegResultat resultat = steg.utførSteg(kontekst);
        repository.flushAndClear();

        // Assert
        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        assertThat(resultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);

        Optional<TilbakekrevingValg> tilbakekrevingValg = tilbakekrevingRepository.hent(behandling.getId());
        assertThat(tilbakekrevingValg).isPresent();
        assertThat(tilbakekrevingValg.get().getVidereBehandling()).isEqualTo(TilbakekrevingVidereBehandling.INNTREKK);

        Optional<TilbakekrevingInntrekkEntitet> tilbakekrevingInntrekk = tilbakekrevingRepository.hentTilbakekrevingInntrekk(
                behandling.getId());
        assertThat(tilbakekrevingInntrekk).isPresent();
        assertThat(tilbakekrevingInntrekk.get().isAvslåttInntrekk()).isFalse();
    }

    @Test
    public void skal_kalle_kanseller_oppdrag_ved_tilbakehopp() {
        // Arrange
        steg = new SimulerOppdragSteg(repositoryProvider, behandlingProsesseringTjeneste, simulerOppdragTjenesteMock,
                simuleringIntegrasjonTjeneste, tilbakekrevingRepository, fpoppdragSystembrukerRestKlientMock,
                fptilbakeRestKlientMock);

        mock(Behandling.class);

        // Act
        steg.vedHoppOverBakover(kontekst, null, null, null);

        // Verify
        verify(fpoppdragSystembrukerRestKlientMock).kansellerSimulering(kontekst.getBehandlingId());
    }

    @Test
    public void skal__ikke_kalle_kanseller_oppdrag_ved_tilbakehopp_tilSimulerOppdragSteget() {
        // Arrange
        steg = new SimulerOppdragSteg(repositoryProvider, behandlingProsesseringTjeneste, simulerOppdragTjenesteMock,
                simuleringIntegrasjonTjeneste, tilbakekrevingRepository, fpoppdragSystembrukerRestKlientMock,
                fptilbakeRestKlientMock);

        mock(Behandling.class);

        // Act
        steg.vedHoppOverBakover(kontekst, null, BehandlingStegType.SIMULER_OPPDRAG, null);

        // Verify
        verify(fpoppdragSystembrukerRestKlientMock, never()).kansellerSimulering(kontekst.getBehandlingId());
    }

    @Test
    public void utførSteg_lagrer_tilbakekrevingoppdater_hvis_det_er_en_åpen_tilbakekreving() {
        when(fptilbakeRestKlientMock.harÅpenTilbakekrevingsbehandling(any(Saksnummer.class))).thenReturn(true);
        when(fpOppdragRestKlientMock.hentResultat(anyLong())).thenReturn(
                Optional.of(new SimuleringResultatDto(-2354L, 0L, true)));

        steg = opprettSteg();

        // Act
        BehandleStegResultat resultat = steg.utførSteg(kontekst);
        repository.flushAndClear();

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        assertThat(resultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);

        Optional<TilbakekrevingValg> tilbakekrevingValg = tilbakekrevingRepository.hent(behandling.getId());
        assertThat(tilbakekrevingValg).isPresent();
        assertThat(tilbakekrevingValg.get().getVidereBehandling()).isEqualTo(
                TilbakekrevingVidereBehandling.TILBAKEKR_OPPDATER);

    }

    @Test
    public void utførSteg_lagrer_tilbakekrevingoppdater_hvis_det_er_en_åpen_tilbakekreving_men_simuleringresultat_ikke_påvirke_grunnlag() {
        when(fptilbakeRestKlientMock.harÅpenTilbakekrevingsbehandling(any(Saksnummer.class))).thenReturn(true);
        when(fpOppdragRestKlientMock.hentResultat(anyLong())).thenReturn(
                Optional.of(new SimuleringResultatDto(0L, 0L, true)));

        steg = opprettSteg();

        // Act
        BehandleStegResultat resultat = steg.utførSteg(kontekst);
        repository.flushAndClear();

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        assertThat(resultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);

        Optional<TilbakekrevingValg> tilbakekrevingValg = tilbakekrevingRepository.hent(behandling.getId());
        assertThat(tilbakekrevingValg).isEmpty();
    }

    @Test
    public void utførSteg_ikke_lagrer_tilbakekrevingoppdater_for_åpen_tilbakekreving_når_simuleringresultat_ikke_finnes() {
        when(fptilbakeRestKlientMock.harÅpenTilbakekrevingsbehandling(any(Saksnummer.class))).thenReturn(true);

        steg = opprettSteg();

        // Act
        BehandleStegResultat resultat = steg.utførSteg(kontekst);
        repository.flushAndClear();

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        assertThat(resultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(tilbakekrevingRepository.hent(behandling.getId())).isEmpty();
    }

    private SimulerOppdragSteg opprettSteg() {
        return new SimulerOppdragSteg(repositoryProvider, behandlingProsesseringTjeneste, simulerOppdragTjenesteMock,
                simuleringIntegrasjonTjeneste, tilbakekrevingRepository, fpoppdragSystembrukerRestKlientMock,
                fptilbakeRestKlientMock);
    }
}
