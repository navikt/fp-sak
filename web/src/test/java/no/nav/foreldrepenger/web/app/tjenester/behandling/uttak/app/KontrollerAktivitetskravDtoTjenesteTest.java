package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.UuidDto;
import no.nav.foreldrepenger.behandling.YtelseMaksdatoTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.AndelGraderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AktivitetskravPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AktivitetskravPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.fp.SkjæringstidspunktTjenesteImpl;
import no.nav.foreldrepenger.skjæringstidspunkt.fp.SkjæringstidspunktUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.KontrollerAktivitetskravAvklaring.I_AKTIVITET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@ExtendWith(FPsakEntityManagerAwareExtension.class)
class KontrollerAktivitetskravDtoTjenesteTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private KontrollerAktivitetskravDtoTjeneste tjeneste;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        ytelsesFordelingRepository = new YtelsesFordelingRepository(entityManager);
        YtelseFordelingTjeneste ytelseFordelingTjeneste = new YtelseFordelingTjeneste(ytelsesFordelingRepository);
        var foreldrepengerUttakTjeneste = new ForeldrepengerUttakTjeneste(new FpUttakRepository(entityManager));
        var andelGraderingTjeneste = new AndelGraderingTjeneste(
            foreldrepengerUttakTjeneste, ytelsesFordelingRepository);
        UttakInputTjeneste uttakInputTjeneste = new UttakInputTjeneste(repositoryProvider, new HentOgLagreBeregningsgrunnlagTjeneste(entityManager),
            new AbakusInMemoryInntektArbeidYtelseTjeneste(), new SkjæringstidspunktTjenesteImpl(repositoryProvider,
            new YtelseMaksdatoTjeneste(repositoryProvider, new RelatertBehandlingTjeneste(repositoryProvider)),
            new SkjæringstidspunktUtils()),
            mock(MedlemTjeneste.class), andelGraderingTjeneste);
        tjeneste = new KontrollerAktivitetskravDtoTjeneste(repositoryProvider.getBehandlingRepository(),
            ytelseFordelingTjeneste, uttakInputTjeneste, foreldrepengerUttakTjeneste);
    }

    @Test
    public void skal_avgrense_avklart_periode_til_søknadsperiode() {
        var behandling = behandlingFraScenario();
        var søknadFom = LocalDate.now().minusDays(10);
        var søknadTom = LocalDate.now().plusDays(5);
        var søknadPeriode = søknadsperiode(søknadFom, søknadTom);

        lagreOpprinneligAktivitetskrav(behandling, søknadFom.minusDays(5), søknadTom.plusDays(5));
        lagreOppgittFordeling(behandling, søknadPeriode);

        var dto = tjeneste.lagDtos(new UuidDto(behandling.getUuid()));
        assertThat(dto).hasSize(1);
        assertThat(dto.get(0).getFom()).isEqualTo(søknadFom);
        assertThat(dto.get(0).getTom()).isEqualTo(søknadTom);
        assertThat(dto.get(0).getAvklaring()).isEqualTo(I_AKTIVITET);
    }


    @Test
    public void skal_dele_opp_en_avklart_periode_som_dekker_flere_søknadsperioder() {
        var behandling = behandlingFraScenario();
        var førsteSøknadsperiodeFom = LocalDate.now().minusDays(10);
        var førsteSøknadsperiodeTom = LocalDate.now();
        var andreSøknadsperiodeFom = førsteSøknadsperiodeTom.plusDays(1);
        var andreSøknadsperiodeTom = andreSøknadsperiodeFom.plusDays(10);
        var førsteSøknadsperiode = søknadsperiode(førsteSøknadsperiodeFom, førsteSøknadsperiodeTom);
        var andreSøknadsperiode = søknadsperiode(andreSøknadsperiodeFom, andreSøknadsperiodeTom);

        lagreOpprinneligAktivitetskrav(behandling, førsteSøknadsperiodeFom.minusDays(7), andreSøknadsperiodeTom);
        lagreOppgittFordeling(behandling, førsteSøknadsperiode, andreSøknadsperiode);
        var dto = tjeneste.lagDtos(new UuidDto(behandling.getUuid()));

        assertThat(dto).hasSize(2);
        assertThat(dto.get(0).getFom()).isEqualTo(førsteSøknadsperiodeFom);
        assertThat(dto.get(0).getTom()).isEqualTo(førsteSøknadsperiodeTom);
        assertThat(dto.get(0).getAvklaring()).isEqualTo(I_AKTIVITET);
        assertThat(dto.get(1).getFom()).isEqualTo(andreSøknadsperiodeFom);
        assertThat(dto.get(1).getTom()).isEqualTo(andreSøknadsperiodeTom);
        assertThat(dto.get(1).getAvklaring()).isEqualTo(I_AKTIVITET);
    }

    @Test
    public void skal_avklare_hele_perioden_når_eksisterende_avklaring_ikke_dekker_hele_søknadsperioden() {
        var behandling = behandlingFraScenario();
        var søknadFom = LocalDate.now().minusDays(10);
        var søknadTom = LocalDate.now().plusDays(5);
        var søknadPeriode = søknadsperiode(søknadFom, søknadTom);
        lagreOpprinneligAktivitetskrav(behandling, søknadFom.plusDays(1), søknadTom.plusDays(1));
        lagreOppgittFordeling(behandling, søknadPeriode);
        var dto = tjeneste.lagDtos(new UuidDto(behandling.getUuid()));
        assertThat(dto).hasSize(1);
        assertThat(dto.get(0).getFom()).isEqualTo(søknadFom);
        assertThat(dto.get(0).getTom()).isEqualTo(søknadTom);
        assertNull(dto.get(0).getAvklaring());
    }

    private Behandling behandlingFraScenario() {
        ScenarioFarSøkerForeldrepenger scenario = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(AktørId.dummy());
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now().minusDays(60)).medAntallBarn(1);
        scenario.medOppgittRettighet(new OppgittRettighetEntitet(true, false, false));
        scenario.lagre(repositoryProvider);
        return scenario.getBehandling();
    }

    private void lagreOpprinneligAktivitetskrav(Behandling behandling, LocalDate førsteFom, LocalDate førsteTom) {
        var aktivitetsKrav = new AktivitetskravPeriodeEntitet(førsteFom, førsteTom,
            I_AKTIVITET, "ok.");
        repositoryProvider.getYtelsesFordelingRepository()
            .lagreOpprinnelige(behandling.getId(), new AktivitetskravPerioderEntitet().leggTil(aktivitetsKrav));
    }

    private void lagreOppgittFordeling(Behandling behandling, OppgittPeriodeEntitet... søknadPeriode) {
        ytelsesFordelingRepository.lagre(behandling.getId(),
            new OppgittFordelingEntitet(List.of(søknadPeriode), true));
    }

    private static OppgittPeriodeEntitet søknadsperiode(LocalDate førsteSøknadsperiodeFom, LocalDate førsteSøknadsperiodeTom) {
        return OppgittPeriodeBuilder.ny()
            .medPeriode(førsteSøknadsperiodeFom, førsteSøknadsperiodeTom)
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medMorsAktivitet(MorsAktivitet.ARBEID)
            .build();
    }
}
