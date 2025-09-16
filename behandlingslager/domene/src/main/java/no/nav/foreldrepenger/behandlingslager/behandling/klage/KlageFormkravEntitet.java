package no.nav.foreldrepenger.behandlingslager.behandling.klage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Table(name = "KLAGE_FORMKRAV")
@Entity(name = "KlageFormkrav")
public class KlageFormkravEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_KLAGE_FORMKRAV")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "klage_resultat_id", nullable = false, updatable = false)
    private KlageResultatEntitet klageResultat;

    @Convert(converter = KlageVurdertAv.KodeverdiConverter.class)
    @Column(name = "klage_vurdert_av", nullable = false)
    private KlageVurdertAv klageVurdertAv;

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

    @Column(name = "begrunnelse", nullable = false)
    private String begrunnelse;

    @Column(name = "mottatt_dato")
    private LocalDate mottattDato;

    public KlageFormkravEntitet() {
        // Hibernate
    }

    private KlageFormkravEntitet(KlageFormkravEntitet entitet) {
        this.klageResultat = entitet.klageResultat;
        this.klageVurdertAv = entitet.klageVurdertAv;
        this.gjelderVedtak = entitet.gjelderVedtak;
        this.erKlagerPart = entitet.erKlagerPart;
        this.erFristOverholdt = entitet.erFristOverholdt;
        this.erKonkret = entitet.erKonkret;
        this.erSignert = entitet.erSignert;
        this.begrunnelse = entitet.begrunnelse;
        this.mottattDato = entitet.mottattDato;
    }

    public void setKlageVurdertAv(KlageVurdertAv klageVurdertAv) {
        this.klageVurdertAv = klageVurdertAv;
    }

    public void setGjelderVedtak(boolean gjelderVedtak) {
        this.gjelderVedtak = gjelderVedtak;
    }

    public void setErKlagerPart(boolean erKlagerPart) {
        this.erKlagerPart = erKlagerPart;
    }

    public void setErFristOverholdt(boolean erFristOverholdt) {
        this.erFristOverholdt = erFristOverholdt;
    }

    public void setErKonkret(boolean erKonkret) {
        this.erKonkret = erKonkret;
    }

    public void setErSignert(boolean erSignert) {
        this.erSignert = erSignert;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public void setMottattDato(LocalDate mottattDato) {
        this.mottattDato = mottattDato;
    }

    public Long hentId() {
        return id;
    }

    public KlageResultatEntitet hentKlageResultat() {
        return klageResultat;
    }

    public KlageVurdertAv getKlageVurdertAv() {
        return klageVurdertAv;
    }

    public boolean hentGjelderVedtak() {
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

    public String hentBegrunnelse() {
        return begrunnelse;
    }

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    public List<KlageAvvistÅrsak> hentAvvistÅrsaker() {
        List<KlageAvvistÅrsak> avvistÅrsaker = new ArrayList<>();
        if (!erFristOverholdt) {
            avvistÅrsaker.add(KlageAvvistÅrsak.KLAGET_FOR_SENT);
        }
        if (!erKlagerPart) {
            avvistÅrsaker.add(KlageAvvistÅrsak.KLAGER_IKKE_PART);
        }
        if (!erKonkret) {
            avvistÅrsaker.add(KlageAvvistÅrsak.IKKE_KONKRET);
        }
        if (!erSignert) {
            avvistÅrsaker.add(KlageAvvistÅrsak.IKKE_SIGNERT);
        }
        if (!gjelderVedtak) {
            avvistÅrsaker.add(KlageAvvistÅrsak.IKKE_PAKLAGD_VEDTAK);
        }
        return avvistÅrsaker;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (KlageFormkravEntitet) o;
        return harLikVurdering(that) &&
            Objects.equals(klageResultat, that.klageResultat);
    }

    public boolean harLikVurdering(KlageFormkravEntitet that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;
        return gjelderVedtak == that.gjelderVedtak &&
            erKlagerPart == that.erKlagerPart &&
            erFristOverholdt == that.erFristOverholdt &&
            erKonkret == that.erKonkret &&
            erSignert == that.erSignert &&
            klageVurdertAv == that.klageVurdertAv &&
            Objects.equals(begrunnelse, that.begrunnelse) &&
            Objects.equals(mottattDato, that.mottattDato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(klageResultat, klageVurdertAv, gjelderVedtak, erKlagerPart, erFristOverholdt, erKonkret, erSignert, begrunnelse, mottattDato);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(KlageFormkravEntitet klageFormkravEntitet) {
        return new Builder(klageFormkravEntitet);
    }

    public static class Builder {
        private KlageFormkravEntitet klageFormkravEntitetMal;

        private Builder() {
            klageFormkravEntitetMal = new KlageFormkravEntitet();
        }

        private Builder(KlageFormkravEntitet klageFormkravEntitet) {
            klageFormkravEntitetMal = new KlageFormkravEntitet(klageFormkravEntitet);
        }

        public Builder medKlageResultat(KlageResultatEntitet klageResultat) {
            klageFormkravEntitetMal.klageResultat = klageResultat;
            return this;
        }

        public Builder medKlageVurdertAv(KlageVurdertAv klageVurdertAv) {
            klageFormkravEntitetMal.klageVurdertAv = klageVurdertAv;
            return this;
        }

        public Builder medGjelderVedtak(boolean gjelderVedtak) {
            klageFormkravEntitetMal.gjelderVedtak = gjelderVedtak;
            return this;
        }

        public Builder medErKlagerPart(boolean erKlagerPart) {
            klageFormkravEntitetMal.erKlagerPart = erKlagerPart;
            return this;
        }

        public Builder medErFristOverholdt(boolean erFristOverholdt) {
            klageFormkravEntitetMal.erFristOverholdt = erFristOverholdt;
            return this;
        }

        public Builder medErKonkret(boolean erKonkret) {
            klageFormkravEntitetMal.erKonkret = erKonkret;
            return this;
        }

        public Builder medErSignert(boolean erSignert) {
            klageFormkravEntitetMal.erSignert = erSignert;
            return this;
        }

        public Builder medBegrunnelse(String begrunnelse) {
            klageFormkravEntitetMal.begrunnelse = begrunnelse;
            return this;
        }

        public Builder medMottattDato(LocalDate mottattDato) {
            klageFormkravEntitetMal.mottattDato = mottattDato;
            return this;
        }

        public KlageFormkravEntitet build() {
            verifyStateForBuild();
            return klageFormkravEntitetMal;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(klageFormkravEntitetMal.klageVurdertAv, "klageVurdertAv");
            Objects.requireNonNull(klageFormkravEntitetMal.gjelderVedtak, "gjelderVedtak");
            Objects.requireNonNull(klageFormkravEntitetMal.erKlagerPart, "erKlagerPart");
            Objects.requireNonNull(klageFormkravEntitetMal.erFristOverholdt, "erFristOverholdt");
            Objects.requireNonNull(klageFormkravEntitetMal.erKonkret, "erKonkret");
            Objects.requireNonNull(klageFormkravEntitetMal.erSignert, "erSignert");
            Objects.requireNonNull(klageFormkravEntitetMal.begrunnelse, "begrunnelse");
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            (id != null ? "id=" + id + ", " : "")
            + "klageResultat=" + hentKlageResultat() + ", "
            + "klageVurdertAv=" + getKlageVurdertAv() + ", "
            + "gjelderVedtak=" + hentGjelderVedtak() + ", "
            + "erKlagerPart=" + erKlagerPart() + ", "
            + "erFristOverholdt=" + erFristOverholdt() + ", "
            + "erKonkret=" + erKonkret() + ", "
            + "erSignert=" + erSignert() + ", "
            + "begrunnelse=" + hentBegrunnelse() + ", "
            + "mottattDato=" + getMottattDato() + ", "
            + ">";
    }

}
