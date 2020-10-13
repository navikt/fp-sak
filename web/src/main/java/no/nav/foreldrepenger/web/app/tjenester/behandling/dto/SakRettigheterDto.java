package no.nav.foreldrepenger.web.app.tjenester.behandling.dto;

import java.util.List;

public class SakRettigheterDto {

    private boolean sakSkalTilInfotrygd;

    private List<BehandlingOpprettingDto> behandlingTypeKanOpprettes;

    private List<BehandlingOperasjonerDto> behandlingTillatteOperasjoner;

    public SakRettigheterDto(boolean sakSkalTilInfotrygd, List<BehandlingOpprettingDto> behandlingTypeKanOpprettes, List<BehandlingOperasjonerDto> behandlingTillatteOperasjoner) {
        this.sakSkalTilInfotrygd = sakSkalTilInfotrygd;
        this.behandlingTypeKanOpprettes = behandlingTypeKanOpprettes;
        this.behandlingTillatteOperasjoner = behandlingTillatteOperasjoner;
    }

    public boolean isSakSkalTilInfotrygd() {
        return sakSkalTilInfotrygd;
    }

    public List<BehandlingOpprettingDto> getBehandlingTypeKanOpprettes() {
        return behandlingTypeKanOpprettes;
    }

    public List<BehandlingOperasjonerDto> getBehandlingTillatteOperasjoner() {
        return behandlingTillatteOperasjoner;
    }
}
