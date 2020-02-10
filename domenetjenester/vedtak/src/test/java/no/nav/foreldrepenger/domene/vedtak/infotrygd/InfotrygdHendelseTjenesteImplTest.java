package no.nav.foreldrepenger.domene.vedtak.infotrygd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegTilstand;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.FeedDto;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.FeedElement;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;

public class InfotrygdHendelseTjenesteImplTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private Behandling behandling;

    @Mock
    private AktørId aktørId;

    @Mock
    private BehandlingStegTilstand tilstand;

    @Mock
    private OidcRestClient oidcRestClient;

    private InfotrygdHendelseTjenesteImpl tjeneste;

    private static final String BASE_URL_FEED = "https://infotrygd-hendelser-api-t10.nais.preprod.local/infotrygd/hendelser";
    private URI expectedStartUri;
    private URI endpoint = URI.create(BASE_URL_FEED);

    @Before
    public void setUp() {
        tjeneste = new InfotrygdHendelseTjenesteImpl(endpoint, oidcRestClient);
        String expectedQueryParams = "?fomDato=2018-05-14T08:15:30Z&aktorId=0000000000000";
        String expectedQueryParamsEncoded = konverterTilUTF8(expectedQueryParams);
        expectedStartUri = URI.create(BASE_URL_FEED + expectedQueryParamsEncoded);
    }

    private String konverterTilUTF8(String expectedQueryParams) {
        String colonEncoded = "%3A";
        return expectedQueryParams.replace(":", colonEncoded);
    }

    @Test
    public void skal_lese_fra_infotrygd_feed() {

        //Arrange
        FeedDto feed = lagTestData();
        LocalDateTime opprettetTidspunkt = LocalDateTime.of(2018, 5, 14, 10, 15, 30, 294);
        when(oidcRestClient.get(expectedStartUri, FeedDto.class)).thenReturn(feed);
        when(behandling.getAktørId()).thenReturn(aktørId);
        when(aktørId.getId()).thenReturn("0000000000000");
        when(behandling.getBehandlingStegTilstandHistorikk()).thenReturn(Stream.of(tilstand));
        when(tilstand.getBehandlingSteg()).thenReturn(BehandlingStegType.FATTE_VEDTAK);
        when(tilstand.getOpprettetTidspunkt()).thenReturn(opprettetTidspunkt);

        //Act
        List<InfotrygdHendelse> infotrygdHendelse = tjeneste.hentHendelsesListFraInfotrygdFeed(behandling);

        //Assert
        assertThat(infotrygdHendelse).hasSize(2);
    }

    private FeedDto lagTestData() {
        return new FeedDto.Builder()
            .medTittel("enhetstest")
            .medElementer(Arrays.asList(
                lagElement(1, new InfotrygdAnnulert()),
                lagElement(2, new InfotrygdInnvilget())))
            .build();
    }

    private FeedElement lagElement(long sequence, Object melding) {
        String type;
        if (melding instanceof InfotrygdAnnulert) {
            type = "ANNULERT_v1";
        } else {
            type = "INNVILGET_v1";
        }
        return new FeedElement.Builder()
            .medSekvensId(sequence)
            .medType(type)
            .medInnhold(lagInnhold(melding))
            .build();
    }

    private Innhold lagInnhold(Object melding) {
        Innhold innhold = (Innhold) melding;
        innhold.setAktoerId("0000000000000");
        innhold.setFom(LocalDate.now());
        innhold.setIdentDato(konverterFomDatoTilString(LocalDate.now()));
        innhold.setTypeYtelse("Type");

        return innhold;

    }

    private String konverterFomDatoTilString(LocalDate dato) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return dato.format(formatter);
    }
}
