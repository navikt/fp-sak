package no.nav.foreldrepenger.behandlingslager.behandling.personopplysning;

import java.util.Objects;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.HarAktørId;

@Entity(name = "PersonopplysningStatsborgerskap")
@Table(name = "PO_STATSBORGERSKAP")
public class StatsborgerskapEntitet extends BaseEntitet implements HarAktørId, IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PO_STATSBORGERSKAP")
    private Long id;

    @ChangeTracked
    @Embedded
    @AttributeOverride(name = "aktørId", column = @Column(name = "aktoer_id", updatable = false))
    private AktørId aktørId;

    @ChangeTracked
    @Embedded
    private DatoIntervallEntitet periode;

    @ChangeTracked
    @Convert(converter = Landkoder.KodeverdiConverter.class)
    @Column(name="statsborgerskap", nullable = false)
    private Landkoder statsborgerskap;

    @ManyToOne(optional = false)
    @JoinColumn(name = "po_informasjon_id", nullable = false, updatable = false)
    private PersonInformasjonEntitet personopplysningInformasjon;

    StatsborgerskapEntitet() {
    }

    StatsborgerskapEntitet(StatsborgerskapEntitet statsborgerskap) {
        this.aktørId = statsborgerskap.getAktørId();
        this.periode = statsborgerskap.getPeriode();
        this.statsborgerskap = statsborgerskap.getStatsborgerskap();
    }


    @Override
    public String getIndexKey() {
        return IndexKey.createKey(aktørId, statsborgerskap, periode);
    }

    void setPersonopplysningInformasjon(PersonInformasjonEntitet personopplysningInformasjon) {
        this.personopplysningInformasjon = personopplysningInformasjon;
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


    public Landkoder getStatsborgerskap() {
        return statsborgerskap;
    }


    void setStatsborgerskap(Landkoder statsborgerskap) {
        this.statsborgerskap = statsborgerskap;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var entitet = (StatsborgerskapEntitet) o;
        return Objects.equals(aktørId, entitet.aktørId) &&
                Objects.equals(periode, entitet.periode) &&
                Objects.equals(statsborgerskap, entitet.statsborgerskap);
    }


    @Override
    public int hashCode() {
        return Objects.hash(aktørId, periode, statsborgerskap);
    }


    @Override
    public String toString() {
        var sb = new StringBuilder("StatsborgerskapEntitet{");
        sb.append("gyldighetsperiode=").append(periode);
        sb.append(", statsborgerskap=").append(statsborgerskap);
        sb.append('}');
        return sb.toString();
    }

}
