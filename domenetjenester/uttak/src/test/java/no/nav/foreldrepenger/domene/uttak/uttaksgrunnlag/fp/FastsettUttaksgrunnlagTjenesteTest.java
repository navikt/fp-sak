package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder.fraEksisterende;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder.ny;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FELLESPERIODE;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.MØDREKVOTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.OriginalBehandling;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;

class FastsettUttaksgrunnlagTjenesteTest {

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryStubProvider();
    private final EndringsdatoFørstegangsbehandlingUtleder endringsdatoUtleder = new EndringsdatoFørstegangsbehandlingUtleder(repositoryProvider.getYtelsesFordelingRepository());
    private final EndringsdatoRevurderingUtleder endringsdatoRevurderingUtleder = mock(EndringsdatoRevurderingUtleder.class);
    private final FastsettUttaksgrunnlagTjeneste tjeneste = new FastsettUttaksgrunnlagTjeneste(repositoryProvider, endringsdatoUtleder,
        endringsdatoRevurderingUtleder);

    @Test
    void skal_kopiere_søknadsperioder_fra_forrige_behandling_hvis_forrige_behandling_ikke_har_uttaksresultat() {

        var førsteUttaksdato = LocalDate.now();
        var periode = ny()
                .medPeriode(førsteUttaksdato, LocalDate.now().plusDays(10))
                .medPeriodeType(FELLESPERIODE)
                .build();
        var oppgittFordelingForrigeBehandling = new OppgittFordelingEntitet(List.of(periode), true);

        var førstegangsbehandlingScenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        førstegangsbehandlingScenario.medFordeling(oppgittFordelingForrigeBehandling);
        var førstegangsbehandling = førstegangsbehandlingScenario.lagre(repositoryProvider);

        var revurdering = ScenarioFarSøkerForeldrepenger.forFødsel();
        revurdering.medOriginalBehandling(førstegangsbehandling, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_OPPTJENING);
        revurdering.medFordeling(new OppgittFordelingEntitet(Collections.emptyList(), true));

        var revurderingBehandling = revurdering.lagre(repositoryProvider);

        var familieHendelse = FamilieHendelse.forAdopsjonOmsorgsovertakelse(LocalDate.now(), List.of(), 0, null, false);
        var originalBehandling = new OriginalBehandling(førstegangsbehandling.getId(),
                new FamilieHendelser().medBekreftetHendelse(FamilieHendelse.forFødsel(null, LocalDate.now(), List.of(new Barn()), 1)));
        var fpGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(new FamilieHendelser().medSøknadHendelse(familieHendelse))
                .medOriginalBehandling(originalBehandling);
        tjeneste.fastsettUttaksgrunnlag(lagInput(revurderingBehandling, fpGrunnlag));

        var forrigeBehandlingFordeling = repositoryProvider.getYtelsesFordelingRepository()
                .hentAggregat(førstegangsbehandling.getId());
        var resultat = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(revurderingBehandling.getId());

        assertThat(resultat.getOppgittFordeling().getPerioder()).isEmpty();
        assertThat(resultat.getGjeldendeFordeling().getPerioder()).isEqualTo(
                forrigeBehandlingFordeling.getOppgittFordeling().getPerioder());
        assertThat(resultat.getOppgittFordeling().getErAnnenForelderInformert()).isEqualTo(
                forrigeBehandlingFordeling.getOppgittFordeling().getErAnnenForelderInformert());
    }

    private UttakInput lagInput(Behandling behandling, ForeldrepengerGrunnlag ytelsespesifiktGrunnlag) {
        return lagInput(behandling, ytelsespesifiktGrunnlag, false);
    }

    private UttakInput lagInput(Behandling behandling, ForeldrepengerGrunnlag ytelsespesifiktGrunnlag, boolean sammenhengendeUttak) {
        var stp = Skjæringstidspunkt.builder()
                .medUtledetSkjæringstidspunkt(LocalDate.now())
                .medKreverSammenhengendeUttak(sammenhengendeUttak)
                .build();
        var ref = BehandlingReferanse.fra(behandling);
        return new UttakInput(ref, stp, InntektArbeidYtelseGrunnlagBuilder.nytt().build(), ytelsespesifiktGrunnlag);
    }

    @Test
    void skal_lagre_opprinnelig_endringsdato() {

        var førsteUttaksdato = LocalDate.now();
        var periode = ny()
                .medPeriode(førsteUttaksdato, LocalDate.now().plusDays(10))
                .medPeriodeType(FELLESPERIODE)
                .build();
        var fordeling = new OppgittFordelingEntitet(List.of(periode), true);

        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(fordeling)
            .lagre(repositoryProvider);

        var familieHendelse = FamilieHendelse.forFødsel(null, førsteUttaksdato, List.of(), 0);
        var familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);
        tjeneste.fastsettUttaksgrunnlag(lagInput(behandling, new ForeldrepengerGrunnlag().medFamilieHendelser(familieHendelser)));

        var resultat = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());

        assertThat(resultat.getGjeldendeEndringsdato()).isEqualTo(førsteUttaksdato);
        assertThat(resultat.getAvklarteDatoer().get().getOpprinneligEndringsdato()).isEqualTo(førsteUttaksdato);
    }

    @Test
    void skal_beholde_overstyrt_dersom_uendret_justering() {

        var førsteUttaksdato = LocalDate.now();
        var periode = ny()
            .medPeriode(førsteUttaksdato, LocalDate.now().plusDays(10))
            .medPeriodeType(FELLESPERIODE)
            .build();
        var fordeling = new OppgittFordelingEntitet(List.of(periode), true);
        var justertFordeling = new OppgittFordelingEntitet(List.of(periode), true);
        var overstyrtFordeling = new OppgittFordelingEntitet(List.of(fraEksisterende(periode)
            .medPeriodeType(MØDREKVOTE).build()), true);

        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder()
            .medOpprinneligEndringsdato(førsteUttaksdato)
            .build();
        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(fordeling)
            .medJustertFordeling(justertFordeling)
            .medAvklarteUttakDatoer(avklarteUttakDatoer)
            .medOverstyrtFordeling(overstyrtFordeling)
            .lagre(repositoryProvider);

        var familieHendelse = FamilieHendelse.forFødsel(null, førsteUttaksdato, List.of(), 0);
        var familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);
        tjeneste.fastsettUttaksgrunnlag(lagInput(behandling, new ForeldrepengerGrunnlag().medFamilieHendelser(familieHendelser)));

        var resultat = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());

        assertThat(resultat.getJustertFordeling().orElse(null)).isEqualTo(justertFordeling);
        assertThat(resultat.getOverstyrtFordeling()).isPresent();
    }

    @Test
    void skal_fjerne_overstyrt_dersom_endret_justering() {

        var førsteUttaksdato = LocalDate.now();
        var periode = ny()
            .medPeriode(førsteUttaksdato, LocalDate.now().plusDays(10))
            .medPeriodeType(FELLESPERIODE)
            .build();
        var periodeMK = ny()
            .medPeriode(førsteUttaksdato, LocalDate.now().plusDays(10))
            .medPeriodeType(MØDREKVOTE)
            .build();
        var fordeling = new OppgittFordelingEntitet(List.of(periode), true);
        var justertFordeling = new OppgittFordelingEntitet(List.of(periodeMK), true);
        var overstyrtFordeling = new OppgittFordelingEntitet(List.of(periodeMK), true);

        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder()
            .medOpprinneligEndringsdato(LocalDate.of(2020, 10, 10))
            .build();
        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(fordeling)
            .medJustertFordeling(justertFordeling)
            .medAvklarteUttakDatoer(avklarteUttakDatoer)
            .medOverstyrtFordeling(overstyrtFordeling)
            .lagre(repositoryProvider);

        var familieHendelse = FamilieHendelse.forFødsel(null, førsteUttaksdato, List.of(), 0);
        var familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);
        tjeneste.fastsettUttaksgrunnlag(lagInput(behandling, new ForeldrepengerGrunnlag().medFamilieHendelser(familieHendelser)));

        var resultat = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());

        assertThat(resultat.getJustertFordeling().orElse(null)).isEqualTo(fordeling);
        assertThat(resultat.getOverstyrtFordeling()).isEmpty();
    }

    @Test
    void skal_ikke_forskyve_søknadsperioder_hvis_både_termin_og_fødsel_er_oppgitt_i_søknad() {
        var søknadFom = LocalDate.of(2019, 7, 31);
        var periode = ny()
                .medPeriode(søknadFom, søknadFom.plusDays(10))
                .medPeriodeType(FELLESPERIODE)
                .build();
        var fordeling = new OppgittFordelingEntitet(List.of(periode), true);

        var førstegangsbehandlingScenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        førstegangsbehandlingScenario.medFordeling(fordeling);

        var behandling = førstegangsbehandlingScenario.lagre(repositoryProvider);

        var søknadFamilieHendelse = FamilieHendelse.forFødsel(søknadFom, søknadFom.minusWeeks(2), List.of(), 0);
        var fpGrunnlag = new ForeldrepengerGrunnlag()
            .medFamilieHendelser(new FamilieHendelser().medSøknadHendelse(søknadFamilieHendelse));
        tjeneste.fastsettUttaksgrunnlag(lagInput(behandling, fpGrunnlag));

        var resultat = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());

        assertThat(resultat.getGjeldendeFordeling().getPerioder()).isEqualTo(fordeling.getPerioder());
    }

    @Test
    void skal_ikke_forskyve_søknadsperioder_hvis_ny_førstegangssøknad_med_endret_termindato() {
        //søkte uttaket her vil gjøre at fødselsjusteringen gir esxception på overlapp
        var termindato1 = LocalDate.of(2024, 5, 4);
        var termindato2 = LocalDate.of(2024, 4, 29);

        var fordeling1 = new OppgittFordelingEntitet(List.of(
            ny().medPeriode(LocalDate.of(2024, 4, 15), LocalDate.of(2024, 5, 3)).medPeriodeType(FORELDREPENGER_FØR_FØDSEL).build(),
            ny().medPeriode(LocalDate.of(2024, 5, 6), LocalDate.of(2024, 9, 13)).medPeriodeType(MØDREKVOTE).build(),
            ny().medPeriode(LocalDate.of(2024, 9, 16), LocalDate.of(2025, 1, 17)).medPeriodeType(FELLESPERIODE).build()
        ), true);
        var uttaksperiodeFørstegang = new UttakResultatPeriodeEntitet.Builder(termindato1, termindato1.plusWeeks(10))
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .build();
        var førstegangsbehandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(fordeling1)
            .medUttak(new UttakResultatPerioderEntitet().leggTilPeriode(uttaksperiodeFørstegang))
            .lagre(repositoryProvider);
        var fordeling2 = new OppgittFordelingEntitet(List.of(
            ny().medPeriode(LocalDate.of(2024, 4, 15), LocalDate.of(2024, 4, 26)).medPeriodeType(FORELDREPENGER_FØR_FØDSEL).build(),
            ny().medPeriode(LocalDate.of(2024, 4, 29), LocalDate.of(2024, 6, 11)).medPeriodeType(MØDREKVOTE).build(),
            ny().medPeriode(LocalDate.of(2024, 7, 12), LocalDate.of(2024, 11, 14)).medPeriodeType(FELLESPERIODE).build(),
            ny().medPeriode(LocalDate.of(2024, 11, 15), LocalDate.of(2025, 2, 11)).medPeriodeType(MØDREKVOTE).build()
        ), true);
        var revurdering = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOriginalBehandling(førstegangsbehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
            .medFordeling(fordeling2)
            .lagre(repositoryProvider);

        var familieHendelseRevurdering = FamilieHendelse.forFødsel(termindato2, null, List.of(), 1);
        var familieHendelseFørstegangsbehandling = FamilieHendelse.forFødsel(termindato1, null, List.of(), 1);
        var fpGrunnlag = new ForeldrepengerGrunnlag().medOriginalBehandling(new OriginalBehandling(førstegangsbehandling.getId(),
                new FamilieHendelser().medSøknadHendelse(familieHendelseFørstegangsbehandling)))
            .medFamilieHendelser(new FamilieHendelser().medSøknadHendelse(familieHendelseRevurdering).medBekreftetHendelse(familieHendelseRevurdering));
        var endringsdatoRevurderingUtleder = mock(EndringsdatoRevurderingUtleder.class);
        when(endringsdatoRevurderingUtleder.utledEndringsdato(any())).thenReturn(LocalDate.of(2024, 4, 15));
        var tjeneste = new FastsettUttaksgrunnlagTjeneste(repositoryProvider, endringsdatoUtleder, endringsdatoRevurderingUtleder);
        tjeneste.fastsettUttaksgrunnlag(lagInput(revurdering, fpGrunnlag));

        var resultat = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(revurdering.getId());

        assertThat(resultat.getGjeldendeFordeling().getPerioder().get(0).getFom()).isEqualTo(LocalDate.of(2024, 4, 15));
    }

    @Test
    void sammenhengende_uttak_skal_fjerne_oppholdsperioder_på_slutten_av_fordelingen() {
        var søknadFom = LocalDate.of(2019, 7, 31);
        var periode1 = ny()
                .medPeriode(søknadFom, søknadFom.plusDays(10))
                .medPeriodeType(FELLESPERIODE)
                .build();
        var opphold1 = ny()
                .medÅrsak(OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER)
                .medPeriode(periode1.getTom().plusDays(1), periode1.getTom().plusDays(10))
                .build();
        var periode2 = ny()
                .medPeriode(opphold1.getTom().plusDays(1), opphold1.getTom().plusDays(10))
                .medPeriodeType(FELLESPERIODE)
                .build();
        var opphold2 = ny()
                .medÅrsak(OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER)
                .medPeriode(periode2.getTom().plusDays(1), periode2.getTom().plusDays(10))
                .build();
        var opphold3 = ny()
                .medÅrsak(OppholdÅrsak.KVOTE_FELLESPERIODE_ANNEN_FORELDER)
                .medPeriode(opphold2.getTom().plusDays(1), opphold2.getTom().plusDays(10))
                .build();
        var fordeling = new OppgittFordelingEntitet(List.of(periode1, opphold2, opphold3, opphold1, periode2), true);

        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel().medFordeling(fordeling);

        var behandling = scenario.lagre(repositoryProvider);

        var søknadFamilieHendelse = FamilieHendelse.forFødsel(søknadFom, null, List.of(), 0);
        var fpGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(
                new FamilieHendelser().medSøknadHendelse(søknadFamilieHendelse));
        tjeneste.fastsettUttaksgrunnlag(lagInput(behandling, fpGrunnlag, true));

        var resultat = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());

        var oppgittePerioder = resultat.getGjeldendeFordeling().getPerioder();
        assertThat(oppgittePerioder).hasSize(3);
        assertThat(oppgittePerioder.get(0).getFom()).isEqualTo(periode1.getFom());
        assertThat(oppgittePerioder.get(1).getFom()).isEqualTo(opphold1.getFom());
        assertThat(oppgittePerioder.get(2).getFom()).isEqualTo(periode2.getFom());
    }

    @Test
    void adopsjon_uten_justering_men_oppdatere_mottatt_dato() {
        var søknadFom = LocalDate.of(2019, 7, 31);
        var søknadMottatt = søknadFom.plusMonths(5);
        var søknadsperiode = ny()
                .medPeriodeType(MØDREKVOTE)
                .medPeriode(søknadFom.plusDays(1), søknadFom.plusDays(10))
                .medMottattDato(søknadMottatt)
                .medTidligstMottattDato(søknadMottatt)
                .build();
        var fordeling = new OppgittFordelingEntitet(List.of(søknadsperiode), true);

        var scenario = ScenarioMorSøkerForeldrepenger.forAdopsjon().medFordeling(fordeling);

        var behandling = scenario.lagre(repositoryProvider);

        repositoryProvider.getUttaksperiodegrenseRepository().lagre(behandling.getId(), new Uttaksperiodegrense(søknadFom));

        var søknadFamilieHendelse = FamilieHendelse.forAdopsjonOmsorgsovertakelse(søknadFom, List.of(new Barn(null)), 1,
                søknadFom.minusWeeks(2), false);
        var fpGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(
                new FamilieHendelser().medSøknadHendelse(søknadFamilieHendelse));
        tjeneste.fastsettUttaksgrunnlag(lagInput(behandling, fpGrunnlag));

        var resultat = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());

        var oppgittePerioder = resultat.getGjeldendeFordeling().getPerioder();
        assertThat(oppgittePerioder).hasSize(1);
        assertThat(oppgittePerioder.get(0).getFom()).isEqualTo(søknadsperiode.getFom());
        assertThat(oppgittePerioder.get(0).getTom()).isEqualTo(søknadsperiode.getTom());
        assertThat(oppgittePerioder.get(0).getMottattDato()).isEqualTo(søknadMottatt);
        assertThat(oppgittePerioder.get(0).getTidligstMottattDato().orElseThrow()).isEqualTo(søknadFom);
    }

    @Test
    void skal_fjerne_oppholdsperioder() {
        var søknadFom = LocalDate.of(2022, 7, 31);
        var periode1 = ny()
                .medPeriode(søknadFom, søknadFom.plusDays(10))
                .medPeriodeType(FELLESPERIODE)
                .build();
        var opphold1 = ny()
                .medÅrsak(OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER)
                .medPeriode(periode1.getTom().plusDays(1), periode1.getTom().plusDays(10))
                .build();
        var periode2 = ny()
                .medPeriode(opphold1.getTom().plusDays(1), opphold1.getTom().plusDays(10))
                .medPeriodeType(FELLESPERIODE)
                .build();
        var opphold2 = ny()
                .medÅrsak(OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER)
                .medPeriode(periode2.getTom().plusDays(1), periode2.getTom().plusDays(10))
                .build();
        var fordeling = new OppgittFordelingEntitet(List.of(periode1, opphold2, opphold1, periode2), true);

        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel().medFordeling(fordeling);

        var behandling = scenario.lagre(repositoryProvider);

        var søknadFamilieHendelse = FamilieHendelse.forFødsel(søknadFom, null, List.of(), 0);
        var fpGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(
                new FamilieHendelser().medSøknadHendelse(søknadFamilieHendelse));
        tjeneste.fastsettUttaksgrunnlag(lagInput(behandling, fpGrunnlag));

        var resultat = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());

        var oppgittePerioder = resultat.getGjeldendeFordeling().getPerioder();
        assertThat(oppgittePerioder).hasSize(2);
        assertThat(oppgittePerioder.get(0).getFom()).isEqualTo(periode1.getFom());
        assertThat(oppgittePerioder.get(1).getFom()).isEqualTo(periode2.getFom());
    }

    @Test
    void tillater_overlappende_perioder_men_skal_ikke_justeres_bare_slås_sammen_hvis_mulig() {
        var fødselsdato = LocalDate.of(2018, 1, 1);
        var mødrekvote = ny()
            .medPeriode(fødselsdato, fødselsdato.plusDays(10))
            .medPeriodeType(MØDREKVOTE)
            .build();
        // overlapper med mødrekvoten
        var fellesperiode = ny()
            .medPeriode(fødselsdato.plusDays(5), fødselsdato.plusDays(20))
            .medPeriodeType(FELLESPERIODE)
            .build();
        var fordeling = new OppgittFordelingEntitet(List.of(mødrekvote, fellesperiode), true);

        var førstegangsbehandlingScenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        førstegangsbehandlingScenario.medFordeling(fordeling);

        var behandling = førstegangsbehandlingScenario.lagre(repositoryProvider);

        var søknadFamilieHendelse = FamilieHendelse.forFødsel(fødselsdato, fødselsdato.minusWeeks(2), List.of(), 0);
        var fpGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(
            new FamilieHendelser().medSøknadHendelse(søknadFamilieHendelse));
        tjeneste.fastsettUttaksgrunnlag(lagInput(behandling, fpGrunnlag));

        var resultat = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());

        assertThat(resultat.getGjeldendeFordeling().getPerioder()).isEqualTo(fordeling.getPerioder());
    }

    @Test
    void fjerner_perioder_før_endringsdato_i_revuderinger() {
        var fødselsdato = LocalDate.of(2024, 3, 6);
        var mødrekvote1 = ny()
            .medPeriode(fødselsdato, fødselsdato.plusWeeks(3).minusDays(1))
            .medPeriodeType(MØDREKVOTE)
            .build();
        var mødrekvote2 = ny()
            .medPeriode(fødselsdato.plusWeeks(3), fødselsdato.plusWeeks(10))
            .medPeriodeType(MØDREKVOTE)
            .build();
        var fordeling = new OppgittFordelingEntitet(List.of(mødrekvote1, mødrekvote2), true);
        var uttaksperiode = new UttakResultatPeriodeEntitet.Builder(mødrekvote1.getFom(), mødrekvote2.getTom())
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .build();
        var uttak = new UttakResultatPerioderEntitet().leggTilPeriode(uttaksperiode);
        var førstegangsbehandlingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medUttak(uttak)
            .medFordeling(fordeling);
        var behandling = førstegangsbehandlingScenario.lagre(repositoryProvider);

        var revurdering = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOriginalBehandling(behandling, BehandlingÅrsakType.RE_VEDTAK_PLEIEPENGER)
            .medFordeling(fordeling)
            .lagre(repositoryProvider);

        var søknadFamilieHendelse = FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 0);
        var fpGrunnlag = new ForeldrepengerGrunnlag()
            .medFamilieHendelser(new FamilieHendelser().medSøknadHendelse(søknadFamilieHendelse));

        var input = lagInput(revurdering, fpGrunnlag);
        var endringsdato = fødselsdato.plusWeeks(8);
        when(endringsdatoRevurderingUtleder.utledEndringsdato(input)).thenReturn(endringsdato);

        tjeneste.fastsettUttaksgrunnlag(input);

        var resultat = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(revurdering.getId());

        assertThat(resultat.getGjeldendeFordeling().getPerioder()).hasSize(1);
        assertThat(resultat.getGjeldendeFordeling().getPerioder().getFirst().getFom()).isEqualTo(endringsdato);
        assertThat(resultat.getGjeldendeFordeling().getPerioder().getFirst().getTom()).isEqualTo(mødrekvote2.getTom());
    }
}
