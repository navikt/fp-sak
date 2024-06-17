package no.nav.foreldrepenger.skjæringstidspunkt;

import java.time.LocalDate;

import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;

/**
 * Brukes som factory for å gi spesifikk tjeneste avh. av ytelse.
 */
public class SkjæringstidspunktRegisterinnhentingTjenesteImpl implements SkjæringstidspunktRegisterinnhentingTjeneste {

    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktRegisterinnhentingTjeneste engangsstønad;
    private SkjæringstidspunktRegisterinnhentingTjeneste foreldrepenger;
    private SkjæringstidspunktRegisterinnhentingTjeneste svangerskapspenger;

    public SkjæringstidspunktRegisterinnhentingTjenesteImpl() {
        // for CDI
    }

    @Inject
    public SkjæringstidspunktRegisterinnhentingTjenesteImpl(BehandlingRepository behandlingRepository,
                                                            @FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD) SkjæringstidspunktRegisterinnhentingTjeneste engangsstønad,
                                                            @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) SkjæringstidspunktRegisterinnhentingTjeneste foreldrepenger,
                                                            @FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER) SkjæringstidspunktRegisterinnhentingTjeneste svangerskapspenger) {
        this.behandlingRepository = behandlingRepository;
        this.engangsstønad = engangsstønad;
        this.foreldrepenger = foreldrepenger;
        this.svangerskapspenger = svangerskapspenger;
    }

    @Override
    public LocalDate utledSkjæringstidspunktForRegisterInnhenting(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var ytelseType = behandling.getFagsakYtelseType();
        if (FagsakYtelseType.ENGANGSTØNAD.equals(ytelseType)) {
            return engangsstønad.utledSkjæringstidspunktForRegisterInnhenting(behandlingId);
        }
        if (FagsakYtelseType.FORELDREPENGER.equals(ytelseType)) {
            return foreldrepenger.utledSkjæringstidspunktForRegisterInnhenting(behandlingId);
        }
        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(ytelseType)) {
            return svangerskapspenger.utledSkjæringstidspunktForRegisterInnhenting(behandlingId);
        }
        throw new IllegalStateException(
            "Utvikler-feil: har ikke " + SkjæringstidspunktRegisterinnhentingTjeneste.class.getName() + " for behandling " + behandlingId
                + ", ytelseType=" + ytelseType);
    }

    @Override
    public SimpleLocalDateInterval vurderOverstyrtStartdatoForRegisterInnhenting(Long behandlingId, SimpleLocalDateInterval intervall) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var ytelseType = behandling.getFagsakYtelseType();
        return FagsakYtelseType.FORELDREPENGER.equals(ytelseType) ? foreldrepenger.vurderOverstyrtStartdatoForRegisterInnhenting(behandlingId,
            intervall) : intervall;
    }

}
