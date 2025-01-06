package no.nav.foreldrepenger.domene.modell;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class BesteberegningGrunnlag {
    private static final int ANTALL_BESTEBEREGNING_MÅNEDER = 6;

    private List<BesteberegningMånedsgrunnlag> seksBesteMåneder = new ArrayList<>();
    private BigDecimal avvik;

    public List<BesteberegningMånedsgrunnlag> getSeksBesteMåneder() {
        return seksBesteMåneder.stream().sorted(Comparator.comparing(b -> b.getPeriode().getFomDato())).toList();
    }

    public Optional<BigDecimal> getAvvik() {
        return Optional.ofNullable(avvik);
    }

    private void leggTilMånedsgrunnlag(BesteberegningMånedsgrunnlag månedsgrunnlagEntitet) {
        if (seksBesteMåneder.size() >= ANTALL_BESTEBEREGNING_MÅNEDER) {
            throw new IllegalStateException("Kan ikke legge til mer en + " +
                ANTALL_BESTEBEREGNING_MÅNEDER + " måneder for bestebergning");
        }
        if (seksBesteMåneder.stream().anyMatch(m -> m.getPeriode().overlapper(månedsgrunnlagEntitet.getPeriode()))) {
            throw new IllegalStateException("Det finnes allerede et månedsgrunnlag for " + månedsgrunnlagEntitet.getPeriode());
        }
        this.seksBesteMåneder.add(månedsgrunnlagEntitet);
    }

    public static Builder ny() {
        return new Builder();
    }

    public static class Builder {
        private final BesteberegningGrunnlag kladd;

        public Builder() {
            kladd = new BesteberegningGrunnlag();
        }

        public Builder leggTilMånedsgrunnlag(BesteberegningMånedsgrunnlag månedsgrunnlagEntitet) {
            kladd.leggTilMånedsgrunnlag(månedsgrunnlagEntitet);
            return this;
        }

        public Builder medAvvik(BigDecimal avvik) {
            kladd.avvik = avvik;
            return this;
        }

        public BesteberegningGrunnlag build() {
            return kladd;
        }

    }

}
