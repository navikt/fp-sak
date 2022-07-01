package no.nav.foreldrepenger.domene.modell;

import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;

public class BeregningsgrunnlagGrunnlagBuilder {
    private BeregningsgrunnlagGrunnlag kladd;
    private boolean built;

    private BeregningsgrunnlagGrunnlagBuilder(BeregningsgrunnlagGrunnlag kladd) {
        this.kladd = kladd;
    }

    public static BeregningsgrunnlagGrunnlagBuilder nytt() {
        return new BeregningsgrunnlagGrunnlagBuilder(new BeregningsgrunnlagGrunnlag());
    }

    public static BeregningsgrunnlagGrunnlagBuilder oppdatere(BeregningsgrunnlagGrunnlag kladd) {
        return new BeregningsgrunnlagGrunnlagBuilder(new BeregningsgrunnlagGrunnlag(kladd));
    }

    public static BeregningsgrunnlagGrunnlagBuilder oppdatere(Optional<BeregningsgrunnlagGrunnlag> kladd) {
        return kladd.map(BeregningsgrunnlagGrunnlagBuilder::oppdatere).orElseGet(BeregningsgrunnlagGrunnlagBuilder::nytt);
    }

    public BeregningsgrunnlagGrunnlagBuilder medBeregningsgrunnlag(Beregningsgrunnlag beregningsgrunnlag) {
        verifiserKanModifisere();
        kladd.setBeregningsgrunnlag(beregningsgrunnlag);
        return this;
    }

    public BeregningsgrunnlagGrunnlagBuilder medFakta(FaktaAggregat faktaAggregat) {
        verifiserKanModifisere();
        kladd.setFaktaAggregat(faktaAggregat);
        return this;
    }


    public BeregningsgrunnlagGrunnlagBuilder medRegisterAktiviteter(BeregningAktivitetAggregat registerAktiviteter) {
        verifiserKanModifisere();
        kladd.setRegisterAktiviteter(registerAktiviteter);
        return this;
    }

    public BeregningsgrunnlagGrunnlagBuilder medRefusjonOverstyring(BeregningRefusjonOverstyringer beregningRefusjonOverstyringer){
        verifiserKanModifisere();
        kladd.setRefusjonOverstyringer(beregningRefusjonOverstyringer);
        return this;
    }

    public BeregningsgrunnlagGrunnlagBuilder medSaksbehandletAktiviteter(BeregningAktivitetAggregat saksbehandletAktiviteter) {
        verifiserKanModifisere();
        kladd.setSaksbehandletAktiviteter(saksbehandletAktiviteter);
        return this;
    }

    public BeregningsgrunnlagGrunnlag build(BeregningsgrunnlagTilstand beregningsgrunnlagTilstand) {
        if(built) {
            return kladd;
        }
        Objects.requireNonNull(beregningsgrunnlagTilstand);
        kladd.setBeregningsgrunnlagTilstand(beregningsgrunnlagTilstand);
        built = true;
        return kladd;
    }

    public BeregningsgrunnlagGrunnlagBuilder medOverstyring(BeregningAktivitetOverstyringer beregningAktivitetOverstyringer) {
        verifiserKanModifisere();
        kladd.setOverstyringer(beregningAktivitetOverstyringer);
        return this;
    }

    private void verifiserKanModifisere() {
        if(built) {
            throw new IllegalStateException("Er allerede bygd, kan ikke oppdatere videre: " + this.kladd);
        }
    }
}
