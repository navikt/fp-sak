package no.nav.foreldrepenger.domene.person;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.aktør.FamilierelasjonVL;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.FiktiveFnr;
import no.nav.foreldrepenger.domene.person.pdl.AktørTjeneste;
import no.nav.foreldrepenger.domene.person.pdl.FødselTjeneste;
import no.nav.foreldrepenger.domene.person.pdl.PersonBasisTjeneste;
import no.nav.foreldrepenger.domene.person.pdl.PersoninfoTjeneste;
import no.nav.foreldrepenger.domene.person.pdl.TilknytningTjeneste;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@ExtendWith(MockitoExtension.class)
class PersoninfoAdapterTest {

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
        var kjerneinfoSøker = lagHentPersonResponseForSøker();
        var kjerneinfobarn = lagHentPersonResponseForBarn();

        var aktørConsumer = mock(AktørTjeneste.class);
        lenient().when(aktørConsumer.hentAktørIdForPersonIdent(FNR_BARN)).thenReturn(Optional.of(AKTØR_ID_BARN));
        lenient().when(aktørConsumer.hentPersonIdentForAktørId(AKTØR_ID_SØKER)).thenReturn(Optional.of(FNR_SØKER));
        lenient().when(aktørConsumer.hentPersonIdentForAktørId(AKTØR_ID_BARN)).thenReturn(Optional.of(FNR_BARN));
        lenient().when(personinfoTjeneste.hentPersoninfo(FagsakYtelseType.FORELDREPENGER, AKTØR_ID_BARN, FNR_BARN, true)).thenReturn(kjerneinfobarn);
        lenient().when(personinfoTjeneste.hentPersoninfo(FagsakYtelseType.FORELDREPENGER, AKTØR_ID_SØKER, FNR_SØKER, false)).thenReturn(kjerneinfoSøker);

        mockPersoninfo = mock(Personinfo.class);
        lenient().when(mockPersoninfo.getFødselsdato()).thenReturn(LocalDate.now()); // trenger bare en verdi

        adapter = new PersoninfoAdapter(aktørConsumer, fødselTjeneste, tilknytningTjeneste, basisTjeneste, personinfoTjeneste, null);
    }

    @Test
    void skal_innhente_saksopplysninger_for_søker() {
        lenient().when(mockPersoninfo.getAktørId()).thenReturn(AKTØR_ID_SØKER);

        var søker = adapter.innhentPersonopplysningerFor(FagsakYtelseType.FORELDREPENGER, AKTØR_ID_SØKER).orElse(null);

        assertThat(søker).isNotNull();
        assertThat(søker.getAktørId()).isEqualTo(AKTØR_ID_SØKER);
        assertThat(søker.getKjønn()).isEqualTo(NavBrukerKjønn.KVINNE);
    }

    @Test
    void skal_innhente_saksopplysninger_for_barn() {
        lenient().when(mockPersoninfo.getAktørId()).thenReturn(AKTØR_ID_BARN);

        var barn = adapter.innhentPersonopplysningerFor(FagsakYtelseType.FORELDREPENGER, FNR_BARN, true);

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

    @Test
    void skal_innhente_alle_fødte_barn_i_intervall() {
        var mottattDato = LocalDate.now().minusDays(30);
        var intervall = new LocalDateInterval(mottattDato.minusWeeks(6), mottattDato.plusWeeks(6));
        var antallBarn = 1;

        var aktørId = AktørId.dummy();
        var personinfo = opprettPersonInfo(aktørId, antallBarn);
        when(fødselTjeneste.hentFødteBarnInfoFor(any(), any(), any(), any())).thenReturn(genererBarn(personinfo.getFamilierelasjoner(), mottattDato));

        var fødslerRelatertTilBehandling = adapter.innhentAlleFødteForBehandlingIntervaller(FagsakYtelseType.FORELDREPENGER, aktørId, List.of(intervall));

        assertThat(fødslerRelatertTilBehandling).hasSize(antallBarn);
    }

    private List<FødtBarnInfo> genererBarn(Set<FamilierelasjonVL> familierelasjoner, LocalDate startdatoIntervall) {
        var barn = new ArrayList<FødtBarnInfo>();
        for (var familierelasjon : familierelasjoner) {
            barn.add(new FødtBarnInfo.Builder()
                .medFødselsdato(genererFødselsdag(startdatoIntervall.minusWeeks(1)))
                .medIdent(familierelasjon.getPersonIdent())
                .build());
        }
        return barn;
    }

    private Personinfo opprettPersonInfo(AktørId aktørId, int antallBarn) {
        var builder = new Personinfo.Builder();
        builder.medAktørId(aktørId)
            .medNavn("Test")
            .medNavBrukerKjønn(NavBrukerKjønn.KVINNE)
            .medFødselsdato(LocalDate.now().minusYears(30))
            .medPersonIdent(new PersonIdent("123"))
            .medFamilierelasjon(genererBarn(antallBarn));
        return builder.build();
    }

    private Set<FamilierelasjonVL> genererBarn(int antallBarn) {
        final Set<FamilierelasjonVL> set = new HashSet<>();
        IntStream.range(0, Math.toIntExact(antallBarn))
            .forEach(barnNr -> set
                .add(new FamilierelasjonVL(new PersonIdent("" + barnNr + 10L), RelasjonsRolleType.BARN)));
        return set;
    }

    private LocalDate genererFødselsdag(LocalDate startdatoIntervall) {
        var datoIntervallEntitet = DatoIntervallEntitet.fraOgMedTilOgMed(startdatoIntervall, LocalDate.now());
        var l = datoIntervallEntitet.antallDager();

        var v = Math.random() * l;
        return startdatoIntervall.plusDays((long) v);
    }
}
