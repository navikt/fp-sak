package no.nav.foreldrepenger.behandlingslager.behandling.klage;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;


@Entity(name = "KlageResultat")
@Table(name = "KLAGE_RESULTAT")
public class KlageResultatEntitet extends BaseEntitet {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_KLAGE_RESULTAT")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "klage_behandling_id", nullable = false, updatable = false)
    private Behandling klageBehandling;

    @ManyToOne
    @JoinColumn(name = "paaklagd_behandling_id")
    private Behandling påKlagdBehandling;

    @Column(name = "paaklagd_ekstern_uuid")
    private UUID påKlagdEksternBehandlingUuId;

    public KlageResultatEntitet() {
        // Hibernate
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    public Behandling getKlageBehandling() {
        return klageBehandling;
    }

    public Optional<Behandling> getPåKlagdBehandling() {
        return Optional.ofNullable(påKlagdBehandling);
    }

    public Optional<UUID> getPåKlagdEksternBehandling() {
        return Optional.ofNullable(påKlagdEksternBehandlingUuId);
    }

    public void settPåKlagdBehandling(Behandling behandling) {
        this.påKlagdBehandling = behandling;
    }

    public void settPåKlagdEksternBehandlingId(UUID påKlagdEksternBehandlingId) {
        this.påKlagdEksternBehandlingUuId = påKlagdEksternBehandlingId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof KlageResultatEntitet)) {
            return false;
        }
        KlageResultatEntitet other = (KlageResultatEntitet) obj;
        return Objects.equals(this.getId(), other.getId()) //Skal det sammenliknes på id?
            && Objects.equals(this.getKlageBehandling(), other.getKlageBehandling());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getKlageBehandling());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + //$NON-NLS-1$
            (id != null ? "id=" + id + ", " : "") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            + "klageBehandling=" + getKlageBehandling() + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + ">"; //$NON-NLS-1$
    }

    public static class Builder {
        private KlageResultatEntitet klageResultatEntitetMal;

        public Builder() {
            klageResultatEntitetMal = new KlageResultatEntitet();
        }

        public Builder medKlageBehandling(Behandling klageBehandling) {
            klageResultatEntitetMal.klageBehandling = klageBehandling;
            return this;
        }

        public Builder medPåKlagdBehandling(Behandling påKlagdBehandling) {
            klageResultatEntitetMal.påKlagdBehandling = påKlagdBehandling;
            return this;
        }

        public Builder medPåKlagdEksternBehandling(UUID påKlagdEksternBehandlingId) {
            klageResultatEntitetMal.påKlagdEksternBehandlingUuId = påKlagdEksternBehandlingId;
            return this;
        }


        public KlageResultatEntitet build() {
            verifyStateForBuild();
            return klageResultatEntitetMal;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(klageResultatEntitetMal.klageBehandling, "KlageBehandling");
        }
    }
}
