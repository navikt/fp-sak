package no.nav.foreldrepenger.domene.uttak.testutilities.behandling;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.*;
import no.nav.foreldrepenger.behandlingslager.laas.FagsakRelasjonLås;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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
    public void lagre(Fagsak fagsak, Long behandlingId, Stønadskontoberegning stønadskontoberegning) {
        var fagsakRelasjon = finnRelasjonForHvisEksisterer(fagsak).orElse(null);
        if (fagsakRelasjon == null) {
            fagsakRelasjon = opprettRelasjon(fagsak, Dekningsgrad._100);
        }
        var ny = new FagsakRelasjon(fagsakRelasjon.getFagsakNrEn(), fagsakRelasjon.getFagsakNrTo().orElse(null),
            stønadskontoberegning, fagsakRelasjon.getOverstyrtStønadskontoberegning().orElse(null),
            fagsakRelasjon.getDekningsgrad(), fagsakRelasjon.getOverstyrtDekningsgrad().orElse(null),
            fagsakRelasjon.getAvsluttningsdato());
        lagre(fagsak, ny);
    }

    @Override
    public FagsakRelasjon opprettRelasjon(Fagsak fagsak, Dekningsgrad dekningsgrad) {
        var fagsakRelasjon = new FagsakRelasjon(fagsak, null, null, null, dekningsgrad, null, null);
        lagre(fagsak, fagsakRelasjon);
        return fagsakRelasjon;
    }

    private void lagre(Fagsak fagsak, FagsakRelasjon fagsakRelasjon) {
        relasjonMap.put(fagsak.getId(), fagsakRelasjon);
        relasjonMapSaksnummer.put(fagsak.getSaksnummer(), fagsakRelasjon);
    }

    @Override
    public FagsakRelasjon overstyrDekningsgrad(Fagsak fagsak, Dekningsgrad overstyrtVerdi) {
        var fagsakRelasjon = finnRelasjonFor(fagsak);
        var ny = new FagsakRelasjon(fagsakRelasjon.getFagsakNrEn(), fagsakRelasjon.getFagsakNrTo().orElse(null),
            fagsakRelasjon.getStønadskontoberegning().orElse(null), fagsakRelasjon.getOverstyrtStønadskontoberegning()
            .orElse(null), fagsakRelasjon.getDekningsgrad(), overstyrtVerdi, fagsakRelasjon.getAvsluttningsdato());
        lagre(fagsak, ny);
        return ny;
    }

    @Override
    public Optional<FagsakRelasjon> opprettEllerOppdaterRelasjon(Fagsak fagsak,
                                                                 Optional<FagsakRelasjon> fagsakRelasjon,
                                                                 Dekningsgrad dekningsgrad) {
        throw new IkkeImplementertForTestException();
    }

    @Override
    public Optional<FagsakRelasjon> kobleFagsaker(Fagsak fagsakEn, Fagsak fagsakTo, Behandling behandlingEn) {
        var fagsakRelasjon = finnRelasjonFor(fagsakEn);
        var ny = new FagsakRelasjon(fagsakEn, fagsakTo, fagsakRelasjon.getStønadskontoberegning().orElse(null),
            fagsakRelasjon.getOverstyrtStønadskontoberegning().orElse(null), fagsakRelasjon.getDekningsgrad(),
            fagsakRelasjon.getOverstyrtDekningsgrad().orElse(null), fagsakRelasjon.getAvsluttningsdato());
        lagre(fagsakEn, ny);
        lagre(fagsakTo, ny);
        return Optional.of(ny);
    }

    @Override
    public Optional<FagsakRelasjon> fraKobleFagsaker(Fagsak fagsakEn, Fagsak fagsakTo) {
        throw new IkkeImplementertForTestException();
    }

    @Override
    public FagsakRelasjon nullstillOverstyrtDekningsgrad(Fagsak fagsak) {
        throw new IkkeImplementertForTestException();
    }

    @Override
    public Optional<FagsakRelasjon> nullstillOverstyrtStønadskontoberegning(Fagsak fagsak) {
        throw new IkkeImplementertForTestException();
    }

    @Override
    public void overstyrStønadskontoberegning(Fagsak fagsak,
                                              Long behandlingId,
                                              Stønadskontoberegning stønadskontoberegning) {
        throw new IkkeImplementertForTestException();
    }

    @Override
    public Optional<FagsakRelasjon> oppdaterMedAvsluttningsdato(FagsakRelasjon relasjon,
                                                                LocalDate avsluttningsdato,
                                                                FagsakRelasjonLås lås,
                                                                Optional<FagsakLås> fagsak1Lås,
                                                                Optional<FagsakLås> fagsak2Lås) {
        throw new IkkeImplementertForTestException();
    }

    @Override
    public List<Fagsak> finnFagsakerForAvsluttning(LocalDate localDate) {
        throw new IkkeImplementertForTestException();
    }
}
