package no.nav.foreldrepenger.domene.prosess;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

/** Enkel builder for å lage en enkel behandling for internt bruk her. */
public class FagsakBehandlingBuilder {

    private final EntityManager em;
    private final BehandlingRepository behandlingRepository;

    private Fagsak fagsak;

    private final AktørId aktørId = AktørId.dummy();

    private final Map<AktørId, NavBruker> lagredeBrukere = new HashMap<>();

    public FagsakBehandlingBuilder(EntityManager em) {
        this.em = em;
        behandlingRepository = new BehandlingRepository(em);
    }

    public Behandling opprettOgLagreFørstegangssøknad(FagsakYtelseType ytelse) {
        var fagsak = opprettFagsak(ytelse);
        return opprettOgLagreFørstegangssøknad(fagsak);
    }

    public Behandling opprettOgLagreFørstegangssøknad(Fagsak fagsak) {
        final var builder = Behandling.forFørstegangssøknad(fagsak);
        var behandling = builder.build();

        lagreBehandling(behandling);

        em.flush();
        return behandling;
    }

    private void lagreBehandling(Behandling behandling) {
        var lås = taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
    }

    private BehandlingLås taSkriveLås(Behandling behandling) {
        return behandlingRepository.taSkriveLås(behandling);
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
