package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
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
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;

@Entity(name = "VurdertMedlemskapPeriode")
@Table(name = "MEDLEMSKAP_VURDERING_PERIODE")
public class VurdertMedlemskapPeriodeEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MEDLEMSKAP_VP")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @OneToMany(mappedBy = "periodeHolder")
    @ChangeTracked
    private Set<VurdertLøpendeMedlemskapEntitet> perioder = new HashSet<>();

    public VurdertMedlemskapPeriodeEntitet() {
        // hibernate
    }

    public Set<VurdertLøpendeMedlemskapEntitet> getPerioder() {
        return Collections.unmodifiableSet(perioder);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        var that = (VurdertMedlemskapPeriodeEntitet) o;
        return Objects.equals(perioder, that.perioder);
    }


    @Override
    public int hashCode() {
        return Objects.hash(perioder);
    }
}
