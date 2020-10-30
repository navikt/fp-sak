package no.nav.foreldrepenger.domene.uttak.kontroller.fakta.uttakperioder;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.InnvilgetÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.Annenpart;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;

public class AnnenForelderIkkeRettOgLøpendeVedtakAksjonspunktUtlederTest extends EntityManagerAwareTest {

    private UttakRepositoryProvider repositoryProvider;
    private ForeldrepengerUttakTjeneste uttakTjeneste;

    @BeforeEach
    public void setup() {
        repositoryProvider = new UttakRepositoryProvider(getEntityManager());
        uttakTjeneste = new ForeldrepengerUttakTjeneste(repositoryProvider.getFpUttakRepository());
    }

    @Test
    public void ingen_aksjonspunkt_hvis_uten_ytelsefordeling() {
        AnnenForelderIkkeRettOgLøpendeVedtakAksjonspunktUtleder utleder = utleder();

        ScenarioFarSøkerForeldrepenger scenario = ScenarioFarSøkerForeldrepenger.forFødsel();
        Behandling behandling = scenario.lagre(repositoryProvider);

        var resultat = utleder.utledAksjonspunkterFor(input(behandling, null));

        assertThat(resultat).isEmpty();
    }

    @Test
    public void aksjonspunkt_hvis_foreldrepenger_og_oppgitt_annen_forelder_ikke_rett_men_annen_forelder_har_løpende_vedtak_og_søkt_foreldrepenger_kvote() {
        AnnenForelderIkkeRettOgLøpendeVedtakAksjonspunktUtleder utleder = utleder();

        Behandling morBehandling = morBehandlingMedLøpendeUtbetaling();

        ScenarioFarSøkerForeldrepenger scenarioFar = ScenarioFarSøkerForeldrepenger.forFødsel();
        scenarioFar.medOppgittRettighet(new OppgittRettighetEntitet(false, true, false));
        OppgittPeriodeEntitet søknadsperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(LocalDate.of(2019, 3, 29), LocalDate.of(2019, 3, 29))
            .build();
        scenarioFar.medFordeling(new OppgittFordelingEntitet(List.of(søknadsperiode), true));
        Behandling farBehandling = scenarioFar.lagre(repositoryProvider);

        kobleSaker(morBehandling, farBehandling);

        var resultat = utleder.utledAksjonspunkterFor(input(farBehandling, new Annenpart(false, morBehandling.getId())));

        assertThat(resultat).hasSize(1);
    }

    @Test
    public void ingen_aksjonspunkt_hvis_foreldrepenger_og_oppgitt_annen_forelder_har_rett_og_annen_forelder_har_løpende_vedtak_og_søkt_foreldrepenger_kvote() {
        AnnenForelderIkkeRettOgLøpendeVedtakAksjonspunktUtleder utleder = utleder();

        Behandling morBehandling = morBehandlingMedLøpendeUtbetaling();

        ScenarioFarSøkerForeldrepenger scenarioFar = ScenarioFarSøkerForeldrepenger.forFødsel();
        scenarioFar.medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));
        OppgittPeriodeEntitet søknadsperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .medPeriode(LocalDate.of(2019, 3, 29), LocalDate.of(2019, 3, 29))
            .build();
        scenarioFar.medFordeling(new OppgittFordelingEntitet(List.of(søknadsperiode), true));
        Behandling farBehandling = scenarioFar.lagre(repositoryProvider);

        kobleSaker(morBehandling, farBehandling);

        var resultat = utleder.utledAksjonspunkterFor(input(farBehandling, new Annenpart(false, morBehandling.getId())));

        assertThat(resultat).isEmpty();
    }

    private void kobleSaker(Behandling morBehandling, Behandling farBehandling) {
        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(morBehandling.getFagsak(), Dekningsgrad._100);
        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(morBehandling.getFagsak(), farBehandling.getFagsak(), morBehandling);
    }

    private Behandling morBehandlingMedLøpendeUtbetaling() {
        ScenarioMorSøkerForeldrepenger scenarioMor = ScenarioMorSøkerForeldrepenger.forFødsel();
        UttakResultatPerioderEntitet uttakMor = new UttakResultatPerioderEntitet();
        UttakResultatPeriodeEntitet morUttakPeriode =
            new UttakResultatPeriodeEntitet.Builder(LocalDate.of(2019, 3, 28), LocalDate.of(2019, 3, 28))
                .medResultatType(PeriodeResultatType.INNVILGET, InnvilgetÅrsak.UTTAK_OPPFYLT)
                .build();
        UttakResultatPeriodeAktivitetEntitet morUttakAktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(morUttakPeriode,
            new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.FRILANS).build())
            .medUtbetalingsgrad(new Utbetalingsgrad(100))
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
