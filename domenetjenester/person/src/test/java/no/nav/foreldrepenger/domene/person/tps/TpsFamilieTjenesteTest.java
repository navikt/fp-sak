package no.nav.foreldrepenger.domene.person.tps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.person.pdl.AktørTjeneste;
import no.nav.foreldrepenger.domene.person.pdl.FødselTjeneste;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@ExtendWith(MockitoExtension.class)
class TpsFamilieTjenesteTest {

    private static final AktørId AKTØR = AktørId.dummy();
    @Mock
    private FødselTjeneste fødselTjeneste;
    @Mock
    private AktørTjeneste aktørConsumer;
    private PersoninfoAdapter personinfoAdapter;

    @BeforeEach
    public void setUp() {
        personinfoAdapter = new PersoninfoAdapter(aktørConsumer, fødselTjeneste, null, null, null, null);
    }

    @Test
    void test() {
        final var mottattDato = LocalDate.now().minusDays(30);
        final var intervall = new LocalDateInterval(mottattDato.minusWeeks(6), mottattDato.plusWeeks(6));
        final var antallBarn = 1;

        final var personinfo = opprettPersonInfo(AKTØR, antallBarn, mottattDato);
        when(fødselTjeneste.hentFødteBarnInfoFor(any(), any())).thenReturn(genererBarn(personinfo.getFamilierelasjoner(), mottattDato));

        final var fødslerRelatertTilBehandling = personinfoAdapter.innhentAlleFødteForBehandlingIntervaller(AKTØR, List.of(intervall));

        assertThat(fødslerRelatertTilBehandling).hasSize(antallBarn);
    }

    private List<FødtBarnInfo> genererBarn(Set<FamilierelasjonVL> familierelasjoner, LocalDate startdatoIntervall) {
        final var barn = new ArrayList<FødtBarnInfo>();
        for (var familierelasjon : familierelasjoner) {
            barn.add(new FødtBarnInfo.Builder()
                    .medFødselsdato(genererFødselsdag(startdatoIntervall.minusWeeks(1)))
                    .medIdent(familierelasjon.getPersonIdent())
                    .build());
        }
        return barn;
    }

    private Personinfo opprettPersonInfo(AktørId aktørId, int antallBarn, LocalDate startdatoIntervall) {
        final var builder = new Personinfo.Builder();
        builder.medAktørId(aktørId)
                .medNavn("Test")
                .medNavBrukerKjønn(NavBrukerKjønn.KVINNE)
                .medFødselsdato(LocalDate.now().minusYears(30))
                .medPersonIdent(new PersonIdent("123"))
                .medFamilierelasjon(genererBarn(antallBarn, startdatoIntervall));
        return builder.build();
    }

    private Set<FamilierelasjonVL> genererBarn(int antallBarn, LocalDate startdatoIntervall) {
        final Set<FamilierelasjonVL> set = new HashSet<>();
        IntStream.range(0, Math.toIntExact(antallBarn))
                .forEach(barnNr -> set
                        .add(new FamilierelasjonVL(new PersonIdent("" + barnNr + 10L), RelasjonsRolleType.BARN)));
        return set;
    }

    private LocalDate genererFødselsdag(LocalDate startdatoIntervall) {
        final var datoIntervallEntitet = DatoIntervallEntitet.fraOgMedTilOgMed(startdatoIntervall, LocalDate.now());
        final var l = datoIntervallEntitet.antallDager();

        final var v = Math.random() * l;
        return startdatoIntervall.plusDays((long) v);
    }

}
