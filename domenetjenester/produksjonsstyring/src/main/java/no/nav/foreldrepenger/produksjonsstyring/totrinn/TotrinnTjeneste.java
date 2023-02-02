package no.nav.foreldrepenger.produksjonsstyring.totrinn;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/** Totrinn tjeneste som eksponeres ut av modulen.*/
@ApplicationScoped
public class TotrinnTjeneste {

    private TotrinnRepository totrinnRepository;

    TotrinnTjeneste() {
        // CDI
    }

    @Inject
    public TotrinnTjeneste(TotrinnRepository totrinnRepository) {
        this.totrinnRepository = totrinnRepository;
    }

    public Optional<Totrinnresultatgrunnlag> hentTotrinngrunnlagHvisEksisterer(Long behandlingId) {
        return totrinnRepository.hentTotrinngrunnlag(behandlingId);
    }

    public Collection<Totrinnsvurdering> hentTotrinnaksjonspunktvurderinger(Long behandlingId) {
        return totrinnRepository.hentTotrinnaksjonspunktvurderinger(behandlingId);
    }

    public void settNyeTotrinnaksjonspunktvurderinger(List<Totrinnsvurdering> vurderinger) {
        totrinnRepository.lagreOgFlush(vurderinger);
    }

    public void lagreNyttTotrinnresultat(Totrinnresultatgrunnlag totrinnresultatgrunnlag) {
        totrinnRepository.lagreOgFlush(totrinnresultatgrunnlag);
    }
}
