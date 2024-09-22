package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

/**
 * Entitetsklasse for medlemskap perioder.
 * <p>
 * Implementert iht. builder pattern (ref. "Effective Java, 2. ed." J.Bloch).
 * Non-public constructors og setters, dvs. immutable.
 * <p>
 * OBS: Legger man til nye felter så skal dette oppdateres mange steder:
 * builder, equals, hashcode etc.
 */

@Entity(name = "MedlemskapPerioder")
@Table(name = "MEDLEMSKAP_PERIODER")
public class MedlemskapPerioderEntitet extends BaseEntitet implements IndexKey {

    public static final Comparator<MedlemskapPerioderEntitet> COMP_MEDLEMSKAP_PERIODER = Comparator
        .comparing(MedlemskapPerioderEntitet::getMedlId, Comparator.nullsFirst(Comparator.naturalOrder()));

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MEDLEMSKAP_PERIODER")
    private Long id;

    @Embedded
    @ChangeTracked
    private DatoIntervallEntitet periode;

    @ChangeTracked
    @Column(name = "beslutningsdato")
    private LocalDate beslutningsdato;

    @ChangeTracked
    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "er_medlem", nullable = false)
    private boolean erMedlem;

    @ChangeTracked
    @Convert(converter = Landkoder.KodeverdiConverter.class)
    @Column(name="lovvalg_land", nullable = false)
    private Landkoder lovvalgLand = Landkoder.UDEFINERT;

    @ChangeTracked
    @Convert(converter = Landkoder.KodeverdiConverter.class)
    @Column(name="studie_land", nullable = false)
    private Landkoder studieLand = Landkoder.UDEFINERT;

    @ChangeTracked
    @Convert(converter = MedlemskapType.KodeverdiConverter.class)
    @Column(name="medlemskap_type", nullable = false)
    private MedlemskapType medlemskapType = MedlemskapType.UDEFINERT;

    @ChangeTracked
    @Convert(converter = MedlemskapDekningType.KodeverdiConverter.class)
    @Column(name="dekning_type", nullable = false)
    private MedlemskapDekningType dekningType = MedlemskapDekningType.UDEFINERT;

    @ChangeTracked
    @Convert(converter = MedlemskapKildeType.KodeverdiConverter.class)
    @Column(name="kilde_type", nullable = false)
    private MedlemskapKildeType kildeType = MedlemskapKildeType.UDEFINERT;

    @Column(name = "medl_id")
    private Long medlId;

    MedlemskapPerioderEntitet() {
        // hibernate
    }

    /**
     * Deep copy.
     */
    public MedlemskapPerioderEntitet(MedlemskapPerioderEntitet medlemskapPerioderMal) {
        this.periode = medlemskapPerioderMal.getPeriode();
        this.beslutningsdato = medlemskapPerioderMal.getBeslutningsdato();
        this.erMedlem = medlemskapPerioderMal.getErMedlem();
        this.setLovvalgLand(medlemskapPerioderMal.getLovvalgLand());
        this.setStudieland(medlemskapPerioderMal.getStudieland());
        this.setMedlemskapType(medlemskapPerioderMal.getMedlemskapType());
        this.setDekningType(medlemskapPerioderMal.getDekningType());
        this.setKildeType(medlemskapPerioderMal.getKildeType());
        this.setMedlId(medlemskapPerioderMal.getMedlId());
    }


    @Override
    public String getIndexKey() {
        //redusert fra equals
        return IndexKey.createKey(periode, medlId, dekningType, kildeType);
    }


    public LocalDate getFom() {
        return periode != null ? periode.getFomDato() : null;
    }


    public LocalDate getTom() {
        return periode != null ? periode.getTomDato() : null;
    }


    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public void setPeriode(LocalDate fom, LocalDate tom) {
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
    }


    public LocalDate getBeslutningsdato() {
        return beslutningsdato;
    }

    void setBeslutningsdato(LocalDate beslutningsdato) {
        this.beslutningsdato = beslutningsdato;
    }


    public boolean getErMedlem() {
        return erMedlem;
    }

    void setErMedlem(boolean erMedlem) {
        this.erMedlem = erMedlem;
    }


    public MedlemskapType getMedlemskapType() {
        return medlemskapType == null || Objects.equals(MedlemskapType.UDEFINERT, medlemskapType) ? null : medlemskapType;
    }

    void setMedlemskapType(MedlemskapType medlemskapType) {
        this.medlemskapType = medlemskapType == null ? MedlemskapType.UDEFINERT : medlemskapType;
    }


    public MedlemskapDekningType getDekningType() {
        return dekningType == null || Objects.equals(MedlemskapDekningType.UDEFINERT, dekningType) ? null : dekningType;
    }

    void setDekningType(MedlemskapDekningType dekningType) {
        this.dekningType = dekningType == null ? MedlemskapDekningType.UDEFINERT : dekningType;
    }


    public MedlemskapKildeType getKildeType() {
        return kildeType == null || Objects.equals(kildeType, MedlemskapKildeType.UDEFINERT) ? null : kildeType;
    }

    void setKildeType(MedlemskapKildeType kildeType) {
        this.kildeType = kildeType == null ? MedlemskapKildeType.UDEFINERT : kildeType;
    }


    public Landkoder getLovvalgLand() {
        return lovvalgLand == null || Objects.equals(lovvalgLand, Landkoder.UDEFINERT) ? null : lovvalgLand;
    }

    void setLovvalgLand(Landkoder lovvalgsland) {
        this.lovvalgLand = lovvalgsland == null ? Landkoder.UDEFINERT : lovvalgsland;
    }


    public Landkoder getStudieland() {
        return studieLand == null || Objects.equals(studieLand, Landkoder.UDEFINERT) ? null : studieLand;
    }

    void setStudieland(Landkoder studieland) {
        this.studieLand = studieland == null ? Landkoder.UDEFINERT : studieland;
    }


    public Long getMedlId() {
        return medlId;
    }

    void setMedlId(Long medlId) {
        this.medlId = medlId;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof MedlemskapPerioderEntitet other)) {
            return false;
        }
        // minste sett med felter som angir ett medlemskap periode(uten 'muterbare' felter)
        return Objects.equals(this.getFom(), other.getFom())
                && Objects.equals(this.getTom(), other.getTom())
                && Objects.equals(this.getDekningType(), other.getDekningType())
                && Objects.equals(this.getKildeType(), other.getKildeType())
                && Objects.equals(this.getMedlId(), other.getMedlId());
    }


    @Override
    public int hashCode() {
        return Objects.hash(getFom(), getTom(), getDekningType(), getKildeType(), getMedlId());
    }


}
