package no.nav.foreldrepenger.behandlingslager.behandling.beregning;

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;

@Entity(name = "LegacyESBeregningResultat")
@Table(name = "BR_LEGACY_ES_BEREGNING_RES")
public class LegacyESBeregningsresultat extends BaseEntitet {

    @OneToMany(mappedBy = "beregningResultat")
    private Set<LegacyESBeregning> beregninger = new HashSet<>();

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BR_LEGACY_ES_BEREGNING")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    LegacyESBeregningsresultat() {
        // for hibernate
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof LegacyESBeregningsresultat other)) {
            return false;
        }
        return Objects.equals(beregninger, other.beregninger);
    }

    public boolean isOverstyrt() {
        return beregninger.stream().anyMatch(LegacyESBeregning::isOverstyrt);
    }

    // Returnerer immuterbar liste
    public List<LegacyESBeregning> getBeregninger() {
        return new ArrayList<>(beregninger);
    }

    public Optional<LegacyESBeregning> getSisteBeregning() {
        // tar overstyrt over siste
        var comparator = Comparator
                .comparing(LegacyESBeregning::isOverstyrt)
                .thenComparing(LegacyESBeregning::getBeregnetTidspunkt)
                .reversed()
                ;
        return beregninger.stream().min(comparator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(beregninger);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<>";
    }

    void setBeregninger(Set<LegacyESBeregning> beregninger) {
        this.beregninger = beregninger;
    }

    /**
     * Builder for å modifisere et beregningResultat.
     */
    public static class Builder {

        private LegacyESBeregningsresultat eksisterendeResultat;
        private boolean modifisert;
        private Set<LegacyESBeregning> oppdaterteBeregninger = new HashSet<>();
        private Set<LegacyESBeregning> originaleBeregninger = new HashSet<>();
        private LegacyESBeregningsresultat resultatMal = new LegacyESBeregningsresultat();

        Builder() {
            super();
        }

        Builder(LegacyESBeregningsresultat eksisterendeResultat) {
            super();
            this.eksisterendeResultat = eksisterendeResultat;
            if (eksisterendeResultat != null) {
                this.originaleBeregninger.addAll(eksisterendeResultat.getBeregninger());
            }
        }

        public LegacyESBeregningsresultat buildFor(Behandling behandling, Behandlingsresultat resultat) {
            var behandlingsresultat = resultat != null ? resultat : Behandlingsresultat.builderForBeregningResultat().buildFor(behandling);
            return buildFor(behandlingsresultat);
        }

        public LegacyESBeregningsresultat.Builder medBeregning(LegacyESBeregning beregning) {
            this.modifisert = true;
            this.oppdaterteBeregninger.add(beregning);
            return this;
        }

        public LegacyESBeregningsresultat.Builder nullstillBeregninger() {
            this.modifisert = true;
            this.originaleBeregninger.clear();
            return this;
        }

        /**
         * Bygg nytt resultat for angitt behandlingsresultat.
         *
         * @return Returner nytt resultat HVIS det oprettes.
         */
        private LegacyESBeregningsresultat buildFor(Behandlingsresultat behandlingsresultat) {
            if (eksisterendeResultat != null) {
                if (!modifisert) {
                    // samme behandling som originalt, oppdaterer original
                    return eksisterendeResultat; // samme som før
                }
                oppdaterBeregninger(behandlingsresultat.getBehandlingId(), eksisterendeResultat);
                return eksisterendeResultat;
            }
            oppdaterBeregninger(behandlingsresultat.getBehandlingId(), resultatMal);
            behandlingsresultat.medOppdatertBeregningResultat(resultatMal);
            return resultatMal;
        }

        private void oppdaterBeregninger(Long behandlingId, LegacyESBeregningsresultat resultat) {
            var nye = oppdaterteBeregninger.stream()
                .map(beregning -> new LegacyESBeregning(behandlingId, resultat, beregning.getSatsVerdi(), beregning.getAntallBarn(),
                    beregning.getBeregnetTilkjentYtelse(), beregning.getBeregnetTidspunkt(), beregning.isOverstyrt(),
                    beregning.getOpprinneligBeregnetTilkjentYtelse()))
                .collect(toSet());
            var urørte = this.originaleBeregninger.stream()
                .filter(beregning -> !oppdaterteBeregninger.contains(beregning))
                .collect(toSet());
            nye.addAll(urørte);

            resultat.setBeregninger(nye);
        }
    }

    public static LegacyESBeregningsresultat.Builder builder() {
        return new LegacyESBeregningsresultat.Builder();
    }

    public static LegacyESBeregningsresultat.Builder builderFraEksisterende(LegacyESBeregningsresultat eksisterendeResultat) {
        return new LegacyESBeregningsresultat.Builder(eksisterendeResultat);
    }
}
