package no.nav.foreldrepenger.domene.fpinntektsmelding;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonValue;

public record OpprettForespørselRequest(@NotNull @Valid AktørIdDto aktørId,
                                        @Valid OrganisasjonsnummerDto orgnummer,
                                        @NotNull LocalDate skjæringstidspunkt,
                                        @NotNull YtelseType ytelsetype,
                                        @NotNull @Valid SaksnummerDto fagsakSaksnummer,
                                        @Valid LocalDate førsteUttaksdato,
                                        @Valid List<OrganisasjonsnummerDto> organisasjonsnumre,
                                        @Valid @NotNull boolean migrering) {
    protected record AktørIdDto(@NotNull @JsonValue String id){
        @Override
        public String toString() {
            return getClass().getSimpleName() + "<" + maskerAktørId() + ">";
        }

        private String maskerAktørId() {
            if (id == null) {
                return "";
            }
            var length = id.length();
            if (length <= 4) {
                return "*".repeat(length);
            }
            return "*".repeat(length - 4) + id.substring(length - 4);
        }
    }
    protected enum YtelseType {
        FORELDREPENGER,
        SVANGERSKAPSPENGER
    }
}
