package no.nav.foreldrepenger.økonomistøtte.grensesnittavstemming;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.batch.task.BatchRunnerTask;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Sats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.TypeSats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomioppdragRepository;
import no.nav.foreldrepenger.økonomistøtte.BehandleØkonomioppdragKvitteringTest;
import no.nav.foreldrepenger.økonomistøtte.grensesnittavstemming.queue.producer.GrensesnittavstemmingJmsProducer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ExtendWith(MockitoExtension.class)
class GrensesnittavstemmingBatchTjenesteTest {

    private GrensesnittavstemmingBatchTjeneste grensesnittavstemmingApplikasjonTjeneste;

    @Mock
    private ØkonomioppdragRepository økonomiRepository;

    @Mock
    private GrensesnittavstemmingJmsProducer grensesnittavstemmingJmsProducer;

    private final List<Oppdrag110> oppdragsliste = new ArrayList<>();

    @BeforeEach
    void setUp() {
        grensesnittavstemmingApplikasjonTjeneste = new GrensesnittavstemmingBatchTjeneste(økonomiRepository,
            grensesnittavstemmingJmsProducer);
        when(økonomiRepository.hentOppdrag110ForPeriodeOgFagområde(Mockito.any(), Mockito.any(),
            Mockito.any())).thenReturn(oppdragsliste);
    }

    @Test
    void avstemmingUtenOppdragSkalIkkeSendeAvstemmingsmeldinger() {
        // Arrange
        var argMap = ProsessTaskData.forProsessTask(BatchRunnerTask.class);
        argMap.setProperty("tom", LocalDate.of(2017, Month.AUGUST, 23).toString());
        argMap.setProperty("fom", LocalDate.of(2017, Month.AUGUST, 17).toString());
        argMap.setProperty("fagomrade", "FP");

        // Act
        var launch = grensesnittavstemmingApplikasjonTjeneste.launch(argMap.getProperties());
        assertThat(launch).startsWith(grensesnittavstemmingApplikasjonTjeneste.getBatchName());

        // Assert
        verify(grensesnittavstemmingJmsProducer, Mockito.never()).sendGrensesnittavstemming(Mockito.any());
    }

    private void setupOppdragsliste() {
        var oppdrag = opprettOppdrag();
        oppdragsliste.addAll(oppdrag.getOppdrag110Liste());
    }

    @Test
    void avstemmingSkalSendeAvstemmingsmeldingerUtenParametere() {
        // Arrange
        setupOppdragsliste();
        var argMap = ProsessTaskData.forProsessTask(BatchRunnerTask.class);
        argMap.setProperty("fagomrade", "REFUTG");

        // Act
        grensesnittavstemmingApplikasjonTjeneste.launch(argMap.getProperties());

        // Assert
        verify(grensesnittavstemmingJmsProducer, Mockito.times(3)).sendGrensesnittavstemming(Mockito.any());
    }

    @Test
    void avstemmingSkalSendeAvstemmingsmeldinger() {
        // Arrange
        setupOppdragsliste();
        var argMap = ProsessTaskData.forProsessTask(BatchRunnerTask.class);
        argMap.setProperty("tom", LocalDate.of(2017, Month.AUGUST, 23).toString());
        argMap.setProperty("fom", LocalDate.of(2017, Month.AUGUST, 17).toString());
        argMap.setProperty("fagomrade", "REFUTG");

        // Act
        grensesnittavstemmingApplikasjonTjeneste.launch(argMap.getProperties());

        // Assert
        verify(grensesnittavstemmingJmsProducer, Mockito.times(3)).sendGrensesnittavstemming(Mockito.any());
    }

    private Oppdragskontroll opprettOppdrag() {
        var oppdrag = new Oppdragskontroll();
        var o110 = new Oppdrag110.Builder()
            .medAvstemming(Avstemming.ny())
            .medKodeEndring(BehandleØkonomioppdragKvitteringTest.KODEENDRING)
            .medKodeFagomrade(BehandleØkonomioppdragKvitteringTest.KODEFAGOMRADE_ES)
            .medFagSystemId(BehandleØkonomioppdragKvitteringTest.FAGSYSTEMID_BRUKER)
            .medOppdragGjelderId(BehandleØkonomioppdragKvitteringTest.OPPDRAGGJELDERID)
            .medSaksbehId(BehandleØkonomioppdragKvitteringTest.SAKSBEHID)
            .medOppdragskontroll(oppdrag)
            .build();
        new Oppdragslinje150.Builder().medVedtakId(
            BehandleØkonomioppdragKvitteringTest.VEDTAKID)
            .medKodeEndringLinje(KodeEndringLinje.NY)
            .medKodeKlassifik(KodeKlassifik.ES_FØDSEL)
            .medVedtakFomOgTom(LocalDate.now(), LocalDate.now())
            .medSats(Sats.på(654L))
            .medTypeSats(TypeSats.ENG)
            .medUtbetalesTilId(BehandleØkonomioppdragKvitteringTest.OPPDRAGGJELDERID)
            .medOppdrag110(o110)
            .build();
        oppdrag.getOppdrag110Liste().add(o110);
        return oppdrag;
    }
}
