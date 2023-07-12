package no.nav.foreldrepenger.behandlingslager.behandling.personopplysning;

import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.HarAktørId;

@Entity(name = "PersonopplysningPersonstatus")
@Table(name = "PO_PERSONSTATUS")
public class PersonstatusEntitet extends BaseEntitet implements HarAktørId, IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PO_PERSONSTATUS")
    private Long id;

    @Embedded
    @AttributeOverride(name = "aktørId", column = @Column(name = "aktoer_id", updatable = false, nullable=false))
    private AktørId aktørId;

    @Embedded
    private DatoIntervallEntitet periode;

    @ChangeTracked
    @Convert(converter = PersonstatusType.KodeverdiConverter.class)
    @Column(name="personstatus", nullable = false)
    private PersonstatusType personstatus = PersonstatusType.UDEFINERT;

    @ManyToOne(optional = false)
    @JoinColumn(name = "po_informasjon_id", nullable = false, updatable = false)
    private PersonInformasjonEntitet personopplysningInformasjon;

    PersonstatusEntitet() {
    }

    PersonstatusEntitet(PersonstatusEntitet personstatus) {
        this.aktørId = personstatus.getAktørId();
        this.periode = personstatus.getPeriode();
        this.personstatus = personstatus.getPersonstatus();
    }


    @Override
    public String getIndexKey() {
        return IndexKey.createKey(aktørId, personstatus, periode);
    }

    void setPersonInformasjon(PersonInformasjonEntitet personInformasjon) {
        this.personopplysningInformasjon = personInformasjon;
    }

    void setId(Long id) {
        this.id = id;
    }

    void setPersonstatus(PersonstatusType personstatus) {
        this.personstatus = personstatus;
    }


    @Override
    public AktørId getAktørId() {
        return aktørId;
    }

    void setAktørId(AktørId aktørId) {
        this.aktørId = aktørId;
    }


    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    void setPeriode(DatoIntervallEntitet gyldighetsperiode) {
        this.periode = gyldighetsperiode;
    }


    public PersonstatusType getPersonstatus() {
        return personstatus;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var entitet = (PersonstatusEntitet) o;
        return Objects.equals(aktørId, entitet.aktørId) &&
                Objects.equals(periode, entitet.periode) &&
                Objects.equals(personstatus, entitet.personstatus);
    }


    @Override
    public int hashCode() {
        return Objects.hash(aktørId, periode, personstatus);
    }


    @Override
    public String toString() {
        var sb = new StringBuilder("PersonstatusEntitet{");
        sb.append("gyldighetsperiode=").append(periode);
        sb.append(", personstatus=").append(personstatus);
        sb.append('}');
        return sb.toString();
    }

}
