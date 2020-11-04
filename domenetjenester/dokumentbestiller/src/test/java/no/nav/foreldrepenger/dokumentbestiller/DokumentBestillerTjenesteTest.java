package no.nav.foreldrepenger.dokumentbestiller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.foreldrepenger.dokumentbestiller.kafka.DokumentKafkaBestiller;

@ExtendWith(MockitoExtension.class)
public class DokumentBestillerTjenesteTest {
    @Mock
    private HistorikkRepository historikkRepositoryMock;

    @Mock
    private DokumentKafkaBestiller dokumentKafkaBestiller;

    private Behandling behandling;
    private BehandlingRepositoryProvider repositoryProvider;
    private DokumentBestillerTjeneste tjeneste;

    private void settOpp(AbstractTestScenario<?> scenario) {
        this.behandling = scenario.lagMocked();
        this.repositoryProvider = scenario.mockBehandlingRepositoryProvider();

        BrevHistorikkinnslag brevHistorikkinnslag = new BrevHistorikkinnslag(historikkRepositoryMock);

        tjeneste = new DokumentBestillerTjeneste(
                repositoryProvider.getBehandlingRepository(),
                null,
                null,
                brevHistorikkinnslag,
                dokumentKafkaBestiller);
    }

    @Test
    public void skal_bestille_brev_fra_fpformidling() {
        // Arrange
        AbstractTestScenario<?> scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        settOpp(scenario);

        DokumentMalType dokumentMalTypeInput = DokumentMalType.INNHENT_DOK;
        HistorikkAktør historikkAktør = HistorikkAktør.SAKSBEHANDLER;
        BestillBrevDto bestillBrevDto = new BestillBrevDto(behandling.getId(), dokumentMalTypeInput, "fritekst");

        // Act
        tjeneste.bestillDokument(bestillBrevDto, historikkAktør, false);

        // Assert
        verify(dokumentKafkaBestiller).bestillBrevFraKafka(bestillBrevDto, historikkAktør);
    }

    @Test
    public void skal_bestille_manuelt_brev_fra_fpformidling() {
        // Arrange
        AbstractTestScenario<?> scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        settOpp(scenario);

        DokumentMalType dokumentMalTypeInput = DokumentMalType.INNHENT_DOK;
        HistorikkAktør historikkAktør = HistorikkAktør.SAKSBEHANDLER;
        BestillBrevDto bestillBrevDto = new BestillBrevDto(behandling.getId(), dokumentMalTypeInput, "fritekst");

        // Act
        tjeneste.bestillDokument(bestillBrevDto, historikkAktør, true);

        // Assert
        verify(dokumentKafkaBestiller).bestillBrevFraKafka(bestillBrevDto, historikkAktør);

        ArgumentCaptor<Historikkinnslag> historikkinnslagCaptor = ArgumentCaptor.forClass(Historikkinnslag.class);
        verify(historikkRepositoryMock).lagre(historikkinnslagCaptor.capture());
        Historikkinnslag historikkinnslag = historikkinnslagCaptor.getValue();
        assertThat(historikkinnslag.getType()).isEqualTo(HistorikkinnslagType.BREV_BESTILT);
    }

}
