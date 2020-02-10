package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto;

import java.time.LocalDate;
import java.util.Optional;

public class UidentifisertBarnDto {
    private LocalDate fodselsdato;
    private LocalDate dodsdato;

    UidentifisertBarnDto() {
        // For Jackson
    }

    public UidentifisertBarnDto(LocalDate fodselsdato, LocalDate dodsdato) {
        this.fodselsdato = fodselsdato;
        this.dodsdato = dodsdato;
    }

    public LocalDate getFodselsdato() {
        return fodselsdato;
    }

    public Optional<LocalDate> getDodsdato() {
        return Optional.ofNullable(dodsdato);
    }
}
