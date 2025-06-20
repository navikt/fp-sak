package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.eøs;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.eøs.EøsUttakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.eøs.EøsUttaksperiodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;

@ApplicationScoped
public class AvklarUttakEøsForAnnenforelderTjeneste {

    private BehandlingRepository behandlingRepository;
    private EøsUttakRepository eøsUttakRepository;

    public AvklarUttakEøsForAnnenforelderTjeneste() {
        // CDI
    }

    @Inject
    public AvklarUttakEøsForAnnenforelderTjeneste(BehandlingRepository behandlingRepository, EøsUttakRepository eøsUttakRepository) {
        this.behandlingRepository = behandlingRepository;
        this.eøsUttakRepository = eøsUttakRepository;
    }

    public List<AvklarUttakEøsForAnnenforelderDto.EøsUttakPeriodeDto> annenpartsPerioder(@NotNull @Valid UuidDto uuidDto) {
        var behandlingId = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid()).getId();
        var grunnlagOpt = eøsUttakRepository.hentGrunnlag(behandlingId);
        return grunnlagOpt.stream()
            .flatMap(g -> g.getSaksbehandlerPerioder().getPerioder().stream())
            .map(this::tilDto)
            .toList();
    }

    private AvklarUttakEøsForAnnenforelderDto.EøsUttakPeriodeDto tilDto(EøsUttaksperiodeEntitet periode) {
        return new AvklarUttakEøsForAnnenforelderDto.EøsUttakPeriodeDto(
            periode.getPeriode().getFomDato(),
            periode.getPeriode().getTomDato(),
            periode.getTrekkdager().decimalValue(),
            periode.getTrekkonto()
        );
    }
}
