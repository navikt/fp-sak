package no.nav.foreldrepenger.behandlingslager.behandling.anke;

import jakarta.persistence.*;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;

import java.util.Objects;
import java.util.Optional;


@Entity(name = "AnkeResultat")
@Table(name = "ANKE_RESULTAT")
public class AnkeResultatEntitet extends BaseEntitet {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ANKE_RESULTAT")
    private Long id;

    @Column(name = "anke_behandling_id", nullable = false)
    private Long ankeBehandlingId;

    @Column(name = "paa_anket_behandling_id")
    private Long påAnketKlageBehandlingId;

    @Column(name = "kabal_referanse") // Klageinstansens saksbehandlingssystem
    private String kabalReferanse;

    public AnkeResultatEntitet() {
        // Hibernate
    }

    private AnkeResultatEntitet(AnkeResultatEntitet entitet) {
        this.ankeBehandlingId = entitet.ankeBehandlingId;
        this.påAnketKlageBehandlingId = entitet.påAnketKlageBehandlingId;
        this.kabalReferanse = entitet.kabalReferanse;
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

    public Optional<Long> getPåAnketKlageBehandlingId() {
        return Optional.ofNullable(påAnketKlageBehandlingId);
    }

    public void settPåAnketKlageBehandling(Long påAnketKlageBehandlingId) {
        this.påAnketKlageBehandlingId = påAnketKlageBehandlingId;
    }

    public String getKabalReferanse() {
        return kabalReferanse;
    }

    public boolean erBehandletAvKabal() {
        return kabalReferanse != null;
    }

    public void setKabalReferanse(String kabalReferanse) {
        this.kabalReferanse = kabalReferanse;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof AnkeResultatEntitet other)) {
            return false;
        }
        return Objects.equals(this.ankeBehandlingId, other.ankeBehandlingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ankeBehandlingId);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            (id != null ? "id=" + id + ", " : "")
            + "ankeBehandlingId=" + getAnkeBehandlingId() + ", "
            + ">";
    }

    public static class Builder {
        private final AnkeResultatEntitet ankeResultatEntitetMal;

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

        public Builder medPåAnketKlageBehandlingId(Long påAnketKlageBehandlingId) {
            ankeResultatEntitetMal.påAnketKlageBehandlingId = påAnketKlageBehandlingId;
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
