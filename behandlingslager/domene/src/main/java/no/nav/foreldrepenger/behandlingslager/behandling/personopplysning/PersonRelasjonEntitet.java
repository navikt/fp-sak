package no.nav.foreldrepenger.behandlingslager.behandling.personopplysning;

import jakarta.persistence.*;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.HarAktørId;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

import java.util.Objects;

@Entity(name = "PersonopplysningRelasjon")
@Table(name = "PO_RELASJON")
public class PersonRelasjonEntitet extends BaseEntitet implements HarAktørId, IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PO_RELASJON")
    private Long id;

    @Embedded
    @AttributeOverride(name = "aktørId", column = @Column(name = "fra_aktoer_id", updatable = false, nullable=false))
    @ChangeTracked
    private AktørId fraAktørId;

    @Embedded
    @AttributeOverride(name = "aktørId", column = @Column(name = "til_aktoer_id", updatable = false, nullable=false))
    @ChangeTracked
    private AktørId tilAktørId;

    @Convert(converter = RelasjonsRolleType.KodeverdiConverter.class)
    @Column(name="relasjonsrolle", nullable = false)
    @ChangeTracked
    private RelasjonsRolleType relasjonsrolle;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "har_samme_bosted")
    @ChangeTracked
    private Boolean harSammeBosted;

    @ManyToOne(optional = false)
    @JoinColumn(name = "po_informasjon_id", nullable = false, updatable = false)
    private PersonInformasjonEntitet personopplysningInformasjon;

    PersonRelasjonEntitet() {
    }

    PersonRelasjonEntitet(PersonRelasjonEntitet relasjon) {
        this.fraAktørId = relasjon.getAktørId();
        this.tilAktørId = relasjon.getTilAktørId();
        this.relasjonsrolle = relasjon.getRelasjonsrolle();
        this.harSammeBosted = relasjon.getHarSammeBosted();
    }


    @Override
    public String getIndexKey() {
        return IndexKey.createKey(fraAktørId, this.relasjonsrolle, this.tilAktørId);
    }

    void setFraAktørId(AktørId fraAktørId) {
        this.fraAktørId = fraAktørId;
    }

    void setTilAktørId(AktørId tilAktørId) {
        this.tilAktørId = tilAktørId;
    }

    void setHarSammeBosted(Boolean harSammeBosted) {
        this.harSammeBosted = harSammeBosted;
    }

    void setRelasjonsrolle(RelasjonsRolleType relasjonsrolle) {
        this.relasjonsrolle = relasjonsrolle;
    }

    void setPersonopplysningInformasjon(PersonInformasjonEntitet personopplysningInformasjon) {
        this.personopplysningInformasjon = personopplysningInformasjon;
    }


    @Override
    public AktørId getAktørId() {
        return fraAktørId;
    }


    public AktørId getTilAktørId() {
        return tilAktørId;
    }


    public RelasjonsRolleType getRelasjonsrolle() {
        return relasjonsrolle;
    }


    public Boolean getHarSammeBosted() {
        return harSammeBosted;
    }


    @Override
    public String toString() {
        var sb = new StringBuilder("PersonRelasjonEntitet{");
        sb.append("relasjonsrolle=").append(relasjonsrolle);
        sb.append('}');
        return sb.toString();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var entitet = (PersonRelasjonEntitet) o;
        return Objects.equals(fraAktørId, entitet.fraAktørId) &&
                Objects.equals(tilAktørId, entitet.tilAktørId) &&
                Objects.equals(harSammeBosted, entitet.harSammeBosted) &&
                Objects.equals(relasjonsrolle, entitet.relasjonsrolle);
    }


    @Override
    public int hashCode() {
        return Objects.hash(fraAktørId, tilAktørId, harSammeBosted, relasjonsrolle);
    }

}
