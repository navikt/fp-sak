package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;

import java.time.LocalDate;
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
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "VilkårMedlemskap")
@Table(name = "VILKAR_MEDLEMSKAP")
public class VilkårMedlemskap extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_VILKAR_MEDLEMSKAP")
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "vilkar_resultat_id", nullable = false, updatable = false)
    private VilkårResultat vilkårResultat;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Column(name = "opphor_fom")
    private LocalDate opphørFom;

    @Convert(converter = Avslagsårsak.KodeverdiConverter.class)
    @Column(name="opphor_arsak", nullable = false)
    private Avslagsårsak opphørsårsak = Avslagsårsak.UDEFINERT;

    @Column(name = "medlem_fom")
    private LocalDate medlemFom;


    VilkårMedlemskap() {
    }

    public VilkårMedlemskap(VilkårResultat vilkårResultat,
                            MedlemskapOpphør medlemskapOpphør,
                            LocalDate medlemFom) {
        Objects.requireNonNull(vilkårResultat);
        if (medlemskapOpphør == null && medlemFom == null) {
            throw new IllegalArgumentException("Forventer enten opphør eller medlemFom");
        }
        this.vilkårResultat = vilkårResultat;
        this.opphørFom = medlemskapOpphør == null ? null : medlemskapOpphør.fom();
        this.opphørsårsak = medlemskapOpphør == null ? Avslagsårsak.UDEFINERT : medlemskapOpphør.årsak();
        this.medlemFom = medlemFom;
    }

    public static VilkårMedlemskap forOpphør(VilkårResultat vilkårResultat, LocalDate opphørFom, Avslagsårsak avslagsårsak) {
        return new VilkårMedlemskap(vilkårResultat, new MedlemskapOpphør(opphørFom, avslagsårsak), null);
    }

    public static VilkårMedlemskap forMedlemFom(VilkårResultat vilkårResultat, LocalDate medlemFom) {
        return new VilkårMedlemskap(vilkårResultat, null, medlemFom);
    }

    public VilkårResultat getVilkårResultat() {
        return vilkårResultat;
    }

    public Optional<MedlemskapOpphør> getOpphør() {
        if (opphørFom == null) {
            return Optional.empty();
        }
        return Optional.of(new MedlemskapOpphør(opphørFom, opphørsårsak));
    }

    public Optional<LocalDate> getMedlemFom() {
        return Optional.ofNullable(medlemFom);
    }

    public void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        var that = (VilkårMedlemskap) o;
        return Objects.equals(vilkårResultat, that.vilkårResultat);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(vilkårResultat);
    }

}
