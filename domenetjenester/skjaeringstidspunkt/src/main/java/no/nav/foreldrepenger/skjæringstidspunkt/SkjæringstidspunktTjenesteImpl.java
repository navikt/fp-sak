package no.nav.foreldrepenger.skjæringstidspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@ApplicationScoped
public class SkjæringstidspunktTjenesteImpl implements SkjæringstidspunktTjeneste {

    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste engangsstønadTjeneste;
    private SkjæringstidspunktTjeneste foreldrepengerTjeneste;
    private SkjæringstidspunktTjeneste svangerskapspengerTjeneste;

    SkjæringstidspunktTjenesteImpl() {
        // CDI
    }

    @Inject
    public SkjæringstidspunktTjenesteImpl(BehandlingRepository behandlingRepository,
                                          @FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD) SkjæringstidspunktTjeneste engangsstønadTjeneste,
                                          @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) SkjæringstidspunktTjeneste foreldrepengerTjeneste,
                                          @FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER) SkjæringstidspunktTjeneste svangerskapspengerTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.engangsstønadTjeneste = engangsstønadTjeneste;
        this.foreldrepengerTjeneste = foreldrepengerTjeneste;
        this.svangerskapspengerTjeneste = svangerskapspengerTjeneste;
    }

    @Override
    public Skjæringstidspunkt getSkjæringstidspunkter(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (behandling.erYtelseBehandling()) {
            if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
                return engangsstønadTjeneste.getSkjæringstidspunkter(behandlingId);
            }
            if (FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType())) {
                return foreldrepengerTjeneste.getSkjæringstidspunkter(behandlingId);
            }
            if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(behandling.getFagsakYtelseType())) {
                return svangerskapspengerTjeneste.getSkjæringstidspunkter(behandlingId);
            }
            throw new IllegalStateException("Ukjent ytelse type.");
        }
        // returner tom container for andre behandlingtyper
        // (så ser vi om det evt. er noen call paths som kaller på noen form for skjæringstidspunkt)
        return Skjæringstidspunkt.builder().build();
    }

}
