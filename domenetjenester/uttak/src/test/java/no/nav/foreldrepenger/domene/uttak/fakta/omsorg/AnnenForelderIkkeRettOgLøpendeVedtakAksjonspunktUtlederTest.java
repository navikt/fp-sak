package no.nav.foreldrepenger.domene.uttak.fakta.omsorg;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.Annenpart;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;

public class AnnenForelderIkkeRettOgLøpendeVedtakAksjonspunktUtlederTest {

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryStubProvider();
    private final ForeldrepengerUttakTjeneste uttakTjeneste = new ForeldrepengerUttakTjeneste(
        repositoryProvider.getFpUttakRepository());

    @Test
    public void ingen_aksjonspunkt_hvis_uten_ytelsefordeling() {
        var utleder = utleder();

        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);

        var resultat = utleder.utledAksjonspunkterFor(input(behandling, null));

        assertThat(resultat).isEmpty();
    }

    @Test
    public void aksjonspunkt_hvis_foreldrepenger_og_oppgitt_annen_forelder_ikke_rett_men_annen_forelder_har_løpende_vedtak_og_søkt_foreldrepenger_kvote() {
        var utleder = utleder();

        var morBehandling = morBehandlingMedLøpendeUtbetaling();

        var scenarioFar = ScenarioFarSøkerForeldrepenger.forFødsel();
        scenarioFar.medOppgittRettighet(new OppgittRettighetEntitet(false, true, false));
        var søknadsperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(LocalDate.of(2019, 3, 29), LocalDate.of(2019, 3, 29))
            .build();
        scenarioFar.medFordeling(new OppgittFordelingEntitet(List.of(søknadsperiode), true));
        var farBehandling = scenarioFar.lagre(repositoryProvider);

        kobleSaker(morBehandling, farBehandling);

        var resultat = utleder.utledAksjonspunkterFor(
            input(farBehandling, new Annenpart(morBehandling.getId(), LocalDate.of(2019, 3, 29).atStartOfDay())));

        assertThat(resultat).hasSize(1);
    }

    @Test
    public void ingen_aksjonspunkt_hvis_foreldrepenger_og_oppgitt_annen_forelder_har_rett_og_annen_forelder_har_løpende_vedtak_og_søkt_foreldrepenger_kvote() {
        var utleder = utleder();

        var morBehandling = morBehandlingMedLøpendeUtbetaling();

        var scenarioFar = ScenarioFarSøkerForeldrepenger.forFødsel();
        scenarioFar.medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));
        var søknadsperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(LocalDate.of(2019, 3, 29), LocalDate.of(2019, 3, 29))
            .build();
        scenarioFar.medFordeling(new OppgittFordelingEntitet(List.of(søknadsperiode), true));
        var farBehandling = scenarioFar.lagre(repositoryProvider);

        kobleSaker(morBehandling, farBehandling);

        var resultat = utleder.utledAksjonspunkterFor(
            input(farBehandling, new Annenpart(morBehandling.getId(), LocalDate.of(2019, 3, 29).atStartOfDay())));

        assertThat(resultat).isEmpty();
    }

    private void kobleSaker(Behandling morBehandling, Behandling farBehandling) {
        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(morBehandling.getFagsak(), Dekningsgrad._100);
        repositoryProvider.getFagsakRelasjonRepository()
            .kobleFagsaker(morBehandling.getFagsak(), farBehandling.getFagsak(), morBehandling);
    }

    private Behandling morBehandlingMedLøpendeUtbetaling() {
        var scenarioMor = ScenarioMorSøkerForeldrepenger.forFødsel();
        var uttakMor = new UttakResultatPerioderEntitet();
        var morUttakPeriode = new UttakResultatPeriodeEntitet.Builder(LocalDate.of(2019, 3, 28),
            LocalDate.of(2019, 3, 28)).medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.FELLESPERIODE_ELLER_FORELDREPENGER)
            .build();
        var morUttakAktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(
            morUttakPeriode,
            new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.FRILANS).build()).medUtbetalingsgrad(
            new Utbetalingsgrad(100))
            .medTrekkdager(new Trekkdager(10))
            .medArbeidsprosent(BigDecimal.valueOf(100))
            .medTrekkonto(StønadskontoType.MØDREKVOTE)
            .build();
        morUttakPeriode.leggTilAktivitet(morUttakAktivitet);

        uttakMor.leggTilPeriode(morUttakPeriode);
        scenarioMor.medUttak(uttakMor);
        return scenarioMor.lagre(repositoryProvider);
    }

    private UttakInput input(Behandling behandling, Annenpart annenpart) {
        var fpGrunnlag = new ForeldrepengerGrunnlag().medAnnenpart(annenpart);
        return new UttakInput(BehandlingReferanse.fra(behandling), null, fpGrunnlag);
    }

    private AnnenForelderIkkeRettOgLøpendeVedtakAksjonspunktUtleder utleder() {
        return new AnnenForelderIkkeRettOgLøpendeVedtakAksjonspunktUtleder(repositoryProvider, uttakTjeneste);
    }
}
