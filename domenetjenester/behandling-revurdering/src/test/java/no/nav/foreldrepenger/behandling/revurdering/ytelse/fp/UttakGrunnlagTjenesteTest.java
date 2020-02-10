package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;


import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

@RunWith(CdiRunner.class)
public class UttakGrunnlagTjenesteTest {

    @Rule
    public RepositoryRule repositoryRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());

    @Inject
    @FagsakYtelseTypeRef("FP")
    private UttakGrunnlagTjeneste tjeneste;

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

        assertThat(grunnlagRevurdering.orElseThrow().getOriginalBehandling().orElseThrow().getFamilieHendelser().getOverstyrtFamilieHendelse()).isEmpty();
        assertThat(grunnlagRevurdering.orElseThrow().getOriginalBehandling().orElseThrow().getFamilieHendelser().getGjeldendeFamilieHendelse().getFamilieHendelseDato()).isEqualTo(fødselsDato);
        assertThat(grunnlagFørstegangsBehandling.orElseThrow().getFamilieHendelser().getOverstyrtFamilieHendelse()).isEmpty();
        assertThat(grunnlagFørstegangsBehandling.orElseThrow().getFamilieHendelser().getGjeldendeFamilieHendelse().getFamilieHendelseDato()).isEqualTo(fødselsDato);
    }
}
