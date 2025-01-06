package no.nav.foreldrepenger.domene.modell;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.modell.kodeverk.Hjemmel;
import no.nav.foreldrepenger.domene.typer.Beløp;

public class Beregningsgrunnlag {

    private LocalDate skjæringstidspunkt;
    private BesteberegningGrunnlag besteberegningGrunnlag;
    private Beløp grunnbeløp;
    private boolean overstyrt = false;
    private final List<BeregningsgrunnlagAktivitetStatus> aktivitetStatuser = new ArrayList<>();
    private List<BeregningsgrunnlagPeriode> beregningsgrunnlagPerioder = new ArrayList<>();
    private final List<SammenligningsgrunnlagPrStatus> sammenligningsgrunnlagPrStatusListe = new ArrayList<>();
    private final List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller = new ArrayList<>();

    private Beregningsgrunnlag() {
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public Optional<BesteberegningGrunnlag> getBesteberegningGrunnlag() {
        return Optional.ofNullable(besteberegningGrunnlag);
    }

    public List<BeregningsgrunnlagAktivitetStatus> getAktivitetStatuser() {
        return Collections.unmodifiableList(aktivitetStatuser);
    }

    public List<BeregningsgrunnlagPeriode> getBeregningsgrunnlagPerioder() {
        return beregningsgrunnlagPerioder
            .stream()
            .sorted(Comparator.comparing(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPeriodeFom))
            .toList();
    }

    public Beløp getGrunnbeløp() {
        return grunnbeløp;
    }

    void leggTilBeregningsgrunnlagAktivitetStatus(BeregningsgrunnlagAktivitetStatus bgAktivitetStatus) {
        Objects.requireNonNull(bgAktivitetStatus, "beregningsgrunnlagAktivitetStatus");
        aktivitetStatuser.remove(bgAktivitetStatus);
        aktivitetStatuser.add(bgAktivitetStatus);
    }

    void leggTilBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode bgPeriode) {
        Objects.requireNonNull(bgPeriode, "beregningsgrunnlagPeriode");
        if (!beregningsgrunnlagPerioder.contains(bgPeriode)) {
            beregningsgrunnlagPerioder.add(bgPeriode);
        }
    }

    void leggTilSammenligningsgrunnlagPrStatus(SammenligningsgrunnlagPrStatus sammenligningsgrunnlagPrStatus) {
        Objects.requireNonNull(sammenligningsgrunnlagPrStatus, "sammenligningsgrunnlagPrStatus");
        verifiserIngenLikeSammenligningsgrunnlag(sammenligningsgrunnlagPrStatus);
        if (!sammenligningsgrunnlagPrStatusListe.contains(sammenligningsgrunnlagPrStatus)) {
            sammenligningsgrunnlagPrStatusListe.add(sammenligningsgrunnlagPrStatus);
        }
    }


    public Hjemmel getHjemmel() {
        if (aktivitetStatuser.isEmpty()) {
            return Hjemmel.UDEFINERT;
        }
        if (aktivitetStatuser.size() == 1) {
            return aktivitetStatuser.get(0).getHjemmel();
        }
        var dagpenger = aktivitetStatuser.stream()
            .filter(as -> Hjemmel.F_14_7_8_49.equals(as.getHjemmel()))
            .findFirst();
        if (dagpenger.isPresent()) {
            return dagpenger.get().getHjemmel();
        }
        var gjelder = aktivitetStatuser.stream()
            .filter(as -> !Hjemmel.F_14_7.equals(as.getHjemmel()))
            .findFirst();
        return gjelder.isPresent() ? gjelder.get().getHjemmel() : Hjemmel.F_14_7;
    }

    public List<FaktaOmBeregningTilfelle> getFaktaOmBeregningTilfeller() {
        return Collections.unmodifiableList(faktaOmBeregningTilfeller);
    }

    public List<SammenligningsgrunnlagPrStatus> getSammenligningsgrunnlagPrStatusListe() {
        return sammenligningsgrunnlagPrStatusListe;
    }

    public boolean isOverstyrt() {
        return overstyrt;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof Beregningsgrunnlag)) {
            return false;
        }
        var other = (Beregningsgrunnlag) obj;
        return Objects.equals(this.getSkjæringstidspunkt(), other.getSkjæringstidspunkt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(skjæringstidspunkt);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<"
            + "skjæringstidspunkt=" + skjæringstidspunkt + ", "
            + ", grunnbeløp=" + grunnbeløp
            + ", beregningsgrunnlagPerioder=" + this.beregningsgrunnlagPerioder
            + ">";
    }

    private void verifiserIngenLikeSammenligningsgrunnlag(SammenligningsgrunnlagPrStatus sammenligningsgrunnlagPrStatus) {
        var finnesAlleredeSGAForStatus = sammenligningsgrunnlagPrStatusListe.stream()
            .anyMatch(sg -> sg.getSammenligningsgrunnlagType().equals(sammenligningsgrunnlagPrStatus.getSammenligningsgrunnlagType()));
        if (finnesAlleredeSGAForStatus) {
            throw new IllegalStateException("FEIL: Prøver legge til sammenligningsgrunnlag med status " + sammenligningsgrunnlagPrStatus.getSammenligningsgrunnlagType() +
                " men et sammenligningsgrunnlag med denne statusen finnes allerede på grunnlaget!");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(Beregningsgrunnlag original) {
        return new Builder(original);
    }

    public static class Builder {
        private boolean built;
        private final Beregningsgrunnlag kladd;

        private Builder() {
            kladd = new Beregningsgrunnlag();
        }

        private Builder(Beregningsgrunnlag original) {
            kladd = original;
        }

        public Builder medSkjæringstidspunkt(LocalDate skjæringstidspunkt) {
            verifiserKanModifisere();
            kladd.skjæringstidspunkt = skjæringstidspunkt;
            return this;
        }

        public Builder medGrunnbeløp(BigDecimal grunnbeløp) {
            verifiserKanModifisere();
            kladd.grunnbeløp = new Beløp(grunnbeløp);
            return this;
        }

        public Builder medGrunnbeløp(Beløp grunnbeløp) {
            verifiserKanModifisere();
            kladd.grunnbeløp = grunnbeløp;
            return this;
        }

        public Builder leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus aktivitetStatus) {
            verifiserKanModifisere();
            kladd.leggTilBeregningsgrunnlagAktivitetStatus(aktivitetStatus);
            return this;
        }

        public Builder leggTilBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {
            verifiserKanModifisere();
            kladd.leggTilBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode);
            return this;
        }

        public Builder leggTilFaktaOmBeregningTilfelle(FaktaOmBeregningTilfelle tilfelle) {
            if (!kladd.faktaOmBeregningTilfeller.contains(tilfelle)) {
                this.kladd.faktaOmBeregningTilfeller.add(tilfelle);
            }
            return this;
        }

        public Builder leggTilSammenligningsgrunnlagPrStatus(SammenligningsgrunnlagPrStatus sammenligningsgrunnlagPrStatus) {
            verifiserKanModifisere();
            this.kladd.leggTilSammenligningsgrunnlagPrStatus(sammenligningsgrunnlagPrStatus);
            return this;
        }

        public Builder medBesteberegningsgrunnlag(BesteberegningGrunnlag besteberegningGrunnlag) {
            verifiserKanModifisere();
            kladd.besteberegningGrunnlag = besteberegningGrunnlag;
            return this;
        }

        public Builder medOverstyring(boolean overstyrt) {
            verifiserKanModifisere();
            kladd.overstyrt = overstyrt;
            return this;
        }

        public Beregningsgrunnlag build() {
            verifyStateForBuild();
            built = true;
            return kladd;
        }

        private void verifiserKanModifisere() {
            if (built) {
                throw new IllegalStateException("Er allerede bygd, kan ikke oppdatere videre: " + this.kladd);
            }
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(kladd.skjæringstidspunkt, "skjæringstidspunkt");
        }
    }
}
