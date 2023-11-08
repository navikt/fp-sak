package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import java.util.Optional;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.util.InputValideringRegex;

public class AktoerIdDto {

    @Size(max = 20)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private final String aktoerId;

    public AktoerIdDto(String aktoerId) {
        this.aktoerId = aktoerId;
    }

    public Optional<AktørId> get() {
        if (aktoerId.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new AktørId(aktoerId));
    }

    public String getAktoerId() {
        return aktoerId;
    }
}
