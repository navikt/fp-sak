package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import java.util.Optional;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;
import no.nav.vedtak.util.InputValideringRegex;

public class AktoerIdDto implements AbacDto {

    @Size(min = 0, max = 20)
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

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.AKTØR_ID, aktoerId);
    }
}
