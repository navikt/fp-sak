package no.nav.foreldrepenger.domene.uttak.fakta.uttak;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;

class TidligOppstartFarDokumentasjonUtlederTest {

    private static final LocalDate FOM = LocalDate.of(2022, 10, 10);

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryStubProvider();

    @Test
    void farEllerMedmorSøktOmTidligOppstartFellesperiode() {
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(FOM, FOM.plusWeeks(1))
            .build();
        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(oppgittPeriode), true))
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett())
            .lagre(repositoryProvider);

        var yfa = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());

        var foreldrepengerGrunnlag = new ForeldrepengerGrunnlag()
            .medFamilieHendelser(new FamilieHendelser().medBekreftetHendelse(FamilieHendelse.forFødsel(FOM, FOM, List.of(), 1)));
        var input = getInput(behandling, foreldrepengerGrunnlag);
        var behov = TidligOppstartFarDokumentasjonUtleder.utledBehov(oppgittPeriode, input, yfa);

        assertThat(behov).isEmpty();
    }

    @Test
    void farEllerMedmorSøktOmTidligOppstartFedrekvote() {
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriode(FOM, FOM.plusWeeks(1))
            .build();
        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(oppgittPeriode), true))
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett())
            .lagre(repositoryProvider);

        var yfa = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());

        var foreldrepengerGrunnlag = new ForeldrepengerGrunnlag()
            .medFamilieHendelser(new FamilieHendelser().medBekreftetHendelse(FamilieHendelse.forFødsel(FOM, FOM, List.of(), 1)));
        var input = getInput(behandling, foreldrepengerGrunnlag);
        var behov = TidligOppstartFarDokumentasjonUtleder.utledBehov(oppgittPeriode, input, yfa);

        assertThat(behov).isPresent();
        assertThat(behov.get().type()).isEqualTo(DokumentasjonVurderingBehov.Behov.Type.UTTAK);
        assertThat(behov.get().årsak()).isEqualTo(DokumentasjonVurderingBehov.Behov.Årsak.TIDLIG_OPPSTART_FAR);
    }

    @Test
    void morSøkerFedrekvote() {
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medÅrsak(OverføringÅrsak.ALENEOMSORG)
            .medPeriode(FOM, FOM.plusWeeks(1))
            .build();
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(oppgittPeriode), true))
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett())
            .lagre(repositoryProvider);

        var yfa = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());

        var foreldrepengerGrunnlag = new ForeldrepengerGrunnlag()
            .medFamilieHendelser(new FamilieHendelser().medBekreftetHendelse(FamilieHendelse.forFødsel(FOM, FOM, List.of(), 1)));
        var input = getInput(behandling, foreldrepengerGrunnlag);
        var behov = TidligOppstartFarDokumentasjonUtleder.utledBehov(oppgittPeriode, input, yfa);

        assertThat(behov).isEmpty();
    }

    @Test
    void farEllerMedmorSøktOmTidligOppstartForeldrepengerBfhr() {
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(FOM, FOM.plusWeeks(1))
            .build();
        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(OppgittRettighetEntitet.bareSøkerRett())
            .medFordeling(new OppgittFordelingEntitet(List.of(oppgittPeriode), true))
            .lagre(repositoryProvider);

        var yfa = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());

        var foreldrepengerGrunnlag = new ForeldrepengerGrunnlag()
            .medFamilieHendelser(new FamilieHendelser().medBekreftetHendelse(FamilieHendelse.forFødsel(FOM, FOM, List.of(), 1)));
        var input = getInput(behandling, foreldrepengerGrunnlag);
        var behov = TidligOppstartFarDokumentasjonUtleder.utledBehov(oppgittPeriode, input, yfa);

        assertThat(behov).isEmpty();
    }

    @Test
    void farEllerMedmorSøktOmTidligOppstartForeldrepengerAleneomsorg() {
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(FOM, FOM.plusWeeks(1))
            .build();
        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(OppgittRettighetEntitet.aleneomsorg())
            .medFordeling(new OppgittFordelingEntitet(List.of(oppgittPeriode), true))
            .lagre(repositoryProvider);

        var yfa = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());

        var foreldrepengerGrunnlag = new ForeldrepengerGrunnlag()
            .medFamilieHendelser(new FamilieHendelser().medBekreftetHendelse(FamilieHendelse.forFødsel(FOM, FOM, List.of(), 1)));
        var input = getInput(behandling, foreldrepengerGrunnlag);
        var behov = TidligOppstartFarDokumentasjonUtleder.utledBehov(oppgittPeriode, input, yfa);

        assertThat(behov).isEmpty();
    }

    @Test
    void farEllerMedmorSøktOmTidligOppstartMedFlerbarnsdager() {
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medFlerbarnsdager(true)
            .medPeriode(FOM, FOM.plusWeeks(1))
            .build();
        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett())
            .medFordeling(new OppgittFordelingEntitet(List.of(oppgittPeriode), true))
            .lagre(repositoryProvider);

        var yfa = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());

        var foreldrepengerGrunnlag = new ForeldrepengerGrunnlag()
            .medFamilieHendelser(new FamilieHendelser().medBekreftetHendelse(FamilieHendelse.forFødsel(FOM, FOM, List.of(), 1)));
        var input = getInput(behandling, foreldrepengerGrunnlag);
        var behov = TidligOppstartFarDokumentasjonUtleder.utledBehov(oppgittPeriode, input, yfa);

        assertThat(behov).isEmpty();
    }

    @Test
    void farEllerMedmorSøktOmUttakRundtFødsel() {
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(new SamtidigUttaksprosent(100))
            .medPeriode(FOM, FOM.plusWeeks(2))
            .build();
        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett())
            .medFordeling(new OppgittFordelingEntitet(List.of(oppgittPeriode), true))
            .lagre(repositoryProvider);

        var yfa = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());

        var foreldrepengerGrunnlag = new ForeldrepengerGrunnlag()
            .medFamilieHendelser(new FamilieHendelser().medBekreftetHendelse(FamilieHendelse.forFødsel(FOM, FOM, List.of(), 1)));
        var input = getInput(behandling, foreldrepengerGrunnlag);
        var behov = TidligOppstartFarDokumentasjonUtleder.utledBehov(oppgittPeriode, input, yfa);

        assertThat(behov).isEmpty();
    }

    @Test
    void farEllerMedmorSøktOmUttakRundtFødselForLangPeriode() {
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(new SamtidigUttaksprosent(100))
            .medPeriode(FOM, FOM.plusWeeks(3))
            .build();

        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett())
            .medFordeling(new OppgittFordelingEntitet(List.of(oppgittPeriode), true))
            .lagre(repositoryProvider);

        var yfa = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());

        var foreldrepengerGrunnlag = new ForeldrepengerGrunnlag()
            .medFamilieHendelser(new FamilieHendelser().medBekreftetHendelse(FamilieHendelse.forFødsel(FOM, FOM, List.of(), 1)));
        var input = getInput(behandling, foreldrepengerGrunnlag);
        var behov = TidligOppstartFarDokumentasjonUtleder.utledBehov(oppgittPeriode, input, yfa);

        assertThat(behov).isEmpty();
    }

    private static UttakInput getInput(Behandling behandling, ForeldrepengerGrunnlag foreldrepengerGrunnlag) {
        return new UttakInput(BehandlingReferanse.fra(behandling), null, null, foreldrepengerGrunnlag);
    }

    @Test
    void farEllerMedmorSøktForeldrepengerForTidligUtTilSjekk() {
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(new SamtidigUttaksprosent(100))
            .medPeriode(FOM, FOM.plusWeeks(3))
            .build();

        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett())
            .medFordeling(new OppgittFordelingEntitet(List.of(oppgittPeriode), true))
            .lagre(repositoryProvider);

        var yfa = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());

        var foreldrepengerGrunnlag = new ForeldrepengerGrunnlag()
            .medFamilieHendelser(new FamilieHendelser().medBekreftetHendelse(FamilieHendelse.forFødsel(FOM.plusWeeks(3), FOM.plusWeeks(3), List.of(), 1)));
        var input = getInput(behandling, foreldrepengerGrunnlag);
        var behov = TidligOppstartFarDokumentasjonUtleder.utledBehov(oppgittPeriode, input, yfa);

        assertThat(behov).isPresent();
    }

    @Test
    void bfhrSøktOmUttakRundtFødselMorUfør() {
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(FOM, FOM.plusWeeks(2).minusDays(3))
            .medMorsAktivitet(MorsAktivitet.UFØRE)
            .build();

        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett())
            .medFordeling(new OppgittFordelingEntitet(List.of(oppgittPeriode), true))
            .lagre(repositoryProvider);

        var yfa = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());

        var foreldrepengerGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(new FamilieHendelser().medBekreftetHendelse(FamilieHendelse.forFødsel(FOM.plusWeeks(1), FOM.plusWeeks(1), List.of(), 1)));
        var input = getInput(behandling, foreldrepengerGrunnlag);
        var behov = TidligOppstartFarDokumentasjonUtleder.utledBehov(oppgittPeriode, input, yfa);

        assertThat(behov).isEmpty();
    }

}
