package no.nav.foreldrepenger.behandlingslager.testutilities.behandling;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLås;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.laas.FagsakRelasjonLås;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

class FagsakRelasjonRepositoryStub extends FagsakRelasjonRepository {

    private static final String IKKE_STOTTET = "Ikke støttet av FagsakRelasjonRepositoryStub";
    private Map<Saksnummer, FagsakRelasjon> map = new HashMap<>();

    @Override
    public FagsakRelasjon finnRelasjonFor(Fagsak fagsak) {
        return finnRelasjonFor(fagsak.getSaksnummer());
    }

    @Override
    public FagsakRelasjon finnRelasjonFor(Saksnummer saksnummer) {
        return map.get(saksnummer);
    }

    @Override
    public Optional<FagsakRelasjon> finnRelasjonForHvisEksisterer(Fagsak fagsak) {
        return Optional.ofNullable(map.get(fagsak.getSaksnummer()));
    }

    @Override
    public Optional<FagsakRelasjon> finnRelasjonHvisEksisterer(Saksnummer saksnummer) {
        return Optional.ofNullable(map.get(saksnummer));
    }

    @Override
    public void lagre(Fagsak fagsak, Long behandlingId, Stønadskontoberegning stønadskontoberegning) {
        throw new UnsupportedOperationException(IKKE_STOTTET);
    }

    @Override
    public FagsakRelasjon opprettRelasjon(Fagsak fagsak, Dekningsgrad dekningsgrad) {
        var fagsakRelasjon = mock(FagsakRelasjon.class);
        lenient().when(fagsakRelasjon.getFagsakNrEn()).thenReturn(fagsak);
        lenient().when(fagsakRelasjon.getGjeldendeDekningsgrad()).thenReturn(dekningsgrad);
        map.put(fagsak.getSaksnummer(), fagsakRelasjon);
        return fagsakRelasjon;
    }

    @Override
    public FagsakRelasjon overstyrDekningsgrad(Fagsak fagsak, Dekningsgrad overstyrtVerdi) {
        throw new UnsupportedOperationException(IKKE_STOTTET);
    }

    @Override
    public Optional<FagsakRelasjon> opprettRelasjon(Fagsak fagsak, Optional<FagsakRelasjon> fagsakRelasjon, Dekningsgrad dekningsgrad) {
        throw new UnsupportedOperationException(IKKE_STOTTET);
    }

    @Override
    public FagsakRelasjon oppdaterDekningsgrad(Fagsak fagsak, Dekningsgrad dekningsgrad, Dekningsgrad overstyrtDekningsgrad) {
        throw new UnsupportedOperationException(IKKE_STOTTET);
    }

    @Override
    public Optional<FagsakRelasjon> kobleFagsaker(Fagsak fagsakEn, Fagsak fagsakTo, Behandling behandlingEn) {
        var eksisterendeEn = finnRelasjonFor(fagsakEn).getDekningsgrad();
        var fagsakRelasjon = mock(FagsakRelasjon.class);
        lenient().when(fagsakRelasjon.getFagsakNrEn()).thenReturn(fagsakEn);
        lenient().when(fagsakRelasjon.getFagsakNrTo()).thenReturn(Optional.of(fagsakTo));
        lenient().when(fagsakRelasjon.getRelatertFagsak(fagsakEn)).thenReturn(Optional.of(fagsakTo));
        lenient().when(fagsakRelasjon.getRelatertFagsak(fagsakTo)).thenReturn(Optional.of(fagsakEn));
        lenient().when(fagsakRelasjon.getGjeldendeDekningsgrad()).thenReturn(eksisterendeEn);
        map.put(fagsakEn.getSaksnummer(), fagsakRelasjon);
        map.put(fagsakTo.getSaksnummer(), fagsakRelasjon);
        return Optional.of(fagsakRelasjon);
    }

    @Override
    public Optional<FagsakRelasjon> fraKobleFagsaker(Fagsak fagsakEn, Fagsak fagsakTo) {
        throw new UnsupportedOperationException(IKKE_STOTTET);
    }

    @Override
    public FagsakRelasjon nullstillOverstyrtDekningsgrad(Fagsak fagsak) {
        throw new UnsupportedOperationException(IKKE_STOTTET);
    }

    @Override
    public Optional<FagsakRelasjon> nullstillOverstyrtStønadskontoberegning(Fagsak fagsak) {
        throw new UnsupportedOperationException(IKKE_STOTTET);
    }

    @Override
    public List<Fagsak> finnFagsakerForAvsluttning(LocalDate localDate) {
        throw new UnsupportedOperationException(IKKE_STOTTET);
    }

    @Override
    public Optional<FagsakRelasjon> oppdaterMedAvsluttningsdato(FagsakRelasjon relasjon, LocalDate avsluttningsdato, FagsakRelasjonLås relasjonLås, Optional<FagsakLås> fagsak1Lås, Optional<FagsakLås> fagsak2Lås) {
        throw new UnsupportedOperationException(IKKE_STOTTET);
    }
}
