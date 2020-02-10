package no.nav.foreldrepenger.mottak.hendelser;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.hendelser.HendelseSorteringRepository;
import no.nav.foreldrepenger.domene.typer.AktørId;

@ApplicationScoped
public class HendelseSorteringTjeneste {

    private HendelseSorteringRepository sorteringRepository;

    HendelseSorteringTjeneste() {
        // CDI
    }

    @Inject
    public HendelseSorteringTjeneste(HendelseSorteringRepository sorteringRepository) {
        this.sorteringRepository = sorteringRepository;
    }

    public List<AktørId> hentAktørIderTilknyttetSak(List<AktørId> aktørIdList) {
        return sorteringRepository.hentEksisterendeAktørIderMedSak(aktørIdList);
    }
}
