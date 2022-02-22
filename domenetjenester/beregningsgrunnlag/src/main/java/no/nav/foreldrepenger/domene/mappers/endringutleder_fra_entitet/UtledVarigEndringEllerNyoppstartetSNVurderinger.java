package no.nav.foreldrepenger.domene.mappers.endringutleder_fra_entitet;

import java.util.Optional;

import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.OppgittEgenNæring;
import no.nav.foreldrepenger.domene.oppdateringresultat.ToggleEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.VarigEndretNæringVurdering;

public class UtledVarigEndringEllerNyoppstartetSNVurderinger {

    private UtledVarigEndringEllerNyoppstartetSNVurderinger() {
        // Skjul
    }

    public static VarigEndretNæringVurdering utled(BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                                                   Optional<BeregningsgrunnlagEntitet> forrigeBeregningsgrunnlagOpt,
                                                   InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var bgPeriode = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        var forrigePeriode = forrigeBeregningsgrunnlagOpt.map(bg -> bg.getBeregningsgrunnlagPerioder().get(0));
        var næringAndel = finnNæring(bgPeriode)
                .orElseThrow(() -> new IllegalStateException("Forventer å finne andel for næring ved vurdering av varig endret næring."));
        var forrigeNæring = forrigePeriode.flatMap(UtledVarigEndringEllerNyoppstartetSNVurderinger::finnNæring);
        boolean tilVerdi = næringAndel.getOverstyrtPrÅr() != null;
        Boolean fraVerdi = forrigeNæring.isPresent() && forrigeNæring.get().getOverstyrtPrÅr() != null ? true : null;
        ToggleEndring endring = new ToggleEndring(fraVerdi, tilVerdi);
        boolean harOppgittVarigEndring = iayGrunnlag.getOppgittOpptjening().stream()
                .flatMap(o -> o.getEgenNæring().stream())
                .anyMatch(OppgittEgenNæring::getVarigEndring);
        boolean harOppgittNyoppstartet = iayGrunnlag.getOppgittOpptjening().stream()
                .flatMap(o -> o.getEgenNæring().stream())
                .anyMatch(OppgittEgenNæring::getNyoppstartet);
        return new VarigEndretNæringVurdering(
                harOppgittVarigEndring ? endring : null,
                harOppgittNyoppstartet ? endring : null);
    }

    private static Optional<BeregningsgrunnlagPrStatusOgAndel> finnNæring(BeregningsgrunnlagPeriode bgPeriode) {
        return bgPeriode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
                .filter(a -> a.getAktivitetStatus().erSelvstendigNæringsdrivende())
                .findFirst();
    }

}
