package no.nav.foreldrepenger.behandlingslager.behandling.klage;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;


@Entity(name = "KlageResultat")
@Table(name = "KLAGE_RESULTAT")
public class KlageResultatEntitet extends BaseEntitet {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_KLAGE_RESULTAT")
    private Long id;

    @Column(name = "klage_behandling_id", nullable = false)
    private Long klageBehandlingId;

    @Column(name = "paaklagd_behandling_id")
    private Long påKlagdBehandlingId;

    @Column(name = "paaklagd_ekstern_uuid")
    private UUID påKlagdEksternBehandlingUuid;

    @Column(name = "kabal_referanse") // Klageinstansens saksbehandlingssystem
    private String kabalReferanse;

    public KlageResultatEntitet() {
        // Hibernate
    }

    private KlageResultatEntitet(KlageResultatEntitet entitet) {
        this.klageBehandlingId = entitet.klageBehandlingId;
        this.påKlagdBehandlingId = entitet.påKlagdBehandlingId;
        this.påKlagdEksternBehandlingUuid = entitet.påKlagdEksternBehandlingUuid;
        this.kabalReferanse = entitet.kabalReferanse;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(KlageResultatEntitet klageResultatEntitet) {
        return new Builder(klageResultatEntitet);
    }

    public Long getId() {
        return id;
    }

    public Long getKlageBehandlingId() {
        return klageBehandlingId;
    }

    public Optional<Long> getPåKlagdBehandlingId() {
        return Optional.ofNullable(påKlagdBehandlingId);
    }

    public Optional<UUID> getPåKlagdEksternBehandlingUuid() {
        return Optional.ofNullable(påKlagdEksternBehandlingUuid);
    }

    public void settPåKlagdBehandlingId(Long behandlingId) {
        this.påKlagdBehandlingId = behandlingId;
    }

    public void settPåKlagdEksternBehandlingUuid(UUID påKlagdEksternBehandlingUuid) {
        this.påKlagdEksternBehandlingUuid = påKlagdEksternBehandlingUuid;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (KlageResultatEntitet) o;
        return Objects.equals(klageBehandlingId, that.klageBehandlingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(klageBehandlingId);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            (id != null ? "id=" + id + ", " : "")
            + "klageBehandlingId=" + getKlageBehandlingId() + ", "
            + ">";
    }

    public static class Builder {
        private KlageResultatEntitet klageResultatEntitetMal;

        private Builder() {
            klageResultatEntitetMal = new KlageResultatEntitet();
        }

        private Builder(KlageResultatEntitet klageResultatEntitet) {
            klageResultatEntitetMal = new KlageResultatEntitet(klageResultatEntitet);
        }

        public Builder medKlageBehandlingId(Long klageBehandlingId) {
            klageResultatEntitetMal.klageBehandlingId = klageBehandlingId;
            return this;
        }

        public Builder medPåKlagdBehandlingId(Long påKlagdBehandlingId) {
            klageResultatEntitetMal.påKlagdBehandlingId = påKlagdBehandlingId;
            return this;
        }

        public Builder medPåKlagdEksternBehandlingUuid(UUID påKlagdEksternBehandlingUuid) {
            klageResultatEntitetMal.påKlagdEksternBehandlingUuid = påKlagdEksternBehandlingUuid;
            return this;
        }

        public KlageResultatEntitet build() {
            verifyStateForBuild();
            return klageResultatEntitetMal;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(klageResultatEntitetMal.klageBehandlingId, "KlageBehandling");
        }
    }
}
