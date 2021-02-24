package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.testutilities.behandling;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.RepositoryProvider;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagTilstand;

public class BeregningAktivitetScenario implements TestScenarioTillegg {
    private BeregningAktivitetAggregatEntitet.Builder beregningAktiviteterBuilder;

    BeregningAktivitetScenario() {
        this.beregningAktiviteterBuilder = BeregningAktivitetAggregatEntitet.builder();
    }

    BeregningAktivitetAggregatEntitet.Builder getBeregningAktiviteterBuilder() {
        return beregningAktiviteterBuilder;
    }

    @Override
    public void lagre(Behandling behandling, RepositoryProvider repositoryProvider) {
        BeregningsgrunnlagRepository beregningsgrunnlagRepository = repositoryProvider.getBeregningsgrunnlagRepository();
        BeregningAktivitetAggregatEntitet beregningAktiviteter = beregningAktiviteterBuilder.build();
        BeregningsgrunnlagGrunnlagBuilder builder = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty()).medRegisterAktiviteter(beregningAktiviteter);
        beregningsgrunnlagRepository.lagre(behandling.getId(), builder, BeregningsgrunnlagTilstand.OPPRETTET);
    }
}
