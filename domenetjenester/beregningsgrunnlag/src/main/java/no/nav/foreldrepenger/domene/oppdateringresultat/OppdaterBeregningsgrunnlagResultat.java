package no.nav.foreldrepenger.domene.oppdateringresultat;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public class OppdaterBeregningsgrunnlagResultat {

    private LocalDate skjæringstidspunkt;
    private final UUID referanse;
    private final BeregningsgrunnlagEndring beregningsgrunnlagEndring;
    private final BeregningAktiviteterEndring beregningAktiviteterEndring;
    private final FaktaOmBeregningVurderinger faktaOmBeregningVurderinger;
    private final VarigEndretNæringVurdering varigEndretNæringVurdering;
    private final RefusjonoverstyringEndring refusjonoverstyringEndring;

    public OppdaterBeregningsgrunnlagResultat(BeregningsgrunnlagEndring beregningsgrunnlagEndring,
                                              BeregningAktiviteterEndring beregningAktiviteterEndring,
                                              FaktaOmBeregningVurderinger faktaOmBeregningVurderinger,
                                              VarigEndretNæringVurdering varigEndretNæringVurdering,
                                              RefusjonoverstyringEndring refusjonoverstyringEndring,
                                              UUID referanse) {
        this.beregningsgrunnlagEndring = beregningsgrunnlagEndring;
        this.beregningAktiviteterEndring = beregningAktiviteterEndring;
        this.faktaOmBeregningVurderinger = faktaOmBeregningVurderinger;
        this.refusjonoverstyringEndring = refusjonoverstyringEndring;
        this.referanse = referanse;
        this.varigEndretNæringVurdering = varigEndretNæringVurdering;
    }

    public Optional<BeregningsgrunnlagEndring> getBeregningsgrunnlagEndring() {
        return Optional.ofNullable(beregningsgrunnlagEndring);
    }

    public BeregningAktiviteterEndring getBeregningAktiviteterEndring() {
        return beregningAktiviteterEndring;
    }

    public Optional<FaktaOmBeregningVurderinger> getFaktaOmBeregningVurderinger() {
        return Optional.ofNullable(faktaOmBeregningVurderinger);
    }

    public Optional<VarigEndretNæringVurdering> getVarigEndretNæringVurdering() {
        return Optional.ofNullable(varigEndretNæringVurdering);
    }

    public Optional<RefusjonoverstyringEndring> getRefusjonoverstyringEndring() {
        return Optional.of(refusjonoverstyringEndring);
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public UUID getReferanse() {
        return referanse;
    }


    public void setSkjæringstidspunkt(LocalDate skjæringstidspunkt) {
        this.skjæringstidspunkt = skjæringstidspunkt;
    }

}
