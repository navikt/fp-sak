package no.nav.foreldrepenger.behandling;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonRelasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.*;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
public class FagsakTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(FagsakTjeneste.class);

    private FagsakRepository fagsakRepository;
    private FagsakStatusEventPubliserer fagsakStatusEventPubliserer;
    private SøknadRepository søknadRepository;

    FagsakTjeneste() {
        // for CDI proxy
    }

    @Inject
    public FagsakTjeneste(FagsakRepository fagsakRepository, SøknadRepository søknadRepository,
            FagsakStatusEventPubliserer fagsakStatusEventPubliserer) {
        this.fagsakRepository = fagsakRepository;
        this.fagsakStatusEventPubliserer = fagsakStatusEventPubliserer;
        this.søknadRepository = søknadRepository;
    }

    /**
     * FIXME Marius ønsker endre interface på denne, men løser slikt inntil videre
     *
     * @deprecated Skal ikke lenger oppdatere fagsaks relasjonsrolle basert på
     *             registerdata, men sette det ut fra oppgitte data i søknad (steg {
     *             {@link BehandlingStegType#REGISTRER_SØKNAD}} ) Likevel finnes et
     *             unntak som gjør at metoden ikke kan fjernes: gammelt søknadformat
     *             angir ikke relasjonsrolle. Teknisk gjeld: se PFP-6758
     */
    @Deprecated
    public void oppdaterFagsak(Behandling behandling, PersonopplysningerAggregat personopplysninger,
            List<PersonopplysningEntitet> barnSøktStønadFor) {

        var fagsak = behandling.getFagsak();
        validerEksisterendeFagsak(fagsak);

        // Oppdatering basert på søkers oppgitte relasjon til barn
        var oppgittRelasjonsRolle = søknadRepository.hentSøknadHvisEksisterer(behandling.getId())
                .map(SøknadEntitet::getRelasjonsRolleType);
        if (oppgittRelasjonsRolle.isPresent()) {
            if (!Objects.equals(behandling.getRelasjonsRolleType(), oppgittRelasjonsRolle.get())) {
                LOG.info("oppdaterRelasjonsRolle fagsak har {} fra søknad {}", fagsak.getRelasjonsRolleType().getKode(),
                    oppgittRelasjonsRolle.get().getKode());
                fagsakRepository.oppdaterRelasjonsRolle(fagsak.getId(), oppgittRelasjonsRolle.get());
            }
            return;
        }

        // Oppdatering basert på søkers registrerte relasjon til barn
        var funnetRelasjon = finnBarnetsRelasjonTilSøker(barnSøktStønadFor, personopplysninger);
        if (funnetRelasjon.isPresent()) {
            var brukerRolle = funnetRelasjon.map(PersonRelasjonEntitet::getRelasjonsrolle);
            if (brukerRolle.isPresent()) {
                if (!Objects.equals(behandling.getRelasjonsRolleType(), brukerRolle.get())) {
                    LOG.info("oppdaterRelasjonsRolle fagsak har {} fra register {}", fagsak.getRelasjonsRolleType().getKode(),
                        brukerRolle.get().getKode());
                    fagsakRepository.oppdaterRelasjonsRolle(fagsak.getId(), brukerRolle.get());
                }
                return;
            }
        }
        LOG.info("oppdaterRelasjonsRolle fagsak har {} ingen oppdatering", fagsak.getRelasjonsRolleType().getKode());
    }

    public Fagsak opprettFagsak(FagsakYtelseType ytelseType, NavBruker bruker) {
        var nyFagsak = Fagsak.opprettNy(ytelseType, bruker, genererNyttSaksnummer());
        validerNyFagsak(nyFagsak);
        validerHarSaksnummer(nyFagsak);
        fagsakRepository.opprettNy(nyFagsak);
        if (fagsakStatusEventPubliserer != null) {
            fagsakStatusEventPubliserer.fireEvent(nyFagsak, nyFagsak.getStatus());
        }
        return nyFagsak;
    }

    private Saksnummer genererNyttSaksnummer() {
        return fagsakRepository.genererNyttSaksnummer();
    }

    private void validerNyFagsak(Fagsak fagsak) {
        if (fagsak.getId() != null || !Objects.equals(fagsak.getStatus(), FagsakStatus.OPPRETTET)) {
            throw new IllegalArgumentException("Kan ikke kalle opprett fagsak med eksisterende: " + fagsak);
        }
    }

    private void validerHarSaksnummer(Fagsak fagsak) {
        if (fagsak.getSaksnummer() == null) {
            throw new IllegalArgumentException("Kan ikke kalle opprett fagsak uten saksnummer");
        }
    }

    private void validerEksisterendeFagsak(Fagsak fagsak) {
        if (fagsak.getId() == null || Objects.equals(fagsak.getStatus(), FagsakStatus.OPPRETTET)) {
            throw new IllegalArgumentException("Kan ikke kalle oppdater med ny fagsak: " + fagsak);
        }
    }

    public Optional<Fagsak> finnFagsakGittSaksnummer(Saksnummer saksnummer, boolean taSkriveLås) {
        return fagsakRepository.hentSakGittSaksnummer(saksnummer, taSkriveLås);
    }

    public List<Fagsak> finnFagsakerForAktør(AktørId aktørId) {
        return fagsakRepository.hentForBruker(aktørId);
    }

    public boolean finnesFagsakerForAktør(AktørId aktørId) {
        return !fagsakRepository.hentForBruker(aktørId).isEmpty();
    }

    public Fagsak finnEksaktFagsak(long fagsakId) {
        return fagsakRepository.finnEksaktFagsak(fagsakId);
    }

    public void lagreJournalPost(Journalpost journalpost) {
        fagsakRepository.lagre(journalpost);
    }

    public Optional<Journalpost> hentJournalpost(JournalpostId journalpostId) {
        return fagsakRepository.hentJournalpost(journalpostId);
    }

    private Optional<PersonRelasjonEntitet> finnBarnetsRelasjonTilSøker(List<PersonopplysningEntitet> barnaSøktStøtteFor,
            PersonopplysningerAggregat personopplysningerAggregat) {

        var barn = personopplysningerAggregat.getBarna().stream()
                .filter(e -> barnaSøktStøtteFor.stream()
                        .anyMatch(kandidat -> kandidat.getAktørId().equals(e.getAktørId())))
                .findFirst();

        if (barn.isPresent()) {
            return personopplysningerAggregat.finnRelasjon(barn.get().getAktørId(), personopplysningerAggregat.getSøker().getAktørId());
        }
        return Optional.empty();
    }

}
