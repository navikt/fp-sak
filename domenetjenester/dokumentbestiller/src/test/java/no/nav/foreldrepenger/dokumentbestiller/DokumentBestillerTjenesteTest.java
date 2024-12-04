package no.nav.foreldrepenger.dokumentbestiller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dokumentbestiller.formidling.DokumentBestiller;

@ExtendWith(MockitoExtension.class)
class DokumentBestillerTjenesteTest {

    private Behandling behandling;
    private BehandlingRepositoryProvider repositoryProvider;
    private DokumentBestillerTjeneste tjeneste;
    @Mock private DokumentBestiller dokumentBestiller;
    @Mock private BehandlingVedtak behandlingVedtakMock;
    @Mock private Behandlingsresultat behandlingResultatMock;

    private void settOpp(AbstractTestScenario<?> scenario) {
        this.behandling = scenario.lagMocked();
        this.repositoryProvider = scenario.mockBehandlingRepositoryProvider();

        tjeneste = new DokumentBestillerTjeneste(repositoryProvider.getBehandlingRepository(), null, dokumentBestiller);
    }

    @Test
    void skal_bestille_brev_fra_fpformidling() {
        // Arrange
        AbstractTestScenario<?> scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        settOpp(scenario);

        var dokumentMal = DokumentMalType.INNHENTE_OPPLYSNINGER;
        var historikkAktør = HistorikkAktør.SAKSBEHANDLER;
        var dokumentBestilling = DokumentBestilling.builder()
            .medBehandlingUuid(behandling.getUuid())
            .medSaksnummer(behandling.getSaksnummer())
            .medDokumentMal(dokumentMal)
            .medFritekst("fritekst")
            .build();

        // Act
        tjeneste.bestillDokument(dokumentBestilling, historikkAktør);

        // Assert
        verify(dokumentBestiller).bestillDokument(dokumentBestilling, historikkAktør);
    }

    @Test
    void skal_bestille_vedtak_brev_fritekst() {
        // Arrange
        AbstractTestScenario<?> scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        settOpp(scenario);

        when(behandlingResultatMock.getBehandlingId()).thenReturn(behandling.getId());
        when(behandlingResultatMock.getBehandlingResultatType()).thenReturn(BehandlingResultatType.INNVILGET);
        when(behandlingResultatMock.getVedtaksbrev()).thenReturn(Vedtaksbrev.FRITEKST);
        when(behandlingResultatMock.getKonsekvenserForYtelsen()).thenReturn(List.of(KonsekvensForYtelsen.ENDRING_I_BEREGNING));

        when(behandlingVedtakMock.getBehandlingsresultat()).thenReturn(behandlingResultatMock);
        when(behandlingVedtakMock.getVedtakResultatType()).thenReturn(VedtakResultatType.INNVILGET);
        when(behandlingVedtakMock.isBeslutningsvedtak()).thenReturn(false);

        var historikkAktør = HistorikkAktør.VEDTAKSLØSNINGEN;

        // Act
        tjeneste.produserVedtaksbrev(behandlingVedtakMock);

        var bestillingCaptor = ArgumentCaptor.forClass(DokumentBestilling.class);

        // Assert
        verify(dokumentBestiller).bestillDokument(bestillingCaptor.capture(), eq(historikkAktør));
        var bestilling = bestillingCaptor.getValue();

        assertThat(bestilling.behandlingUuid()).isEqualTo(behandling.getUuid());
        assertThat(bestilling.dokumentMal()).isEqualTo(DokumentMalType.FRITEKSTBREV);
        assertThat(bestilling.bestillingUuid()).isNotNull();
        assertThat(bestilling.journalførSom()).isEqualTo(DokumentMalType.FORELDREPENGER_INNVILGELSE);
    }

    @Test
    void skal_bestille_vedtak_brev_fritekst_endring_i_utbetaling() {
        // Arrange
        AbstractTestScenario<?> scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        settOpp(scenario);

        when(behandlingResultatMock.getBehandlingId()).thenReturn(behandling.getId());
        when(behandlingResultatMock.getBehandlingResultatType()).thenReturn(BehandlingResultatType.FORELDREPENGER_ENDRET);
        when(behandlingResultatMock.getVedtaksbrev()).thenReturn(Vedtaksbrev.FRITEKST);
        when(behandlingResultatMock.getKonsekvenserForYtelsen()).thenReturn(List.of(KonsekvensForYtelsen.ENDRING_I_FORDELING_AV_YTELSEN));

        when(behandlingVedtakMock.getBehandlingsresultat()).thenReturn(behandlingResultatMock);
        when(behandlingVedtakMock.getVedtakResultatType()).thenReturn(VedtakResultatType.INNVILGET);
        when(behandlingVedtakMock.isBeslutningsvedtak()).thenReturn(false);

        var historikkAktør = HistorikkAktør.VEDTAKSLØSNINGEN;

        // Act
        tjeneste.produserVedtaksbrev(behandlingVedtakMock);

        var bestillingCaptor = ArgumentCaptor.forClass(DokumentBestilling.class);

        // Assert
        verify(dokumentBestiller).bestillDokument(bestillingCaptor.capture(), eq(historikkAktør));
        var bestilling = bestillingCaptor.getValue();

        assertThat(bestilling.behandlingUuid()).isEqualTo(behandling.getUuid());
        assertThat(bestilling.dokumentMal()).isEqualTo(DokumentMalType.FRITEKSTBREV);
        assertThat(bestilling.bestillingUuid()).isNotNull();
        assertThat(bestilling.journalførSom()).isEqualTo(DokumentMalType.ENDRING_UTBETALING);
    }
}
