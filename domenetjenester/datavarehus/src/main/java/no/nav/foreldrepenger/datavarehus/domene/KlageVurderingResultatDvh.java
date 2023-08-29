package no.nav.foreldrepenger.datavarehus.domene;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "KlageVurderingResultatDvh")
@Table(name = "KLAGE_VURDERING_RESULTAT_DVH")
public class KlageVurderingResultatDvh extends DvhBaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_KLAGE_VURDERING_RES_DVH")
    @Column(name = "TRANS_ID")
    private Long id;

    @Column(name = "klage_behandling_id", nullable = false, updatable = false)
    private Long klageBehandlingId;

    @Column(name = "klage_vurdert_av")
    private String klageVurdertAv;

    @Column(name = "klagevurdering")
    private String klageVurdering;

    @Column(name = "klage_medhold_aarsak")
    private String klageMedholdÅrsak;

    @Column(name = "klage_vurdering_omgjoer")
    private String klageVurderingOmgjør;

    @Column(name = "klage_hjemmel")
    private String klageHjemmel;

    @Column(name = "opprettet_tid", nullable = false, updatable = false)
    private LocalDateTime opprettetTidspunkt;

    KlageVurderingResultatDvh() {
        // hibernate
    }

    public Long getId() {
        return id;
    }

    public Long getKlageBehandlingId() {
        return klageBehandlingId;
    }

    public String getKlageVurdertAv() {
        return klageVurdertAv;
    }

    public String getKlageVurdering() {
        return klageVurdering;
    }

    public String getKlageMedholdÅrsak() {
        return klageMedholdÅrsak;
    }

    public String getKlageVurderingOmgjør() {
        return klageVurderingOmgjør;
    }

    public String getKlageHjemmel() {
        return klageHjemmel;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        var that = (KlageVurderingResultatDvh) o;
        return Objects.equals(klageBehandlingId, that.klageBehandlingId) &&
            Objects.equals(klageVurdertAv, that.klageVurdertAv) &&
            Objects.equals(klageVurdering, that.klageVurdering) &&
            Objects.equals(klageMedholdÅrsak, that.klageMedholdÅrsak) &&
            Objects.equals(klageVurderingOmgjør, that.klageVurderingOmgjør) &&
            Objects.equals(klageHjemmel, that.klageHjemmel) &&
            Objects.equals(opprettetTidspunkt, that.opprettetTidspunkt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), klageBehandlingId, klageVurdertAv, klageVurdering, klageMedholdÅrsak, klageVurderingOmgjør, klageHjemmel, opprettetTidspunkt);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private KlageVurderingResultatDvh klageVurderingResultatDvh = new KlageVurderingResultatDvh();

        public Builder medKlageBehandlingId(Long klageBehandlingId) {
            klageVurderingResultatDvh.klageBehandlingId = klageBehandlingId;
            return this;
        }

        public Builder medKlageVurdertAv(String klageVurdertAv) {
            klageVurderingResultatDvh.klageVurdertAv = klageVurdertAv;
            return this;
        }

        public Builder medKlageVurdering(String klageVurdering) {
            klageVurderingResultatDvh.klageVurdering = klageVurdering;
            return this;
        }

        public Builder medKlageMedholdÅrsak(String klageMedholdÅrsak) {
            klageVurderingResultatDvh.klageMedholdÅrsak = klageMedholdÅrsak;
            return this;
        }

        public Builder medKlageVurderingOmgjør(String klageVurderingOmgjør) {
            klageVurderingResultatDvh.klageVurderingOmgjør = klageVurderingOmgjør;
            return this;
        }

        public Builder medKlageHjemmel(String klageHjemmel) {
            klageVurderingResultatDvh.klageHjemmel = klageHjemmel;
            return this;
        }

        public Builder medOpprettetTidspunkt(LocalDateTime opprettetTidspunkt) {
            klageVurderingResultatDvh.opprettetTidspunkt = opprettetTidspunkt;
            return this;
        }

        public KlageVurderingResultatDvh build() {
            return this.klageVurderingResultatDvh;
        }
    }
}
