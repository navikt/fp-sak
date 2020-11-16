package no.nav.foreldrepenger.domene.person;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.FiktiveFnr;
import no.nav.foreldrepenger.domene.person.pdl.FødselTjeneste;
import no.nav.foreldrepenger.domene.person.pdl.PersonBasisTjeneste;
import no.nav.foreldrepenger.domene.person.pdl.PersoninfoTjeneste;
import no.nav.foreldrepenger.domene.person.pdl.TilknytningTjeneste;
import no.nav.foreldrepenger.domene.person.tps.TpsAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;

@ExtendWith(MockitoExtension.class)
public class PersoninfoAdapterTest {

    @Mock
    private FødselTjeneste fødselTjeneste;
    @Mock
    private TilknytningTjeneste tilknytningTjeneste;
    @Mock
    private PersonBasisTjeneste basisTjeneste;
    @Mock
    private PersoninfoTjeneste personinfoTjeneste;

    private PersoninfoAdapter adapter; // objektet vi tester

    private static final AktørId AKTØR_ID_SØKER = AktørId.dummy();
    private static final AktørId AKTØR_ID_BARN = AktørId.dummy();

    private static final PersonIdent FNR_SØKER = new PersonIdent(new FiktiveFnr().nesteKvinneFnr());
    private static final PersonIdent FNR_BARN = new PersonIdent(new FiktiveFnr().nesteBarnFnr());

    private Personinfo mockPersoninfo;

    @BeforeEach
    public void setup() {
        Personinfo kjerneinfoSøker = lagHentPersonResponseForSøker();
        Personinfo kjerneinfobarn = lagHentPersonResponseForBarn();

        TpsAdapter mockTpsAdapter = mock(TpsAdapter.class);
        lenient().when(mockTpsAdapter.hentAktørIdForPersonIdent(FNR_BARN)).thenReturn(Optional.of(AKTØR_ID_BARN));
        lenient().when(mockTpsAdapter.hentIdentForAktørId(AKTØR_ID_SØKER)).thenReturn(Optional.of(FNR_SØKER));
        lenient().when(mockTpsAdapter.hentIdentForAktørId(AKTØR_ID_BARN)).thenReturn(Optional.of(FNR_BARN));
        lenient().when(mockTpsAdapter.hentKjerneinformasjon(FNR_BARN, AKTØR_ID_BARN)).thenReturn(kjerneinfobarn);
        lenient().when(mockTpsAdapter.hentKjerneinformasjon(FNR_SØKER, AKTØR_ID_SØKER)).thenReturn(kjerneinfoSøker);

        mockPersoninfo = mock(Personinfo.class);
        lenient().when(mockPersoninfo.getFødselsdato()).thenReturn(LocalDate.now()); // trenger bare en verdi

        adapter = new PersoninfoAdapter(mockTpsAdapter, fødselTjeneste, tilknytningTjeneste, basisTjeneste, personinfoTjeneste, null);
    }

    @Test
    public void skal_innhente_saksopplysninger_for_søker() {
        lenient().when(mockPersoninfo.getAktørId()).thenReturn(AKTØR_ID_SØKER);

        Personinfo søker = adapter.innhentSaksopplysningerForSøker(AKTØR_ID_SØKER);

        assertThat(søker).isNotNull();
        assertThat(søker.getAktørId()).isEqualTo(AKTØR_ID_SØKER);
        assertThat(søker.getKjønn()).isEqualTo(NavBrukerKjønn.KVINNE);
    }

    @Test
    public void skal_innhente_saksopplysninger_for_barn() {
        lenient().when(mockPersoninfo.getAktørId()).thenReturn(AKTØR_ID_BARN);

        Optional<Personinfo> barn = adapter.innhentSaksopplysningerFor(FNR_BARN);

        assertThat(barn).isPresent();
        assertThat(barn.get().getAktørId()).isEqualTo(AKTØR_ID_BARN);
        assertThat(barn.get().getFødselsdato()).isNotNull();
    }

    private Personinfo lagHentPersonResponseForSøker() {
        return new Personinfo.Builder().medAktørId(AKTØR_ID_SØKER).medPersonIdent(FNR_SØKER).medNavn("Kari Nordmann")
                .medFødselsdato(LocalDate.of(1985, 7, 7)).medNavBrukerKjønn(NavBrukerKjønn.KVINNE).build();
    }

    private Personinfo lagHentPersonResponseForBarn() {
        return new Personinfo.Builder().medAktørId(AKTØR_ID_BARN).medPersonIdent(FNR_BARN).medNavn("Kari Nordmann Junior")
                .medFødselsdato(LocalDate.of(2000, 7, 7)).medNavBrukerKjønn(NavBrukerKjønn.KVINNE).build();
    }
}
