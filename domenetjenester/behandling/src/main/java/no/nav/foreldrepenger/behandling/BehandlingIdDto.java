package no.nav.foreldrepenger.behandling;


import java.util.Objects;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

/**
 * Referanse til en behandling.
 * Enten {@link #behandlingId} eller {@link #behandlingUuid} vil være satt.
 */
@JsonInclude(Include.NON_NULL)
public class BehandlingIdDto implements AbacDto {

    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long saksnummer;

    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long behandlingId;

    /**
     * Behandling UUID (nytt alternativ til intern behandlingId. Bør brukes av eksterne systemer).
     */
    @Valid
    private UUID behandlingUuid;

    public BehandlingIdDto() {
        behandlingId = null; // NOSONAR
    }

    /**
     * Default ctor for å instantiere med en type id. Støtter både Long id og UUID.
     */
    public BehandlingIdDto(String id) {
        Objects.requireNonNull(id, "behandlingId");
        if (id.contains("-")) {
            this.behandlingUuid = UUID.fromString(id);
        } else {
            this.behandlingId = Long.valueOf(id);
        }
    }

    public BehandlingIdDto(UuidDto uuidDto) {
        this.behandlingUuid = uuidDto.getBehandlingUuid();
    }

    public BehandlingIdDto(Long saksnummer, Long behandlingId, UUID behandlingUuid) {
        this.saksnummer = saksnummer;
        this.behandlingId = behandlingId;
        this.behandlingUuid = behandlingUuid;
    }

    public BehandlingIdDto(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    /**
     * Denne er kun intern nøkkel, bør ikke eksponeres ut men foreløpig støttes både Long id og UUID id for behandling på grensesnittene.
     */
    public Long getBehandlingId() {
        return behandlingId;
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public Long getSaksnummer() {
        return saksnummer;
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        AbacDataAttributter abac = AbacDataAttributter.opprett();
        if (behandlingId != null) {
            abac.leggTil(AppAbacAttributtType.BEHANDLING_ID, behandlingId);
        } else if (behandlingUuid != null) {
            abac.leggTil(AppAbacAttributtType.BEHANDLING_UUID, behandlingUuid);
        } else {
            throw new IllegalArgumentException("Må ha en av behandlingId/behandlingUuid spesifisert");
        }

        return abac;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '<' +
            (saksnummer == null ? "" : "saksnummer=" + saksnummer + ", ") +
            (behandlingId != null ? "behandlingId=" + behandlingId : "") +
            (behandlingId != null && behandlingUuid != null ? ", " : "") +
            (behandlingUuid != null ? "behandlingUuid=" + behandlingUuid : "") +
            '>';
    }
}
