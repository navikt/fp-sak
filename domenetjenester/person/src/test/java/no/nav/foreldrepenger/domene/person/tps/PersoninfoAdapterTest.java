package no.nav.foreldrepenger.domene.person.tps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.FiktiveFnr;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;

public class PersoninfoAdapterTest {

    private PersoninfoAdapter adapter; // objektet vi tester

    private static final AktørId AKTØR_ID_SØKER = AktørId.dummy();
    private static final AktørId AKTØR_ID_BARN = AktørId.dummy();

    private static final PersonIdent FNR_SØKER = new PersonIdent(new FiktiveFnr().nesteKvinneFnr());
    private static final PersonIdent FNR_BARN = new PersonIdent(new FiktiveFnr().nesteBarnFnr());

    private Personinfo mockPersoninfo;

    @Before
    public void setup() {
        Personinfo kjerneinfoSøker = lagHentPersonResponseForSøker();
        Personinfo kjerneinfobarn = lagHentPersonResponseForBarn();

        TpsAdapter mockTpsAdapter = mock(TpsAdapter.class);
        TpsFamilieTjeneste mockTpsFamilieTjeneste = mock(TpsFamilieTjeneste.class);
        when(mockTpsAdapter.hentAktørIdForPersonIdent(FNR_BARN)).thenReturn(Optional.of(AKTØR_ID_BARN));
        when(mockTpsAdapter.hentIdentForAktørId(AKTØR_ID_SØKER)).thenReturn(Optional.of(FNR_SØKER));
        when(mockTpsAdapter.hentIdentForAktørId(AKTØR_ID_BARN)).thenReturn(Optional.of(FNR_BARN));
        when(mockTpsAdapter.hentKjerneinformasjon(FNR_BARN, AKTØR_ID_BARN)).thenReturn(kjerneinfobarn);
        when(mockTpsAdapter.hentKjerneinformasjon(FNR_SØKER, AKTØR_ID_SØKER)).thenReturn(kjerneinfoSøker);

        mockPersoninfo = mock(Personinfo.class);
        when(mockPersoninfo.getFødselsdato()).thenReturn(LocalDate.now()); // trenger bare en verdi

        adapter = new PersoninfoAdapter(mockTpsAdapter, mockTpsFamilieTjeneste);
    }

    @Test
    public void skal_innhente_saksopplysninger_for_søker() {
        when(mockPersoninfo.getAktørId()).thenReturn(AKTØR_ID_SØKER);
        when(mockPersoninfo.getKjønn()).thenReturn(NavBrukerKjønn.KVINNE);

        Personinfo søker = adapter.innhentSaksopplysningerForSøker(AKTØR_ID_SØKER);

        assertNotNull(søker);
        assertEquals(AKTØR_ID_SØKER, søker.getAktørId());
        assertEquals(NavBrukerKjønn.KVINNE, søker.getKjønn());
    }

    @Test
    public void skal_innhente_saksopplysninger_for_barn() {
        when(mockPersoninfo.getAktørId()).thenReturn(AKTØR_ID_BARN);
        when(mockPersoninfo.getKjønn()).thenReturn(NavBrukerKjønn.KVINNE);

        Optional<Personinfo> barn = adapter.innhentSaksopplysningerForBarn(FNR_BARN);

        assertTrue(barn.isPresent());
        assertEquals(AKTØR_ID_BARN, barn.get().getAktørId());
        assertNotNull(barn.get().getFødselsdato());
    }

    private Personinfo lagHentPersonResponseForSøker() {
        return new Personinfo.Builder().medAktørId(AKTØR_ID_SØKER).medPersonIdent(FNR_SØKER).medNavn("Kari Nordmann").medFødselsdato(LocalDate.of(1985, 7, 7)).medNavBrukerKjønn(NavBrukerKjønn.KVINNE).build();
    }

    private Personinfo lagHentPersonResponseForBarn() {
        return new Personinfo.Builder().medAktørId(AKTØR_ID_BARN).medPersonIdent(FNR_BARN).medNavn("Kari Nordmann Junior").medFødselsdato(LocalDate.of(2000, 7, 7)).medNavBrukerKjønn(NavBrukerKjønn.KVINNE).build();
    }
}
