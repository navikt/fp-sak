package no.nav.foreldrepenger.web.app.soap.sak.tjeneste;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Journalpost;
import no.nav.foreldrepenger.domene.bruker.NavBrukerTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
@Transactional
/* HACK (u139158): Transaksjonsgrensen her er flyttet hit fra webservice'en OpprettSakService
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
    public OpprettSakTjeneste(FagsakTjeneste fagsakTjeneste,
                              NavBrukerTjeneste brukerTjeneste) {
        this.fagsakTjeneste = fagsakTjeneste;
        this.brukerTjeneste = brukerTjeneste;
    }

    public Fagsak opprettSakVL(AktørId bruker, FagsakYtelseType ytelseType) {
        NavBruker navBruker = brukerTjeneste.hentEllerOpprettFraAktørId(bruker);
        return fagsakTjeneste.opprettFagsak(ytelseType, navBruker);
    }

    public Fagsak opprettSakVL(AktørId bruker, FagsakYtelseType ytelseType, JournalpostId journalpostId) {
        NavBruker navBruker = brukerTjeneste.hentEllerOpprettFraAktørId(bruker);
        Fagsak fagsak = fagsakTjeneste.opprettFagsak(ytelseType, navBruker);
        knyttFagsakOgJournalpost(fagsak.getId(), journalpostId);
        return fagsak;
    }

    public FagsakYtelseType utledYtelseType(BehandlingTema behandlingTema) {

        FagsakYtelseType fagsakYtelseType = behandlingTema.getFagsakYtelseType();
        if (FagsakYtelseType.UDEFINERT.equals(fagsakYtelseType)) {
            throw OpprettSakFeil.FACTORY.ukjentBehandlingstemaKode(behandlingTema.getOffisiellKode()).toException();
        }
        return fagsakYtelseType;
    }

    public void knyttSakOgJournalpost(Saksnummer saksnummer, JournalpostId journalPostId) {

        //Sjekk om det allerede finnes knytning.
        Optional<Journalpost> journalpost = fagsakTjeneste.hentJournalpost(journalPostId);
        if (journalpost.isPresent()) {
            Saksnummer knyttetTilSaksnummer = journalpost.get().getFagsak().getSaksnummer();
            if (knyttetTilSaksnummer.equals(saksnummer)) {
                //Vi har knytning mot samme sak. Vi er HAPPY og returnerer herfra.
                return;
            } else {
                //Knyttet til en annen fagsak
                throw OpprettSakFeil.FACTORY.JournalpostAlleredeKnyttetTilAnnenFagsak(journalPostId, knyttetTilSaksnummer, saksnummer.getVerdi()).toException();
            }
        }

        //HER: Finnes ikke knytnign mellom journalpost og sak. La oss oprpette en:
        Optional<Fagsak> fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, true);
        if (fagsak.isPresent()) {
            fagsakTjeneste.lagreJournalPost(new Journalpost(journalPostId, fagsak.get()));
        } else {
            throw OpprettSakFeil.FACTORY.finnerIkkeFagsakMedSaksnummer(saksnummer).toException();
        }
    }

    public void flyttJournalpostTilSak(JournalpostId journalPostId, Saksnummer saksnummer) {
        Journalpost journalpost = fagsakTjeneste.hentJournalpost(journalPostId).orElse(null);
        Optional<Fagsak> fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, true);
        if (journalpost != null && fagsak.isPresent()) {
            journalpost.knyttJournalpostTilFagsak(fagsak.get());
            fagsakTjeneste.lagreJournalPost(journalpost);
        }
    }

    private void knyttFagsakOgJournalpost(Long fagsakId, JournalpostId journalpostId) {
        Optional<Journalpost> journalpost = fagsakTjeneste.hentJournalpost(journalpostId);
        if (journalpost.isPresent()) {
            if (journalpost.get().getFagsak().getId().equals(fagsakId)) {
                //Vi har knytning mot samme sak. Vi er HAPPY og returnerer herfra.
                return;
            } else {
                //Knyttet til en annen fagsak
                throw OpprettSakFeil.FACTORY.JournalpostAlleredeKnyttetTilAnnenFagsak(journalpostId, journalpost.get().getFagsak().getSaksnummer(), fagsakId.toString()).toException();
            }
        }

        //HER: Finnes ikke knytning mellom journalpost og sak. La oss opprette en:
        Fagsak fagsak = fagsakTjeneste.finnEksaktFagsak(fagsakId);
        fagsakTjeneste.lagreJournalPost(new Journalpost(journalpostId, fagsak));
    }

}
