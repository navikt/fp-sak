package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.Objects;
import java.util.UUID;

import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class BehandlingIdFagsakIdAktorId {

    private String fagsakYtelseType;
    private Saksnummer saksnummer;
    private String aktorId;
    private Long behandlingId;
    private UUID behandlingUuid;

    public BehandlingIdFagsakIdAktorId(String fagsakYtelseType, Saksnummer saksnummer, String aktorId, Long behandlingId, UUID behandlingUuid) {
        this.fagsakYtelseType = fagsakYtelseType;
        this.saksnummer = saksnummer;
        this.aktorId = aktorId;
        this.behandlingId = behandlingId;
        this.behandlingUuid = behandlingUuid;
    }

    public String getFagsakYtelseType() {
        return fagsakYtelseType;
    }

    public void setFagsakYtelseType(String fagsakYtelseType) {
        this.fagsakYtelseType = fagsakYtelseType;
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(Saksnummer saksnummer) {
        this.saksnummer = saksnummer;
    }

    public String getAktorId() {
        return aktorId;
    }

    public void setAktorId(String aktorId) {
        this.aktorId = aktorId;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public void setBehandlingUuid(UUID behandlingUuid) {
        this.behandlingUuid = behandlingUuid;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BehandlingIdFagsakIdAktorId that = (BehandlingIdFagsakIdAktorId) o;
        return Objects.equals(fagsakYtelseType, that.fagsakYtelseType) &&
            Objects.equals(saksnummer, that.saksnummer) &&
            Objects.equals(aktorId, that.aktorId) &&
            Objects.equals(behandlingId, that.behandlingId) &&
            Objects.equals(behandlingUuid, that.behandlingUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fagsakYtelseType, saksnummer, aktorId, behandlingId, behandlingUuid);
    }

    @Override
    public String toString() {
        return "BehandlingIdFagsakIdAktorId{" +
            "fagsakYtelseType='" + fagsakYtelseType + '\'' +
            ", saksnummer=" + saksnummer +
            ", aktorId='" + aktorId + '\'' +
            ", behandlingId=" + behandlingId +
            ", behandlingUuid=" + behandlingUuid +
            '}';
    }
}
