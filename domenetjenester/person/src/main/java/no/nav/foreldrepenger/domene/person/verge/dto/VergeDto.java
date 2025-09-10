package no.nav.foreldrepenger.domene.person.verge.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeType;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.util.InputValideringRegex;

public record VergeDto(@NotNull @ValidKodeverk VergeType vergeType,
                       @NotNull LocalDate gyldigFom,
                       LocalDate gyldigTom,
                       @NotNull@Size(max = 100) @Pattern(regexp = InputValideringRegex.FRITEKST) String navn,
                       @Pattern(regexp = "^\\d{11}$") String fnr,
                       @Pattern(regexp = "^\\d{9}$") String organisasjonsnummer) {

    public static VergeDto tomPayload() {
        return new VergeDto(VergeType.ANNEN_F, null, null, null, null, null);
    }

    public static VergeDto person(VergeType vergeType, LocalDate gyldigFom, LocalDate gyldigTom,
                                  String navn, String fnr) {
        return new VergeDto(vergeType, gyldigFom, gyldigTom, navn, fnr, null);
    }

    public static VergeDto organisasjon(VergeType vergeType, LocalDate gyldigFom, LocalDate gyldigTom,
                                  String navn, String organisasjonsnummer) {
        return new VergeDto(vergeType, gyldigFom, gyldigTom, navn, null, organisasjonsnummer);
    }

    @Override
    public String toString() {
        return "VergeDto{" + "vergeType=" + vergeType + ", gyldigFom=" + gyldigFom + ", gyldigTom=" + gyldigTom + '}';
    }

}
