package no.nav.foreldrepenger.økonomistøtte.grensesnittavstemming;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndringLinjeType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.økonomistøtte.grensesnittavstemming.queue.producer.GrensesnittavstemmingJmsProducer;
import no.nav.foreldrepenger.økonomistøtte.BehandleØkonomioppdragKvitteringTest;
import no.nav.foreldrepenger.økonomistøtte.ØkonomioppdragRepository;

@ExtendWith(MockitoExtension.class)
public class GrensesnittavstemmingBatchTjenesteTest {

    private GrensesnittavstemmingBatchTjeneste grensesnittavstemmingApplikasjonTjeneste;

    @Mock
    private ØkonomioppdragRepository økonomiRepository;

    @Mock
    private GrensesnittavstemmingJmsProducer grensesnittavstemmingJmsProducer;

    private final List<Oppdrag110> oppdragsliste = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        grensesnittavstemmingApplikasjonTjeneste = new GrensesnittavstemmingBatchTjeneste(økonomiRepository,
            grensesnittavstemmingJmsProducer);
        when(økonomiRepository.hentOppdrag110ForPeriodeOgFagområde(Mockito.any(), Mockito.any(),
            Mockito.any())).thenReturn(oppdragsliste);
    }

    @Test
    public void avstemmingUtenOppdragSkalIkkeSendeAvstemmingsmeldinger() {
        // Arrange
        final HashMap<String, String> argMap = new HashMap<>();
        argMap.put("tom", "23-08-2017");
        argMap.put("fom", "17-08-2017");
        argMap.put("fagomrade", "FP");

        // Act
        final String launch = grensesnittavstemmingApplikasjonTjeneste.launch(
            new GrensesnittavstemmingBatchArguments(argMap));
        assertThat(launch).startsWith(grensesnittavstemmingApplikasjonTjeneste.getBatchName());

        // Assert
        verify(grensesnittavstemmingJmsProducer, Mockito.never()).sendGrensesnittavstemming(Mockito.any());
    }

    private void setupOppdragsliste() {
        Oppdragskontroll oppdrag = opprettOppdrag();
        oppdragsliste.addAll(oppdrag.getOppdrag110Liste());
    }

    @Test
    public void avstemmingSkalSendeAvstemmingsmeldingerUtenParametere() {
        // Arrange
        setupOppdragsliste();
        final HashMap<String, String> argMap = new HashMap<>();
        argMap.put("fagomrade", "REFUTG");

        // Act
        grensesnittavstemmingApplikasjonTjeneste.launch(new GrensesnittavstemmingBatchArguments(argMap));

        // Assert
        verify(grensesnittavstemmingJmsProducer, Mockito.times(3)).sendGrensesnittavstemming(Mockito.any());
    }

    @Test
    public void avstemmingSkalSendeAvstemmingsmeldinger() {
        // Arrange
        setupOppdragsliste();
        final HashMap<String, String> argMap = new HashMap<>();
        argMap.put("tom", "23-08-2017");
        argMap.put("fom", "17-08-2017");
        argMap.put("fagomrade", "REFUTG");

        // Act
        grensesnittavstemmingApplikasjonTjeneste.launch(new GrensesnittavstemmingBatchArguments(argMap));

        // Assert
        verify(grensesnittavstemmingJmsProducer, Mockito.times(3)).sendGrensesnittavstemming(Mockito.any());
    }

    private Oppdragskontroll opprettOppdrag() {
        Oppdragskontroll oppdrag = new Oppdragskontroll();
        Oppdrag110 o110 = new Oppdrag110.Builder()
            .medAvstemming(Avstemming.ny())
            .medKodeAksjon(BehandleØkonomioppdragKvitteringTest.KODEAKSJON)
            .medKodeEndring(BehandleØkonomioppdragKvitteringTest.KODEENDRING)
            .medKodeFagomrade(BehandleØkonomioppdragKvitteringTest.KODEFAGOMRADE_ES)
            .medFagSystemId(BehandleØkonomioppdragKvitteringTest.FAGSYSTEMID_BRUKER)
            .medUtbetFrekvens(BehandleØkonomioppdragKvitteringTest.UTBETFREKVENS)
            .medOppdragGjelderId(BehandleØkonomioppdragKvitteringTest.OPPDRAGGJELDERID)
            .medDatoOppdragGjelderFom(LocalDate.now())
            .medSaksbehId(BehandleØkonomioppdragKvitteringTest.SAKSBEHID)
            .medOppdragskontroll(oppdrag)
            .build();
        new Oppdragslinje150.Builder().medVedtakId(
            BehandleØkonomioppdragKvitteringTest.VEDTAKID)
            .medKodeEndringLinje(KodeEndringLinjeType.NY)
            .medKodeKlassifik(BehandleØkonomioppdragKvitteringTest.KODEKLASSIFIK_ES)
            .medVedtakFomOgTom(LocalDate.now(), LocalDate.now())
            .medSats(BehandleØkonomioppdragKvitteringTest.SATS)
            .medTypeSats(BehandleØkonomioppdragKvitteringTest.TYPESATS_ES)
            .medUtbetalesTilId(BehandleØkonomioppdragKvitteringTest.OPPDRAGGJELDERID)
            .medOppdrag110(o110)
            .build();
        oppdrag.getOppdrag110Liste().add(o110);
        return oppdrag;
    }
}
