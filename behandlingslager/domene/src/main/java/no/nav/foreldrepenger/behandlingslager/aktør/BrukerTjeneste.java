package no.nav.foreldrepenger.behandlingslager.aktør;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.domene.typer.AktørId;

@ApplicationScoped
public class BrukerTjeneste {

    private NavBrukerRepository brukerRepository;

    public BrukerTjeneste() {
    }

    @Inject
    public BrukerTjeneste(NavBrukerRepository brukerRepository) {
        this.brukerRepository = brukerRepository;
    }

    public NavBruker hentEllerOpprettFraAktorId(AktørId aktørId, Språkkode språk) {
        Optional<NavBruker> hent = brukerRepository.hent(aktørId);
        return hent.orElseGet(() -> NavBruker.opprettNy(aktørId, språk));
    }

    public Optional<NavBruker> hentBrukerForAktørId(AktørId aktørId) {
        return brukerRepository.hent(aktørId);
    }

}
