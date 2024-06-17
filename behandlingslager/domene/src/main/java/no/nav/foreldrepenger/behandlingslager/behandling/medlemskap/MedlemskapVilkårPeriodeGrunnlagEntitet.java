package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;

import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "MedlemskapVilkårPeriodeGrunnlag")
@Table(name = "GR_MEDLEMSKAP_VILKAR_PERIODE")
public class MedlemskapVilkårPeriodeGrunnlagEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_MEDLEMSKAP_VILKAR_PER")
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "vilkar_resultat_id", nullable = false, updatable = false)
    private VilkårResultat vilkårResultat;

    @OneToOne(optional = false)
    @JoinColumn(name = "medlemskap_vilkar_periode_id", nullable = false, updatable = false, unique = true)
    @ChangeTracked
    private MedlemskapsvilkårPeriodeEntitet medlemskapsvilkårPeriode;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    MedlemskapVilkårPeriodeGrunnlagEntitet() {
    }

    private MedlemskapVilkårPeriodeGrunnlagEntitet(VilkårResultat vilkårResultat) {
        this.vilkårResultat = vilkårResultat;
    }

    MedlemskapVilkårPeriodeGrunnlagEntitet(MedlemskapVilkårPeriodeGrunnlagEntitet entitet) {
        this.vilkårResultat = entitet.vilkårResultat;
        this.medlemskapsvilkårPeriode = entitet.medlemskapsvilkårPeriode;
    }

    public static MedlemskapVilkårPeriodeGrunnlagEntitet fra(Optional<MedlemskapVilkårPeriodeGrunnlagEntitet> eksisterendeGrunnlag,
                                                             Behandling nyBehandling) {
        return kopierTidligerGrunnlag(eksisterendeGrunnlag, nyBehandling);
    }

    private static MedlemskapVilkårPeriodeGrunnlagEntitet kopierTidligerGrunnlag(Optional<MedlemskapVilkårPeriodeGrunnlagEntitet> tidligereGrunnlagOpt,
                                                                                 Behandling nyBehandling) {
        var vilkårResultat = nyBehandling.getBehandlingsresultat().getVilkårResultat();
        var nyttGrunnlag = new MedlemskapVilkårPeriodeGrunnlagEntitet(vilkårResultat);

        if (tidligereGrunnlagOpt.isPresent()) {
            var tidligereGrunnlag = tidligereGrunnlagOpt.get();
            nyttGrunnlag.setMedlemskapsvilkårPeriode(tidligereGrunnlag.getMedlemskapsvilkårPeriode());
        }
        return nyttGrunnlag;
    }


    public MedlemskapsvilkårPeriodeEntitet getMedlemskapsvilkårPeriode() {
        return medlemskapsvilkårPeriode;
    }

    void setMedlemskapsvilkårPeriode(MedlemskapsvilkårPeriodeEntitet medlemskapsvilkårPeriode) {
        this.medlemskapsvilkårPeriode = medlemskapsvilkårPeriode;
    }

    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    public VilkårResultat getVilkårResultat() {
        return vilkårResultat;
    }

    void setVilkårResultat(VilkårResultat vilkårResultat) {
        this.vilkårResultat = vilkårResultat;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (MedlemskapVilkårPeriodeGrunnlagEntitet) o;
        return aktiv == that.aktiv && Objects.equals(medlemskapsvilkårPeriode, that.medlemskapsvilkårPeriode);
    }


    @Override
    public int hashCode() {
        return Objects.hash(aktiv, medlemskapsvilkårPeriode);
    }

    public static class Builder {
        private MedlemskapVilkårPeriodeGrunnlagEntitet kladd;

        private Builder() {
            this.kladd = new MedlemskapVilkårPeriodeGrunnlagEntitet();
        }

        private Builder(MedlemskapVilkårPeriodeGrunnlagEntitet kladd) {
            this.kladd = kladd;
        }

        private static Builder oppdatere(MedlemskapVilkårPeriodeGrunnlagEntitet aggregat) {
            return new Builder(aggregat);
        }

        public static Builder oppdatere(Optional<MedlemskapVilkårPeriodeGrunnlagEntitet> aggregat) {
            return aggregat.map(Builder::oppdatere).orElseGet(Builder::new);
        }

        public Builder medMedlemskapsvilkårPeriode(MedlemskapsvilkårPeriodeEntitet.Builder builder) {
            kladd.setMedlemskapsvilkårPeriode(builder.build());
            return this;
        }

        public MedlemskapsvilkårPeriodeEntitet.Builder getPeriodeBuilder() {
            return MedlemskapsvilkårPeriodeEntitet.Builder.oppdatere(Optional.ofNullable(kladd.medlemskapsvilkårPeriode));
        }

        public MedlemskapVilkårPeriodeGrunnlagEntitet build() {
            return kladd;
        }
    }
}
