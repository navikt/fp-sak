package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling;

import static java.time.Month.JANUARY;
import static no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn.KVINNE;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

/** Enkel builder for å lage en enkel behandling for internt bruk her. */
public class BasicBehandlingBuilder {

    private EntityManager em;
    private final BehandlingRepository behandlingRepository;

    private Fagsak fagsak;
    private VilkårResultatRepository vilkårResultatRepository;

    private final AktørId aktørId = AktørId.dummy();

    private Map<AktørId, NavBruker> lagredeBrukere = new HashMap<>();

    public BasicBehandlingBuilder(EntityManager em) {
        this.em = em;
        behandlingRepository = new BehandlingRepository(em);
        vilkårResultatRepository = new VilkårResultatRepository(em);
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

    public VilkårResultat leggTilTomtVilkårResultat(Behandling behandling) {
        VilkårResultat vilkårResultat = VilkårResultat.builder().buildFor(behandling);
        behandlingRepository.lagre(vilkårResultat, taSkriveLås(behandling));
        lagreBehandling(behandling);
        return vilkårResultat;
    }

    public void lagreVilkårResultat(Long behandlingId, VilkårResultat vilkårResultat) {
        vilkårResultatRepository.lagre(behandlingId, vilkårResultat);
    }
}
