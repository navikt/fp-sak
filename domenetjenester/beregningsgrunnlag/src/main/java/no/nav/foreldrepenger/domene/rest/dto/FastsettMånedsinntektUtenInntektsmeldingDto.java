package no.nav.foreldrepenger.domene.rest.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FastsettMånedsinntektUtenInntektsmeldingDto(@NotNull @Size(max = 100) List<@Valid FastsettMånedsinntektUtenInntektsmeldingAndelDto> andelListe) {

}
