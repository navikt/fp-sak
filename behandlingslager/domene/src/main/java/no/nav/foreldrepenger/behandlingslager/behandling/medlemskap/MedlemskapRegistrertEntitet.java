package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;

@Entity(name = "MedlemskapRegistrert")
@Table(name = "MEDLEMSKAP_REGISTRERT")
public class MedlemskapRegistrertEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MEDLEMSKAP_REGISTRERT")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @OneToMany(cascade = {CascadeType.ALL}, orphanRemoval = true /* ok med orphanremoval siden perioder er eid av denne */)
    @JoinColumn(name = "MEDLEMSKAP_REGISTRERT_ID")
    @ChangeTracked
    private Set<MedlemskapPerioderEntitet> medlemskapPerioder = new HashSet<>(2);

    MedlemskapRegistrertEntitet() {
        // default tom entitet for hibernate
    }

    /**
     * deep copy av MedlemskapRegistrertEntitet.
     */
    MedlemskapRegistrertEntitet(MedlemskapRegistrertEntitet medlemskapRegistrert) {
        this(Collections.unmodifiableCollection(medlemskapRegistrert.getMedlemskapPerioder()));
    }

    MedlemskapRegistrertEntitet(Collection<MedlemskapPerioderEntitet> registrertMedlemskapPerioder) {
        for (var rmp : registrertMedlemskapPerioder) {
            var periode = new MedlemskapPerioderEntitet(rmp);
            this.medlemskapPerioder.add(periode);
        }
    }

    Set<MedlemskapPerioderEntitet> getMedlemskapPerioder() {
        return Collections.unmodifiableSet(medlemskapPerioder);
    }

}
