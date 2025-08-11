package no.nav.foreldrepenger.behandlingslager.behandling.eøs;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseCreateableEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

@Entity(name = "EøsUttaksperiode")
@Table(name = "EOS_UTTAKSPERIODE")
public class EøsUttaksperiodeEntitet extends BaseCreateableEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_EOS_UTTAKSPERIODE")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "eos_uttaksperioder_id", updatable = false, nullable = false)
    private EøsUttaksperioderEntitet eosUttaksperioder;

    @Embedded
    private DatoIntervallEntitet periode;

    @Convert(converter = UttakPeriodeType.KodeverdiConverter.class)
    @Column(name="trekkonto", nullable = false)
    private UttakPeriodeType trekkonto = UttakPeriodeType.UDEFINERT;

    @Embedded
    @AttributeOverride(name = "verdi", column = @Column(name = "trekkdager"))
    private Trekkdager trekkdager;

    EøsUttaksperiodeEntitet() {
        // For Hibernate
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public UttakPeriodeType getTrekkonto() {
        return trekkonto;
    }

    public Trekkdager getTrekkdager() {
        return trekkdager;
    }

    public void setEosUttaksperioder(EøsUttaksperioderEntitet eosUttaksperioder) {
        this.eosUttaksperioder = eosUttaksperioder;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        EøsUttaksperiodeEntitet that = (EøsUttaksperiodeEntitet) o;
        return Objects.equals(periode, that.periode) && trekkonto == that.trekkonto && Objects.equals(trekkdager, that.trekkdager);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, trekkonto, trekkdager);
    }

    public static class Builder {
        private final EøsUttaksperiodeEntitet kladd;

        public Builder() {
            kladd = new EøsUttaksperiodeEntitet();
        }

        public Builder medPeriode(DatoIntervallEntitet periode) {
            kladd.periode = periode;
            return this;
        }

        public Builder medPeriode(LocalDate fom, LocalDate tom) {
            return medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        }

        public Builder medTrekkonto(UttakPeriodeType trekkonto) {
            kladd.trekkonto = trekkonto;
            return this;
        }

        public Builder medTrekkdager(Trekkdager trekkdager) {
            kladd.trekkdager = trekkdager;
            return this;
        }

        public EøsUttaksperiodeEntitet build() {
            Objects.requireNonNull(kladd.periode, "periode");
            Objects.requireNonNull(kladd.trekkonto, "trekkonto");
            Objects.requireNonNull(kladd.trekkdager, "trekkdager");
            return kladd;
        }
    }
}
