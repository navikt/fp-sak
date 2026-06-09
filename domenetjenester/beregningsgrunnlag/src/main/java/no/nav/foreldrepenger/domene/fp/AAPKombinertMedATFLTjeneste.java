package no.nav.foreldrepenger.domene.fp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;

@ApplicationScoped
public class AAPKombinertMedATFLTjeneste {

    private BeregningTjeneste beregningTjeneste;

    protected AAPKombinertMedATFLTjeneste() {
        // CDI
    }

    @Inject
    public AAPKombinertMedATFLTjeneste(BeregningTjeneste beregningTjeneste) {
        this.beregningTjeneste = beregningTjeneste;
    }

    public boolean harAAPKombinertMedATFL(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        if (!FagsakYtelseType.FORELDREPENGER.equals(ref.fagsakYtelseType()) || stp.getSkjæringstidspunktHvisUtledet().isEmpty()) {
            return false;
        }
        var bg = beregningTjeneste.hent(ref).flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag).orElse(null);

        if (bg == null || bg.isOverstyrt()) {
            return false;
        }

        var statuser = bg.getAktivitetStatuser().stream().map(BeregningsgrunnlagAktivitetStatus::getAktivitetStatus).toList();

        return statuser.contains(AktivitetStatus.ARBEIDSAVKLARINGSPENGER) && statuser.stream().anyMatch(s -> s.erArbeidstaker() || s.erFrilanser());
    }
}
