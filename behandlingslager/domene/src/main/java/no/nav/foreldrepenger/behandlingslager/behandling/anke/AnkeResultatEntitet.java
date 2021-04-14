package no.nav.foreldrepenger.behandlingslager.behandling.anke;

import java.util.Objects;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;


@Entity(name = "AnkeResultat")
@Table(name = "ANKE_RESULTAT")
public class AnkeResultatEntitet extends BaseEntitet {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ANKE_RESULTAT")
    private Long id;

    @Column(name = "anke_behandling_id", nullable = false)
    private Long ankeBehandlingId;

    @Column(name = "paa_anket_behandling_id")
    private Long påAnketBehandlingId;

    public AnkeResultatEntitet() {
        // Hibernate
    }

    private AnkeResultatEntitet(AnkeResultatEntitet entitet) {
        this.ankeBehandlingId = entitet.ankeBehandlingId;
        this.påAnketBehandlingId = entitet.påAnketBehandlingId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(AnkeResultatEntitet ankeResultatEntitet) {
        return new Builder(ankeResultatEntitet);
    }

    public Long getId() {
        return id;
    }

    public Long getAnkeBehandlingId() {
        return ankeBehandlingId;
    }

    public Optional<Long> getPåAnketBehandlingId() {
        return Optional.ofNullable(påAnketBehandlingId);
    }

    public void settPåAnketBehandling(Long behandlingId) {
        this.påAnketBehandlingId = behandlingId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof AnkeResultatEntitet)) {
            return false;
        }
        var other = (AnkeResultatEntitet) obj;
        return Objects.equals(this.ankeBehandlingId, other.ankeBehandlingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ankeBehandlingId);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + //$NON-NLS-1$
            (id != null ? "id=" + id + ", " : "") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            + "ankeBehandlingId=" + getAnkeBehandlingId() + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + ">"; //$NON-NLS-1$
    }

    public static class Builder {
        private AnkeResultatEntitet ankeResultatEntitetMal;

        private Builder() {
            ankeResultatEntitetMal = new AnkeResultatEntitet();
        }

        private Builder(AnkeResultatEntitet ankeResultatEntitet) {
            ankeResultatEntitetMal = new AnkeResultatEntitet(ankeResultatEntitet);
        }

        public Builder medAnkeBehandlingId(Long ankeBehandlingId) {
            ankeResultatEntitetMal.ankeBehandlingId = ankeBehandlingId;
            return this;
        }

        public Builder medPåAnketBehandlingId(Long påAnketBehandlingId) {
            ankeResultatEntitetMal.påAnketBehandlingId = påAnketBehandlingId;
            return this;
        }


        public AnkeResultatEntitet build() {
            verifyStateForBuild();
            return ankeResultatEntitetMal;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(ankeResultatEntitetMal.ankeBehandlingId, "AnkeBehandlingId");
        }
    }
}
