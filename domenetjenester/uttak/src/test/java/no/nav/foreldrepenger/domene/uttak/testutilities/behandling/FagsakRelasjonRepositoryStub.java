package no.nav.foreldrepenger.domene.uttak.testutilities.behandling;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLås;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

class FagsakRelasjonRepositoryStub extends FagsakRelasjonRepository {

    private final Map<Long, FagsakRelasjon> relasjonMap = new ConcurrentHashMap<>();
    private final Map<Saksnummer, FagsakRelasjon> relasjonMapSaksnummer = new ConcurrentHashMap<>();

    @Override
    public FagsakRelasjon finnRelasjonFor(Fagsak fagsak) {
        return finnRelasjonForHvisEksisterer(fagsak).orElseThrow();
    }

    @Override
    public FagsakRelasjon finnRelasjonFor(Saksnummer saksnummer) {
        return finnRelasjonHvisEksisterer(saksnummer).orElseThrow();
    }

    @Override
    public Optional<FagsakRelasjon> finnRelasjonHvisEksisterer(Saksnummer saksnummer) {
        var fagsakRelasjon = relasjonMapSaksnummer.get(saksnummer);
        if (fagsakRelasjon == null) {
            return Optional.empty();
        }
        return Optional.of(fagsakRelasjon);
    }

    @Override
    public Optional<FagsakRelasjon> finnRelasjonForHvisEksisterer(Fagsak fagsak) {
        var fagsakRelasjon = relasjonMap.get(fagsak.getId());
        if (fagsakRelasjon == null) {
            return Optional.empty();
        }
        return Optional.of(fagsakRelasjon);
    }

    @Override
    public Optional<FagsakRelasjon> finnRelasjonForHvisEksisterer(long fagsakId) {
        throw new IkkeImplementertForTestException();
    }

    @Override
    public FagsakRelasjon opprettRelasjon(Fagsak fagsak) {
        var relasjon = new FagsakRelasjon(fagsak, null, null, null, null);
        lagre(fagsak, relasjon);
        return relasjon;
    }

    @Override
    public void lagre(Fagsak fagsak, Stønadskontoberegning stønadskontoberegning) {
        var fagsakRelasjon = finnRelasjonForHvisEksisterer(fagsak).orElse(null);
        if (fagsakRelasjon == null) {
            fagsakRelasjon = opprettRelasjon(fagsak);
        }
        var ny = new FagsakRelasjon(fagsakRelasjon.getFagsakNrEn(), fagsakRelasjon.getFagsakNrTo().orElse(null), stønadskontoberegning,
            fagsakRelasjon.getDekningsgrad(), fagsakRelasjon.getAvsluttningsdato());
        lagre(fagsak, ny);
    }

    @Override
    public void oppdaterDekningsgrad(Fagsak fagsak, Dekningsgrad dekningsgrad) {
        var fagsakRelasjon = finnRelasjonForHvisEksisterer(fagsak).orElse(null);
        if (fagsakRelasjon == null) {
            fagsakRelasjon = opprettRelasjon(fagsak);
        }
        var ny = new FagsakRelasjon(fagsakRelasjon.getFagsakNrEn(), fagsakRelasjon.getFagsakNrTo().orElse(null),
            fagsakRelasjon.getStønadskontoberegning().orElse(null), dekningsgrad,
            fagsakRelasjon.getAvsluttningsdato());
        lagre(fagsak, ny);
    }

    private void lagre(Fagsak fagsak, FagsakRelasjon fagsakRelasjon) {
        relasjonMap.put(fagsak.getId(), fagsakRelasjon);
        relasjonMapSaksnummer.put(fagsak.getSaksnummer(), fagsakRelasjon);
    }

    @Override
    public Optional<FagsakRelasjon> kobleFagsaker(Fagsak fagsakEn, Fagsak fagsakTo) {
        var fagsakRelasjon = finnRelasjonFor(fagsakEn);
        var ny = new FagsakRelasjon(fagsakEn, fagsakTo, fagsakRelasjon.getStønadskontoberegning().orElse(null),
                fagsakRelasjon.getDekningsgrad(), fagsakRelasjon.getAvsluttningsdato());
        lagre(fagsakEn, ny);
        lagre(fagsakTo, ny);
        return Optional.of(ny);
    }

    @Override
    public void fraKobleFagsaker(Fagsak fagsakEn, Fagsak fagsakTo) {
        throw new IkkeImplementertForTestException();
    }

    @Override
    public void nullstillOverstyrtStønadskontoberegning(Fagsak fagsak) {
        throw new IkkeImplementertForTestException();
    }

    @Override
    public void oppdaterMedAvsluttningsdato(FagsakRelasjon relasjon,
                                                                LocalDate avsluttningsdato, Optional<FagsakLås> fagsak1Lås,
                                                                Optional<FagsakLås> fagsak2Lås) {
        throw new IkkeImplementertForTestException();
    }

    @Override
    public List<Fagsak> finnFagsakerForAvsluttning(LocalDate localDate) {
        throw new IkkeImplementertForTestException();
    }
}
