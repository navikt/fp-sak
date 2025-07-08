package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.eøs;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.eøs.EøsUttakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.eøs.EøsUttaksperiodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;

@ApplicationScoped
public class EøsUttakTjeneste {

    private BehandlingRepository behandlingRepository;
    private EøsUttakRepository eøsUttakRepository;

    public EøsUttakTjeneste() {
        // CDI
    }

    @Inject
    public EøsUttakTjeneste(BehandlingRepository behandlingRepository, EøsUttakRepository eøsUttakRepository) {
        this.behandlingRepository = behandlingRepository;
        this.eøsUttakRepository = eøsUttakRepository;
    }

    public List<EøsUttakDto.EøsUttakPeriodeDto> annenpartsPerioder(UuidDto uuidDto) {
        var behandlingId = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid()).getId();
        var grunnlagOpt = eøsUttakRepository.hentGrunnlag(behandlingId);
        return grunnlagOpt.stream()
            .flatMap(g -> g.getPerioder().stream())
            .map(this::tilDto)
            .toList();
    }

    private EøsUttakDto.EøsUttakPeriodeDto tilDto(EøsUttaksperiodeEntitet periode) {
        return new EøsUttakDto.EøsUttakPeriodeDto(
            periode.getPeriode().getFomDato(),
            periode.getPeriode().getTomDato(),
            periode.getTrekkdager().decimalValue(),
            periode.getTrekkonto()
        );
    }
}
