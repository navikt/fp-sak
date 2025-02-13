package no.nav.foreldrepenger.web.app.tjenester.behandling.verge.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.QueryParam;

import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeType;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;
import no.nav.vedtak.util.InputValideringRegex;

import java.time.LocalDate;
import java.util.UUID;

public class NyVergeDto implements AbacDto {
    @Size(max = 100)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String navn;

    @Digits(integer = 11, fraction = 0)
    private String fnr;
    private LocalDate gyldigFom;
    private LocalDate gyldigTom;

    @NotNull
    @ValidKodeverk
    private VergeType vergeType;

    @Pattern(regexp = "[\\d]{9}")
    private String organisasjonsnummer;

    @NotNull
    @Min(0)
    @Max(Long.MAX_VALUE)
    @QueryParam("behandlingVersjon")
    private Long behandlingVersjon;

    @QueryParam("behandlingUuid")
    @Valid
    @NotNull
    private UUID behandlingUuid;


    public NyVergeDto() {
    }

    public NyVergeDto(String navn, String fnr, LocalDate gyldigFom, LocalDate gyldigTom, VergeType vergeType, String organisasjonsnummer) {
        this.navn = navn;
        this.fnr = fnr;
        this.gyldigFom = gyldigFom;
        this.gyldigTom = gyldigTom;
        this.vergeType = vergeType;
        this.organisasjonsnummer = organisasjonsnummer;
    }

    public String getNavn() {
        return navn;
    }

    public String getFnr() {
        return fnr;
    }

    public LocalDate getGyldigFom() {
        return gyldigFom;
    }

    public LocalDate getGyldigTom() {
        return gyldigTom;
    }

    public VergeType getVergeType() {
        return vergeType;
    }

    public String getOrganisasjonsnummer() {
        return organisasjonsnummer;
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public Long getBehandlingVersjon() {
        return behandlingVersjon;
    }


    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett();
    }
}
