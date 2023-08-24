package no.nav.foreldrepenger.domene.uttak;


import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittDekningsgradEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.*;
import no.nav.foreldrepenger.domene.uttak.input.*;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TapteDagerFpffTjenesteTest {

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryStubProvider();

    @Test
    void skal_ikke_ha_tapte_dager_ved_søknad_på_termin() {
        var søktFpff = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            //15 virkedager
            .medPeriode(LocalDate.of(2019, 12, 2), LocalDate.of(2019, 12, 20))
            .build();
        var mødrekvote = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(LocalDate.of(2019, 12, 23), LocalDate.of(2020, 12, 23))
            .build();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(søktFpff, mødrekvote), true))
            .medOppgittDekningsgrad(OppgittDekningsgradEntitet.bruk100());
        var behandling = scenario.lagre(repositoryProvider);
        opprettFagsakRelasjon(behandling, 15);

        var termindato = søktFpff.getTom().plusDays(1);
        var familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse(termindato, null));
        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, new ForeldrepengerGrunnlag().medFamilieHendelser(familieHendelser));
        var resultat = tjeneste().antallTapteDagerFpff(input);

        assertThat(resultat).isZero();
    }

    @Test
    void skal_ikke_regne_tapte_dager_hvis_søknad_på_fødsel_med_termindato() {
        var søktFpff = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            //15 virkedager
            .medPeriode(LocalDate.of(2019, 12, 2), LocalDate.of(2019, 12, 20))
            .build();
        var mødrekvote = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(LocalDate.of(2019, 12, 23), LocalDate.of(2020, 12, 23))
            .build();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(søktFpff, mødrekvote), true))
            .medOppgittDekningsgrad(OppgittDekningsgradEntitet.bruk100());
        var behandling = scenario.lagre(repositoryProvider);
        opprettFagsakRelasjon(behandling, 15);

        var termindato = søktFpff.getTom().plusDays(5);
        var fødselsdato = søktFpff.getTom().plusDays(1);
        var familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse(termindato, fødselsdato));
        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, new ForeldrepengerGrunnlag().medFamilieHendelser(familieHendelser));
        var resultat = tjeneste().antallTapteDagerFpff(input);

        assertThat(resultat).isZero();
    }

    @Test
    void skal_regne_tapte_dager_hvis_søknad_på_termindato_med_fødselshendelse() {
        var søktFpff = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            //15 virkedager
            .medPeriode(LocalDate.of(2019, 12, 2), LocalDate.of(2019, 12, 20))
            .build();
        var mødrekvote = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(LocalDate.of(2019, 12, 23), LocalDate.of(2020, 12, 23))
            .build();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(søktFpff, mødrekvote), true))
            .medOppgittDekningsgrad(OppgittDekningsgradEntitet.bruk100());
        var behandling = scenario.lagre(repositoryProvider);
        opprettFagsakRelasjon(behandling, 15);

        var termindato = søktFpff.getTom().plusDays(1);
        var fødselsdato = termindato.minusDays(4);
        var familieHendelser = new FamilieHendelser()
            .medSøknadHendelse(familieHendelse(termindato, null))
            .medBekreftetHendelse(familieHendelse(termindato, fødselsdato));
        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, new ForeldrepengerGrunnlag().medFamilieHendelser(familieHendelser));
        var resultat = tjeneste().antallTapteDagerFpff(input);

        //Føder 4 dager før termin
        assertThat(resultat).isEqualTo(4);
    }

    @Test
    void skal_ikke_kunne_tape_flere_dager_enn_maksdager() {
        var søktFpff = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            //15 virkedager
            .medPeriode(LocalDate.of(2019, 12, 2), LocalDate.of(2019, 12, 20))
            .build();
        var mødrekvote = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(LocalDate.of(2019, 12, 23), LocalDate.of(2020, 12, 23))
            .build();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(søktFpff, mødrekvote), true))
            .medOppgittDekningsgrad(OppgittDekningsgradEntitet.bruk100());
        var behandling = scenario.lagre(repositoryProvider);
        var maksdager = 15;
        opprettFagsakRelasjon(behandling, maksdager);

        var termindato = søktFpff.getTom().plusDays(1);
        var fødselsdato = termindato.minusDays(25);
        var familieHendelser = new FamilieHendelser()
            .medSøknadHendelse(familieHendelse(termindato, null))
            .medBekreftetHendelse(familieHendelse(termindato, fødselsdato));
        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, new ForeldrepengerGrunnlag().medFamilieHendelser(familieHendelser));
        var resultat = tjeneste().antallTapteDagerFpff(input);

        assertThat(resultat).isEqualTo(maksdager);
    }

    @Test
    void ingen_tapte_dager_ut_ifra_gjeldendefødselshendelse() {
        var søktFpff = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            //15 virkedager
            .medPeriode(LocalDate.of(2022, 5, 30), LocalDate.of(2022, 6, 19))
            .build();
        var mødrekvote = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(LocalDate.of(2022, 6, 20), LocalDate.of(2022, 7, 30))
            .build();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(søktFpff, mødrekvote), true))
            .medOppgittDekningsgrad(OppgittDekningsgradEntitet.bruk100());
        var behandling = scenario.lagre(repositoryProvider);

        opprettFagsakRelasjon(behandling, 15);

        var termindato = LocalDate.of(2022, 7, 13);
        var temindateBekreftetHendelse = LocalDate.of(2022, 6, 20);
        var fødselsdato = LocalDate.of(2022, 6, 20);
        var familieHendelser = new FamilieHendelser()
            .medSøknadHendelse(familieHendelse(termindato, null))
            .medBekreftetHendelse(familieHendelse(temindateBekreftetHendelse, fødselsdato));
        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, new ForeldrepengerGrunnlag().medFamilieHendelser(familieHendelser));
        var resultat = tjeneste().antallTapteDagerFpff(input);

        assertThat(resultat).isZero();
    }

    @Test
    void tapte_dager_ut_ifra_gjeldendefødselshendelse() {
        var søktFpff = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            //15 virkedager
            .medPeriode(LocalDate.of(2022, 5, 30), LocalDate.of(2022, 6, 19))
            .build();
        var mødrekvote = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(LocalDate.of(2022, 6, 20), LocalDate.of(2022, 7, 30))
            .build();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(søktFpff, mødrekvote), true))
            .medOppgittDekningsgrad(OppgittDekningsgradEntitet.bruk100());
        var behandling = scenario.lagre(repositoryProvider);

        opprettFagsakRelasjon(behandling, 15);

        var termindato = LocalDate.of(2022, 7, 13);
        var temindateBekreftetHendelse = LocalDate.of(2022, 6, 20);
        var fødselsdato = LocalDate.of(2022, 6, 15);
        var familieHendelser = new FamilieHendelser()
            .medSøknadHendelse(familieHendelse(termindato, null))
            .medBekreftetHendelse(familieHendelse(temindateBekreftetHendelse, fødselsdato));
        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, new ForeldrepengerGrunnlag().medFamilieHendelser(familieHendelser));
        var resultat = tjeneste().antallTapteDagerFpff(input);

        assertThat(resultat).isEqualTo(3);
    }

    private void opprettFagsakRelasjon(Behandling behandling, int maksdagerFpff) {
        var stønadskontoberegning = new Stønadskontoberegning.Builder()
            .medStønadskonto(new Stønadskonto.Builder()
                .medMaxDager(maksdagerFpff)
                .medStønadskontoType(StønadskontoType.FORELDREPENGER_FØR_FØDSEL)
                .build())
            .medRegelEvaluering(" ")
            .medRegelInput(" ")
            .build();
        repositoryProvider.getFagsakRelasjonRepository().lagre(behandling.getFagsak(), behandling.getId(), stønadskontoberegning);
    }

    @Test
    void skal_regne_tapte_dager_hvis_søknad_på_termindato_og_revurdering_med_fødselshendelse() {
        var søktFpff = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            //15 virkedager
            .medPeriode(LocalDate.of(2019, 12, 2), LocalDate.of(2019, 12, 13))
            .build();
        var mødrekvote = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(LocalDate.of(2019, 12, 14), LocalDate.of(2020, 12, 23))
            .build();
        var førstegangsScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(søktFpff, mødrekvote), true))
            .medOppgittDekningsgrad(OppgittDekningsgradEntitet.bruk100());
        var førstegangsBehandling = førstegangsScenario.lagre(repositoryProvider);
        opprettFagsakRelasjon(førstegangsBehandling, 15);

        var uttakPeriode = new UttakResultatPeriodeEntitet.Builder(søktFpff.getFom(), søktFpff.getTom())
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .build();
        var aktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(uttakPeriode, new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.FRILANS).build())
            .medTrekkonto(StønadskontoType.FORELDREPENGER_FØR_FØDSEL)
            .medArbeidsprosent(BigDecimal.ZERO)
            .medTrekkdager(new Trekkdager(10))
            .build();
        uttakPeriode.leggTilAktivitet(aktivitet);

        var uttak = new UttakResultatPerioderEntitet();
        uttak.leggTilPeriode(uttakPeriode);
        repositoryProvider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(førstegangsBehandling.getId(), uttak);

        var revurderingBehandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOriginalBehandling(førstegangsBehandling, BehandlingÅrsakType.RE_HENDELSE_FØDSEL)
            .medFordeling(new OppgittFordelingEntitet(List.of(), true))
            .lagre(repositoryProvider);

        var termindato = mødrekvote.getFom();
        //2 virkedager
        var fødselsdato = termindato.minusDays(2);
        var familieHendelser = new FamilieHendelser()
            .medSøknadHendelse(familieHendelse(termindato, null))
            .medBekreftetHendelse(familieHendelse(termindato, fødselsdato));
        var fpGrunnlag = new ForeldrepengerGrunnlag()
            .medFamilieHendelser(familieHendelser)
            .medOriginalBehandling(new OriginalBehandling(førstegangsBehandling.getId(), null));
        var input = new UttakInput(BehandlingReferanse.fra(revurderingBehandling), null, fpGrunnlag);
        var resultat = tjeneste().antallTapteDagerFpff(input);

        //Føder 2 virkedager før termin
        assertThat(resultat).isEqualTo(2);
    }

    private FamilieHendelse familieHendelse(LocalDate termindato, LocalDate fødselsdato) {
        return FamilieHendelse.forFødsel(termindato, fødselsdato, List.of(new Barn()), 1);
    }

    private TapteDagerFpffTjeneste tjeneste() {
        return new TapteDagerFpffTjeneste(repositoryProvider, new YtelseFordelingTjeneste(repositoryProvider.getYtelsesFordelingRepository()));
    }
}
