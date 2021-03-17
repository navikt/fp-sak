package no.nav.foreldrepenger.behandlingslager.virksomhet;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.tilMaskertNummer;

import java.time.LocalDate;
import java.util.Objects;

public class Virksomhet {

    private String orgnr;
    private String navn;
    private LocalDate registrert;
    private LocalDate avsluttet;
    private LocalDate oppstart;
    private Organisasjonstype organisasjonstype = Organisasjonstype.UDEFINERT;

    public Virksomhet() {
    }

    public String getOrgnr() {
        return orgnr;
    }

    public String getNavn() {
        return navn;
    }

    public LocalDate getRegistrert() {
        return registrert;
    }

    public LocalDate getOppstart() {
        return oppstart;
    }

    public LocalDate getAvslutt() {
        return avsluttet;
    }

    public boolean erKunstig() {
        return Organisasjonstype.KUNSTIG.equals(getOrganisasjonstype());
    }

    public Organisasjonstype getOrganisasjonstype() {
        return organisasjonstype;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof Virksomhet)) {
            return false;
        }
        Virksomhet other = (Virksomhet) obj;
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
            ", orgnr=" + tilMaskertNummer(orgnr) +
            '}';
    }

    public static Builder getBuilder() {
        return new Builder();
    }

    public static class Builder {
        private Virksomhet mal;

        /**
         * For oppretting av
         */
        public Builder() {
            this.mal = new Virksomhet();
        }

        /**
         * For oppdatering av data fra Enhetsregisteret
         * <p>
         * Tillater mutering av entitet da vi ville mistet alle eksisterende koblinger ved oppdatering
         *
         * @param virksomhet virksomheten som skal oppdaters
         */
        public Builder(Virksomhet virksomhet) {
            this.mal = virksomhet; // NOSONAR
        }

        public Builder medOrgnr(String orgnr) {
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

        public Builder medOrganisasjonstype(Organisasjonstype organisasjonsType) {
            this.mal.organisasjonstype = organisasjonsType;
            return this;
        }

        public Virksomhet build() {
            return mal;
        }
    }
}
