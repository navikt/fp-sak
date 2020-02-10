package no.nav.foreldrepenger.skjæringstidspunkt;

import java.time.LocalDate;

import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

/** Brukes som factory for å gi spesifikk tjeneste avh. av ytelse. */
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
                                                            @FagsakYtelseTypeRef("ES") SkjæringstidspunktRegisterinnhentingTjeneste engangsstønad,
                                                            @FagsakYtelseTypeRef("FP") SkjæringstidspunktRegisterinnhentingTjeneste foreldrepenger,
                                                            @FagsakYtelseTypeRef("SVP") SkjæringstidspunktRegisterinnhentingTjeneste svangerskapspenger) {
        this.behandlingRepository = behandlingRepository;
        this.engangsstønad = engangsstønad;
        this.foreldrepenger = foreldrepenger;
        this.svangerskapspenger = svangerskapspenger;
    }

    @Override
    public LocalDate utledSkjæringstidspunktForRegisterInnhenting(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        FagsakYtelseType ytelseType = behandling.getFagsakYtelseType();
        if (FagsakYtelseType.ENGANGSTØNAD.equals(ytelseType)) {
            return engangsstønad.utledSkjæringstidspunktForRegisterInnhenting(behandlingId);
        } else if (FagsakYtelseType.FORELDREPENGER.equals(ytelseType)) {
            return foreldrepenger.utledSkjæringstidspunktForRegisterInnhenting(behandlingId);
        } else if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(ytelseType)) {
            return svangerskapspenger.utledSkjæringstidspunktForRegisterInnhenting(behandlingId);
        } else {
            throw new IllegalStateException("Utvikler-feil: har ikke " + SkjæringstidspunktRegisterinnhentingTjeneste.class.getName() + " for behandling "
                + behandlingId + ", ytelseType=" + ytelseType);
        }
    }

}
