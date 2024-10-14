package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

/** Enkel builder for å lage en enkel behandling for internt bruk her. */
public class BasicBehandlingBuilder {

    private EntityManager em;
    private final BehandlingRepository behandlingRepository;

    private Fagsak fagsak;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    private final AktørId aktørId = AktørId.dummy();

    private Map<AktørId, NavBruker> lagredeBrukere = new HashMap<>();

    public BasicBehandlingBuilder(EntityManager em) {
        this.em = em;
        behandlingRepository = new BehandlingRepository(em);
        behandlingsresultatRepository = new BehandlingsresultatRepository(em);
        vilkårResultatRepository = new VilkårResultatRepository(em);
    }

    public Behandling opprettOgLagreFørstegangssøknad(FagsakYtelseType ytelse) {
        var fagsak = opprettFagsak(ytelse);
        return opprettOgLagreFørstegangssøknad(fagsak);
    }

    public Behandling opprettOgLagreFørstegangssøknad(Fagsak fagsak) {
        var builder = Behandling.forFørstegangssøknad(fagsak);
        var behandling = builder.build();

        lagreBehandling(behandling);

        em.flush();
        return behandling;
    }

    public void lagreBehandlingsresultat(Long behandlingId, Behandlingsresultat resultat) {
        behandlingsresultatRepository.lagre(behandlingId, resultat);
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

    public void lagreVilkårResultat(Long behandlingId, VilkårResultat vilkårResultat) {
        vilkårResultatRepository.lagre(behandlingId, vilkårResultat);
    }
}
