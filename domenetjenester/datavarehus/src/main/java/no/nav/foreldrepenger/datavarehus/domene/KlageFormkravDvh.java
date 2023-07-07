package no.nav.foreldrepenger.datavarehus.domene;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "KlageFormkravDvh")
@Table(name = "KLAGE_FORMKRAV_DVH")
public class KlageFormkravDvh extends DvhBaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_KLAGE_FORMKRAV_DVH")
    @Column(name = "TRANS_ID")
    private Long id;

    @Column(name = "klage_behandling_id", nullable = false, updatable = false)
    private Long klageBehandlingId;

    @Column(name = "klage_vurdert_av", nullable = false)
    private String klageVurdertAv;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "gjelder_vedtak", nullable = false)
    private boolean gjelderVedtak;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "er_klager_part", nullable = false)
    private boolean erKlagerPart;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "er_frist_overholdt", nullable = false)
    private boolean erFristOverholdt;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "er_konkret", nullable = false)
    private boolean erKonkret;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "er_signert", nullable = false)
    private boolean erSignert;

    @Column(name = "opprettet_tid", nullable = false, updatable = false)
    private LocalDateTime opprettetTidspunkt;

    private KlageFormkravDvh() {
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

    public boolean gjelderVedtak() {
        return gjelderVedtak;
    }

    public boolean erKlagerPart() {
        return erKlagerPart;
    }

    public boolean erFristOverholdt() {
        return erFristOverholdt;
    }

    public boolean erKonkret() {
        return erKonkret;
    }

    public boolean erSignert() {
        return erSignert;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        var that = (KlageFormkravDvh) o;
        return gjelderVedtak == that.gjelderVedtak &&
            erKlagerPart == that.erKlagerPart &&
            erFristOverholdt == that.erFristOverholdt &&
            erKonkret == that.erKonkret &&
            erSignert == that.erSignert &&
            Objects.equals(klageBehandlingId, that.klageBehandlingId) &&
            Objects.equals(klageVurdertAv, that.klageVurdertAv) &&
            Objects.equals(opprettetTidspunkt, that.opprettetTidspunkt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), klageBehandlingId, klageVurdertAv, gjelderVedtak, erKlagerPart, erFristOverholdt, erKonkret, erSignert, opprettetTidspunkt);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private KlageFormkravDvh klageFormkravDvh = new KlageFormkravDvh();

        public Builder medKlageBehandlingId(Long klageBehandlingId) {
            klageFormkravDvh.klageBehandlingId = klageBehandlingId;
            return this;
        }

        public Builder medKlageVurdertAv(String klageVurdertAv) {
            klageFormkravDvh.klageVurdertAv = klageVurdertAv;
            return this;
        }

        public Builder medGjelderVedtak(boolean gjelderVedtak) {
            klageFormkravDvh.gjelderVedtak = gjelderVedtak;
            return this;
        }

        public Builder medErKlagerPart(boolean erKlagerPart) {
            klageFormkravDvh.erKlagerPart = erKlagerPart;
            return this;
        }

        public Builder medErFristOverholdt(boolean erFristOverholdt) {
            klageFormkravDvh.erFristOverholdt = erFristOverholdt;
            return this;
        }

        public Builder medErKonkret(boolean erKonkret) {
            klageFormkravDvh.erKonkret = erKonkret;
            return this;
        }

        public Builder medErSignert(boolean erSignert) {
            klageFormkravDvh.erSignert = erSignert;
            return this;
        }

        public Builder medOpprettetTidspunkt(LocalDateTime opprettetTidspunkt) {
            klageFormkravDvh.opprettetTidspunkt = opprettetTidspunkt;
            return this;
        }

        public KlageFormkravDvh build() {
            return this.klageFormkravDvh;
        }
    }
}
