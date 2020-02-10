package no.nav.foreldrepenger.behandling;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

import javax.validation.Valid;
import java.util.UUID;

public class UuidDto implements AbacDto {

    public static final String NAME = "uuid";

    public static final String DESC = "behandlingUUID";

    /**
     * Behandling UUID (nytt alternativ til intern behandlingId. Bør brukes av eksterne systemer).
     */
    @Valid
    private UUID behandlingUuid;

    public UuidDto(String behandlingUuid) {
        this.behandlingUuid = UUID.fromString(behandlingUuid);
    }

    public UuidDto(UUID behandlingUuid) {
        this.behandlingUuid = behandlingUuid;
    }

    @JsonProperty(NAME)
    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.BEHANDLING_UUID, behandlingUuid);
    }

}
