package no.nav.foreldrepenger.økonomi.grensesnittavstemming;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Attestant180;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming115;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragsenhet120;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.økonomi.grensesnittavstemming.GrensesnittavstemmingApplikasjonBatchTjeneste;
import no.nav.foreldrepenger.økonomi.grensesnittavstemming.GrensesnittavstemmingBatchArguments;
import no.nav.foreldrepenger.økonomi.grensesnittavstemming.queue.producer.GrensesnittavstemmingJmsProducer;
import no.nav.foreldrepenger.økonomi.økonomistøtte.BehandleØkonomioppdragKvitteringImplTest;
import no.nav.foreldrepenger.økonomi.økonomistøtte.ØkonomioppdragRepository;
import no.nav.foreldrepenger.økonomi.økonomistøtte.ØkonomistøtteUtils;

public class GrensesnittavstemmingApplikasjonBatchTjenesteTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    private GrensesnittavstemmingApplikasjonBatchTjeneste grensesnittavstemmingApplikasjonTjeneste;

    @Mock
    private ØkonomioppdragRepository økonomiRepository;

    @Mock
    private GrensesnittavstemmingJmsProducer grensesnittavstemmingJmsProducer;

    private List<Oppdrag110> oppdragsliste = new ArrayList<>();

    @Before
    public void setUp() {
        grensesnittavstemmingApplikasjonTjeneste = new GrensesnittavstemmingApplikasjonBatchTjeneste(økonomiRepository, grensesnittavstemmingJmsProducer);
        when(økonomiRepository.hentOppdrag110ForPeriodeOgFagområde(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(oppdragsliste);
    }

    @Test
    public void avstemmingUtenOppdragSkalIkkeSendeAvstemmingsmeldinger(){
         // Arrange
        final HashMap<String, String> argMap = new HashMap<>();
        argMap.put("tom", "23-08-2017");
        argMap.put("fom", "17-08-2017");
        argMap.put("fagomrade", "FP");

        // Act
        final String launch = grensesnittavstemmingApplikasjonTjeneste.launch(new GrensesnittavstemmingBatchArguments(argMap));
        assertThat(launch).startsWith(grensesnittavstemmingApplikasjonTjeneste.getBatchName());

        // Assert
        verify(grensesnittavstemmingJmsProducer, Mockito.never()).sendGrensesnittavstemming(Mockito.any());
    }

    private void setupOppdragsliste() {
        Oppdragskontroll oppdrag = opprettOppdrag();
        oppdragsliste.addAll(oppdrag.getOppdrag110Liste());
    }

    @Test
    public void avstemmingSkalSendeAvstemmingsmeldingerUtenParametere(){
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
    public void avstemmingSkalSendeAvstemmingsmeldinger(){
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
        Avstemming115 a115 = mock(Avstemming115.class);
        when(a115.getKodekomponent()).thenReturn("KK");
        String localDateTimeStr = ØkonomistøtteUtils.tilSpesialkodetDatoOgKlokkeslett(LocalDateTime.now());
        when(a115.getNokkelAvstemming()).thenReturn(localDateTimeStr);
        when(a115.getTidspnktMelding()).thenReturn(localDateTimeStr);
        Oppdrag110 o110 = new Oppdrag110.Builder()
                .medAvstemming115(a115)
                .medKodeAksjon(BehandleØkonomioppdragKvitteringImplTest.KODEAKSJON)
                .medKodeEndring(BehandleØkonomioppdragKvitteringImplTest.KODEENDRING)
                .medKodeFagomrade(BehandleØkonomioppdragKvitteringImplTest.KODEFAGOMRADE_ES)
                .medFagSystemId(BehandleØkonomioppdragKvitteringImplTest.FAGSYSTEMID_BRUKER)
                .medUtbetFrekvens(BehandleØkonomioppdragKvitteringImplTest.UTBETFREKVENS)
                .medOppdragGjelderId(BehandleØkonomioppdragKvitteringImplTest.OPPDRAGGJELDERID)
                .medDatoOppdragGjelderFom(LocalDate.now())
                .medSaksbehId(BehandleØkonomioppdragKvitteringImplTest.SAKSBEHID)
                .medOppdragskontroll(oppdrag)
                .build();
        new Oppdragsenhet120.Builder()
                .medTypeEnhet(BehandleØkonomioppdragKvitteringImplTest.TYPEENHET)
                .medDatoEnhetFom(LocalDate.now())
                .medEnhet(BehandleØkonomioppdragKvitteringImplTest.ENHET)
                .medOppdrag110(o110)
                .build();
        Oppdragslinje150 o150 = new Oppdragslinje150.Builder()
                .medVedtakId(BehandleØkonomioppdragKvitteringImplTest.VEDTAKID)
                .medKodeEndringLinje(BehandleØkonomioppdragKvitteringImplTest.KODEENDRINGLINJE)
                .medKodeKlassifik(BehandleØkonomioppdragKvitteringImplTest.KODEKLASSIFIK_ES)
            .medVedtakFomOgTom(LocalDate.now(), LocalDate.now())
                .medSats(BehandleØkonomioppdragKvitteringImplTest.SATS)
                .medFradragTillegg(BehandleØkonomioppdragKvitteringImplTest.FRADRAGTILLEGG)
                .medTypeSats(BehandleØkonomioppdragKvitteringImplTest.TYPESATS_ES)
                .medBrukKjoreplan("N")
                .medSaksbehId(BehandleØkonomioppdragKvitteringImplTest.SAKSBEHID)
                .medUtbetalesTilId(BehandleØkonomioppdragKvitteringImplTest.OPPDRAGGJELDERID)
                .medHenvisning(BehandleØkonomioppdragKvitteringImplTest.BEHANDLINGID_ES)
                .medOppdrag110(o110)
                .build();
        Attestant180.builder()
                .medAttestantId(BehandleØkonomioppdragKvitteringImplTest.SAKSBEHID)
                .medOppdragslinje150(o150)
                .build();
        oppdrag.getOppdrag110Liste().add(o110);
        return oppdrag;
    }
}
