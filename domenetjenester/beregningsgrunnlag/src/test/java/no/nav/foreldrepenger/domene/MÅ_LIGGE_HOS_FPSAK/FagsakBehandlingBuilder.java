package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static java.time.Month.JANUARY;
import static no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn.KVINNE;

/** Enkel builder for å lage en enkel behandling for internt bruk her. */
public class FagsakBehandlingBuilder {

    private EntityManager em;
    private final BehandlingRepository behandlingRepository;

    private Fagsak fagsak;

    private final AktørId aktørId = AktørId.dummy();

    private Map<AktørId, NavBruker> lagredeBrukere = new HashMap<>();

    public FagsakBehandlingBuilder(EntityManager em) {
        this.em = em;
        behandlingRepository = new BehandlingRepository(em);
    }

    public Behandling opprettOgLagreFørstegangssøknad(FagsakYtelseType ytelse) {
        Fagsak fagsak = opprettFagsak(ytelse);
        return opprettOgLagreFørstegangssøknad(fagsak);
    }

    public Behandling opprettOgLagreFørstegangssøknad(Fagsak fagsak) {
        final Behandling.Builder builder = Behandling.forFørstegangssøknad(fagsak);
        Behandling behandling = builder.build();

        lagreBehandling(behandling);

        em.flush();
        return behandling;
    }

    private void lagreBehandling(Behandling behandling) {
        BehandlingLås lås = taSkriveLås(behandling);
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

        var bruker = lagredeBrukere.computeIfAbsent(aktørId, aid -> {
            return NavBruker.opprettNy(
                new Personinfo.Builder()
                    .medAktørId(aid)
                    .medPersonIdent(new PersonIdent("12345678901"))
                    .medNavn("Kari Nordmann")
                    .medFødselsdato(LocalDate.of(1990, JANUARY, 1))
                    .medForetrukketSpråk(Språkkode.nb)
                    .medNavBrukerKjønn(KVINNE)
                    .build());
        });
        em.persist(bruker);

        // Opprett fagsak
        String randomSaksnummer = System.nanoTime() + "";
        this.fagsak = Fagsak.opprettNy(ytelse, bruker, null, new Saksnummer(randomSaksnummer));
        em.persist(fagsak);
        em.flush();
        return fagsak;
    }

}
