package no.nav.foreldrepenger.behandlingslager.behandling.beregning;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.fasterxml.jackson.annotation.JsonBackReference;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

@Entity(name = "BeregningsresultatPeriode")
@Table(name = "BR_PERIODE")
public class BeregningsresultatPeriode extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BR_PERIODE")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @ManyToOne(optional = false)
    @JoinColumn(name = "BEREGNINGSRESULTAT_FP_ID", nullable = false, updatable = false)
    @JsonBackReference
    private BeregningsresultatEntitet beregningsresultat;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "beregningsresultatPeriode", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private List<BeregningsresultatAndel> beregningsresultatAndelList = new ArrayList<>();

    @Embedded
    @AttributeOverride(name = "fomDato", column = @Column(name = "br_periode_fom"))
    @AttributeOverride(name = "tomDato", column = @Column(name = "br_periode_tom"))
    private DatoIntervallEntitet periode;

    public BeregningsresultatPeriode() {
    }

    public Long getId() {
        return id;
    }

    public LocalDate getBeregningsresultatPeriodeFom() {
        return periode.getFomDato();
    }

    public LocalDate getBeregningsresultatPeriodeTom() {
        return periode.getTomDato();
    }

    public List<BeregningsresultatAndel> getBeregningsresultatAndelList() {
        return Collections.unmodifiableList(beregningsresultatAndelList);
    }

    public BeregningsresultatEntitet getBeregningsresultat() {
        return beregningsresultat;
    }

    void addBeregningsresultatAndel(BeregningsresultatAndel beregningsresultatAndel) {
        Objects.requireNonNull(beregningsresultatAndel, "beregningsresultatAndel");
        if (!beregningsresultatAndelList.contains(beregningsresultatAndel)) {  // Class defines List based fields but uses them like Sets: Ingening å tjene på å bytte til Set ettersom det er små lister
            beregningsresultatAndelList.add(beregningsresultatAndel);
        }
    }

    public int getDagsats() {
        return getBeregningsresultatAndelList().stream()
            .mapToInt(BeregningsresultatAndel::getDagsats)
            .sum();
    }

    public int getDagsatsFraBg() {
        return getBeregningsresultatAndelList().stream()
            .mapToInt(BeregningsresultatAndel::getDagsatsFraBg)
            .sum();
    }

    public Optional<BigDecimal> getLavestUtbetalingsgrad() {
        return getBeregningsresultatAndelList().stream()
            .filter(a -> a.getDagsats() > 0)
            .map(BeregningsresultatAndel::getUtbetalingsgrad)
            .min(Comparator.naturalOrder());
    }

    public BigDecimal getKalkulertUtbetalingsgrad() {
        var utbetalingsgrader = getBeregningsresultatAndelList().stream()
            .map(BeregningsresultatAndel::getUtbetalingsgrad)
            .collect(Collectors.toCollection(TreeSet::new)); // TreeSet bruker compareTo (ref BigDecimal)
        if (utbetalingsgrader.isEmpty()) {
            return BigDecimal.ZERO;
        } else if (utbetalingsgrader.size() == 1) {
            return utbetalingsgrader.getFirst();
        } else {
            // Andeler med ulike utbetalingsgrader, regn ut på tvers av alle andeler
            var dagsats = getDagsats();
            var bgsats = beregningsresultatAndelList.stream()
                .mapToInt(BeregningsresultatAndel::getUtledetDagsatsFraBg)
                .sum();
            if (dagsats == 0 || bgsats == 0)
                return BigDecimal.ZERO;
            return new BigDecimal(100).multiply(new BigDecimal(dagsats)).divide(new BigDecimal(bgsats), 2, RoundingMode.HALF_UP);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof BeregningsresultatPeriode other)) {
            return false;
        }
        return Objects.equals(this.getBeregningsresultatPeriodeFom(), other.getBeregningsresultatPeriodeFom())
            && Objects.equals(this.getBeregningsresultatPeriodeTom(), other.getBeregningsresultatPeriodeTom())
            ;
    }

    @Override
    public String toString() {
        return "BeregningsresultatPeriode{" +
            "beregningsresultatAndelList=" + beregningsresultatAndelList +
            ", periode=" + periode +
            '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(BeregningsresultatPeriode eksisterendeBeregningsresultatPeriode) {
        return new Builder(eksisterendeBeregningsresultatPeriode);
    }

    public static class Builder {
        private BeregningsresultatPeriode beregningsresultatPeriodeMal;

        public Builder() {
            beregningsresultatPeriodeMal = new BeregningsresultatPeriode();
        }

        public Builder(BeregningsresultatPeriode eksisterendeBeregningsresultatPeriode) {
            beregningsresultatPeriodeMal = eksisterendeBeregningsresultatPeriode;
        }

        public Builder medBeregningsresultatPeriodeFomOgTom(LocalDate beregningsresultatPeriodeFom, LocalDate beregningsresultatPeriodeTom) {
            beregningsresultatPeriodeMal.periode = DatoIntervallEntitet.fraOgMedTilOgMed(beregningsresultatPeriodeFom, beregningsresultatPeriodeTom);
            return this;
        }

        public BeregningsresultatPeriode build(BeregningsresultatEntitet beregningsresultat) {
            beregningsresultatPeriodeMal.beregningsresultat = beregningsresultat;
            verifyStateForBuild();
            beregningsresultatPeriodeMal.beregningsresultat.addBeregningsresultatPeriode(beregningsresultatPeriodeMal);
            return beregningsresultatPeriodeMal;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(beregningsresultatPeriodeMal.beregningsresultatAndelList, "beregningsresultatAndeler");
            Objects.requireNonNull(beregningsresultatPeriodeMal.beregningsresultat, "beregningsresultat");
            Objects.requireNonNull(beregningsresultatPeriodeMal.periode, "beregningsresultatPeriodePeriode");
            Objects.requireNonNull(beregningsresultatPeriodeMal.periode.getFomDato(), "beregningsresultaPeriodeFom");
            Objects.requireNonNull(beregningsresultatPeriodeMal.periode.getTomDato(), "beregningsresultaPeriodeTom");
        }
    }
}

