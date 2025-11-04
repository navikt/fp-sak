package no.nav.foreldrepenger.skjæringstidspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.Søknadsfristdatoer;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@ApplicationScoped
public class SøknadsperiodeFristTjenesteImpl implements SøknadsperiodeFristTjeneste {

    private BehandlingRepository behandlingRepository;
    private SøknadsperiodeFristTjeneste engangsstønadTjeneste;
    private SøknadsperiodeFristTjeneste foreldrepengerTjeneste;
    private SøknadsperiodeFristTjeneste svangerskapspengerTjeneste;

    SøknadsperiodeFristTjenesteImpl() {
        // CDI
    }

    @Inject
    public SøknadsperiodeFristTjenesteImpl(BehandlingRepository behandlingRepository,
                                           @FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD) SøknadsperiodeFristTjeneste engangsstønadTjeneste,
                                           @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) SøknadsperiodeFristTjeneste foreldrepengerTjeneste,
                                           @FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER) SøknadsperiodeFristTjeneste svangerskapspengerTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.engangsstønadTjeneste = engangsstønadTjeneste;
        this.foreldrepengerTjeneste = foreldrepengerTjeneste;
        this.svangerskapspengerTjeneste =svangerskapspengerTjeneste;
    }

    @Override
    public Søknadsfristdatoer finnSøknadsfrist(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (behandling.erYtelseBehandling()) {
            if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
                return engangsstønadTjeneste.finnSøknadsfrist(behandlingId);
            }
            if (FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType())) {
                return foreldrepengerTjeneste.finnSøknadsfrist(behandlingId);
            }
            if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(behandling.getFagsakYtelseType())) {
                return svangerskapspengerTjeneste.finnSøknadsfrist(behandlingId);
            }
            throw new IllegalStateException("Ukjent ytelse type.");
        }
        // returner tom container for andre behandlingtyper
        // (så ser vi om det evt. er noen call paths som kaller på noen form for skjæringstidspunkt)
        return Søknadsfristdatoer.builder().build();
    }

}
