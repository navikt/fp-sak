package no.nav.foreldrepenger.domene.uttak.testutilities.behandling;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.Journalpost;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

class FagsakRepositoryStub extends FagsakRepository {

    private static final AtomicLong ID = new AtomicLong(0L);

    private final Map<Long, Fagsak> fagsakMap = new ConcurrentHashMap<>();

    @Override
    public Fagsak finnEksaktFagsak(long fagsakId) {
        return finnUnikFagsak(fagsakId).orElseThrow();
    }

    @Override
    public Fagsak finnEksaktFagsakReadOnly(long fagsakId) {
        return finnUnikFagsak(fagsakId).orElseThrow();
    }

    @Override
    public Optional<Fagsak> finnUnikFagsak(long fagsakId) {
        var fagsak = fagsakMap.get(fagsakId);
        if (fagsak == null) {
            return Optional.empty();
        }
        return Optional.of(fagsak);
    }

    @Override
    public List<Fagsak> hentForBruker(AktørId aktørId) {
        throw new IkkeImplementertForTestException();
    }

    @Override
    public List<Fagsak> hentForBrukerMulti(Set<AktørId> aktørId) {
        throw new IkkeImplementertForTestException();
    }

    @Override
    public Optional<Journalpost> hentJournalpost(JournalpostId journalpostId) {
        throw new IkkeImplementertForTestException();
    }

    @Override
    public Optional<Fagsak> hentSakGittSaksnummer(Saksnummer saksnummer) {
        throw new IkkeImplementertForTestException();
    }

    @Override
    public Long opprettNy(Fagsak fagsak) {
        fagsak.setId(ID.getAndIncrement());
        fagsakMap.put(fagsak.getId(), fagsak);
        return fagsak.getId();
    }

    @Override
    public void oppdaterRelasjonsRolle(Long fagsakId, RelasjonsRolleType relasjonsRolleType) {
        throw new IkkeImplementertForTestException();
    }

    @Override
    public void oppdaterBruker(Long fagsakId, NavBruker bruker) {
        throw new IkkeImplementertForTestException();
    }

    @Override
    public void oppdaterBrukerMedAktørId(Long fagsakId, AktørId aktørId) {
        throw new IkkeImplementertForTestException();
    }

    @Override
    public Optional<Fagsak> hentSakGittSaksnummer(Saksnummer saksnummer, boolean taSkriveLås) {
        throw new IkkeImplementertForTestException();
    }

    @Override
    public Long lagre(Journalpost journalpost) {
        throw new IkkeImplementertForTestException();
    }

    @Override
    public void oppdaterFagsakStatus(Long fagsakId, FagsakStatus status) {
        throw new IkkeImplementertForTestException();
    }

    @Override
    public List<Fagsak> hentForStatus(FagsakStatus fagsakStatus) {
        throw new IkkeImplementertForTestException();
    }

    @Override
    public List<Saksnummer> hentÅpneFagsakerUtenBehandling() {
        throw new IkkeImplementertForTestException();
    }

    @Override
    public void fagsakSkalStengesForBruk(Long fagsakId) {
        throw new IkkeImplementertForTestException();
    }
}
