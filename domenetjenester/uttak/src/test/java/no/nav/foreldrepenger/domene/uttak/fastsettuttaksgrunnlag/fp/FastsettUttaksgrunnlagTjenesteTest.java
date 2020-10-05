package no.nav.foreldrepenger.domene.uttak.fastsettuttaksgrunnlag.fp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.OriginalBehandling;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class FastsettUttaksgrunnlagTjenesteTest extends EntityManagerAwareTest {

    private UttakRepositoryProvider repositoryProvider;

    private final EndringsdatoFørstegangsbehandlingUtleder endringsdatoUtleder = mock(EndringsdatoFørstegangsbehandlingUtleder.class);
    private FastsettUttaksgrunnlagTjeneste tjeneste;

    @BeforeEach
    public void setup() {
        repositoryProvider = new UttakRepositoryProvider(getEntityManager());
        tjeneste = new FastsettUttaksgrunnlagTjeneste(repositoryProvider,
            endringsdatoUtleder,
            mock(EndringsdatoRevurderingUtlederImpl.class));
    }

    @Test
    public void skal_kopiere_søknadsperioder_fra_forrige_behandling_hvis_forrige_behandling_ikke_har_uttaksresultat() {

        LocalDate førsteUttaksdato = LocalDate.now();
        OppgittPeriodeEntitet periode = OppgittPeriodeBuilder.ny().medPeriode(førsteUttaksdato, LocalDate.now().plusDays(10))
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .build();
        OppgittFordelingEntitet oppgittFordelingForrigeBehandling = new OppgittFordelingEntitet(List.of(periode), true);

        ScenarioFarSøkerForeldrepenger førstegangsbehandlingScenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        førstegangsbehandlingScenario.medFordeling(oppgittFordelingForrigeBehandling);
        førstegangsbehandlingScenario.medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        Behandling førstegangsbehandling = førstegangsbehandlingScenario.lagre(repositoryProvider);

        ScenarioFarSøkerForeldrepenger revurdering = ScenarioFarSøkerForeldrepenger.forFødsel();
        revurdering.medOriginalBehandling(førstegangsbehandling, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_OPPTJENING);
        revurdering.medBehandlingType(BehandlingType.REVURDERING);
        revurdering.medFordeling(new OppgittFordelingEntitet(Collections.emptyList(), true));

        Behandling revurderingBehandling = revurdering.lagre(repositoryProvider);

        var familieHendelse = FamilieHendelse.forAdopsjonOmsorgsovertakelse(LocalDate.now(), List.of(), 0, null, false);
        var originalBehandling = new OriginalBehandling(førstegangsbehandling.getId(),
            new FamilieHendelser().medBekreftetHendelse(FamilieHendelse.forFødsel(null, LocalDate.now(), List.of(new Barn()), 1)));
        ForeldrepengerGrunnlag fpGrunnlag = new ForeldrepengerGrunnlag()
            .medFamilieHendelser(new FamilieHendelser()
            .medSøknadHendelse(familieHendelse))
            .medOriginalBehandling(originalBehandling);
        tjeneste.fastsettUttaksgrunnlag(lagInput(revurderingBehandling, fpGrunnlag));

        YtelseFordelingAggregat forrigeBehandlingFordeling = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(førstegangsbehandling.getId());
        YtelseFordelingAggregat resultat = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(revurderingBehandling.getId());

        assertThat(resultat.getOppgittFordeling().getOppgittePerioder()).isEmpty();
        assertThat(resultat.getGjeldendeSøknadsperioder().getOppgittePerioder()).isEqualTo(forrigeBehandlingFordeling.getOppgittFordeling().getOppgittePerioder());
        assertThat(resultat.getOppgittFordeling().getErAnnenForelderInformert()).isEqualTo(forrigeBehandlingFordeling.getOppgittFordeling().getErAnnenForelderInformert());
    }

    private UttakInput lagInput(Behandling behandling, ForeldrepengerGrunnlag ytelsespesifiktGrunnlag) {
        var ref = BehandlingReferanse.fra(behandling, LocalDate.now());
        return new UttakInput(ref, null, ytelsespesifiktGrunnlag);
    }

    @Test
    public void skal_lagre_opprinnelig_endringsdato() {

        LocalDate førsteUttaksdato = LocalDate.now();
        OppgittPeriodeEntitet periode = OppgittPeriodeBuilder.ny().medPeriode(førsteUttaksdato, LocalDate.now().plusDays(10))
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .build();
        OppgittFordelingEntitet fordeling = new OppgittFordelingEntitet(List.of(periode), true);

        ScenarioFarSøkerForeldrepenger scenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        scenario.medFordeling(fordeling);
        scenario.medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        Behandling behandling = scenario.lagre(repositoryProvider);
        LocalDate endringsdato = LocalDate.of(2020, 10, 10);
        when(endringsdatoUtleder.utledEndringsdato(behandling.getId(), List.of(periode))).thenReturn(endringsdato);

        var familieHendelse = FamilieHendelse.forFødsel(null, førsteUttaksdato, List.of(), 0);
        var familieHendelser = new FamilieHendelser().medSøknadHendelse(familieHendelse);
        tjeneste.fastsettUttaksgrunnlag(lagInput(behandling, new ForeldrepengerGrunnlag().medFamilieHendelser(familieHendelser)));

        YtelseFordelingAggregat resultat = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());

        assertThat(resultat.getGjeldendeEndringsdato()).isEqualTo(endringsdato);
        assertThat(resultat.getAvklarteDatoer().get().getOpprinneligEndringsdato()).isEqualTo(endringsdato);
    }

    @Test
    public void skal_ikke_forskyve_søknadsperioder_hvis_både_termin_og_fødsel_er_oppgitt_i_søknad() {
        LocalDate søknadFom = LocalDate.of(2019, 7, 31);
        OppgittPeriodeEntitet periode = OppgittPeriodeBuilder.ny().medPeriode(søknadFom, søknadFom.plusDays(10))
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .build();
        OppgittFordelingEntitet fordeling = new OppgittFordelingEntitet(List.of(periode), true);

        ScenarioFarSøkerForeldrepenger førstegangsbehandlingScenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        førstegangsbehandlingScenario.medFordeling(fordeling);

        førstegangsbehandlingScenario.medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        Behandling behandling = førstegangsbehandlingScenario.lagre(repositoryProvider);

        var søknadFamilieHendelse = FamilieHendelse.forFødsel(søknadFom, søknadFom.minusWeeks(2), List.of(), 0);
        ForeldrepengerGrunnlag fpGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(new FamilieHendelser().medSøknadHendelse(søknadFamilieHendelse));
        tjeneste.fastsettUttaksgrunnlag(lagInput(behandling, fpGrunnlag));

        YtelseFordelingAggregat resultat = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());

        assertThat(resultat.getGjeldendeSøknadsperioder().getOppgittePerioder().get(0).getFom()).isEqualTo(søknadFom);
    }

    @Test
    public void skal_fjerne_oppholdsperioder_på_slutten_av_fordelingen() {
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

        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(fordeling)
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);

        var behandling = scenario.lagre(repositoryProvider);

        var søknadFamilieHendelse = FamilieHendelse.forFødsel(søknadFom, null, List.of(), 0);
        var fpGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(new FamilieHendelser().medSøknadHendelse(søknadFamilieHendelse));
        tjeneste.fastsettUttaksgrunnlag(lagInput(behandling, fpGrunnlag));

        var resultat = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());

        var oppgittePerioder = resultat.getGjeldendeSøknadsperioder().getOppgittePerioder();
        assertThat(oppgittePerioder).hasSize(3);
        assertThat(oppgittePerioder.get(0).getFom()).isEqualTo(periode1.getFom());
        assertThat(oppgittePerioder.get(1).getFom()).isEqualTo(opphold1.getFom());
        assertThat(oppgittePerioder.get(2).getFom()).isEqualTo(periode2.getFom());
    }

    @Test
    public void skal_ikke_fjerne_oppholdsperioder_på_slutten_hvis_fordelingen_bare_består_av_oppholdsperioder() {
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

        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(fordeling)
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);

        var behandling = scenario.lagre(repositoryProvider);

        var søknadFamilieHendelse = FamilieHendelse.forFødsel(søknadFom, null, List.of(), 0);
        var fpGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(new FamilieHendelser().medSøknadHendelse(søknadFamilieHendelse));
        tjeneste.fastsettUttaksgrunnlag(lagInput(behandling, fpGrunnlag));

        var resultat = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());

        var oppgittePerioder = resultat.getGjeldendeSøknadsperioder().getOppgittePerioder();
        assertThat(oppgittePerioder).hasSize(2);
    }

    @Test
    public void adopsjon_uten_justering() {
        var søknadFom = LocalDate.of(2019, 7, 31);
        var søknadsperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(søknadFom.plusDays(1), søknadFom.plusDays(10))
            .build();
        var fordeling = new OppgittFordelingEntitet(List.of(søknadsperiode), true);

        var scenario = ScenarioMorSøkerForeldrepenger.forAdopsjon()
            .medFordeling(fordeling)
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);

        var behandling = scenario.lagre(repositoryProvider);

        var søknadFamilieHendelse = FamilieHendelse.forAdopsjonOmsorgsovertakelse(søknadFom, List.of(new Barn(null)), 1, søknadFom.minusWeeks(2), false);
        var fpGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(new FamilieHendelser().medSøknadHendelse(søknadFamilieHendelse));
        tjeneste.fastsettUttaksgrunnlag(lagInput(behandling, fpGrunnlag));

        var resultat = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId());

        var oppgittePerioder = resultat.getGjeldendeSøknadsperioder().getOppgittePerioder();
        assertThat(oppgittePerioder).hasSize(1);
        assertThat(oppgittePerioder.get(0).getFom()).isEqualTo(søknadsperiode.getFom());
        assertThat(oppgittePerioder.get(0).getTom()).isEqualTo(søknadsperiode.getTom());
    }
}
