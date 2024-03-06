package no.nav.foreldrepenger.domene.uttak.saldo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
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
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.KontoerGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.RettOgOmsorgGrunnlagBygger;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.saldo.fp.MaksDatoUttakTjenesteImpl;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;

class MaksDatoUttakTjenesteImplTest {

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryStubProvider();

    private final MaksDatoUttakTjenesteImpl maksDatoUttakTjeneste;

    {
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(repositoryProvider.getFagsakRepository(), repositoryProvider.getFagsakRelasjonRepository());
        var stønadskontoSaldoTjeneste = new StønadskontoSaldoTjeneste(repositoryProvider, new KontoerGrunnlagBygger(fagsakRelasjonTjeneste,
            new RettOgOmsorgGrunnlagBygger(repositoryProvider, new ForeldrepengerUttakTjeneste(repositoryProvider.getFpUttakRepository()))), fagsakRelasjonTjeneste);
        maksDatoUttakTjeneste = new MaksDatoUttakTjenesteImpl(
            repositoryProvider.getFpUttakRepository(), stønadskontoSaldoTjeneste);
    }

    @Test
    void maksdato_skal_være_siste_uttaksdato_hvis_tom_konto() {

        var uttak = new UttakResultatPerioderEntitet();
        //mandag
        var fødselsdato = LocalDate.of(2019, 2, 4);
        var fellesperiode = new UttakResultatPeriodeEntitet.Builder(fødselsdato,
            fødselsdato.plusDays(4)).medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(fellesperiode,
            new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.FRILANS).build()).medTrekkonto(
            StønadskontoType.FELLESPERIODE).medTrekkdager(new Trekkdager(5)).medArbeidsprosent(BigDecimal.ZERO).build();
        uttak.leggTilPeriode(fellesperiode);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(), true))
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett());
        scenario.medUttak(uttak);
        var behandling = scenario.lagre(repositoryProvider);

        lagreStønadskonto(behandling,
            new Stønadskonto.Builder().medMaxDager(5).medStønadskontoType(StønadskontoType.FELLESPERIODE).build());

        // Assert
        assertThat(maksDatoUttakTjeneste.beregnMaksDatoUttak(input(behandling))).contains(fellesperiode.getTom());
    }

    @Test
    void maksdato_skal_være_fredag_hvis_tom_konto_når_siste_uttaksdato_er_søndag() {

        var uttak = new UttakResultatPerioderEntitet();
        //søndag
        var fellesperiode = new UttakResultatPeriodeEntitet.Builder(LocalDate.of(2019, 10, 10),
            LocalDate.of(2019, 10, 20)).medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(fellesperiode,
            new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.FRILANS).build()).medTrekkonto(
            StønadskontoType.FELLESPERIODE).medTrekkdager(new Trekkdager(5)).medArbeidsprosent(BigDecimal.ZERO).build();
        uttak.leggTilPeriode(fellesperiode);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(), true))
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett());
        scenario.medUttak(uttak);
        var behandling = scenario.lagre(repositoryProvider);

        lagreStønadskonto(behandling,
            new Stønadskonto.Builder().medMaxDager(5).medStønadskontoType(StønadskontoType.FELLESPERIODE).build());

        // Assert
        assertThat(maksDatoUttakTjeneste.beregnMaksDatoUttak(input(behandling))).contains(LocalDate.of(2019, 10, 18));
    }

    private UttakInput input(Behandling behandling) {
        var foreldrepengerGrunnlag = new ForeldrepengerGrunnlag()
            .medFamilieHendelser(new FamilieHendelser().medSøknadHendelse(FamilieHendelse.forFødsel(null, LocalDate.now(),
                List.of(), 1)));
        return new UttakInput(BehandlingReferanse.fra(behandling), null, foreldrepengerGrunnlag);
    }

    @Test
    void maksdato_skal_være_fredag_hvis_tom_konto_når_siste_uttaksdato_er_lørdag() {

        var uttak = new UttakResultatPerioderEntitet();
        //søndag
        var fellesperiode = new UttakResultatPeriodeEntitet.Builder(LocalDate.of(2019, 10, 10),
            LocalDate.of(2019, 10, 19)).medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(fellesperiode,
            new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.FRILANS).build()).medTrekkonto(
            StønadskontoType.FELLESPERIODE).medTrekkdager(new Trekkdager(5)).medArbeidsprosent(BigDecimal.ZERO).build();
        uttak.leggTilPeriode(fellesperiode);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(), true))
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett());
        scenario.medUttak(uttak);
        var behandling = scenario.lagre(repositoryProvider);

        lagreStønadskonto(behandling,
            new Stønadskonto.Builder().medMaxDager(5).medStønadskontoType(StønadskontoType.FELLESPERIODE).build());

        // Assert
        assertThat(maksDatoUttakTjeneste.beregnMaksDatoUttak(input(behandling))).contains(LocalDate.of(2019, 10, 18));
    }

    private void lagreStønadskonto(Behandling behandling, Stønadskonto stønadskonto) {
        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(behandling.getFagsak(), Dekningsgrad._100);
        var stønadskontoberegning = new Stønadskontoberegning.Builder().medRegelEvaluering(" ")
            .medRegelInput(" ")
            .medStønadskonto(stønadskonto)
            .build();
        repositoryProvider.getFagsakRelasjonRepository()
            .lagre(behandling.getFagsak(), behandling.getId(), stønadskontoberegning);
    }

    @Test
    void skal_legge_på_gjenværende_dager_på_siste_uttaksdato() {

        var uttak = new UttakResultatPerioderEntitet();
        //mandag
        var fødselsdato = LocalDate.of(2019, 2, 4);
        var fellesperiode = new UttakResultatPeriodeEntitet.Builder(fødselsdato,
            fødselsdato.plusDays(4)).medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(fellesperiode,
            new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.FRILANS).build()).medTrekkonto(
            StønadskontoType.FELLESPERIODE).medTrekkdager(new Trekkdager(5)).medArbeidsprosent(BigDecimal.ZERO).build();
        uttak.leggTilPeriode(fellesperiode);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(), true))
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett());
        scenario.medUttak(uttak);
        var behandling = scenario.lagre(repositoryProvider);

        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(behandling.getFagsak(), Dekningsgrad._100);
        var stønadskontoberegning = new Stønadskontoberegning.Builder().medRegelEvaluering(" ")
            .medRegelInput(" ")
            .medStønadskonto(
                new Stønadskonto.Builder().medMaxDager(7).medStønadskontoType(StønadskontoType.FELLESPERIODE).build())
            .medStønadskonto(
                new Stønadskonto.Builder().medMaxDager(1).medStønadskontoType(StønadskontoType.MØDREKVOTE).build())
            .build();
        repositoryProvider.getFagsakRelasjonRepository()
            .lagre(behandling.getFagsak(), behandling.getId(), stønadskontoberegning);

        // Assert
        //siste uttaksdag + 2 fellesperiode + 1 mødrekvote gjenværende
        assertThat(maksDatoUttakTjeneste.beregnMaksDatoUttak(input(behandling))).contains(LocalDate.of(2019, 2, 13));
    }

}
