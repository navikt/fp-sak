package no.nav.foreldrepenger.behandlingslager.behandling.opptjening;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

@Entity(name = "OpptjeningAktivitet")
@Table(name = "OPPTJENING_AKTIVITET")
public class OpptjeningAktivitet extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OPPTJENING_AKTIVITET")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @ChangeTracked
    @Embedded
    private DatoIntervallEntitet periode;

    @ChangeTracked
    @Convert(converter = OpptjeningAktivitetType.KodeverdiConverter.class)
    @Column(name = "aktivitet_type", nullable = false)
    private OpptjeningAktivitetType aktivitetType;

    @ChangeTracked
    @Convert(converter = ReferanseType.KodeverdiConverter.class)
    @Column(name="referanse_type")
    private ReferanseType aktivitetReferanseType = ReferanseType.UDEFINERT;

    /** Custom aktivitet referanse. Form og innhold avhenger av #aktivitetType . */
    @ChangeTracked
    @Column(name = "aktivitet_referanse")
    private String aktivitetReferanse;

    @ChangeTracked
    @Convert(converter = OpptjeningAktivitetKlassifisering.KodeverdiConverter.class)
    @Column(name = "klassifisering", nullable = false)
    private OpptjeningAktivitetKlassifisering klassifisering;

    OpptjeningAktivitet() {
        // fur hibernate
    }

    public OpptjeningAktivitet(LocalDate fom, LocalDate tom, OpptjeningAktivitetType aktivitetType,
                               OpptjeningAktivitetKlassifisering klassifisering) {
        this(fom, tom, aktivitetType, klassifisering, null, null);
    }

    public OpptjeningAktivitet(LocalDate fom, LocalDate tom, OpptjeningAktivitetType aktivitetType,
                               OpptjeningAktivitetKlassifisering klassifisering, String aktivitetReferanse, ReferanseType aktivitetReferanseType) {
        Objects.requireNonNull(fom, "fom");
        Objects.requireNonNull(tom, "tom");
        Objects.requireNonNull(aktivitetType, "aktivitetType");
        Objects.requireNonNull(klassifisering, "klassifisering");
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);

        this.aktivitetType = aktivitetType;
        this.klassifisering = klassifisering;

        if (aktivitetReferanse != null) {
            Objects.requireNonNull(aktivitetReferanseType, "aktivitetReferanseType");
            this.aktivitetReferanse = aktivitetReferanse;
            this.aktivitetReferanseType = aktivitetReferanseType;
        }
    }

    /** copy constructor - kun data uten metadata som aktiv/endretAv etc. */
    public OpptjeningAktivitet(OpptjeningAktivitet annen) {

        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(annen.getFom(), annen.getTom());
        this.aktivitetReferanse = annen.getAktivitetReferanse();
        this.aktivitetType = annen.getAktivitetType();
        this.klassifisering = annen.getKlassifisering();
        this.aktivitetReferanseType = annen.getAktivitetReferanseType() == null ? ReferanseType.UDEFINERT
            : annen.getAktivitetReferanseType();

    }

    public LocalDate getFom() {
        return periode.getFomDato();
    }

    public LocalDate getTom() {
        return periode.getTomDato();
    }

    public String getAktivitetReferanse() {
        return aktivitetReferanse;
    }

    public ReferanseType getAktivitetReferanseType() {
        return ReferanseType.UDEFINERT.equals(aktivitetReferanseType) ? null : aktivitetReferanseType;
    }

    public OpptjeningAktivitetType getAktivitetType() {
        return aktivitetType;
    }

    public OpptjeningAktivitetKlassifisering getKlassifisering() {
        return klassifisering;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !obj.getClass().equals(this.getClass())) {
            return false;
        }

        var other = (OpptjeningAktivitet) obj;
        return Objects.equals(periode, other.periode)
            && Objects.equals(aktivitetType, other.aktivitetType)
            && Objects.equals(aktivitetReferanse, other.aktivitetReferanse)
            && Objects.equals(aktivitetReferanseType, other.aktivitetReferanseType)
        // tar ikke med klassifisering, da det ikke er del av dette objektets identitet
        ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, aktivitetType, aktivitetReferanse, aktivitetReferanseType);
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(periode, aktivitetType, aktivitetReferanse, aktivitetReferanseType);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
            + "<aktivitetType=" + aktivitetType
            + (aktivitetReferanse == null ? "" : ", aktivitetReferanse[" + aktivitetReferanseType + "]=" + aktivitetReferanse)
            + ", klassifisering=" + klassifisering
            + " [" + periode.getFomDato() + ", " + periode.getTomDato() + "]"
            + ">";
    }

}
