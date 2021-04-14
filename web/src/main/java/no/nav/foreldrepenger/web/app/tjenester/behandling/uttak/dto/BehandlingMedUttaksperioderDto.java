package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import no.nav.foreldrepenger.behandling.BehandlingIdDto;
import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class BehandlingMedUttaksperioderDto implements AbacDto {

    @Valid
    private BehandlingIdDto behandlingId;

    @Valid
    @NotNull
    @Size(min = 1, max = 1500)
    private List<UttakResultatPeriodeLagreDto> perioder;

    public BehandlingIdDto getBehandlingId() {
        return behandlingId;
    }

    public void setBehandlingId(BehandlingIdDto behandlingId) {
        this.behandlingId = behandlingId;
    }

    public List<UttakResultatPeriodeLagreDto> getPerioder() {
        return perioder;
    }

    public void setPerioder(List<UttakResultatPeriodeLagreDto> perioder) {
        this.perioder = perioder;
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        var abac = AbacDataAttributter.opprett();

        if(getBehandlingId().getBehandlingId()!=null) {
            abac.leggTil(AppAbacAttributtType.BEHANDLING_ID, getBehandlingId().getBehandlingId());
        } else if (getBehandlingId().getBehandlingUuid() != null) {
            abac.leggTil(AppAbacAttributtType.BEHANDLING_UUID, getBehandlingId().getBehandlingUuid());
        }
        return abac;
    }
}
