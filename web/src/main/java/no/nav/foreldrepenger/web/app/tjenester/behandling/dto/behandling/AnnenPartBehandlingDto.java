package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import java.util.UUID;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class AnnenPartBehandlingDto {

    private Saksnummer saksnr;

    private Long behandlingId;
    private UUID behandlingUuid;

    private AnnenPartBehandlingDto(Saksnummer saksnummer, Long behandlingId, UUID uuid) {
        this.saksnr = saksnummer;
        this.behandlingId = behandlingId;
        this.behandlingUuid = uuid;
    }

    public static AnnenPartBehandlingDto mapFra(Behandling behandling) {
        return new AnnenPartBehandlingDto(
                behandling.getFagsak().getSaksnummer(),
                behandling.getId(), behandling.getUuid());
    }

    public Saksnummer getSaksnr() {
        return saksnr;
    }

    public void setSaksnr(Saksnummer saksnr) {
        this.saksnr = saksnr;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public void setBehandlingUuid(UUID behandlingUuid) {
        this.behandlingUuid = behandlingUuid;
    }
}
