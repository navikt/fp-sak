package no.nav.foreldrepenger.historikk;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

/** Enkel builder for å lage en enkel behandling for internt bruk her. */
public class BasicBehandlingBuilder {

    private EntityManager em;

    private Fagsak fagsak;

    private final AktørId aktørId = AktørId.dummy();

    private Map<AktørId, NavBruker> lagredeBrukere = new HashMap<>();

    public BasicBehandlingBuilder(EntityManager em) {
        this.em = em;
    }

    public Fagsak opprettFagsak(FagsakYtelseType ytelse) {
        if (fagsak != null) {
            assert fagsak.getYtelseType().equals(ytelse) : "Feil ytelsetype - kan ikke gjenbruke fagsak: " + fagsak;
            return fagsak;
        }
        return opprettFagsak(ytelse, aktørId);
    }

    public Fagsak opprettFagsak(FagsakYtelseType ytelse, AktørId aktørId) {

        var bruker = lagredeBrukere.computeIfAbsent(aktørId, NavBruker::opprettNyNB);
        em.persist(bruker);

        // Opprett fagsak
        var randomSaksnummer = System.nanoTime() + "";
        this.fagsak = Fagsak.opprettNy(ytelse, bruker, null, new Saksnummer(randomSaksnummer));
        em.persist(fagsak);
        em.flush();
        return fagsak;
    }
}
