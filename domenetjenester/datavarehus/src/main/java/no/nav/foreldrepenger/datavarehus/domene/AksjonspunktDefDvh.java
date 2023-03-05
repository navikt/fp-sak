package no.nav.foreldrepenger.datavarehus.domene;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

@Entity(name = "AksjonspunktDefDvh")
@Table(name = "AKSJONSPUNKT_DEF_DVH")
public class AksjonspunktDefDvh implements Serializable {

    @Id
    @Column(name = "AKSJONSPUNKT_DEF", nullable = false)
    private String aksjonspunktDef;

    @Column(name = "AKSJONSPUNKT_TYPE", nullable = false)
    private String aksjonspunktType;

    @Column(name = "AKSJONSPUNKT_NAVN")
    private String aksjonspunktNavn;

    @Column(name = "opprettet_tid", nullable = false, updatable=false)
    private LocalDateTime opprettetTidspunkt;

    @Column(name = "endret_tid")
    private LocalDateTime endretTidspunkt;

    @PrePersist
    protected void onCreate() {
        this.opprettetTidspunkt = opprettetTidspunkt != null ? opprettetTidspunkt : LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        endretTidspunkt = LocalDateTime.now();
    }

    AksjonspunktDefDvh() {
        // Hibernate
    }

    public String getAksjonspunktDef() {
        return aksjonspunktDef;
    }

    public String getAksjonspunktType() {
        return aksjonspunktType;
    }

    public String getAksjonspunktNavn() {
        return aksjonspunktNavn;
    }

    public void setAksjonspunktNavn(String aksjonspunktNavn) {
        this.aksjonspunktNavn = aksjonspunktNavn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (AksjonspunktDefDvh) o;
        return aksjonspunktDef.equals(that.aksjonspunktDef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aksjonspunktDef);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private AksjonspunktDefDvh kladd = new AksjonspunktDefDvh();

        public Builder aksjonspunktDef(String aksjonspunktDef) {
            this.kladd.aksjonspunktDef = aksjonspunktDef;
            return this;
        }

        public Builder aksjonspunktType(String aksjonspunktType) {
            this.kladd.aksjonspunktType = aksjonspunktType;
            return this;
        }

        public Builder aksjonspunktNavn(String aksjonspunktNavn) {
            this.kladd.aksjonspunktNavn = aksjonspunktNavn;
            return this;
        }

        public AksjonspunktDefDvh build() {
            Objects.requireNonNull(kladd.aksjonspunktDef, "Mangler aksjonspunkdef");
            Objects.requireNonNull(kladd.aksjonspunktType, "Mangler aksjonspuntType");
            return kladd;
        }
    }
}
