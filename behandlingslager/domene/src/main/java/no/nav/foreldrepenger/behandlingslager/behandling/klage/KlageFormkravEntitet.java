package no.nav.foreldrepenger.behandlingslager.behandling.klage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

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

    public KlageFormkravEntitet() {
        // Hibernate
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
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof KlageFormkravEntitet)) {
            return false;
        }
        KlageFormkravEntitet other = (KlageFormkravEntitet) obj;
        return Objects.equals(this.hentKlageResultat(), other.hentKlageResultat())
            && Objects.equals(this.getKlageVurdertAv(), other.getKlageVurdertAv())
            && Objects.equals(this.hentGjelderVedtak(), other.hentGjelderVedtak())
            && Objects.equals(this.erKlagerPart(), other.erKlagerPart())
            && Objects.equals(this.erFristOverholdt(), other.erFristOverholdt())
            && Objects.equals(this.erKonkret(), other.erKonkret())
            && Objects.equals(this.erSignert(), other.erSignert())
            && Objects.equals(this.hentBegrunnelse(), other.hentBegrunnelse());
    }

    @Override
    public int hashCode() {
        return Objects.hash(hentKlageResultat(), getKlageVurdertAv(), getOpprettetTidspunkt(), hentBegrunnelse());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private KlageFormkravEntitet klageFormkravEntitetMal;

        public Builder() {
            klageFormkravEntitetMal = new KlageFormkravEntitet();
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
        return getClass().getSimpleName() + "<" + //$NON-NLS-1$
            (id != null ? "id=" + id + ", " : "") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            + "klageResultat=" + hentKlageResultat() + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "klageVurdertAv=" + getKlageVurdertAv() + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "gjelderVedtak=" + hentGjelderVedtak() + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "erKlagerPart=" + erKlagerPart() + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "erFristOverholdt=" + erFristOverholdt() + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "erKonkret=" + erKonkret() + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "erSignert=" + erSignert() + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "begrunnelse=" + hentBegrunnelse() + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + ">"; //$NON-NLS-1$
    }

}
