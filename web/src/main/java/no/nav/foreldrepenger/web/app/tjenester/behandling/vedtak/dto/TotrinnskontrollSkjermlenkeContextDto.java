package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;

import java.util.List;

public class TotrinnskontrollSkjermlenkeContextDto {

    @NotNull private SkjermlenkeType skjermlenkeType;
    @NotNull private List<TotrinnskontrollAksjonspunkterDto> totrinnskontrollAksjonspunkter;

    public TotrinnskontrollSkjermlenkeContextDto(SkjermlenkeType skjermlenkeType, List<TotrinnskontrollAksjonspunkterDto> totrinnskontrollAksjonspunkter) {
        this.totrinnskontrollAksjonspunkter = totrinnskontrollAksjonspunkter;
        this.skjermlenkeType = skjermlenkeType;
    }

    public SkjermlenkeType getSkjermlenkeType() {
        return skjermlenkeType;
    }

    public List<TotrinnskontrollAksjonspunkterDto> getTotrinnskontrollAksjonspunkter() {
        return totrinnskontrollAksjonspunkter;
    }
}
