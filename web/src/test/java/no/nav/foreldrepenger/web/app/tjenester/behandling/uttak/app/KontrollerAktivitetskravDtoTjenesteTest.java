package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.KontrollerAktivitetskravAvklaring.I_AKTIVITET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.YtelseMaksdatoTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.BeregningUttakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AktivitetskravPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AktivitetskravPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.fp.SkjæringstidspunktTjenesteImpl;
import no.nav.foreldrepenger.skjæringstidspunkt.fp.SkjæringstidspunktUtils;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.MinsterettBehandling2022;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.UtsettelseBehandling2021;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;

@ExtendWith(JpaExtension.class)
class KontrollerAktivitetskravDtoTjenesteTest {

    private static final LocalDate DATO = LocalDate.of(2021, 1, 11);
    private BehandlingRepositoryProvider repositoryProvider;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private KontrollerAktivitetskravDtoTjeneste tjeneste;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        ytelsesFordelingRepository = new YtelsesFordelingRepository(entityManager);
        var ytelseFordelingTjeneste = new YtelseFordelingTjeneste(ytelsesFordelingRepository);
        var foreldrepengerUttakTjeneste = new ForeldrepengerUttakTjeneste(new FpUttakRepository(entityManager));
        var andelGraderingTjeneste = new BeregningUttakTjeneste(
            foreldrepengerUttakTjeneste, ytelsesFordelingRepository);
        var uttakInputTjeneste = new UttakInputTjeneste(repositoryProvider, new HentOgLagreBeregningsgrunnlagTjeneste(entityManager),
            new AbakusInMemoryInntektArbeidYtelseTjeneste(), new SkjæringstidspunktTjenesteImpl(repositoryProvider,
            new YtelseMaksdatoTjeneste(repositoryProvider, new RelatertBehandlingTjeneste(repositoryProvider)),
            new SkjæringstidspunktUtils(), mock(UtsettelseBehandling2021.class), mock(MinsterettBehandling2022.class)),
            mock(MedlemTjeneste.class), andelGraderingTjeneste, ytelseFordelingTjeneste, false);
        tjeneste = new KontrollerAktivitetskravDtoTjeneste(repositoryProvider.getBehandlingRepository(),
            ytelseFordelingTjeneste, uttakInputTjeneste, foreldrepengerUttakTjeneste);
    }

    @Test
    public void skal_avgrense_avklart_periode_til_søknadsperiode() {
        var behandling = behandlingFraScenario();
        var søknadFom = DATO.minusDays(10);
        var søknadTom = DATO.plusDays(5);
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
        var førsteSøknadsperiodeFom = DATO.minusDays(10);
        var førsteSøknadsperiodeTom = DATO;
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
        var søknadFom = DATO.minusDays(10);
        var søknadTom = DATO.plusDays(5);
        var søknadPeriode = søknadsperiode(søknadFom, søknadTom);
        lagreOpprinneligAktivitetskrav(behandling, søknadFom.plusDays(1), søknadTom.plusDays(1));
        lagreOppgittFordeling(behandling, søknadPeriode);
        var dto = tjeneste.lagDtos(new UuidDto(behandling.getUuid()));
        assertThat(dto).hasSize(1);
        assertThat(dto.get(0).getFom()).isEqualTo(søknadFom);
        assertThat(dto.get(0).getTom()).isEqualTo(søknadTom);
        assertThat(dto.get(0).getAvklaring()).isNull();
    }

    @Test
    public void tidsperiode_på_dto_skal_følge_avklaring_hvis_søknadsperiode_har_fom_tom_i_helg() {
        var behandling = behandlingFraScenario();
        var søknadFom = LocalDate.of(2021, 1, 3); // søndag
        var avklaringFom = LocalDate.of(2021,1, 4); // mandag
        var avklaringTom = LocalDate.of(2021, 1, 8); // fredag
        var søknadTom = LocalDate.of(2021, 1, 10); // søndag
        var søknadPeriode = søknadsperiode(søknadFom, søknadTom);
        lagreOpprinneligAktivitetskrav(behandling, avklaringFom, avklaringTom);
        lagreOppgittFordeling(behandling, søknadPeriode);
        var dto = tjeneste.lagDtos(new UuidDto(behandling.getUuid()));
        assertThat(dto).hasSize(1);
        assertThat(dto.get(0).getFom()).isEqualTo(avklaringFom);
        assertThat(dto.get(0).getTom()).isEqualTo(avklaringTom);
        assertThat(dto.get(0).getAvklaring()).isNotNull();
    }

    private Behandling behandlingFraScenario() {
        var scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(new OppgittRettighetEntitet(true, false, false, false))
            .medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medJustertEndringsdato(DATO.minusDays(60)).build());
        scenario.medSøknadHendelse().medFødselsDato(DATO.minusDays(60)).medAntallBarn(1);
        return scenario.lagre(repositoryProvider);
    }

    private void lagreOpprinneligAktivitetskrav(Behandling behandling, LocalDate førsteFom, LocalDate førsteTom) {
        var aktivitetsKrav = new AktivitetskravPeriodeEntitet(førsteFom, førsteTom,
            I_AKTIVITET, "ok.");
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
            .medSaksbehandledeAktivitetskravPerioder(new AktivitetskravPerioderEntitet().leggTil(aktivitetsKrav));
        ytelsesFordelingRepository.lagre(behandling.getId(), yfBuilder.build());
    }

    private void lagreOppgittFordeling(Behandling behandling, OppgittPeriodeEntitet... søknadPeriode) {
        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
            .medOppgittFordeling(new OppgittFordelingEntitet(List.of(søknadPeriode), true));
        ytelsesFordelingRepository.lagre(behandling.getId(), yfBuilder.build());
    }

    private static OppgittPeriodeEntitet søknadsperiode(LocalDate førsteSøknadsperiodeFom, LocalDate førsteSøknadsperiodeTom) {
        return OppgittPeriodeBuilder.ny()
            .medPeriode(førsteSøknadsperiodeFom, førsteSøknadsperiodeTom)
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medMorsAktivitet(MorsAktivitet.ARBEID)
            .build();
    }
}
