package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

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
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;

@Entity(name = "MedlemskapsvilkårPerioder")
@Table(name = "MEDLEMSKAP_VILKAR_PERIODER")
public class MedlemskapsvilkårPerioderEntitet extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MEDLEMSKAP_VILKAR_PERIODER")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "medlemskap_vilkar_periode_id", nullable = false, updatable = false)
    private MedlemskapsvilkårPeriodeEntitet rot;

    @Column(name = "fom", nullable = false)
    @ChangeTracked
    private LocalDate fom;

    @Column(name = "tom")
    @ChangeTracked
    private LocalDate tom;

    @Column(name = "vurderingsdato")
    @ChangeTracked
    private LocalDate vurderingsdato;

    @ChangeTracked
    @Convert(converter = VilkårUtfallType.KodeverdiConverter.class)
    @Column(name="vilkar_utfall", nullable = false)
    private VilkårUtfallType vilkårUtfall = VilkårUtfallType.UDEFINERT;

    @ChangeTracked
    @Convert(converter = VilkårUtfallMerknad.KodeverdiConverter.class)
    @Column(name = "vilkar_utfall_merknad", nullable = false)
    private VilkårUtfallMerknad vilkårUtfallMerknad = VilkårUtfallMerknad.UDEFINERT;

    public MedlemskapsvilkårPerioderEntitet() {
    }

    MedlemskapsvilkårPerioderEntitet(MedlemskapsvilkårPerioderEntitet entitet) {
        this.fom = entitet.getFom();
        this.tom = entitet.getTom();
        this.vurderingsdato = entitet.getVurderingsdato();
        this.vilkårUtfall = entitet.getVilkårUtfall();
        this.vilkårUtfallMerknad = entitet.getVilkårUtfallMerknad();
    }


    @Override
    public String getIndexKey() {
        return IndexKey.createKey(fom, tom);
    }

    public Long getId() {
        return id;
    }


    public LocalDate getFom() {
        return fom;
    }

    void setFom(LocalDate fom) {
        this.fom = fom;
    }


    public LocalDate getTom() {
        return tom;
    }

    void setTom(LocalDate tom) {
        this.tom = tom;
    }


    public VilkårUtfallType getVilkårUtfall() {
        return vilkårUtfall;
    }


    public LocalDate getVurderingsdato() {
        return vurderingsdato;
    }


    public VilkårUtfallMerknad getVilkårUtfallMerknad() {
        return vilkårUtfallMerknad;
    }

    public MedlemskapsvilkårPeriodeEntitet getRot() {
        return rot;
    }

    void setRot(MedlemskapsvilkårPeriodeEntitet rot) {
        this.rot = rot;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MedlemskapsvilkårPerioderEntitet that = (MedlemskapsvilkårPerioderEntitet) o;
        return Objects.equals(getFom(), that.getFom()) &&
                Objects.equals(getTom(), that.getTom());
    }


    @Override
    public int hashCode() {
        return Objects.hash(getFom(), getTom());
    }

    public static class Builder {
        private MedlemskapsvilkårPerioderEntitet mal;
        private boolean oppdatering = false;

        private Builder() {
            mal = new MedlemskapsvilkårPerioderEntitet();
        }

        private Builder(MedlemskapsvilkårPerioderEntitet mal) {
            this.mal = mal;
            this.oppdatering = true;
        }

        public static Builder oppdater(Optional<MedlemskapsvilkårPerioderEntitet> entitet, LocalDate vurderingsdato) {
            if (entitet.isPresent()) {
                return new Builder(entitet.get());
            }
            Builder builder = new Builder();
            builder.medVurderingsdato(vurderingsdato);
            return builder;
        }

        public Builder medVilkårUtfall(VilkårUtfallType vilkårUtfall) {
            mal.vilkårUtfall = vilkårUtfall;
            return this;
        }

        public Builder medVilkårUtfallMerknad(VilkårUtfallMerknad vilkårUtfallMerknad) {
            mal.vilkårUtfallMerknad = vilkårUtfallMerknad;
            return this;
        }

        public Builder medVurderingsdato(LocalDate vurderingsdato) {
            mal.vurderingsdato = vurderingsdato;

            //fjernes når fom og tom ryddesvekk
            mal.setFom(vurderingsdato);
            mal.setTom(vurderingsdato);
            return this;
        }

        public boolean erOppdatering() {
            return oppdatering;
        }

        public MedlemskapsvilkårPerioderEntitet build() {
            return mal;
        }
    }
}
