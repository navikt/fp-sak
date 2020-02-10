package no.nav.foreldrepenger.domene.person.tps;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.Familierelasjon;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public class TpsFamilieTjenesteTest {

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());
    private TpsTjeneste tpsTjeneste;
    private TpsFamilieTjeneste tpsFamilieTjeneste;

    @Before
    public void setUp() throws Exception {
        tpsTjeneste = mock(TpsTjeneste.class);
        final Period periodeFør = Period.parse("P1W");
        final Period periodeEtter = Period.parse("P4W");
        tpsFamilieTjeneste = new TpsFamilieTjeneste(tpsTjeneste, repositoryProvider, periodeFør, periodeEtter);
    }

    @Test
    public void test() {
        final LocalDate mottattDato = LocalDate.now().minusDays(30);
        final int antallBarn = 1;
        final Behandling behandling = opprettOriginalBehandling(BehandlingResultatType.INNVILGET, mottattDato);
        Behandling revurdering = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_HENDELSE_FØDSEL).medOriginalBehandling(behandling)).build();
        BehandlingLås lås = repositoryProvider.getBehandlingRepository().taSkriveLås(revurdering);
        repositoryProvider.getBehandlingRepository().lagre(revurdering, lås);
        repositoryProvider.getFamilieHendelseRepository().kopierGrunnlagFraEksisterendeBehandling(behandling.getId(), revurdering.getId());

        final Personinfo personinfo = opprettPersonInfo(behandling.getAktørId(), antallBarn, mottattDato);
        when(tpsTjeneste.hentBrukerForAktør(behandling.getAktørId())).thenReturn(Optional.of(personinfo));
        when(tpsTjeneste.hentFødteBarn(behandling.getAktørId())).thenReturn(genererBarn(personinfo.getFamilierelasjoner(), mottattDato));

        final FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        final List<FødtBarnInfo> fødslerRelatertTilBehandling = tpsFamilieTjeneste.getFødslerRelatertTilBehandling(revurdering, familieHendelseGrunnlag);

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

    private Behandling opprettOriginalBehandling(BehandlingResultatType behandlingResultatType, LocalDate mottattDato) {
        var scenario = ScenarioMorSøkerEngangsstønad
            .forFødsel()
            .medDefaultBekreftetTerminbekreftelse();
        scenario.medSøknad().medSøknadsdato(mottattDato).medMottattDato(mottattDato);
        Behandling originalBehandling = scenario.lagre(repositoryProvider);
        Behandlingsresultat originalResultat = Behandlingsresultat.builder()
            .medBehandlingResultatType(behandlingResultatType)
            .buildFor(originalBehandling);

        BehandlingLås behandlingLås = repositoryProvider.getBehandlingRepository().taSkriveLås(originalBehandling);
        repositoryProvider.getBehandlingRepository().lagre(originalBehandling, behandlingLås);

        BehandlingVedtak originalVedtak = BehandlingVedtak.builder()
            .medVedtakstidspunkt(LocalDateTime.now())
            .medBehandlingsresultat(originalResultat)
            .medVedtakResultatType(behandlingResultatType.equals(BehandlingResultatType.INNVILGET) ?
                VedtakResultatType.INNVILGET : VedtakResultatType.AVSLAG)
            .medAnsvarligSaksbehandler("asdf")
            .build();

        repositoryProvider.getBehandlingVedtakRepository().lagre(originalVedtak, behandlingLås);
        return originalBehandling;
    }

}
