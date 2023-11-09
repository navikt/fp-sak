package no.nav.foreldrepenger.web.app.soap.sak.tjeneste;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Journalpost;
import no.nav.foreldrepenger.domene.bruker.NavBrukerTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.exception.TekniskException;

@ApplicationScoped
@Transactional
/* HACK (uuh): Transaksjonsgrensen her er flyttet hit fra webservice'en OpprettSakService
 * Dette er ikke i henhold til standard og kan ikke gjøres uten godkjenning fra sjefsarkitekt.
 * Grunnen for at det er gjort her er for å sikre at de tre kallene går i separate transaksjoner.
 * Se https://jira.adeo.no/browse/PKHUMLE-359 for detaljer.
 */
public class OpprettSakTjeneste {

    private FagsakTjeneste fagsakTjeneste;
    private NavBrukerTjeneste brukerTjeneste;

    public OpprettSakTjeneste() {
        //For CDI
    }

    @Inject
    public OpprettSakTjeneste(FagsakTjeneste fagsakTjeneste, NavBrukerTjeneste brukerTjeneste) {
        this.fagsakTjeneste = fagsakTjeneste;
        this.brukerTjeneste = brukerTjeneste;
    }

    public Fagsak opprettSakVL(AktørId bruker, FagsakYtelseType ytelseType) {
        var navBruker = brukerTjeneste.hentEllerOpprettFraAktørId(bruker);
        return fagsakTjeneste.opprettFagsak(ytelseType, navBruker);
    }

    public Fagsak opprettSakVL(AktørId bruker, FagsakYtelseType ytelseType, JournalpostId journalpostId) {
        var navBruker = brukerTjeneste.hentEllerOpprettFraAktørId(bruker);
        var fagsak = fagsakTjeneste.opprettFagsak(ytelseType, navBruker);
        knyttFagsakOgJournalpost(fagsak.getId(), journalpostId);
        return fagsak;
    }

    @Deprecated(forRemoval = true, since = "TFP-4124")
    public FagsakYtelseType utledYtelseType(BehandlingTema behandlingTema) {

        var fagsakYtelseType = behandlingTema.getFagsakYtelseType();
        if (FagsakYtelseType.UDEFINERT.equals(fagsakYtelseType)) {
            throw new TekniskException("FP-106651", "Ukjent behandlingstemakode " + behandlingTema.getOffisiellKode());
        }
        return fagsakYtelseType;
    }

    public void knyttSakOgJournalpost(Saksnummer saksnummer, JournalpostId journalPostId) {

        //Sjekk om det allerede finnes knytning.
        var journalpost = fagsakTjeneste.hentJournalpost(journalPostId);
        if (journalpost.isPresent()) {
            var knyttetTilSaksnummer = journalpost.get().getFagsak().getSaksnummer();
            if (knyttetTilSaksnummer.equals(saksnummer)) {
                //Vi har knytning mot samme sak. Vi er HAPPY og returnerer herfra.
                return;
            }
            //Knyttet til en annen fagsak
            throw journalpostAlleredeKnyttetTilAnnenFagsak(journalPostId, knyttetTilSaksnummer,
                saksnummer.getVerdi());
        }

        //HER: Finnes ikke knytnign mellom journalpost og sak. La oss oprpette en:
        var fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, true);
        if (fagsak.isPresent()) {
            fagsakTjeneste.lagreJournalPost(new Journalpost(journalPostId, fagsak.get()));
        } else {
            throw new TekniskException("FP-840572", "Finner ikke fagsak med angitt saksnummer " + saksnummer);
        }
    }

    public void flyttJournalpostTilSak(JournalpostId journalPostId, Saksnummer saksnummer) {
        var journalpost = fagsakTjeneste.hentJournalpost(journalPostId).orElse(null);
        var fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, true);
        if (journalpost != null && fagsak.isPresent()) {
            journalpost.knyttJournalpostTilFagsak(fagsak.get());
            fagsakTjeneste.lagreJournalPost(journalpost);
        }
    }

    private void knyttFagsakOgJournalpost(Long fagsakId, JournalpostId journalpostId) {
        var journalpost = fagsakTjeneste.hentJournalpost(journalpostId);
        if (journalpost.isPresent()) {
            if (journalpost.get().getFagsak().getId().equals(fagsakId)) {
                //Vi har knytning mot samme sak. Vi er HAPPY og returnerer herfra.
                return;
            }
            //Knyttet til en annen fagsak
            throw journalpostAlleredeKnyttetTilAnnenFagsak(journalpostId,
                journalpost.get().getFagsak().getSaksnummer(), fagsakId.toString());
        }

        //HER: Finnes ikke knytning mellom journalpost og sak. La oss opprette en:
        var fagsak = fagsakTjeneste.finnEksaktFagsak(fagsakId);
        fagsakTjeneste.lagreJournalPost(new Journalpost(journalpostId, fagsak));
    }

    private static TekniskException journalpostAlleredeKnyttetTilAnnenFagsak(JournalpostId journalPostId,
                                                                             Saksnummer tilknyttetSak,
                                                                             String forsøktSak) {
        var msg = String.format("Journalpost-Fagsak knytning finnes allerede. Journalpost %s er knyttet "
            + "mot fagsak %s. Forsøkt knyttet mot sak %s", journalPostId, tilknyttetSak, forsøktSak);
        return new TekniskException("FP-863070", msg);
    }

}
