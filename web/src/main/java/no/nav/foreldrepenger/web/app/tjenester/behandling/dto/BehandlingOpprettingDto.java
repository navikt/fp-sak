package no.nav.foreldrepenger.web.app.tjenester.behandling.dto;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;

//TODO Utvid med flere relevante opplysninger
public class BehandlingOpprettingDto {

    private Boolean harSoknad;

    private List<BehandlingType> kanOppretteBehandlingType;

    public BehandlingOpprettingDto(Boolean harSoknad, List<BehandlingType> kanOppretteBehandlingType) {
        this.harSoknad = harSoknad;
        this.kanOppretteBehandlingType = kanOppretteBehandlingType;
    }

    public Boolean getHarSoknad() {
        return harSoknad;
    }

    public List<BehandlingType> getKanOppretteBehandlingType() {
        return kanOppretteBehandlingType;
    }
}
