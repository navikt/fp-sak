package no.nav.foreldrepenger.behandling.revurdering.ytelse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.YtelseMaksdatoTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.AndelGraderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.fp.SkjæringstidspunktTjenesteImpl;
import no.nav.foreldrepenger.skjæringstidspunkt.fp.SkjæringstidspunktUtils;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class UttakInputTjenesteTest {

    private UttakInputTjeneste tjeneste;
    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        var ytelsesFordelingRepository = new YtelsesFordelingRepository(entityManager);
        var andelGraderingTjeneste = new AndelGraderingTjeneste(
            new ForeldrepengerUttakTjeneste(new FpUttakRepository(entityManager)), ytelsesFordelingRepository);
        tjeneste = new UttakInputTjeneste(repositoryProvider, new HentOgLagreBeregningsgrunnlagTjeneste(entityManager),
            new AbakusInMemoryInntektArbeidYtelseTjeneste(), new SkjæringstidspunktTjenesteImpl(repositoryProvider,
            new YtelseMaksdatoTjeneste(repositoryProvider, new RelatertBehandlingTjeneste(repositoryProvider)),
            new SkjæringstidspunktUtils()), mock(MedlemTjeneste.class), andelGraderingTjeneste);
    }

    @Test
    public void skal_hente_behandlingsårsaker_fra_behandling() {
        var originalBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medDefaultSøknadTerminbekreftelse()
            .lagre(repositoryProvider);
        var årsak = BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER;
        var revurdering = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOriginalBehandling(originalBehandling, årsak, false)
            .medDefaultOppgittFordeling(LocalDate.of(2019, 11, 6)).medDefaultSøknadTerminbekreftelse()
            .lagre(repositoryProvider);

        var resultat = tjeneste.lagInput(revurdering);

        assertThat(resultat.isBehandlingManueltOpprettet()).isFalse();
        assertThat(resultat.harBehandlingÅrsak(årsak)).isTrue();
    }

    @Test
    public void skal_sette_om_behandling_er_manuelt_behandlet() {
        var originalBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medDefaultSøknadTerminbekreftelse()
            .lagre(repositoryProvider);
        var revurdering = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_INNTEKT, true)
            .medDefaultOppgittFordeling(LocalDate.of(2019, 11, 6)).medDefaultSøknadTerminbekreftelse()
            .lagre(repositoryProvider);

        var resultat = tjeneste.lagInput(revurdering);

        assertThat(resultat.isBehandlingManueltOpprettet()).isTrue();
    }

    @Test
    public void skal_sette_om_opplysninger_om_død_er_endret_hvis_det_er_endringer() {
        var behandlingMedEndretOpplysningerOmDød = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medDefaultSøknadTerminbekreftelse().medDefaultOppgittFordeling(LocalDate.of(2019, 1, 1))
            .lagre(repositoryProvider);
        var personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        Optional<PersonInformasjonEntitet> registerVersjon = personopplysningRepository
            .hentPersonopplysninger(behandlingMedEndretOpplysningerOmDød.getId()).getRegisterVersjon();
        PersonInformasjonBuilder builder = PersonInformasjonBuilder
            .oppdater(registerVersjon, PersonopplysningVersjonType.REGISTRERT);
        builder.leggTil(builder.getPersonopplysningBuilder(behandlingMedEndretOpplysningerOmDød.getAktørId())
            .medDødsdato(LocalDate.now()));
        personopplysningRepository.lagre(behandlingMedEndretOpplysningerOmDød.getId(), builder);

        var resultat = tjeneste.lagInput(behandlingMedEndretOpplysningerOmDød.getId());

        assertThat(resultat.isOpplysningerOmDødEndret()).isTrue();
    }

    @Test
    public void skal_sette_om_opplysninger_om_død_er_endret_hvis_det_er_ingen_endringer() {
        var behandlingUtenEndringIOpplysninger = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medDefaultSøknadTerminbekreftelse().medDefaultOppgittFordeling(LocalDate.of(2019, 1, 1))
            .lagre(repositoryProvider);

        var resultat = tjeneste.lagInput(behandlingUtenEndringIOpplysninger.getId());

        assertThat(resultat.isOpplysningerOmDødEndret()).isFalse();
    }
}
