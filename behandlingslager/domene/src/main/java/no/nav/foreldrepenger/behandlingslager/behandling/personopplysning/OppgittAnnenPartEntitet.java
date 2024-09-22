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
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadAnnenPartType;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
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

    @ChangeTracked
    @Embedded
    @AttributeOverride(name = "aktørId", column = @Column(name = "aktoer_id", updatable = false))
    private AktørId aktørId;

    @Column(name = "navn")
    private String navn;

    @ChangeTracked
    @Column(name = "utl_person_ident")
    private String utenlandskPersonident;

    @ChangeTracked
    @Convert(converter = Landkoder.KodeverdiConverter.class)
    @Column(name="utl_person_ident_land", nullable = false)
    private Landkoder utenlandskPersonidentLand = Landkoder.UDEFINERT;

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
        this.utenlandskPersonident = mal.getUtenlandskPersonident();
        this.setUtenlandskPersonidentLand(mal.getUtenlandskFnrLand());
        this.setType(mal.getType());
    }

    public Long getId() {
        return id;
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

    void setType(SøknadAnnenPartType type) {
        this.type = type == null ? SøknadAnnenPartType.UDEFINERT : type;
    }

    void setNavn(String navn) {
        this.navn = navn;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof OppgittAnnenPartEntitet other)) {
            return false;
        }
        return Objects.equals(this.aktørId, other.getAktørId())
            && Objects.equals(this.getType(), other.getType())
            && Objects.equals(this.utenlandskPersonident, other.getUtenlandskPersonident())
            && Objects.equals(this.getUtenlandskFnrLand(), other.getUtenlandskFnrLand());
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktørId, getType(), utenlandskPersonident, getUtenlandskFnrLand());
    }

}
