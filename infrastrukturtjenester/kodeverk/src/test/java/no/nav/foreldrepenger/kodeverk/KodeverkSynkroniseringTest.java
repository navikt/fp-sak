package no.nav.foreldrepenger.kodeverk;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.kodeverk.KodeverkSynkroniseringRepository;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.tjeneste.virksomhet.kodeverk.v2.HentKodeverkHentKodeverkKodeverkIkkeFunnet;
import no.nav.tjeneste.virksomhet.kodeverk.v2.informasjon.EnkeltKodeverk;
import no.nav.tjeneste.virksomhet.kodeverk.v2.informasjon.Kode;
import no.nav.tjeneste.virksomhet.kodeverk.v2.informasjon.Periode;
import no.nav.tjeneste.virksomhet.kodeverk.v2.informasjon.Term;
import no.nav.tjeneste.virksomhet.kodeverk.v2.meldinger.FinnKodeverkListeResponse;
import no.nav.tjeneste.virksomhet.kodeverk.v2.meldinger.HentKodeverkRequest;
import no.nav.tjeneste.virksomhet.kodeverk.v2.meldinger.HentKodeverkResponse;
import no.nav.vedtak.felles.integrasjon.kodeverk.KodeverkConsumer;

public class KodeverkSynkroniseringTest extends EntityManagerAwareTest {

    private final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private KodeverkSynkroniseringRepository kodeverkSynkroniseringRepository;
    private KodeverkSynkronisering kodeverkSynkronisering;
    private final KodeverkConsumer kodeverkConsumer = mock(KodeverkConsumer.class);

    private final ArgumentMatcher<HentKodeverkRequest> kodeverkRequestArgumentMatcher = o -> o.getNavn().compareTo("Postnummer") == 0;

    @BeforeEach
    public void setup() {
        kodeverkSynkroniseringRepository = Mockito
                .spy(new KodeverkSynkroniseringRepository(getEntityManager()));
        kodeverkSynkronisering = new KodeverkSynkronisering(kodeverkSynkroniseringRepository, new KodeverkTjeneste(kodeverkConsumer));

        // reset oppdateringmetoder slik at de ikke endrer noe
        Mockito.doNothing().when(kodeverkSynkroniseringRepository).oppdaterEksisterendeKode(any(), any(), any(), any(), any(), any());
        Mockito.doNothing().when(kodeverkSynkroniseringRepository).oppdaterEksisterendeKodeverk(any(), any(), any());

        Mockito.doNothing().when(kodeverkSynkroniseringRepository).opprettNyKode(any(), any(), any(), any(), any(), any());

    }

    @Test
    public void skal_synke_kodeverk_ved_ny_versjon() throws HentKodeverkHentKodeverkKodeverkIkkeFunnet {
        // Arrange
        HentKodeverkResponse kodeverkResponse = opprettEnkeltKodeverkResponse();
        when(kodeverkConsumer.hentKodeverk(argThat(kodeverkRequestArgumentMatcher))).thenReturn(kodeverkResponse);

        Map<String, String> eierNavnMap = new HashMap<>();
        eierNavnMap.put("Postnummer", "POSTSTED");
        when(kodeverkSynkroniseringRepository.hentKodeverkEierNavnMap()).thenReturn(eierNavnMap);

        when(kodeverkConsumer.finnKodeverkListe(any())).thenReturn(opprettEnkeltKodeverkListeResponse("Postnummer", "0051"));

        // Act
        kodeverkSynkronisering.synkroniserAlleKodeverk();

        // Assert
        verify(kodeverkSynkroniseringRepository, times(1)).opprettNyKode(anyString(), anyString(), anyString(), anyString(), any(LocalDate.class),
                any(LocalDate.class));
        verify(kodeverkSynkroniseringRepository, times(1)).oppdaterEksisterendeKode(anyString(), anyString(), anyString(), anyString(),
                any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    public void skal_ikke_synke_kodeverk_ved_samme_versjon() throws HentKodeverkHentKodeverkKodeverkIkkeFunnet {
        // Arrange
        HentKodeverkResponse kodeverkResponse = opprettEnkeltKodeverkResponse();
        when(kodeverkConsumer.hentKodeverk(argThat(kodeverkRequestArgumentMatcher))).thenReturn(kodeverkResponse);

        Map<String, String> eierNavnMap = new HashMap<>();
        eierNavnMap.put("Postnummer", "POSTSTED");
        when(kodeverkConsumer.finnKodeverkListe(any())).thenReturn(opprettEnkeltKodeverkListeResponse("Postnummer", "7")); // pt er versjon 7
                                                                                                                           // registrert i script

        // Act
        kodeverkSynkronisering.synkroniserAlleKodeverk();

        // Assert
        verify(kodeverkSynkroniseringRepository, times(0)).opprettNyKode(anyString(), anyString(), anyString(), anyString(), any(LocalDate.class),
                any(LocalDate.class));
        verify(kodeverkSynkroniseringRepository, times(0)).oppdaterEksisterendeKode(anyString(), anyString(), anyString(), anyString(),
                any(LocalDate.class), any(LocalDate.class));
    }

    private static FinnKodeverkListeResponse opprettEnkeltKodeverkListeResponse(String navn, String versjon) {
        no.nav.tjeneste.virksomhet.kodeverk.v2.informasjon.finnkodeverkliste.Kodeverk element = new no.nav.tjeneste.virksomhet.kodeverk.v2.informasjon.finnkodeverkliste.Kodeverk();
        element.setEier("Koderverksforvaltning");
        element.setNavn(navn);
        element.setVersjonsnummer(versjon);
        element.setUri("http://nav.no/kodeverk/Kodeverk/" + navn + "?v=" + versjon);
        element.setVersjoneringsdato(KodeverkTjenesteTest.convertToXMLGregorianCalendar(LocalDate.now().minusDays(1)));
        FinnKodeverkListeResponse response = new FinnKodeverkListeResponse();
        response.getKodeverkListe().add(element);
        return response;
    }

    private HentKodeverkResponse opprettEnkeltKodeverkResponse() {
        EnkeltKodeverk enkeltKodeverk = new EnkeltKodeverk();
        enkeltKodeverk.setNavn("Postnummer");
        enkeltKodeverk.setVersjonsnummer("6");
        enkeltKodeverk.getKode().add(lagKode("7818", "LUND", null,
                LocalDate.of(1900, 1, 1), MAX_DATE));
        enkeltKodeverk.getKode().add(lagKode("8888", "DALSTROKA INNAFOR", null,
                LocalDate.of(2017, 1, 1), MAX_DATE));
        HentKodeverkResponse response = new HentKodeverkResponse();
        response.setKodeverk(enkeltKodeverk);
        return response;
    }

    private static Kode lagKode(String kodeNavn, String termNavn, String uri, LocalDate fom, LocalDate tom) {
        Periode periode = new Periode();
        periode.setFom(KodeverkTjenesteTest.convertToXMLGregorianCalendar(fom));
        periode.setTom(KodeverkTjenesteTest.convertToXMLGregorianCalendar(tom));
        Kode kode = new Kode();
        kode.setNavn(kodeNavn);
        kode.getGyldighetsperiode().add(periode);
        kode.setUri(uri);
        Term term = new Term();
        term.setNavn(termNavn);
        term.setSpraak("nb");
        term.getGyldighetsperiode().add(periode);
        kode.getTerm().add(term);
        return kode;
    }
}
