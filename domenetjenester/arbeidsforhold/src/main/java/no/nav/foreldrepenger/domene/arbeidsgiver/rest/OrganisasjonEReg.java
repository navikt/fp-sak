package no.nav.foreldrepenger.domene.arbeidsgiver.rest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class OrganisasjonEReg {

    @JsonProperty("organisasjonsnummer")
    private String organisasjonsnummer;
    @JsonProperty("type")
    private OrganisasjonstypeEReg type;
    @JsonProperty("navn")
    private Navn navn;
    @JsonProperty("organisasjonDetaljer")
    private OrganisasjonDetaljer organisasjonDetaljer;
    @JsonProperty("virksomhetDetaljer")
    private VirksomhetDetaljer virksomhetDetaljer;

    private OrganisasjonEReg() {
    }

    public String getOrganisasjonsnummer() {
        return organisasjonsnummer;
    }

    public OrganisasjonstypeEReg getType() {
        return type;
    }

    public String getNavn() {
        return navn != null ? navn.getNavn() : null;
    }

    public LocalDate getRegistreringsdato() {
        return organisasjonDetaljer != null ? organisasjonDetaljer.getRegistreringsdato().toLocalDate() : null;
    }

    public LocalDate getOpphørsdato() {
        return organisasjonDetaljer != null ? organisasjonDetaljer.getOpphoersdato() : null;
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

    private static class Navn {

        @JsonProperty("navnelinje1")
        private String navnelinje1;
        @JsonProperty("navnelinje2")
        private String navnelinje2;
        @JsonProperty("navnelinje3")
        private String navnelinje3;
        @JsonProperty("navnelinje4")
        private String navnelinje4;
        @JsonProperty("navnelinje5")
        private String navnelinje5;

        private Navn() {
        }

        private String getNavn() {
            return Stream.of(navnelinje1, navnelinje2, navnelinje3, navnelinje4, navnelinje5)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(n -> !n.isEmpty())
                .reduce("", (a, b) -> a + " " + b).trim();
        }

        @Override
        public String toString() {
            return "Navn{" +
                "navn='" + getNavn() + '\'' +
                '}';
        }
    }

    private static class OrganisasjonDetaljer {

        @JsonProperty("registreringsdato")
        private LocalDateTime registreringsdato;
        @JsonProperty("opphoersdato")
        private LocalDate opphoersdato;

        private OrganisasjonDetaljer() {
        }

        private LocalDateTime getRegistreringsdato() {
            return registreringsdato;
        }

        private LocalDate getOpphoersdato() {
            return opphoersdato;
        }

        @Override
        public String toString() {
            return "OrganisasjonDetaljer{" +
                "registreringsdato=" + registreringsdato +
                '}';
        }
    }

    private static class VirksomhetDetaljer {

        @JsonProperty("oppstartsdato")
        private LocalDate oppstartsdato;
        @JsonProperty("nedleggelsesdato")
        private LocalDate nedleggelsesdato;

        private VirksomhetDetaljer() {
        }

        private LocalDate getOppstartsdato() {
            return oppstartsdato;
        }

        private LocalDate getNedleggelsesdato() {
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

