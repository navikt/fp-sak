package no.nav.foreldrepenger.datavarehus.domene;

import java.time.LocalDateTime;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "AksjonspunktDvh")
@Table(name = "AKSJONSPUNKT_DVH")
public class AksjonspunktDvh extends DvhBaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_AKSJONSPUNKT_DVH")
    @Column(name="TRANS_ID")
    private Long id;

    @Column(name = "BEHANDLING_STEG_ID")
    private Long behandlingStegId;

    @Column(name = "AKSJONSPUNKT_ID", nullable = false)
    private Long aksjonspunktId;

    @Column(name = "BEHANDLING_ID", nullable = false)
    private Long behandlingId;

    @Column(name = "BEHANDLENDE_ENHET_KODE")
    private String behandlendeEnhetKode;

    @Column(name = "ANSVARLIG_BESLUTTER")
    private String ansvarligBeslutter;

    @Column(name = "ANSVARLIG_SAKSBEHANDLER")
    private String ansvarligSaksbehandler;

    @Column(name = "AKSJONSPUNKT_DEF", nullable = false)
    private String aksjonspunktDef;

    @Column(name = "AKSJONSPUNKT_STATUS", nullable = false)
    private String aksjonspunktStatus;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "TOTRINN_BEHANDLING")
    private boolean toTrinnsBehandling;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "TOTRINN_BEHANDLING_GODKJENT")
    private Boolean toTrinnsBehandlingGodkjent;

    @Column(name = "frist_tid")
    private LocalDateTime fristTid;

    @Column(name="vent_aarsak")
    private String venteårsak;

    AksjonspunktDvh() {
        // Hibernate
    }

    public Long getId() {
        return id;
    }

    public Long getBehandlingStegId() {
        return behandlingStegId;
    }

    public Long getAksjonspunktId() {
        return aksjonspunktId;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public String getBehandlendeEnhetKode() {
        return behandlendeEnhetKode;
    }

    public String getAnsvarligBeslutter() {
        return ansvarligBeslutter;
    }

    public String getAnsvarligSaksbehandler() {
        return ansvarligSaksbehandler;
    }

    public String getAksjonspunktDef() {
        return aksjonspunktDef;
    }

    public String getAksjonspunktStatus() {
        return aksjonspunktStatus;
    }

    public boolean isToTrinnsBehandling() {
        return toTrinnsBehandling;
    }

    public Boolean getToTrinnsBehandlingGodkjent() {
        return toTrinnsBehandlingGodkjent;
    }

    public LocalDateTime getFristTid() {
        return fristTid;
    }

    public String getVenteårsak() {
        return venteårsak;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AksjonspunktDvh other)) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        return Objects.equals(behandlingStegId, other.behandlingStegId)
                && Objects.equals(aksjonspunktId, other.aksjonspunktId)
                && Objects.equals(behandlingId, other.behandlingId)
                && Objects.equals(behandlendeEnhetKode, other.behandlendeEnhetKode)
                && Objects.equals(ansvarligBeslutter, other.ansvarligBeslutter)
                && Objects.equals(ansvarligSaksbehandler, other.ansvarligSaksbehandler)
                && Objects.equals(aksjonspunktDef, other.aksjonspunktDef)
                && Objects.equals(aksjonspunktStatus, other.aksjonspunktStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), behandlingStegId, aksjonspunktId, behandlingId,
                behandlendeEnhetKode, ansvarligBeslutter, ansvarligSaksbehandler, aksjonspunktDef, aksjonspunktStatus);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        AksjonspunktDvh kladd = new AksjonspunktDvh();

        public Builder behandlingStegId(Long behandlingStegId) {
            this.kladd.behandlingStegId = behandlingStegId;
            return this;
        }

        public Builder aksjonspunktId(Long aksjonspunktId) {
            this.kladd.aksjonspunktId = aksjonspunktId;
            return this;
        }

        public Builder behandlingId(Long behandlingId) {
            this.kladd.behandlingId = behandlingId;
            return this;
        }

        public Builder behandlendeEnhetKode(String behandlendeEnhetKode) {
            this.kladd.behandlendeEnhetKode = behandlendeEnhetKode;
            return this;
        }

        public Builder ansvarligBeslutter(String ansvarligBeslutter) {
            this.kladd.ansvarligBeslutter = ansvarligBeslutter;
            return this;
        }

        public Builder ansvarligSaksbehandler(String ansvarligSaksbehandler) {
            this.kladd.ansvarligSaksbehandler = ansvarligSaksbehandler;
            return this;
        }

        public Builder aksjonspunktDef(String aksjonspunktDef) {
            this.kladd.aksjonspunktDef = aksjonspunktDef;
            return this;
        }

        public Builder aksjonspunktStatus(String aksjonspunktStatus) {
            this.kladd.aksjonspunktStatus = aksjonspunktStatus;
            return this;
        }

        public Builder funksjonellTid(LocalDateTime funksjonellTid) {
            this.kladd.setFunksjonellTid(funksjonellTid);
            return this;
        }

        public Builder endretAv(String endretAv) {
            this.kladd.setEndretAv(endretAv);
            return this;
        }

        public Builder toTrinnsBehandling(boolean toTrinnsBehandling) {
            this.kladd.toTrinnsBehandling = toTrinnsBehandling;
            return this;
        }

        public Builder toTrinnsBehandlingGodkjent(Boolean toTrinnsBehandlingGodkjent) {
            this.kladd.toTrinnsBehandlingGodkjent = toTrinnsBehandlingGodkjent;
            return this;
        }

        public Builder fristTid(LocalDateTime fristTid) {
            this.kladd.fristTid = fristTid;
            return this;
        }

        public Builder venteårsak(String venteårsak) {
            this.kladd.venteårsak = venteårsak;
            return this;
        }

        public AksjonspunktDvh build() {
            return kladd;
        }
    }
}
