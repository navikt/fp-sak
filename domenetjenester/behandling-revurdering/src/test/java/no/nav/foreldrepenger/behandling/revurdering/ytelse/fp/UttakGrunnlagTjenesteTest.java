package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingGrunnlagRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;

@ExtendWith(JpaExtension.class)
public class UttakGrunnlagTjenesteTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingGrunnlagRepositoryProvider grunnlagRepositoryProvider;

    private UttakGrunnlagTjeneste tjeneste;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        grunnlagRepositoryProvider = new BehandlingGrunnlagRepositoryProvider(entityManager);
        var relatertBehandlingTjeneste = new RelatertBehandlingTjeneste(repositoryProvider);
        var familieHendelseRepository = new FamilieHendelseRepository(entityManager);
        var familieHendelseTjeneste = new FamilieHendelseTjeneste(null, familieHendelseRepository);
        tjeneste = new UttakGrunnlagTjeneste(repositoryProvider, grunnlagRepositoryProvider, relatertBehandlingTjeneste,
                familieHendelseTjeneste);
    }

    @Test
    public void skal_ignorere_overstyrt_familiehendelse_hvis_saksbehandler_har_valgt_at_fødsel_ikke_er_dokumentert() {
        var førstegangsScenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var fødselsDato = LocalDate.of(2019, 10, 10);
        førstegangsScenario.medBekreftetHendelse().medFødselsDato(fødselsDato).medAntallBarn(1).erFødsel();
        førstegangsScenario.medOverstyrtHendelse().medAntallBarn(0).erFødsel();
        var behandling = førstegangsScenario.lagre(repositoryProvider);

        var revurderingScenario = ScenarioMorSøkerEngangsstønad.forFødsel()
                .medOriginalBehandling(behandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        revurderingScenario.medBekreftetHendelse().medAntallBarn(1).medFødselsDato(fødselsDato).erFødsel();
        var revurdering = revurderingScenario.lagre(repositoryProvider);

        var grunnlagRevurdering = tjeneste.grunnlag(BehandlingReferanse.fra(revurdering));
        var grunnlagFørstegangsBehandling = tjeneste.grunnlag(BehandlingReferanse.fra(behandling));

        assertThat(grunnlagRevurdering.orElseThrow()
                .getOriginalBehandling()
                .orElseThrow()
                .getFamilieHendelser()
                .getOverstyrtFamilieHendelse()).isEmpty();
        assertThat(grunnlagRevurdering.orElseThrow()
                .getOriginalBehandling()
                .orElseThrow()
                .getFamilieHendelser()
                .getGjeldendeFamilieHendelse()
                .getFamilieHendelseDato()).isEqualTo(fødselsDato);
        assertThat(
                grunnlagFørstegangsBehandling.orElseThrow().getFamilieHendelser().getOverstyrtFamilieHendelse()).isEmpty();
        assertThat(grunnlagFørstegangsBehandling.orElseThrow()
                .getFamilieHendelser()
                .getGjeldendeFamilieHendelse()
                .getFamilieHendelseDato()).isEqualTo(fødselsDato);
    }
}
