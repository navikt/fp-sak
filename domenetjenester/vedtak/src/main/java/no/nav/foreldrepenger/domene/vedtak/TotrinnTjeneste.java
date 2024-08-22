package no.nav.foreldrepenger.domene.vedtak;

import java.util.Collection;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.totrinn.TotrinnRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.totrinn.Totrinnsvurdering;

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

    public Collection<Totrinnsvurdering> hentTotrinnaksjonspunktvurderinger(Long behandlingId) {
        return totrinnRepository.hentTotrinnaksjonspunktvurderinger(behandlingId);
    }

    public void settNyeTotrinnaksjonspunktvurderinger(List<Totrinnsvurdering> vurderinger) {
        totrinnRepository.lagreOgFlush(vurderinger);
    }
}
