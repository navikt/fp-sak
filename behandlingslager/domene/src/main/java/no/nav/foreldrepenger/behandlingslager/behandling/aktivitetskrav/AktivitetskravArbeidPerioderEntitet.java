package no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseCreateableEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;

@Entity(name = "AktivitetskravArbeidPerioderEntitet")
@Table(name = "AKTIVITETSKRAV_ARBEID_PERIODER")
public class AktivitetskravArbeidPerioderEntitet extends BaseCreateableEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_AKTIVITETSKRAV_ARBEID_PERIODER")
    private Long id;

    @ChangeTracked
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "aktivitetskravArbeidPerioder")
    private List<AktivitetskravArbeidPeriodeEntitet> aktivitetskravArbeidPeriodeListe = new ArrayList<>();

    public AktivitetskravArbeidPerioderEntitet() {
        // CDI
    }

    public List<AktivitetskravArbeidPeriodeEntitet> getAktivitetskravArbeidPeriodeListe() {
        return aktivitetskravArbeidPeriodeListe != null ? Collections.unmodifiableList(aktivitetskravArbeidPeriodeListe) : List.of();
    }

    void leggTilAKtivitetskravArbeidPeriode(AktivitetskravArbeidPeriodeEntitet arbeidPeriode) {
        arbeidPeriode.setAktivitetskravArbeidPerioder(this);
        aktivitetskravArbeidPeriodeListe.add(arbeidPeriode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AktivitetskravArbeidPerioderEntitet that = (AktivitetskravArbeidPerioderEntitet) o;
        return Objects.equals(id, that.id) && Objects.equals(aktivitetskravArbeidPeriodeListe, that.aktivitetskravArbeidPeriodeListe);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, aktivitetskravArbeidPeriodeListe);
    }

    public static class Builder {
        private final AktivitetskravArbeidPerioderEntitet kladd;

        public Builder() {
            this.kladd = new AktivitetskravArbeidPerioderEntitet();
        }

        public Builder leggTil(AktivitetskravArbeidPeriodeEntitet.Builder builder) {
            kladd.leggTilAKtivitetskravArbeidPeriode(builder.build());
            return this;
        }

        public AktivitetskravArbeidPerioderEntitet build() {
            return this.kladd;
        }

    }
}
