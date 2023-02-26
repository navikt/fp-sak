package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import static org.assertj.core.api.Assertions.assertThat;
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
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
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

public class FastsettUttaksgrunnlagTjenesteTest {

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryStubProvider();
    private final EndringsdatoFørstegangsbehandlingUtleder endringsdatoUtleder = mock(EndringsdatoFørstegangsbehandlingUtleder.class);
    private final FastsettUttaksgrunnlagTjeneste tjeneste = new FastsettUttaksgrunnlagTjeneste(repositoryProvider, endringsdatoUtleder,
            mock(EndringsdatoRevurderingUtlederImpl.class));

    @Test
    public void skal_kopiere_søknadsperioder_fra_forrige_behandling_hvis_forrige_behandling_ikke_har_uttaksresultat() {

        var førsteUttaksdato = LocalDate.now();
        var periode = OppgittPeriodeBuilder.ny()
                .medPeriode(førsteUttaksdato, LocalDate.now().plusDays(10))
                .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
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
        var ref = BehandlingReferanse.fra(behandling, stp);
        return new UttakInput(ref, InntektArbeidYtelseGrunnlagBuilder.nytt().build(), ytelsespesifiktGrunnlag);
    }

    @Test
    public void skal_lagre_opprinnelig_endringsdato() {

        var førsteUttaksdato = LocalDate.now();
        var periode = OppgittPeriodeBuilder.ny()
                .medPeriode(førsteUttaksdato, LocalDate.now().plusDays(10))
                .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
                .build();
        var fordeling = new OppgittFordelingEntitet(List.of(periode), true);

        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        scenario.medFordeling(fordeling);
        var behandling = scenario.lagre(repositoryProvider);
        var endringsdato = LocalDate.of(2020, 10, 10);
        when(endringsdatoUtleder.utledEndringsdato(behandling.getId(), List.of(periode))).thenReturn(endringsdato);

        var familieHendelse = FamilieHendelse.forFødsel(null, førsteUttaksdato, List.of(), 0);
        var familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);
        tjeneste.fastsettUttaksgrunnlag(lagInput(behandling, new ForeldrepengerGrunnlag().medFamilieHendelser(familieHendelser)));

        var resultat = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());

        assertThat(resultat.getGjeldendeEndringsdato()).isEqualTo(endringsdato);
        assertThat(resultat.getAvklarteDatoer().get().getOpprinneligEndringsdato()).isEqualTo(endringsdato);
    }

    @Test
    public void skal_beholde_overstyrt_dersom_uendret_justering() {

        var førsteUttaksdato = LocalDate.now();
        var periode = OppgittPeriodeBuilder.ny()
            .medPeriode(førsteUttaksdato, LocalDate.now().plusDays(10))
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .build();
        var fordeling = new OppgittFordelingEntitet(List.of(periode), true);
        var justertFordeling = new OppgittFordelingEntitet(List.of(periode), true);
        var overstyrtFordeling = new OppgittFordelingEntitet(List.of(OppgittPeriodeBuilder.fraEksisterende(periode)
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE).build()), true);

        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        scenario.medFordeling(fordeling).medJustertFordeling(justertFordeling).medOverstyrtFordeling(overstyrtFordeling);
        var behandling = scenario.lagre(repositoryProvider);

        var endringsdato = LocalDate.of(2020, 10, 10);
        when(endringsdatoUtleder.utledEndringsdato(behandling.getId(), List.of(periode))).thenReturn(endringsdato);

        var familieHendelse = FamilieHendelse.forFødsel(null, førsteUttaksdato, List.of(), 0);
        var familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);
        tjeneste.fastsettUttaksgrunnlag(lagInput(behandling, new ForeldrepengerGrunnlag().medFamilieHendelser(familieHendelser)));

        var resultat = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());

        assertThat(resultat.getJustertFordeling().orElse(null)).isEqualTo(justertFordeling);
        assertThat(resultat.getOverstyrtFordeling()).isPresent();
    }

    @Test
    public void skal_fjerne_overstyrt_dersom_endret_justering() {

        var førsteUttaksdato = LocalDate.now();
        var periode = OppgittPeriodeBuilder.ny()
            .medPeriode(førsteUttaksdato, LocalDate.now().plusDays(10))
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .build();
        var periodeMK = OppgittPeriodeBuilder.ny()
            .medPeriode(førsteUttaksdato, LocalDate.now().plusDays(10))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();
        var fordeling = new OppgittFordelingEntitet(List.of(periode), true);
        var justertFordeling = new OppgittFordelingEntitet(List.of(periodeMK), true);
        var overstyrtFordeling = new OppgittFordelingEntitet(List.of(periodeMK), true);

        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        scenario.medFordeling(fordeling).medJustertFordeling(justertFordeling).medOverstyrtFordeling(overstyrtFordeling);
        var behandling = scenario.lagre(repositoryProvider);

        var endringsdato = LocalDate.of(2020, 10, 10);
        when(endringsdatoUtleder.utledEndringsdato(behandling.getId(), List.of(periode))).thenReturn(endringsdato);

        var familieHendelse = FamilieHendelse.forFødsel(null, førsteUttaksdato, List.of(), 0);
        var familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);
        tjeneste.fastsettUttaksgrunnlag(lagInput(behandling, new ForeldrepengerGrunnlag().medFamilieHendelser(familieHendelser)));

        var resultat = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());

        assertThat(resultat.getJustertFordeling().orElse(null)).isEqualTo(fordeling);
        assertThat(resultat.getOverstyrtFordeling()).isEmpty();
    }

    @Test
    public void skal_ikke_forskyve_søknadsperioder_hvis_både_termin_og_fødsel_er_oppgitt_i_søknad() {
        var søknadFom = LocalDate.of(2019, 7, 31);
        var periode = OppgittPeriodeBuilder.ny()
                .medPeriode(søknadFom, søknadFom.plusDays(10))
                .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
                .build();
        var fordeling = new OppgittFordelingEntitet(List.of(periode), true);

        var førstegangsbehandlingScenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        førstegangsbehandlingScenario.medFordeling(fordeling);

        var behandling = førstegangsbehandlingScenario.lagre(repositoryProvider);

        var søknadFamilieHendelse = FamilieHendelse.forFødsel(søknadFom, søknadFom.minusWeeks(2), List.of(), 0);
        var fpGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(
                new FamilieHendelser().medSøknadHendelse(søknadFamilieHendelse));
        tjeneste.fastsettUttaksgrunnlag(lagInput(behandling, fpGrunnlag));

        var resultat = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());

        assertThat(resultat.getGjeldendeFordeling().getPerioder().get(0).getFom()).isEqualTo(søknadFom);
    }

    @Test
    public void sammenhengende_uttak_skal_fjerne_oppholdsperioder_på_slutten_av_fordelingen() {
        var søknadFom = LocalDate.of(2019, 7, 31);
        var periode1 = OppgittPeriodeBuilder.ny()
                .medPeriode(søknadFom, søknadFom.plusDays(10))
                .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
                .build();
        var opphold1 = OppgittPeriodeBuilder.ny()
                .medÅrsak(OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER)
                .medPeriode(periode1.getTom().plusDays(1), periode1.getTom().plusDays(10))
                .build();
        var periode2 = OppgittPeriodeBuilder.ny()
                .medPeriode(opphold1.getTom().plusDays(1), opphold1.getTom().plusDays(10))
                .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
                .build();
        var opphold2 = OppgittPeriodeBuilder.ny()
                .medÅrsak(OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER)
                .medPeriode(periode2.getTom().plusDays(1), periode2.getTom().plusDays(10))
                .build();
        var opphold3 = OppgittPeriodeBuilder.ny()
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
    public void sammenhengende_uttak_skal_ikke_fjerne_oppholdsperioder_på_slutten_hvis_fordelingen_bare_består_av_oppholdsperioder() {
        var søknadFom = LocalDate.of(2019, 7, 31);
        var opphold1 = OppgittPeriodeBuilder.ny()
                .medÅrsak(OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER)
                .medPeriode(søknadFom.plusDays(1), søknadFom.plusDays(10))
                .build();
        var opphold2 = OppgittPeriodeBuilder.ny()
                .medÅrsak(OppholdÅrsak.KVOTE_FELLESPERIODE_ANNEN_FORELDER)
                .medPeriode(opphold1.getTom().plusDays(1), opphold1.getTom().plusDays(10))
                .build();
        var fordeling = new OppgittFordelingEntitet(List.of(opphold1, opphold2), true);

        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel().medFordeling(fordeling);

        var behandling = scenario.lagre(repositoryProvider);

        var søknadFamilieHendelse = FamilieHendelse.forFødsel(søknadFom, null, List.of(), 0);
        var fpGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(
                new FamilieHendelser().medSøknadHendelse(søknadFamilieHendelse));
        tjeneste.fastsettUttaksgrunnlag(lagInput(behandling, fpGrunnlag, true));

        var resultat = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());

        var oppgittePerioder = resultat.getGjeldendeFordeling().getPerioder();
        assertThat(oppgittePerioder).hasSize(2);
    }

    @Test
    public void adopsjon_uten_justering_men_oppdatere_mottatt_dato() {
        var søknadFom = LocalDate.of(2019, 7, 31);
        var søknadMottatt = søknadFom.plusMonths(5);
        var søknadsperiode = OppgittPeriodeBuilder.ny()
                .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
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
    public void skal_fjerne_oppholdsperioder() {
        var søknadFom = LocalDate.of(2019, 7, 31);
        var periode1 = OppgittPeriodeBuilder.ny()
                .medPeriode(søknadFom, søknadFom.plusDays(10))
                .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
                .build();
        var opphold1 = OppgittPeriodeBuilder.ny()
                .medÅrsak(OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER)
                .medPeriode(periode1.getTom().plusDays(1), periode1.getTom().plusDays(10))
                .build();
        var periode2 = OppgittPeriodeBuilder.ny()
                .medPeriode(opphold1.getTom().plusDays(1), opphold1.getTom().plusDays(10))
                .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
                .build();
        var opphold2 = OppgittPeriodeBuilder.ny()
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
}
