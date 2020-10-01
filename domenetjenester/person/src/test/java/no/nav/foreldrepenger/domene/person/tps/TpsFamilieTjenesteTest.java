package no.nav.foreldrepenger.domene.person.tps;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.Familierelasjon;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.fpsak.tidsserie.LocalDateInterval;

public class TpsFamilieTjenesteTest {

    private static final AktørId AKTØR = AktørId.dummy();
    private TpsTjeneste tpsTjeneste;
    private TpsFamilieTjeneste tpsFamilieTjeneste;

    @Before
    public void setUp() throws Exception {
        tpsTjeneste = mock(TpsTjeneste.class);
        tpsFamilieTjeneste = new TpsFamilieTjeneste(tpsTjeneste);
    }

    @Test
    public void test() {
        final LocalDate mottattDato = LocalDate.now().minusDays(30);
        final LocalDateInterval intervall = new LocalDateInterval(mottattDato.minusWeeks(6), mottattDato.plusWeeks(6));
        final int antallBarn = 1;

        final Personinfo personinfo = opprettPersonInfo(AKTØR, antallBarn, mottattDato);
        when(tpsTjeneste.hentBrukerForAktør(AKTØR)).thenReturn(Optional.of(personinfo));
        when(tpsTjeneste.hentFødteBarn(AKTØR)).thenReturn(genererBarn(personinfo.getFamilierelasjoner(), mottattDato));

        final List<FødtBarnInfo> fødslerRelatertTilBehandling = tpsFamilieTjeneste.getFødslerRelatertTilBehandling(AKTØR, List.of(intervall));

        assertThat(fødslerRelatertTilBehandling).hasSize(antallBarn);
    }

    private List<FødtBarnInfo> genererBarn(Set<Familierelasjon> familierelasjoner, LocalDate startdatoIntervall) {
        final ArrayList<FødtBarnInfo> barn = new ArrayList<>();
        for (Familierelasjon familierelasjon : familierelasjoner) {
            barn.add(new FødtBarnInfo.Builder()
                .medFødselsdato(genererFødselsdag(startdatoIntervall.minusWeeks(1)))
                .medIdent(familierelasjon.getPersonIdent())
                .medNavn("navn")
                .medNavBrukerKjønn(NavBrukerKjønn.MANN)
                .build());
        }
        return barn;
    }

    private Personinfo opprettPersonInfo(AktørId aktørId, int antallBarn, LocalDate startdatoIntervall) {
        final Personinfo.Builder builder = new Personinfo.Builder();
        builder.medAktørId(aktørId)
            .medNavn("Test")
            .medNavBrukerKjønn(NavBrukerKjønn.KVINNE)
            .medFødselsdato(LocalDate.now().minusYears(30))
            .medPersonIdent(new PersonIdent("123"))
            .medFamilierelasjon(genererBarn(antallBarn, startdatoIntervall));
        return builder.build();
    }

    private Set<Familierelasjon> genererBarn(int antallBarn, LocalDate startdatoIntervall) {
        final Set<Familierelasjon> set = new HashSet<>();
        LocalDate generertFødselsdag = genererFødselsdag(startdatoIntervall.minusWeeks(1));
        IntStream.range(0, Math.toIntExact(antallBarn))
            .forEach(barnNr -> set.add(new Familierelasjon(new PersonIdent("" + barnNr + 10L), RelasjonsRolleType.BARN, generertFødselsdag, "Adr", true)));
        return set;
    }

    private LocalDate genererFødselsdag(LocalDate startdatoIntervall) {
        final DatoIntervallEntitet datoIntervallEntitet = DatoIntervallEntitet.fraOgMedTilOgMed(startdatoIntervall, LocalDate.now());
        final long l = datoIntervallEntitet.antallDager();

        final double v = Math.random() * l;
        return startdatoIntervall.plusDays((long) v);
    }

}
