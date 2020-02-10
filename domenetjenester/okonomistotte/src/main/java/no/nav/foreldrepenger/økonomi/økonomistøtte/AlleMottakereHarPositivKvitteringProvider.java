package no.nav.foreldrepenger.økonomi.økonomistøtte;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;

@ApplicationScoped
public class AlleMottakereHarPositivKvitteringProvider {
    private BehandlingRepository behandlingRepository;
    private Instance<AlleMottakereHarPositivKvittering> alleMottakereHarPositivKvitteringInstance;

    AlleMottakereHarPositivKvitteringProvider() {
        // CDI
    }

    @Inject
    public AlleMottakereHarPositivKvitteringProvider(BehandlingRepositoryProvider repositoryProvider,
                                                     @Any Instance<AlleMottakereHarPositivKvittering> alleMottakereHarPositivKvitteringInstance) {
        this.alleMottakereHarPositivKvitteringInstance = alleMottakereHarPositivKvitteringInstance;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    AlleMottakereHarPositivKvittering getTjeneste(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        String ytelseKode = behandling.getFagsakYtelseType().getKode();
        FagsakYtelseTypeRef.FagsakYtelseTypeRefLiteral fagsakTypeRef = new FagsakYtelseTypeRef.FagsakYtelseTypeRefLiteral(ytelseKode);
        Instance<AlleMottakereHarPositivKvittering> selected = alleMottakereHarPositivKvitteringInstance.select(fagsakTypeRef);
        if (selected.isAmbiguous()) {
            throw new IllegalArgumentException("Mer enn en implementasjon funnet for fagsakYtelseType: " + ytelseKode);
        } else if (selected.isUnsatisfied()) {
            throw new IllegalArgumentException("Ingen implementasjoner funnet for fagsakYtelseType: " + ytelseKode);
        }
        AlleMottakereHarPositivKvittering alleMottakereHarPositivKvittering = selected.get();
        if (alleMottakereHarPositivKvittering.getClass().isAnnotationPresent(Dependent.class)) {
            throw new IllegalStateException("Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + alleMottakereHarPositivKvittering.getClass());
        }
        return alleMottakereHarPositivKvittering;
    }
}
