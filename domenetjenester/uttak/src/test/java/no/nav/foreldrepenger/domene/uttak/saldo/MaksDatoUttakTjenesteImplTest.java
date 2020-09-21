package no.nav.foreldrepenger.domene.uttak.saldo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class MaksDatoUttakTjenesteImplTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private EntityManager entityManager = repoRule.getEntityManager();
    private UttakRepositoryProvider repositoryProvider = new UttakRepositoryProvider(entityManager);

    @Inject @FagsakYtelseTypeRef("FP")
    private MaksDatoUttakTjeneste maksDatoUttakTjeneste;

    @Test
    public void maksdato_skal_være_siste_uttaksdato_hvis_tom_konto() {

        UttakResultatPerioderEntitet uttak = new UttakResultatPerioderEntitet();
        //mandag
        LocalDate fødselsdato = LocalDate.of(2019, 2, 4);
        UttakResultatPeriodeEntitet fellesperiode = new UttakResultatPeriodeEntitet.Builder(fødselsdato, fødselsdato.plusDays(4))
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(fellesperiode, new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.FRILANS).build())
            .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .medTrekkdager(new Trekkdager(5))
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();
        uttak.leggTilPeriode(fellesperiode);
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medUttak(uttak);
        Behandling behandling = scenario.lagre(repositoryProvider);

        lagreStønadskonto(behandling, new Stønadskonto.Builder()
            .medMaxDager(5)
            .medStønadskontoType(StønadskontoType.FELLESPERIODE)
            .build());

        // Assert
        assertThat(maksDatoUttakTjeneste.beregnMaksDatoUttak(input(behandling)).get()).isEqualTo(fellesperiode.getTom());
    }

    @Test
    public void maksdato_skal_være_fredag_hvis_tom_konto_når_siste_uttaksdato_er_søndag() {

        UttakResultatPerioderEntitet uttak = new UttakResultatPerioderEntitet();
        //søndag
        UttakResultatPeriodeEntitet fellesperiode = new UttakResultatPeriodeEntitet.Builder(LocalDate.of(2019, 10, 10),
            LocalDate.of(2019, 10, 20))
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(fellesperiode, new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.FRILANS).build())
            .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .medTrekkdager(new Trekkdager(5))
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();
        uttak.leggTilPeriode(fellesperiode);
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medUttak(uttak);
        Behandling behandling = scenario.lagre(repositoryProvider);

        lagreStønadskonto(behandling, new Stønadskonto.Builder()
            .medMaxDager(5)
            .medStønadskontoType(StønadskontoType.FELLESPERIODE)
            .build());

        // Assert
        assertThat(maksDatoUttakTjeneste.beregnMaksDatoUttak(input(behandling)).get()).isEqualTo(LocalDate.of(2019, 10, 18));
    }

    private UttakInput input(Behandling behandling) {
        return new UttakInput(BehandlingReferanse.fra(behandling), null, new ForeldrepengerGrunnlag());
    }

    @Test
    public void maksdato_skal_være_fredag_hvis_tom_konto_når_siste_uttaksdato_er_lørdag() {

        UttakResultatPerioderEntitet uttak = new UttakResultatPerioderEntitet();
        //søndag
        UttakResultatPeriodeEntitet fellesperiode = new UttakResultatPeriodeEntitet.Builder(LocalDate.of(2019, 10, 10),
            LocalDate.of(2019, 10, 19))
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(fellesperiode, new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.FRILANS).build())
            .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .medTrekkdager(new Trekkdager(5))
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();
        uttak.leggTilPeriode(fellesperiode);
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medUttak(uttak);
        Behandling behandling = scenario.lagre(repositoryProvider);

        lagreStønadskonto(behandling, new Stønadskonto.Builder()
            .medMaxDager(5)
            .medStønadskontoType(StønadskontoType.FELLESPERIODE)
            .build());

        // Assert
        assertThat(maksDatoUttakTjeneste.beregnMaksDatoUttak(input(behandling)).get()).isEqualTo(LocalDate.of(2019, 10, 18));
    }

    private void lagreStønadskonto(Behandling behandling, Stønadskonto stønadskonto) {
        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(behandling.getFagsak(), Dekningsgrad._100);
        Stønadskontoberegning stønadskontoberegning = new Stønadskontoberegning.Builder()
            .medRegelEvaluering(" ")
            .medRegelInput(" ")
            .medStønadskonto(stønadskonto)
            .build();
        repositoryProvider.getFagsakRelasjonRepository().lagre(behandling.getFagsak(), behandling.getId(), stønadskontoberegning);
    }

    @Test
    public void skal_legge_på_gjenværende_dager_på_siste_uttaksdato() {

        UttakResultatPerioderEntitet uttak = new UttakResultatPerioderEntitet();
        //mandag
        LocalDate fødselsdato = LocalDate.of(2019, 2, 4);
        UttakResultatPeriodeEntitet fellesperiode = new UttakResultatPeriodeEntitet.Builder(fødselsdato, fødselsdato.plusDays(4))
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(fellesperiode, new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.FRILANS).build())
            .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .medTrekkdager(new Trekkdager(5))
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();
        uttak.leggTilPeriode(fellesperiode);
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medUttak(uttak);
        Behandling behandling = scenario.lagre(repositoryProvider);

        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(behandling.getFagsak(), Dekningsgrad._100);
        Stønadskontoberegning stønadskontoberegning = new Stønadskontoberegning.Builder()
            .medRegelEvaluering(" ")
            .medRegelInput(" ")
            .medStønadskonto(new Stønadskonto.Builder()
                .medMaxDager(7)
                .medStønadskontoType(StønadskontoType.FELLESPERIODE)
                .build())
            .medStønadskonto(new Stønadskonto.Builder()
                .medMaxDager(1)
                .medStønadskontoType(StønadskontoType.MØDREKVOTE)
                .build())
            .build();
        repositoryProvider.getFagsakRelasjonRepository().lagre(behandling.getFagsak(), behandling.getId(), stønadskontoberegning);

        // Assert
        //siste uttaksdag + 2 fellesperiode + 1 mødrekvote gjenværende
        assertThat(maksDatoUttakTjeneste.beregnMaksDatoUttak(input(behandling)).get()).isEqualTo(LocalDate.of(2019, 2, 13));
    }

}
