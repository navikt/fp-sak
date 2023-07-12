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
import no.nav.foreldrepenger.behandlingslager.aktør.OppholdstillatelseType;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.HarAktørId;

@Entity(name = "PersonopplysningOpphold")
@Table(name = "PO_OPPHOLD")
public class OppholdstillatelseEntitet extends BaseEntitet implements HarAktørId, IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PO_OPPHOLD")
    private Long id;

    @Embedded
    @AttributeOverride(name = "aktørId", column = @Column(name = "aktoer_id", updatable = false, nullable=false))
    private AktørId aktørId;

    @Embedded
    private DatoIntervallEntitet periode;

    @ChangeTracked
    @Convert(converter = OppholdstillatelseType.KodeverdiConverter.class)
    @Column(name="tillatelse", nullable = false)
    private OppholdstillatelseType tillatelse = OppholdstillatelseType.UDEFINERT;

    @ManyToOne(optional = false)
    @JoinColumn(name = "po_informasjon_id", nullable = false, updatable = false)
    private PersonInformasjonEntitet personopplysningInformasjon;

    public OppholdstillatelseEntitet() {
    }

    OppholdstillatelseEntitet(OppholdstillatelseEntitet personstatus) {
        this.aktørId = personstatus.getAktørId();
        this.periode = personstatus.getPeriode();
        this.tillatelse = personstatus.getTillatelse();
    }


    @Override
    public String getIndexKey() {
        return IndexKey.createKey(aktørId, tillatelse, periode);
    }

    void setPersonInformasjon(PersonInformasjonEntitet personInformasjon) {
        this.personopplysningInformasjon = personInformasjon;
    }

    void setId(Long id) {
        this.id = id;
    }

    public OppholdstillatelseType getTillatelse() {
        return tillatelse;
    }

    public void setTillatelse(OppholdstillatelseType tillatelse) {
        this.tillatelse = tillatelse;
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


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var entitet = (OppholdstillatelseEntitet) o;
        return Objects.equals(aktørId, entitet.aktørId) &&
                Objects.equals(periode, entitet.periode) &&
                Objects.equals(tillatelse, entitet.tillatelse);
    }


    @Override
    public int hashCode() {
        return Objects.hash(aktørId, periode, tillatelse);
    }


    @Override
    public String toString() {
        return "OppholdstillatelseEntitet{" +
            "periode=" + periode +
            ", tillatelse=" + tillatelse +
            '}';
    }

}
