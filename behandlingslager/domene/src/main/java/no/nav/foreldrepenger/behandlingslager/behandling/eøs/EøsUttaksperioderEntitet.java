package no.nav.foreldrepenger.behandlingslager.behandling.eøs;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseCreateableEntitet;

@Entity(name = "EøsUttaksperioder")
@Table(name = "EOS_UTTAKSPERIODER")
public class EøsUttaksperioderEntitet extends BaseCreateableEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_EOS_UTTAKSPERIODER")
    private Long id;

    @OneToMany(mappedBy = "eosUttaksperioder")
    private List<EøsUttaksperiodeEntitet> perioder = new ArrayList<>();

    EøsUttaksperioderEntitet() {
        // For Hibernate
    }

    public List<EøsUttaksperiodeEntitet> getPerioder() {
        return perioder;
    }

    void leggTilEøsUttaksperiode(EøsUttaksperiodeEntitet eøsUttaksperiode) {
        eøsUttaksperiode.setEosUttaksperioder(this);
        perioder.add(eøsUttaksperiode);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        EøsUttaksperioderEntitet that = (EøsUttaksperioderEntitet) o;
        return Objects.equals(perioder, that.perioder);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(perioder);
    }

    public static class Builder {
        private final EøsUttaksperioderEntitet kladd;

        public Builder() {
            this.kladd = new EøsUttaksperioderEntitet();
        }

        public Builder leggTil(List<EøsUttaksperiodeEntitet> perioder) {
            perioder.forEach(kladd::leggTilEøsUttaksperiode);
            return this;
        }

        public EøsUttaksperioderEntitet build() {
            return this.kladd;
        }

    }
}
