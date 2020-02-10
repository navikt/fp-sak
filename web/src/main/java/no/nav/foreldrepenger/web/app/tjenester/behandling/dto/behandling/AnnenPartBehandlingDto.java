package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class AnnenPartBehandlingDto {

    private Saksnummer saksnr;
    
    // TODO (BehandlingIdDto): bør kunne støtte behandlingUuid også?
    private Long behandlingId;

    private AnnenPartBehandlingDto(Saksnummer saksnummer, Long behandlingId) {
        this.saksnr = saksnummer;
        this.behandlingId = behandlingId;
    }

    public static AnnenPartBehandlingDto mapFra(Behandling behandling) {
        return new AnnenPartBehandlingDto(
                behandling.getFagsak().getSaksnummer(),
                behandling.getId());
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
}
