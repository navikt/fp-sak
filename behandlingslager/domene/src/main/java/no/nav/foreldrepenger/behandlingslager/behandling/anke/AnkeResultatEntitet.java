package no.nav.foreldrepenger.behandlingslager.behandling.anke;

import java.util.Objects;
import java.util.Optional;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;


@Entity(name = "AnkeResultat")
@Table(name = "ANKE_RESULTAT")
public class AnkeResultatEntitet extends BaseEntitet {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ANKE_RESULTAT")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "anke_behandling_id", nullable = false, updatable = false)
    private Behandling ankeBehandling;

    @ManyToOne
    @JoinColumn(name = "paa_anket_behandling_id")
    private Behandling påAnketBehandling;

    public AnkeResultatEntitet() {
        // Hibernate
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    public Behandling getAnkeBehandling() {
        return ankeBehandling;
    }

    public Optional<Behandling> getPåAnketBehandling() {
        return påAnketBehandling == null ? Optional.empty() : Optional.of(påAnketBehandling);
    }

    public void settPåAnketBehandling(Behandling behandling) {
        this.påAnketBehandling = behandling;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof AnkeResultatEntitet)) {
            return false;
        }
        AnkeResultatEntitet other = (AnkeResultatEntitet) obj;
        return Objects.equals(this.getId(), other.getId()) //Skal det sammenliknes på id?
            && Objects.equals(this.getAnkeBehandling(), other.getAnkeBehandling());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getAnkeBehandling());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + //$NON-NLS-1$
            (id != null ? "id=" + id + ", " : "") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            + "ankeBehandling=" + getAnkeBehandling() + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + ">"; //$NON-NLS-1$
    }

    public static class Builder {
        private AnkeResultatEntitet ankeResultatEntitetMal;

        public Builder() {
            ankeResultatEntitetMal = new AnkeResultatEntitet();
        }

        public Builder medAnkeBehandling(Behandling ankeBehandling) {
            ankeResultatEntitetMal.ankeBehandling = ankeBehandling;
            return this;
        }

        public Builder medPåAnketBehandling(Behandling påAnketBehandling) {
            ankeResultatEntitetMal.påAnketBehandling = påAnketBehandling;
            return this;
        }


        public AnkeResultatEntitet build() {
            verifyStateForBuild();
            return ankeResultatEntitetMal;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(ankeResultatEntitetMal.ankeBehandling, "AnkeBehandling");
        }
    }
}
