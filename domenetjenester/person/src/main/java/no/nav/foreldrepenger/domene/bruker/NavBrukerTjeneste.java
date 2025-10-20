package no.nav.foreldrepenger.domene.bruker;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerRepository;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;

/*
 * For brukere med sak i VL - de som er lagret i Bruker-tabellen og mottar brev/meldinger
 * Vil hente og oppfriske språkvalg (og kontaktinfo) fra KRR noen ganger i året (data skal bekreftes av bruker hver 3dje måned)
 */

@ApplicationScoped
public class NavBrukerTjeneste {

    private static final Period REFRESH_INTERVAL = Period.ofMonths(4);

    private NavBrukerRepository brukerRepository;
    private PersoninfoAdapter personinfoAdapter;

    public NavBrukerTjeneste() {
        // Plattform trenger tom Ctor (Hibernate, CDI, etc)
    }

    @Inject
    public NavBrukerTjeneste(NavBrukerRepository brukerRepository,
                             PersoninfoAdapter personinfoAdapter) {
        this.brukerRepository = brukerRepository;
        this.personinfoAdapter = personinfoAdapter;
    }

    public NavBruker hentEllerOpprettFraAktørId(AktørId aktørId) {
        return hentMedRefresh(aktørId).orElseGet(() -> opprettBruker(aktørId));
    }

    public Optional<NavBruker> hentBrukerForAktørId(AktørId aktørId) {
        return hentMedRefresh(aktørId);
    }

    private Optional<NavBruker> hentMedRefresh(AktørId aktørId) {
        var bruker = brukerRepository.hent(aktørId);
        if (bruker.isEmpty())
            return bruker;
        var refreshGrense = LocalDateTime.now().minus(REFRESH_INTERVAL);
        var sistoppdatert = bruker.map(b -> b.getEndretTidspunkt() == null ? b.getOpprettetTidspunkt() : b.getEndretTidspunkt())
            .orElseGet(() -> refreshGrense.minus(REFRESH_INTERVAL));
        var refresh = sistoppdatert.isBefore(refreshGrense);
        if (refresh) {
            var språk = personinfoAdapter.hentForetrukketSpråk(aktørId);
            var brukspråk = språk != null && språk.foretrukketSpråk() != null ? språk.foretrukketSpråk() : Språkkode.NB;
            return brukerRepository.oppdaterSpråk(aktørId, brukspråk);
        }
        return bruker;
    }

    private NavBruker opprettBruker(AktørId aktørId) {
        var språk = personinfoAdapter.hentForetrukketSpråk(aktørId);
        var brukspråk = språk != null && språk.foretrukketSpråk() != null ? språk.foretrukketSpråk() : Språkkode.NB;
        return NavBruker.opprettNy(aktørId, brukspråk);
    }

}
