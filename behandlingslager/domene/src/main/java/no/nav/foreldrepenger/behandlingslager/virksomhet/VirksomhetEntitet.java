package no.nav.foreldrepenger.behandlingslager.virksomhet;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.vedtak.util.FPDateUtil;

@Entity(name = "Virksomhet")
@Table(name = "VIRKSOMHET", uniqueConstraints = @UniqueConstraint(columnNames = {"orgnr"}))
public class VirksomhetEntitet extends BaseEntitet implements Virksomhet, IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_VIRKSOMHET")
    private Long id;

    @Column(name = "orgnr", unique = true, nullable = false, updatable = false)
    @ChangeTracked
    private String orgnr;

    @Column(name = "navn")
    @ChangeTracked
    private String navn;

    @Column(name = "registrert")
    @ChangeTracked
    private LocalDate registrert;

    @Column(name = "avsluttet")
    @ChangeTracked
    private LocalDate avsluttet;

    @Column(name = "oppstart")
    @ChangeTracked
    private LocalDate oppstart;

    @Column(name = "opplysninger_oppdatert_tid", nullable = false)
    @ChangeTracked
    private LocalDateTime opplysningerOppdatertTidspunkt = LocalDateTime.now();

    @ChangeTracked
    @Convert(converter = Organisasjonstype.KodeverdiConverter.class)
    @Column(name="organisasjonstype", nullable = false)
    private Organisasjonstype organisasjonstype = Organisasjonstype.UDEFINERT;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public VirksomhetEntitet() {
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(orgnr);
    }

    @Override
    public String getOrgnr() {
        return orgnr;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public LocalDate getRegistrert() {
        return registrert;
    }

    @Override
    public LocalDate getOppstart() {
        return oppstart;
    }

    @Override
    public LocalDate getAvslutt() {
        return avsluttet;
    }
    
    @Override
    public boolean erKunstig() {
        return Organisasjonstype.KUNSTIG.equals(getOrganisasjonstype());
    }

    @Override
    public boolean skalRehentes() {
        return opplysningerOppdatertTidspunkt.isBefore(FPDateUtil.nå().minusDays(1));
    }

    @Override
    public Organisasjonstype getOrganisasjonstype() {
        return organisasjonstype;
    }

    /**
     * @return oppdatert tidspunkt
     */
    public LocalDateTime getOpplysningerOppdatertTidspunkt() {
        return opplysningerOppdatertTidspunkt;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof VirksomhetEntitet)) {
            return false;
        }
        VirksomhetEntitet other = (VirksomhetEntitet) obj;
        return Objects.equals(this.getOrgnr(), other.getOrgnr());
    }

    @Override
    public int hashCode() {
        return Objects.hash(orgnr);
    }

    @Override
    public String toString() {
        return "Virksomhet{" +
            "navn=" + navn +
            ", orgnr=" + orgnr +
            '}';
    }

    public static class Builder {
        private VirksomhetEntitet mal;

        /**
         * For oppretting av
         */
        public Builder() {
            this.mal = new VirksomhetEntitet();
        }

        /**
         * For oppdatering av data fra Enhetsregisteret
         * <p>
         * Tillater mutering av entitet da vi ville mistet alle eksisterende koblinger ved oppdatering
         *
         * @param virksomhet virksomheten som skal oppdaters
         */
        public Builder(Virksomhet virksomhet) {
            this.mal = (VirksomhetEntitet) virksomhet; // NOSONAR
        }

        public Builder medOrgnr(String orgnr) {
            if (this.mal.id != null) {
                throw new IllegalStateException("Skal ikke manipulere orgnr på allerede persistert objekt.");
            }
            this.mal.orgnr = orgnr;
            return this;
        }

        public Builder medNavn(String navn) {
            this.mal.navn = navn;
            return this;
        }

        public Builder medOppstart(LocalDate oppstart) {
            this.mal.oppstart = oppstart;
            return this;
        }

        public Builder medAvsluttet(LocalDate avsluttet) {
            this.mal.avsluttet = avsluttet;
            return this;
        }

        public Builder medRegistrert(LocalDate registrert) {
            this.mal.registrert = registrert;
            return this;
        }

        public Builder oppdatertOpplysningerNå() {
            this.mal.opplysningerOppdatertTidspunkt = FPDateUtil.nå();
            return this;
        }

        public Builder medOrganisasjonstype(Organisasjonstype organisasjonsType) {
            this.mal.organisasjonstype = organisasjonsType;
            return this;
        }

        public VirksomhetEntitet build() {
            return mal;
        }
    }
}
