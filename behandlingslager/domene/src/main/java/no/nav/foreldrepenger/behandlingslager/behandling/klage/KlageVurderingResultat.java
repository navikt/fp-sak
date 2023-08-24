package no.nav.foreldrepenger.behandlingslager.behandling.klage;

import jakarta.persistence.*;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

import java.util.Objects;

@Entity(name = "KlageVurderingResultat")
@Table(name = "KLAGE_VURDERING_RESULTAT")
public class KlageVurderingResultat extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_KLAGE_VURDERING_RESULTAT")
    private Long id;

    @ManyToOne(optional = false)

    @JoinColumn(name = "klage_resultat_id", nullable = false, updatable = false)
    private KlageResultatEntitet klageResultat;

    @Convert(converter = KlageVurdertAv.KodeverdiConverter.class)
    @Column(name = "klage_vurdert_av", nullable = false)
    private KlageVurdertAv klageVurdertAv;

    @Convert(converter = KlageVurdering.KodeverdiConverter.class)
    @Column(name = "klagevurdering", nullable = false)
    private KlageVurdering klageVurdering = KlageVurdering.UDEFINERT;

    @Convert(converter = KlageMedholdÅrsak.KodeverdiConverter.class)
    @Column(name = "klage_medhold_aarsak", nullable = false)
    private KlageMedholdÅrsak klageMedholdÅrsak = KlageMedholdÅrsak.UDEFINERT;

    @Convert(converter = KlageVurderingOmgjør.KodeverdiConverter.class)
    @Column(name = "klage_vurdering_omgjoer", nullable = false)
    private KlageVurderingOmgjør klageVurderingOmgjør = KlageVurderingOmgjør.UDEFINERT;

    @Convert(converter = KlageHjemmel.KodeverdiConverter.class)
    @Column(name = "klage_hjemmel", nullable = false)
    private KlageHjemmel klageHjemmel = KlageHjemmel.UDEFINERT;

    @Column(name = "begrunnelse")
    private String begrunnelse;

    @Column(name = "fritekst_til_brev")
    private String fritekstTilBrev;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "godkjent_av_medunderskriver", nullable = false)
    private boolean godkjentAvMedunderskriver;

    public KlageVurderingResultat() {
        // Hibernate
    }

    private KlageVurderingResultat(KlageVurderingResultat entitet) {
        this.klageResultat = entitet.klageResultat;
        this.klageVurdertAv = entitet.klageVurdertAv;
        this.klageVurdering = entitet.klageVurdering;
        this.klageMedholdÅrsak = entitet.klageMedholdÅrsak;
        this.klageVurderingOmgjør = entitet.klageVurderingOmgjør;
        this.klageHjemmel = entitet.klageHjemmel;
        this.begrunnelse = entitet.begrunnelse;
        this.fritekstTilBrev = entitet.fritekstTilBrev;
        this.godkjentAvMedunderskriver = entitet.godkjentAvMedunderskriver;
    }

    public Long getId() {
        return id;
    }

    public KlageVurdertAv getKlageVurdertAv() {
        return klageVurdertAv;
    }

    public KlageVurdering getKlageVurdering() {
        return klageVurdering;
    }

    public KlageMedholdÅrsak getKlageMedholdÅrsak() {
        return klageMedholdÅrsak;
    }

    public KlageVurderingOmgjør getKlageVurderingOmgjør() {
        return klageVurderingOmgjør;
    }

    public KlageHjemmel getKlageHjemmel() {
        return klageHjemmel;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public String getFritekstTilBrev() {
        return fritekstTilBrev;
    }

    public KlageResultatEntitet getKlageResultat() {
        return klageResultat;
    }

    public boolean isGodkjentAvMedunderskriver() {
        return godkjentAvMedunderskriver;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (KlageVurderingResultat) o;
        return harLikVurdering(that) &&
            godkjentAvMedunderskriver == that.godkjentAvMedunderskriver &&
            Objects.equals(klageResultat, that.klageResultat) &&
            Objects.equals(fritekstTilBrev, that.fritekstTilBrev);
    }

    public boolean harLikVurdering(KlageVurderingResultat that) {
        if (this == that) return true;
        if (that == null || getClass() != that.getClass()) return false;
        return klageVurdertAv == that.klageVurdertAv &&
            klageVurdering == that.klageVurdering &&
            klageMedholdÅrsak == that.klageMedholdÅrsak &&
            klageVurderingOmgjør == that.klageVurderingOmgjør &&
            klageHjemmel == that.klageHjemmel &&
            Objects.equals(begrunnelse, that.begrunnelse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(klageResultat, klageVurdertAv, klageVurdering, klageMedholdÅrsak, klageVurderingOmgjør, klageHjemmel, begrunnelse, fritekstTilBrev, godkjentAvMedunderskriver);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(KlageVurderingResultat klageVurderingResultat) {
        return new Builder(klageVurderingResultat);
    }

    public static class Builder {
        private KlageVurderingResultat klageVurderingResultatMal;

        private Builder() {
            klageVurderingResultatMal = new KlageVurderingResultat();
        }

        private Builder(KlageVurderingResultat klageVurderingResultat) {
            klageVurderingResultatMal = new KlageVurderingResultat(klageVurderingResultat);
        }

        public Builder medKlageVurdertAv(KlageVurdertAv klageVurdertAv) {
            klageVurderingResultatMal.klageVurdertAv = klageVurdertAv;
            return this;
        }

        public Builder medKlageVurdering(KlageVurdering klageVurdering) {
            klageVurderingResultatMal.klageVurdering = klageVurdering == null ? KlageVurdering.UDEFINERT : klageVurdering;
            return this;
        }

        public Builder medKlageMedholdÅrsak(KlageMedholdÅrsak klageMedholdÅrsak) {
            klageVurderingResultatMal.klageMedholdÅrsak = klageMedholdÅrsak == null ? KlageMedholdÅrsak.UDEFINERT : klageMedholdÅrsak;
            return this;
        }

        public Builder medKlageVurderingOmgjør(KlageVurderingOmgjør klageVurderingOmgjør) {
            klageVurderingResultatMal.klageVurderingOmgjør = klageVurderingOmgjør == null ? KlageVurderingOmgjør.UDEFINERT : klageVurderingOmgjør;
            return this;
        }

        public Builder medKlageHjemmel(KlageHjemmel klageHjemmel) {
            klageVurderingResultatMal.klageHjemmel = klageHjemmel == null ? KlageHjemmel.UDEFINERT : klageHjemmel;
            return this;
        }

        public Builder medBegrunnelse(String begrunnelse) {
            klageVurderingResultatMal.begrunnelse = begrunnelse;
            return this;
        }

        public Builder medFritekstTilBrev(String fritekstTilBrev) {
            klageVurderingResultatMal.fritekstTilBrev = fritekstTilBrev;
            return this;
        }

        public Builder medKlageResultat(KlageResultatEntitet klageResultat) {
            klageVurderingResultatMal.klageResultat = klageResultat;
            return this;
        }

        public KlageVurderingResultat build() {
            verifyStateForBuild();
            return klageVurderingResultatMal;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(klageVurderingResultatMal.klageVurdertAv, "klageVurdertAv");
            Objects.requireNonNull(klageVurderingResultatMal.klageResultat, "KlageResultat");
            if (klageVurderingResultatMal.klageVurdering.equals(KlageVurdering.MEDHOLD_I_KLAGE)) {
                Objects.requireNonNull(klageVurderingResultatMal.klageMedholdÅrsak, "klageMedholdÅrsak");
                Objects.requireNonNull(klageVurderingResultatMal.klageVurderingOmgjør, "klageVurderingOmgjør");
            }
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            (id != null ? "id=" + id + ", " : "")
            + "klageVurdertAv=" + getKlageVurdertAv() + ", "
            + "klageVurdering=" + getKlageVurdering() + ", "
            + "klageVurderingOmgjør" + getKlageVurderingOmgjør() + ", "
            + "klageMedholdÅrsak=" + getKlageMedholdÅrsak() + ", "
            + "klageHjemmel=" + getKlageHjemmel() + ", "
            + "begrunnelse=" + begrunnelse + ", "
            + "fritekstTilBrev=" + fritekstTilBrev + ", "
            + ">";
    }
}
