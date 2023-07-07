package no.nav.foreldrepenger.datavarehus.domene;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "BehandlingStegDvh")
@Table(name = "BEHANDLING_STEG_DVH")
public class BehandlingStegDvh extends DvhBaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BEHANDLING_STEG_DVH")
    @Column(name="TRANS_ID")
    private Long id;

    @Column(name = "BEHANDLING_STEG_ID")
    private Long behandlingStegId;

    @Column(name = "BEHANDLING_ID", nullable = false)
    private Long behandlingId;

    @Column(name = "BEHANDLING_STEG_TYPE")
    private String behandlingStegType;

    @Column(name = "BEHANDLING_STEG_STATUS")
    private String behandlingStegStatus;

    BehandlingStegDvh() {
        // Hibernate
    }

    public Long getId() {
        return id;
    }

    public Long getBehandlingStegId() {
        return behandlingStegId;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public String getBehandlingStegType() {
        return behandlingStegType;
    }

    public String getBehandlingStegStatus() {
        return behandlingStegStatus;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BehandlingStegDvh castOther)) {
            return false;
        }
        if (!super.equals(other)) {
            return false;
        }
        return Objects.equals(behandlingStegId, castOther.behandlingStegId)
                && Objects.equals(behandlingId, castOther.behandlingId)
                && Objects.equals(behandlingStegType, castOther.behandlingStegType)
                && Objects.equals(behandlingStegStatus, castOther.behandlingStegStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), behandlingStegId, behandlingId, behandlingStegType, behandlingStegStatus);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long behandlingStegId;
        private Long behandlingId;
        private String behandlingStegType;
        private String behandlingStegStatus;
        private LocalDateTime funksjonellTid;
        private String endretAv;

        public Builder behandlingStegId(Long behandlingStegId) {
            this.behandlingStegId = behandlingStegId;
            return this;
        }

        public Builder behandlingId(Long behandlingId) {
            this.behandlingId = behandlingId;
            return this;
        }

        public Builder behandlingStegType(String behandlingStegType) {
            this.behandlingStegType = behandlingStegType;
            return this;
        }

        public Builder behandlingStegStatus(String behandlingStegStatus) {
            this.behandlingStegStatus = behandlingStegStatus;
            return this;
        }

        public Builder funksjonellTid(LocalDateTime funksjonellTid) {
            this.funksjonellTid = funksjonellTid;
            return this;
        }

        public Builder endretAv(String endretAv) {
            this.endretAv = endretAv;
            return this;
        }

        public BehandlingStegDvh build() {
            var behandlingStegDvh = new BehandlingStegDvh();
            behandlingStegDvh.behandlingStegId = behandlingStegId;
            behandlingStegDvh.behandlingId = behandlingId;
            behandlingStegDvh.behandlingStegType = behandlingStegType;
            behandlingStegDvh.behandlingStegStatus = behandlingStegStatus;
            behandlingStegDvh.setFunksjonellTid(funksjonellTid);
            behandlingStegDvh.setEndretAv(endretAv);
            return behandlingStegDvh;
        }
    }
}
