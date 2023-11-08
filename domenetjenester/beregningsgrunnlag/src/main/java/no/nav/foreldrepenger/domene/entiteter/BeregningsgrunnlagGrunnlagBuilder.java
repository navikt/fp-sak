package no.nav.foreldrepenger.domene.entiteter;

import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;


public class BeregningsgrunnlagGrunnlagBuilder {
    private BeregningsgrunnlagGrunnlagEntitet kladd;
    private boolean built;

    private BeregningsgrunnlagGrunnlagBuilder(BeregningsgrunnlagGrunnlagEntitet kladd) {
        this.kladd = kladd;
    }

    public static BeregningsgrunnlagGrunnlagBuilder nytt() {
        return new BeregningsgrunnlagGrunnlagBuilder(new BeregningsgrunnlagGrunnlagEntitet());
    }

    public static BeregningsgrunnlagGrunnlagBuilder oppdatere(BeregningsgrunnlagGrunnlagEntitet kladd) {
        return new BeregningsgrunnlagGrunnlagBuilder(kladd);
    }

    public static BeregningsgrunnlagGrunnlagBuilder kopi(BeregningsgrunnlagGrunnlagEntitet kladd) {
        return new BeregningsgrunnlagGrunnlagBuilder(new BeregningsgrunnlagGrunnlagEntitet(kladd));
    }

    public static BeregningsgrunnlagGrunnlagBuilder oppdatere(Optional<BeregningsgrunnlagGrunnlagEntitet> kladd) {
        return kladd.map(BeregningsgrunnlagGrunnlagBuilder::oppdatere).orElseGet(BeregningsgrunnlagGrunnlagBuilder::nytt);
    }

    public static BeregningsgrunnlagGrunnlagBuilder kopi(Optional<BeregningsgrunnlagGrunnlagEntitet> kladd) {
        return kladd.map(BeregningsgrunnlagGrunnlagBuilder::kopi).orElseGet(BeregningsgrunnlagGrunnlagBuilder::nytt);
    }

    public BeregningsgrunnlagEntitet.Builder getBeregningsgrunnlagBuilder() {
        return BeregningsgrunnlagEntitet.Builder.oppdater(kladd.getBeregningsgrunnlag());
    }

    public BeregningsgrunnlagGrunnlagBuilder medBeregningsgrunnlag(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        verifiserKanModifisere();
        kladd.setBeregningsgrunnlag(beregningsgrunnlag);
        return this;
    }

    public BeregningsgrunnlagGrunnlagBuilder medRegisterAktiviteter(BeregningAktivitetAggregatEntitet registerAktiviteter) {
        verifiserKanModifisere();
        kladd.setRegisterAktiviteter(registerAktiviteter);
        return this;
    }

    public BeregningsgrunnlagGrunnlagBuilder medRefusjonOverstyring(BeregningRefusjonOverstyringerEntitet beregningRefusjonOverstyringer){
        verifiserKanModifisere();
        kladd.setRefusjonOverstyringer(beregningRefusjonOverstyringer);
        return this;
    }

    public BeregningsgrunnlagGrunnlagBuilder medSaksbehandletAktiviteter(BeregningAktivitetAggregatEntitet saksbehandletAktiviteter) {
        verifiserKanModifisere();
        kladd.setSaksbehandletAktiviteter(saksbehandletAktiviteter);
        return this;
    }

    public BeregningsgrunnlagGrunnlagEntitet build(Long behandlingId, BeregningsgrunnlagTilstand beregningsgrunnlagTilstand) {
        if(built) {
            return kladd;
        }
        Objects.requireNonNull(behandlingId);
        Objects.requireNonNull(beregningsgrunnlagTilstand);
        kladd.setBehandlingId(behandlingId);
        kladd.setBeregningsgrunnlagTilstand(beregningsgrunnlagTilstand);
        built = true;
        return kladd;
    }

    public BeregningsgrunnlagGrunnlagEntitet buildUtenIdOgTilstand() {
        return kladd;
    }

    public BeregningsgrunnlagGrunnlagBuilder medOverstyring(BeregningAktivitetOverstyringerEntitet beregningAktivitetOverstyringer) {
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
