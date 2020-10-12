package no.nav.foreldrepenger.web.app.tjenester.behandling.dto;

import java.util.List;

//TODO Utvid med flere relevante opplysninger
public class BehandlingOpprettingDto {

    private Boolean harSoknad;

    private List<BehandlingOpprettingMuligDto> kanOppretteBehandlingType;

    public BehandlingOpprettingDto(Boolean harSoknad, List<BehandlingOpprettingMuligDto> kanOppretteBehandlingType) {
        this.harSoknad = harSoknad;
        this.kanOppretteBehandlingType = kanOppretteBehandlingType;
    }

    public Boolean getHarSoknad() {
        return harSoknad;
    }

    public List<BehandlingOpprettingMuligDto> getKanOppretteBehandlingType() {
        return kanOppretteBehandlingType;
    }
}
