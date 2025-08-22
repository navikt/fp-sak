package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravArbeidRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingGrunnlagRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;

@ExtendWith(JpaExtension.class)
class UttakGrunnlagTjenesteTest {

    private BehandlingRepositoryProvider repositoryProvider;

    private UttakGrunnlagTjeneste tjeneste;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        var grunnlagRepositoryProvider = new BehandlingGrunnlagRepositoryProvider(entityManager);
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(repositoryProvider);
        var relatertBehandlingTjeneste = new RelatertBehandlingTjeneste(repositoryProvider, fagsakRelasjonTjeneste);
        var familieHendelseRepository = new FamilieHendelseRepository(entityManager);
        var familieHendelseTjeneste = new FamilieHendelseTjeneste(null, familieHendelseRepository);
        tjeneste = new UttakGrunnlagTjeneste(repositoryProvider, grunnlagRepositoryProvider, relatertBehandlingTjeneste, familieHendelseTjeneste,
            fagsakRelasjonTjeneste, new AktivitetskravArbeidRepository(entityManager));
    }

    @Test
    void skal_ignorere_overstyrt_familiehendelse_hvis_saksbehandler_har_valgt_at_fødsel_ikke_er_dokumentert() {
        var førstegangsScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var fødselsDato = LocalDate.of(2019, 10, 10);
        førstegangsScenario.medBekreftetHendelse().medFødselsDato(fødselsDato).medAntallBarn(1).medFødselType();
        førstegangsScenario.medOverstyrtHendelse().medAntallBarn(0).medFødselType();
        var behandling = førstegangsScenario.lagre(repositoryProvider);

        var revurderingScenario = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medOriginalBehandling(behandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        revurderingScenario.medBekreftetHendelse().medAntallBarn(1).medFødselsDato(fødselsDato).medFødselType();
        var revurdering = revurderingScenario.lagre(repositoryProvider);

        var grunnlagRevurdering = tjeneste.grunnlag(BehandlingReferanse.fra(revurdering));
        var grunnlagFørstegangsBehandling = tjeneste.grunnlag(BehandlingReferanse.fra(behandling));

        var originalBehandling = grunnlagRevurdering.getOriginalBehandling().orElseThrow();
        var originalBehandlingFamilieHendelser = originalBehandling.getFamilieHendelser();
        assertThat(originalBehandlingFamilieHendelser.getOverstyrtFamilieHendelse()).isEmpty();
        assertThat(originalBehandlingFamilieHendelser.getGjeldendeFamilieHendelse().getFamilieHendelseDato()).isEqualTo(fødselsDato);
        var førstegangsBehandlingFamilieHendelser = grunnlagFørstegangsBehandling.getFamilieHendelser();
        assertThat(førstegangsBehandlingFamilieHendelser.getOverstyrtFamilieHendelse()).isEmpty();
        assertThat(førstegangsBehandlingFamilieHendelser.getGjeldendeFamilieHendelse().getFamilieHendelseDato()).isEqualTo(fødselsDato);
    }

    @Test
    void skal_sette_dødsfall_hvis_det_er_endringer_om_død() {
        var behandlingMedEndretOpplysningerOmDød = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medDefaultSøknadTerminbekreftelse().medDefaultFordeling(LocalDate.of(2019, 1, 1))
            .lagre(repositoryProvider);
        var personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        var registerVersjon = personopplysningRepository
            .hentPersonopplysninger(behandlingMedEndretOpplysningerOmDød.getId()).getRegisterVersjon();
        var builder = PersonInformasjonBuilder
            .oppdater(registerVersjon, PersonopplysningVersjonType.REGISTRERT);
        builder.leggTil(builder.getPersonopplysningBuilder(behandlingMedEndretOpplysningerOmDød.getAktørId())
            .medDødsdato(LocalDate.now()));
        personopplysningRepository.lagre(behandlingMedEndretOpplysningerOmDød.getId(), builder);

        var resultat = tjeneste.grunnlag(BehandlingReferanse.fra(behandlingMedEndretOpplysningerOmDød));

        assertThat(resultat.isDødsfall()).isTrue();
    }

    @Test
    void skal_sette_dødsfall_hvis_det_er_ingen_endringer_om_død() {
        var behandlingUtenEndringIOpplysninger = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medDefaultSøknadTerminbekreftelse().medDefaultFordeling(LocalDate.of(2019, 1, 1))
            .lagre(repositoryProvider);

        var resultat = tjeneste.grunnlag(BehandlingReferanse.fra(behandlingUtenEndringIOpplysninger));

        assertThat(resultat.isDødsfall()).isFalse();
    }

    @Test
    void skal_sette_dødsfall_hvis_behandlingårsak_død() {
        var fom = LocalDate.of(2019, 1, 1);
        var originalBehandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medDefaultBekreftetTerminbekreftelse().medDefaultFordeling(fom)
            .lagre(repositoryProvider);
        var behandlingUtenEndringIOpplysninger = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_HENDELSE_DØD_BARN)
            .medDefaultSøknadTerminbekreftelse().medDefaultFordeling(fom)
            .lagre(repositoryProvider);

        var resultat = tjeneste.grunnlag(BehandlingReferanse.fra(behandlingUtenEndringIOpplysninger));

        assertThat(resultat.isDødsfall()).isTrue();
    }
}
