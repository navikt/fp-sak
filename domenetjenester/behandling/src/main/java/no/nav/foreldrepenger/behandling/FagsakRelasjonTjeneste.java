package no.nav.foreldrepenger.behandling;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLås;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
public class FagsakRelasjonTjeneste {

    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private FagsakRepository fagsakRepository;

    @Inject
    public FagsakRelasjonTjeneste(FagsakRepository fagsakRepository,
                                  FagsakRelasjonRepository fagsakRelasjonRepository) {
        this.fagsakRepository = fagsakRepository;
        this.fagsakRelasjonRepository = fagsakRelasjonRepository;
    }

    public FagsakRelasjonTjeneste(BehandlingRepositoryProvider repositoryProvider) {
        this(repositoryProvider.getFagsakRepository(), repositoryProvider.getFagsakRelasjonRepository());
    }

    FagsakRelasjonTjeneste() {
        // for CDI proxy
    }

    public FagsakRelasjon finnRelasjonFor(Fagsak fagsak) {
        return fagsakRelasjonRepository.finnRelasjonFor(fagsak);
    }

    public FagsakRelasjon finnRelasjonFor(Saksnummer saksnummer) {
        return fagsakRelasjonRepository.finnRelasjonFor(saksnummer);
    }

    public Optional<FagsakRelasjon> finnRelasjonForHvisEksisterer(Fagsak fagsak) {
        return fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(fagsak);
    }

    public Optional<FagsakRelasjon> finnRelasjonHvisEksisterer(Saksnummer saksnummer) {
        return fagsakRelasjonRepository.finnRelasjonHvisEksisterer(saksnummer);
    }

    public Optional<FagsakRelasjon> finnRelasjonForHvisEksisterer(long fagsakId) {
        return fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(fagsakId);
    }

    public Optional<FagsakRelasjon> finnRelasjonForHvisEksisterer(long fagsakId, LocalDateTime aktivPåTidspunkt) {
        return fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(fagsakId, aktivPåTidspunkt);
    }

    public void lagre(long fagsakId,
                      Stønadskontoberegning stønadskontoberegning) {
        var fagsak = finnFagsak(fagsakId);
        fagsakRelasjonRepository.lagre(fagsak, stønadskontoberegning);
    }

    public FagsakRelasjon opprettRelasjon(Fagsak fagsak) {
        return fagsakRelasjonRepository.opprettRelasjon(fagsak);
    }

    public void oppdaterDekningsgrad(Long fagsakId, Dekningsgrad dekningsgrad) {
        fagsakRelasjonRepository.oppdaterDekningsgrad(fagsakRepository.finnEksaktFagsakReadOnly(fagsakId), dekningsgrad);
    }

    public void kobleFagsaker(Fagsak fagsakEn, Fagsak fagsakTo) {
        fagsakRelasjonRepository.kobleFagsaker(fagsakEn, fagsakTo);
    }

    public void fraKobleFagsaker(Fagsak fagsakEn, Fagsak fagsakTo) {
        fagsakRelasjonRepository.fraKobleFagsaker(fagsakEn, fagsakTo);
    }

    private Fagsak finnFagsak(long fagsakId) {
        return fagsakRepository.finnEksaktFagsak(fagsakId);
    }

    public void oppdaterMedAvslutningsdato(FagsakRelasjon relasjon,
                                           LocalDate avsluttningsdato,
                                           Optional<FagsakLås> fagsak1Lås,
                                           Optional<FagsakLås> fagsak2Lås) {
        fagsakRelasjonRepository.oppdaterMedAvsluttningsdato(relasjon, avsluttningsdato, fagsak1Lås, fagsak2Lås);
    }

    public void nullstillOverstyrtStønadskontoberegning(Fagsak fagsak) {
        fagsakRelasjonRepository.nullstillOverstyrtStønadskontoberegning(fagsak);
    }

    public List<Fagsak> finnFagsakerForAvsluttning(LocalDate dato) {
        return fagsakRelasjonRepository.finnFagsakerForAvsluttning(dato);
    }
}
