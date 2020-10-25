package no.nav.foreldrepenger.domene.person.tps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.FiktiveFnr;
import no.nav.foreldrepenger.domene.person.pdl.AktørTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.integrasjon.person.PersonConsumer;

public class TpsAdapterTest {

    private TpsAdapter tpsAdapterImpl;

    private AktørTjeneste aktørConsumerMock = Mockito.mock(AktørTjeneste.class);
    private PersonConsumer personProxyServiceMock = Mockito.mock(PersonConsumer.class);

    private final AktørId aktørId = AktørId.dummy();
    private final PersonIdent fnr = new PersonIdent(new FiktiveFnr().nesteKvinneFnr());

    @BeforeEach
    public void setup() {
        TpsAdresseOversetter tpsAdresseOversetter = new TpsAdresseOversetter(null);
        TpsOversetter tpsOversetter = new TpsOversetter(tpsAdresseOversetter);
        tpsAdapterImpl = new TpsAdapter(aktørConsumerMock, personProxyServiceMock, tpsOversetter);
    }


    @Test
    public void test_hentKjerneinformasjon_normal() throws Exception {
        AktørId aktørId = AktørId.dummy();
        String navn = "John Doe";
        LocalDate fødselsdato = LocalDate.of(1343, 12, 12);
        NavBrukerKjønn kjønn = NavBrukerKjønn.KVINNE;

        HentPersonResponse response = new HentPersonResponse();
        Bruker person = new Bruker();
        response.setPerson(person);
        Mockito.when(personProxyServiceMock.hentPersonResponse(Mockito.any())).thenReturn(response);

        TpsOversetter tpsOversetterMock = Mockito.mock(TpsOversetter.class);
        Personinfo personinfo0 = new Personinfo.Builder()
                .medPersonIdent(fnr)
                .medNavn(navn)
                .medFødselsdato(fødselsdato)
                .medNavBrukerKjønn(kjønn)
                .medAktørId(aktørId)
                .build();

        Mockito.when(tpsOversetterMock.tilBrukerInfo(Mockito.any(AktørId.class), eq(person))).thenReturn(personinfo0);
        tpsAdapterImpl = new TpsAdapter(aktørConsumerMock, personProxyServiceMock, tpsOversetterMock);

        Personinfo personinfo = tpsAdapterImpl.hentKjerneinformasjon(fnr, aktørId);
        assertThat(personinfo).isNotNull();
        assertThat(personinfo.getAktørId()).isEqualTo(aktørId);
        assertThat(personinfo.getPersonIdent()).isEqualTo(fnr);
        assertThat(personinfo.getNavn()).isEqualTo(navn);
        assertThat(personinfo.getFødselsdato()).isEqualTo(fødselsdato);
    }

    @Test
    public void skal_få_exception_når_tjenesten_ikke_kan_finne_personen() throws Exception {
        Mockito.when(personProxyServiceMock.hentPersonResponse(Mockito.any()))
                .thenThrow(new HentPersonPersonIkkeFunnet(null, null));

        assertThrows(TekniskException.class, () -> tpsAdapterImpl.hentKjerneinformasjon(fnr, aktørId));
    }

    @Test
    public void skal_få_exception_når_tjenesten_ikke_kan_aksesseres_pga_manglende_tilgang() throws Exception {
        when(personProxyServiceMock.hentPersonResponse(any(HentPersonRequest.class)))
                .thenThrow(new HentPersonSikkerhetsbegrensning(null, null));

        assertThrows(ManglerTilgangException.class, () -> tpsAdapterImpl.hentKjerneinformasjon(fnr, aktørId));
    }


}
