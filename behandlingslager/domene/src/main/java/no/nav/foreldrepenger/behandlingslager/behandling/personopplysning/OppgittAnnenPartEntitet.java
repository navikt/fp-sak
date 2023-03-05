package no.nav.foreldrepenger.behandlingslager.behandling.personopplysning;

import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadAnnenPartType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.HarAktørId;

/**
 * Entitetsklasse for søknad annen part.
 *
 * Implementert iht. builder pattern (ref. "Effective Java, 2. ed." J.Bloch).
 * Non-public constructors og setters, dvs. immutable.
 *
 * OBS: Legger man til nye felter så skal dette oppdateres mange steder:
 * builder, equals, hashcode etc.
 */
@Entity(name = "SøknadAnnenPart")
@Table(name = "SO_ANNEN_PART")
public class OppgittAnnenPartEntitet extends BaseEntitet implements HarAktørId {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SOEKNAD_ANNEN_PART")
    private Long id;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "aktoer_id", updatable = false)))
    private AktørId aktørId;

    @Column(name = "navn")
    private String navn;

    @Column(name = "utl_person_ident")
    private String utenlandskPersonident;

    @Convert(converter = Landkoder.KodeverdiConverter.class)
    @Column(name="utl_person_ident_land", nullable = false)
    private Landkoder utenlandskPersonidentLand = Landkoder.UDEFINERT;

    @Column(name = "ARSAK")
    private String årsak;

    @Column(name = "begrunnelse")
    private String begrunnelse;

    @Convert(converter = SøknadAnnenPartType.KodeverdiConverter.class)
    @Column(name = "type", nullable = false)
    private SøknadAnnenPartType type = SøknadAnnenPartType.UDEFINERT;

    OppgittAnnenPartEntitet() {
        // Hibernate
    }

    public OppgittAnnenPartEntitet(OppgittAnnenPartEntitet oppgittAnnenPartMal) {
        deepCopyFra(oppgittAnnenPartMal);  // NOSONAR - kommer ikke utenom "call to non-final method" her
    }

    void deepCopyFra(OppgittAnnenPartEntitet mal) {
        this.aktørId = mal.getAktørId();
        this.navn = mal.getNavn();
        this.begrunnelse = mal.getBegrunnelse();
        this.utenlandskPersonident = mal.getUtenlandskPersonident();
        this.årsak = mal.getÅrsak();
        this.setUtenlandskPersonidentLand(mal.getUtenlandskFnrLand());
        this.setType(mal.getType());
    }

    public Long getId() {
        return id;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    @Override
    public AktørId getAktørId() {
        return aktørId;
    }

    public String getNavn() {
        return navn;
    }

    public String getUtenlandskPersonident() {
        return utenlandskPersonident;
    }

    public Landkoder getUtenlandskFnrLand() {
        return Objects.equals(utenlandskPersonidentLand, Landkoder.UDEFINERT) ? null : utenlandskPersonidentLand;
    }

    public String getÅrsak() {
        return årsak;
    }

    public SøknadAnnenPartType getType() {
        return Objects.equals(SøknadAnnenPartType.UDEFINERT, type) ? null : type;
    }

    void setAktørId(AktørId aktørId) {
        this.aktørId = aktørId;
    }

    void setUtenlandskPersonident(String personident) {
        this.utenlandskPersonident = personident;
    }

    void setUtenlandskPersonidentLand(Landkoder personidentLand) {
        this.utenlandskPersonidentLand = personidentLand == null ? Landkoder.UDEFINERT : personidentLand;
    }

    void setÅrsak(String årsak) {
        this.årsak = årsak;
    }

    void setType(SøknadAnnenPartType type) {
        this.type = type == null ? SøknadAnnenPartType.UDEFINERT : type;
    }

    void setNavn(String navn) {
        this.navn = navn;
    }

    void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof OppgittAnnenPartEntitet)) {
            return false;
        }
        var other = (OppgittAnnenPartEntitet) obj;
        return Objects.equals(this.aktørId, other.getAktørId())
            && Objects.equals(this.begrunnelse, other.getBegrunnelse())
            && Objects.equals(this.getType(), other.getType())
            && Objects.equals(this.utenlandskPersonident, other.getUtenlandskPersonident())
            && Objects.equals(this.getUtenlandskFnrLand(), other.getUtenlandskFnrLand())
            && Objects.equals(this.årsak, other.getÅrsak());
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktørId, begrunnelse, getType(), utenlandskPersonident, getUtenlandskFnrLand(), årsak);
    }

}
