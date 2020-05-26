package no.nav.foreldrepenger.domene.arbeidsgiver.rest;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Organisasjon {

    @JsonProperty("organisasjonsnummer")
    private String organisasjonsnummer;
    @JsonProperty("type")
    private String type;
    @JsonProperty("navn")
    private Navn navn;
    @JsonProperty("organisasjonDetaljer")
    private OrganisasjonDetaljer organisasjonDetaljer;
    @JsonProperty("virksomhetDetaljer")
    private VirksomhetDetaljer virksomhetDetaljer;

    @JsonCreator
    public Organisasjon(@JsonProperty("organisasjonsnummer") String organisasjonsnummer,
                          @JsonProperty("type") String type,
                          @JsonProperty("navn") Navn navn,
                          @JsonProperty("organisasjonDetaljer") OrganisasjonDetaljer organisasjonDetaljer,
                          @JsonProperty("virksomhetDetaljer") VirksomhetDetaljer virksomhetDetaljer) {
        this.organisasjonsnummer = organisasjonsnummer;
        this.type = type;
        this.navn = navn;
        this.organisasjonDetaljer = organisasjonDetaljer;
        this.virksomhetDetaljer = virksomhetDetaljer;
    }

    public String getOrganisasjonsnummer() {
        return organisasjonsnummer;
    }

    public String getType() {
        return type;
    }

    public String getNavn() {
        return navn != null ? navn.getRedigertnavn() : null;
    }

    public LocalDate getRegistreringsdato() {
        return organisasjonDetaljer != null ? organisasjonDetaljer.getRegistreringsdato().toLocalDate() : null;
    }

    public LocalDate getOppstartsdato() {
        return virksomhetDetaljer != null ? virksomhetDetaljer.getOppstartsdato() : null;
    }

    public LocalDate getNedleggelsesdato() {
        return virksomhetDetaljer != null ? virksomhetDetaljer.getNedleggelsesdato() : null;
    }

    @Override
    public String toString() {
        return "Organisasjon{" +
            "organisasjonsnummer='" + organisasjonsnummer + '\'' +
            ", type='" + type + '\'' +
            ", navn=" + navn +
            ", organisasjonDetaljer=" + organisasjonDetaljer +
            ", virksomhetDetaljer=" + virksomhetDetaljer +
            '}';
    }

    static class Navn {

        @JsonProperty("redigertnavn")
        private String redigertnavn;

        @JsonCreator
        public Navn(@JsonProperty("redigertnavn") String redigertnavn) {
            this.redigertnavn = redigertnavn;
        }

        public String getRedigertnavn() {
            return redigertnavn;
        }

        @Override
        public String toString() {
            return "Navn{" +
                "redigertnavn='" + redigertnavn + '\'' +
                '}';
        }
    }

    static class OrganisasjonDetaljer {

        @JsonProperty("registreringsdato")
        private LocalDateTime registreringsdato;

        @JsonCreator
        public OrganisasjonDetaljer(@JsonProperty("registreringsdato") LocalDateTime registreringsdato) {
            this.registreringsdato = registreringsdato;
        }

        public LocalDateTime getRegistreringsdato() {
            return registreringsdato;
        }

        @Override
        public String toString() {
            return "OrganisasjonDetaljer{" +
                "registreringsdato=" + registreringsdato +
                '}';
        }
    }

    static class VirksomhetDetaljer {

        @JsonProperty("oppstartsdato")
        private LocalDate oppstartsdato;
        @JsonProperty("nedleggelsesdato")
        private LocalDate nedleggelsesdato;

        @JsonCreator
        public VirksomhetDetaljer(@JsonProperty("oppstartsdato") LocalDate oppstartsdato,
                                  @JsonProperty("nedleggelsesdato") LocalDate nedleggelsesdato) {
            this.oppstartsdato = oppstartsdato;
            this.nedleggelsesdato = nedleggelsesdato;
        }

        public LocalDate getOppstartsdato() {
            return oppstartsdato;
        }

        public LocalDate getNedleggelsesdato() {
            return nedleggelsesdato;
        }

        @Override
        public String toString() {
            return "VirksomhetDetaljer{" +
                "oppstartsdato=" + oppstartsdato +
                ", nedleggelsesdato=" + nedleggelsesdato +
                '}';
        }
    }

}

