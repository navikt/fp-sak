package no.nav.foreldrepenger.domene.modell;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import no.nav.foreldrepenger.domene.modell.kodeverk.PeriodeÅrsak;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.vedtak.konfig.Tid;


public class BeregningsgrunnlagPeriode {

    private final List<BeregningsgrunnlagPrStatusOgAndel> beregningsgrunnlagPrStatusOgAndelList = new ArrayList<>();
    private ÅpenDatoIntervallEntitet periode;
    private BigDecimal bruttoPrÅr;
    private BigDecimal avkortetPrÅr;
    private BigDecimal redusertPrÅr;
    private Long dagsats;
    private List<PeriodeÅrsak> periodeårsaker = new ArrayList<>();

    private BeregningsgrunnlagPeriode() {
    }

    public List<BeregningsgrunnlagPrStatusOgAndel> getBeregningsgrunnlagPrStatusOgAndelList() {
        return Collections.unmodifiableList(beregningsgrunnlagPrStatusOgAndelList);
    }

    public ÅpenDatoIntervallEntitet getPeriode() {
        if (periode.getTomDato() == null) {
            return ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(periode.getFomDato(), Tid.TIDENES_ENDE);
        }
        return periode;
    }

    public LocalDate getBeregningsgrunnlagPeriodeFom() {
        return periode.getFomDato();
    }

    public LocalDate getBeregningsgrunnlagPeriodeTom() {
        return periode.getTomDato();
    }

    public BigDecimal getBeregnetPrÅr() {
        return beregningsgrunnlagPrStatusOgAndelList.stream()
                .map(BeregningsgrunnlagPrStatusOgAndel::getBeregnetPrÅr)
                .filter(Objects::nonNull)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
    }

    public BigDecimal getBruttoPrÅr() {
        return bruttoPrÅr;
    }

    public BigDecimal getAvkortetPrÅr() {
        return avkortetPrÅr;
    }

    public BigDecimal getRedusertPrÅr() {
        return redusertPrÅr;
    }

    public Long getDagsats() {
        return dagsats;
    }

    public List<PeriodeÅrsak> getPeriodeÅrsaker() {
        return Collections.unmodifiableList(periodeårsaker);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof BeregningsgrunnlagPeriode)) {
            return false;
        }
        var other = (BeregningsgrunnlagPeriode) obj;
        return Objects.equals(this.periode.getFomDato(), other.periode.getFomDato())
                && Objects.equals(this.periode.getTomDato(), other.periode.getTomDato())
                && Objects.equals(this.getBruttoPrÅr(), other.getBruttoPrÅr())
                && Objects.equals(this.getAvkortetPrÅr(), other.getAvkortetPrÅr())
                && Objects.equals(this.getRedusertPrÅr(), other.getRedusertPrÅr())
                && Objects.equals(this.getDagsats(), other.getDagsats());
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, bruttoPrÅr, avkortetPrÅr, redusertPrÅr, dagsats);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<"
                + "periode=" + periode + ", "
                + "bruttoPrÅr=" + bruttoPrÅr + ", "
                + "avkortetPrÅr=" + avkortetPrÅr + ", "
                + "redusertPrÅr=" + redusertPrÅr + ", "
                + "dagsats=" + dagsats + ", "
                + ">";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final BeregningsgrunnlagPeriode kladd;
        private boolean built;

        public Builder() {
            kladd = new BeregningsgrunnlagPeriode();
        }

        public Builder(BeregningsgrunnlagPeriode eksisterendeBeregningsgrunnlagPeriod) {
            kladd = eksisterendeBeregningsgrunnlagPeriod;
        }

        public Builder leggTilBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPrStatusOgAndel beregningsgrunnlagPrStatusOgAndel) {
            verifiserKanModifisere();
            kladd.beregningsgrunnlagPrStatusOgAndelList.add(beregningsgrunnlagPrStatusOgAndel);
            return this;
        }

        public Builder medBeregningsgrunnlagPeriode(LocalDate fraOgMed, LocalDate tilOgMed) {
            verifiserKanModifisere();
            kladd.periode = ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);
            return this;
        }

        public Builder medBruttoPrÅr(BigDecimal bruttoPrÅr) {
            verifiserKanModifisere();
            kladd.bruttoPrÅr = bruttoPrÅr;
            return this;
        }

        public Builder medAvkortetPrÅr(BigDecimal avkortetPrÅr) {
            verifiserKanModifisere();
            kladd.avkortetPrÅr = avkortetPrÅr;
            return this;
        }

        public Builder medRedusertPrÅr(BigDecimal redusertPrÅr) {
            verifiserKanModifisere();
            kladd.redusertPrÅr = redusertPrÅr;
            return this;
        }

        public Builder medDagsats(Long dagsats) {
            verifiserKanModifisere();
            kladd.dagsats = dagsats;
            return this;
        }

        public Builder leggTilPeriodeÅrsak(PeriodeÅrsak periodeÅrsak) {
            verifiserKanModifisere();
            if (!kladd.getPeriodeÅrsaker().contains(periodeÅrsak)) {
                kladd.periodeårsaker.add(periodeÅrsak);
            }
            return this;
        }

        public BeregningsgrunnlagPeriode build() {
            verifyStateForBuild();
            verifiserAndelsnr();
            built = true;
            return kladd;
        }

        private void verifiserKanModifisere() {
            if(built) {
                throw new IllegalStateException("Er allerede bygd, kan ikke oppdatere videre: " + this.kladd);
            }
        }

        private void verifiserAndelsnr() {
            Set<Long> andelsnrIBruk = new HashSet<>();
            kladd.beregningsgrunnlagPrStatusOgAndelList.stream()
                .map(BeregningsgrunnlagPrStatusOgAndel::getAndelsnr)
                .forEach(andelsnr -> {
                    if (andelsnrIBruk.contains(andelsnr)) {
                        throw new IllegalStateException("Utviklerfeil: Kan ikke bygge andel. Andelsnr eksisterer allerede på en annen andel i samme bgPeriode.");
                    }
                    andelsnrIBruk.add(andelsnr);
                });
        }

        private void verifyStateForBuild() {
            Objects.requireNonNull(kladd.beregningsgrunnlagPrStatusOgAndelList, "beregningsgrunnlagPrStatusOgAndelList");
            Objects.requireNonNull(kladd.periode, "beregningsgrunnlagPeriodeFom");
        }

    }


}
