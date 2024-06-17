package no.nav.foreldrepenger.behandlingslager.kodeverk;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity(name = "LagretKodeverdiNavn")
@Table(name = "KODEVERDI_NAVN")
public class LagretKodeverdiNavn implements Serializable {

    @Id
    @Column(name = "KODEVERK", nullable = false)
    private String kodeverk;

    @Id
    @Column(name = "KODE", nullable = false)
    private String kode;

    @Column(name = "NAVN")
    private String navn;

    @Column(name = "OPPRETTET_TID", nullable = false, updatable = false)
    private LocalDateTime opprettetTidspunkt;

    @Column(name = "ENDRET_TID")
    private LocalDateTime endretTidspunkt;

    @PrePersist
    protected void onCreate() {
        this.opprettetTidspunkt = opprettetTidspunkt != null ? opprettetTidspunkt : LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        endretTidspunkt = LocalDateTime.now();
    }

    LagretKodeverdiNavn() {
        // Hibernate
    }

    public String getKodeverk() {
        return kodeverk;
    }

    public String getKode() {
        return kode;
    }

    public String getNavn() {
        return navn;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    public LocalDateTime getEndretTidspunkt() {
        return endretTidspunkt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (LagretKodeverdiNavn) o;
        return kodeverk.equals(that.kodeverk) && kode.equals(that.kode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kodeverk, kode);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private LagretKodeverdiNavn kladd = new LagretKodeverdiNavn();

        public Builder kodeverk(String kodeverk) {
            this.kladd.kodeverk = kodeverk;
            return this;
        }

        public Builder kode(String kode) {
            this.kladd.kode = kode;
            return this;
        }

        public Builder navn(String navn) {
            this.kladd.navn = navn;
            return this;
        }

        public LagretKodeverdiNavn build() {
            Objects.requireNonNull(kladd.kodeverk, "Mangler kodeverk");
            Objects.requireNonNull(kladd.kode, "Mangler kode");
            return kladd;
        }
    }
}
