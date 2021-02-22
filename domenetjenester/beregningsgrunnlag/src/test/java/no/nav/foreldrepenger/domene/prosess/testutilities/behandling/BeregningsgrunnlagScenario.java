package no.nav.foreldrepenger.domene.prosess.testutilities.behandling;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.domene.prosess.RepositoryProvider;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagTilstand;

public class BeregningsgrunnlagScenario implements TestScenarioTillegg {

    private BeregningsgrunnlagEntitet.Builder beregningsgrunnlagBuilder;

    BeregningsgrunnlagScenario() {
        this.beregningsgrunnlagBuilder = BeregningsgrunnlagEntitet.ny();
    }

    BeregningsgrunnlagEntitet.Builder getBeregningsgrunnlagBuilder() {
        return beregningsgrunnlagBuilder;
    }

    @Override
    public void lagre(Behandling behandling, RepositoryProvider repositoryProvider) {
        BeregningsgrunnlagRepository beregningsgrunnlagRepository = repositoryProvider.getBeregningsgrunnlagRepository();
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningsgrunnlagBuilder.build();
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.OPPRETTET);
    }
}
