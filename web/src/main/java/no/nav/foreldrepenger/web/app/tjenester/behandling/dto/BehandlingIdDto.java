package no.nav.foreldrepenger.web.app.tjenester.behandling.dto;

import java.util.Objects;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Referanse til en behandling. Enten {@link #behandlingId} eller
 * {@link #behandlingUuid} vil være satt.
 */
@JsonInclude(Include.NON_NULL)
public class BehandlingIdDto  {

    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long behandlingId;

    /**
     * Behandling UUID (nytt alternativ til intern behandlingId. Bør brukes av
     * eksterne systemer).
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

    public BehandlingIdDto(Long behandlingId, UUID behandlingUuid) {
        this.behandlingId = behandlingId;
        this.behandlingUuid = behandlingUuid;
    }

    public BehandlingIdDto(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    /**
     * Denne er kun intern nøkkel, bør ikke eksponeres ut men foreløpig støttes både
     * Long id og UUID id for behandling på grensesnittene.
     */
    public Long getBehandlingId() {
        return behandlingId;
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '<' +
                (behandlingId != null ? "behandlingId=" + behandlingId : "") +
                ((behandlingId != null) && (behandlingUuid != null) ? ", " : "") +
                (behandlingUuid != null ? "behandlingUuid=" + behandlingUuid : "") +
                '>';
    }
}
