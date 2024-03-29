package no.nav.foreldrepenger.datavarehus.domene;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "BehandlingVedtakDvh")
@Table(name = "BEHANDLING_VEDTAK_DVH")
public class BehandlingVedtakDvh extends DvhBaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BEHANDLING_VEDTAK_DVH")
    @Column(name="TRANS_ID")
    private Long id;

    @Column(name = "VEDTAK_ID", nullable = false)
    private Long vedtakId;

    @Column(name = "BEHANDLING_ID", nullable = false)
    private Long behandlingId;

    @Column(name = "OPPRETTET_DATO", nullable = false)
    private LocalDate opprettetDato;

    @Column(name = "VEDTAK_DATO", nullable = false)
    private LocalDate vedtakDato;

    @Column(name = "VEDTAK_TID")
    private LocalDateTime vedtakTid;

    @Column(name = "IVERKSETTING_STATUS", nullable = false)
    private String iverksettingStatus;

    @Column(name = "GODKJENNENDE_ENHET")
    private String godkjennendeEnhet;

    @Column(name = "ANSVARLIG_SAKSBEHANDLER")
    private String ansvarligSaksbehandler;

    @Column(name = "ANSVARLIG_BESLUTTER")
    private String ansvarligBeslutter;

    @Column(name = "VEDTAK_RESULTAT_TYPE_KODE")
    public String vedtakResultatTypeKode;

    @Column(name = "UTBETALT_TID")
    private LocalDate utbetaltTid;

    @Column(name = "VILKAR_IKKE_OPPFYLT")
    private String vilkårIkkeOppfylt;

    BehandlingVedtakDvh() {
        // Hibernate
    }

    public Long getId() {
        return id;
    }

    public Long getVedtakId() {
        return vedtakId;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public LocalDate getOpprettetDato() {
        return opprettetDato;
    }

    public LocalDate getVedtakDato() {
        return vedtakDato;
    }

    public LocalDateTime getVedtakTid() {
        return vedtakTid;
    }

    public String getIverksettingStatus() {
        return iverksettingStatus;
    }

    public String getGodkjennendeEnhet() {
        return godkjennendeEnhet;
    }

    public String getAnsvarligSaksbehandler() {
        return ansvarligSaksbehandler;
    }

    public String getAnsvarligBeslutter() {
        return ansvarligBeslutter;
    }

    public String getVedtakResultatTypeKode() {
        return vedtakResultatTypeKode;
    }

    public LocalDate getUtbetaltTid() {
        return utbetaltTid;
    }

    public String getVilkårIkkeOppfylt() {
        return vilkårIkkeOppfylt;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BehandlingVedtakDvh castOther)) {
            return false;
        }
        if (!super.equals(other)) {
            return false;
        }
        return Objects.equals(vedtakId, castOther.vedtakId)
            && Objects.equals(behandlingId, castOther.behandlingId)
                && Objects.equals(opprettetDato, castOther.opprettetDato)
            && Objects.equals(vedtakDato, castOther.vedtakDato)
                && Objects.equals(iverksettingStatus, castOther.iverksettingStatus)
                && Objects.equals(godkjennendeEnhet, castOther.godkjennendeEnhet)
                && Objects.equals(ansvarligSaksbehandler, castOther.ansvarligSaksbehandler)
                && Objects.equals(ansvarligBeslutter, castOther.ansvarligBeslutter)
            && Objects.equals(vedtakTid, castOther.vedtakTid)
            && Objects.equals(utbetaltTid, castOther.utbetaltTid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), vedtakId, behandlingId, opprettetDato, vedtakDato, iverksettingStatus, godkjennendeEnhet,
                ansvarligSaksbehandler, ansvarligBeslutter, vedtakTid, utbetaltTid);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long vedtakId;
        private Long behandlingId;
        private LocalDate opprettetDato;
        private LocalDate vedtakDato;
        private LocalDateTime vedtakTid;
        private String iverksettingStatus;
        private String godkjennendeEnhet;
        private String ansvarligSaksbehandler;
        private String ansvarligBeslutter;
        private LocalDateTime funksjonellTid;
        private String endretAv;
        private String vedtakResultatTypeKode;
        private LocalDate utbetaltTid;
        private String vilkårIkkeOppfylt;

        public Builder vedtakId(Long vedtakId) {
            this.vedtakId = vedtakId;
            return this;
        }

        public Builder behandlingId(Long behandlingId) {
            this.behandlingId = behandlingId;
            return this;
        }

        public Builder opprettetDato(LocalDate opprettetDato) {
            this.opprettetDato = opprettetDato;
            return this;
        }

        public Builder vedtakDato(LocalDate vedtakDato) {
            this.vedtakDato = vedtakDato;
            return this;
        }

        public Builder vedtakTid(LocalDateTime vedtakTid) {
            this.vedtakTid = vedtakTid;
            return this;
        }

        public Builder iverksettingStatus(String iverksettingStatus) {
            this.iverksettingStatus = iverksettingStatus;
            return this;
        }

        public Builder godkjennendeEnhet(String godkjennendeEnhet) {
            this.godkjennendeEnhet = godkjennendeEnhet;
            return this;
        }

        public Builder ansvarligSaksbehandler(String ansvarligSaksbehandler) {
            this.ansvarligSaksbehandler = ansvarligSaksbehandler;
            return this;
        }

        public Builder ansvarligBeslutter(String ansvarligBeslutter) {
            this.ansvarligBeslutter = ansvarligBeslutter;
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

        public Builder vedtakResultatTypeKode(String vedtakResultatTypeKode) {
            this.vedtakResultatTypeKode = vedtakResultatTypeKode;
            return this;
        }

        public Builder utbetaltTid(LocalDate utbetaltTid) {
            this.utbetaltTid = utbetaltTid;
            return this;
        }

        public Builder vilkårIkkeOppfylt(VilkårIkkeOppfylt vilkårIkkeOppfylt) {
            this.vilkårIkkeOppfylt = vilkårIkkeOppfylt != null ? vilkårIkkeOppfylt.name() : null;
            return this;
        }

        public BehandlingVedtakDvh build() {
            var vedtak = new BehandlingVedtakDvh();
            vedtak.ansvarligBeslutter = ansvarligBeslutter;
            vedtak.ansvarligSaksbehandler = ansvarligSaksbehandler;
            vedtak.behandlingId = behandlingId;
            vedtak.godkjennendeEnhet = godkjennendeEnhet;
            vedtak.iverksettingStatus = iverksettingStatus;
            vedtak.opprettetDato = opprettetDato;
            vedtak.vedtakId = vedtakId;
            vedtak.vedtakDato = vedtakDato;
            vedtak.vedtakResultatTypeKode = vedtakResultatTypeKode;
            vedtak.vedtakTid = vedtakTid;
            vedtak.utbetaltTid = utbetaltTid;
            vedtak.vilkårIkkeOppfylt = vilkårIkkeOppfylt;
            vedtak.setFunksjonellTid(funksjonellTid);
            vedtak.setEndretAv(endretAv);
            return vedtak;
        }


    }
}
