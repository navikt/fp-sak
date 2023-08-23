package no.nav.foreldrepenger.datavarehus.domene;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@MappedSuperclass
public class DvhBaseEntitet implements Serializable {

    @Column(name = "TRANS_TID", nullable = false)
    private LocalDateTime transTid;

    @Column(name = "FUNKSJONELL_TID", nullable = false)
    private LocalDateTime funksjonellTid;

    @Column(name = "ENDRET_AV")
    private String endretAv;

    @PrePersist
    protected void onCreate() {
        this.transTid = LocalDateTime.now();
        if (this.funksjonellTid == null) {
            this.funksjonellTid = LocalDateTime.now();
        }
    }

    public LocalDateTime getFunksjonellTid() {
        return funksjonellTid;
    }

    public void setFunksjonellTid(LocalDateTime funksjonellTid) {
        this.funksjonellTid = funksjonellTid;
    }
    public void setTransTid(LocalDateTime transTid) {
        this.transTid = transTid;
    }

    public String getEndretAv() {
        return endretAv;
    }

    public void setEndretAv(String endretAv) {
        this.endretAv = endretAv;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DvhBaseEntitet other)) {
            return false;
        }
        return Objects.equals(transTid, other.transTid)
                && Objects.equals(funksjonellTid, other.funksjonellTid)
                && Objects.equals(endretAv, other.endretAv);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transTid, funksjonellTid, endretAv);
    }
}
