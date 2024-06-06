package no.nav.foreldrepenger.behandling;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
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
import no.nav.foreldrepenger.behandlingslager.laas.FagsakRelasjonLås;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
public class FagsakRelasjonTjeneste {

    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private FagsakRelasjonEventPubliserer fagsakRelasjonEventPubliserer;
    private FagsakRepository fagsakRepository;

    @Inject
    public FagsakRelasjonTjeneste(FagsakRepository fagsakRepository,
                                  FagsakRelasjonEventPubliserer fagsakRelasjonEventPubliserer,
                                  FagsakRelasjonRepository fagsakRelasjonRepository) {
        this.fagsakRepository = fagsakRepository;
        this.fagsakRelasjonEventPubliserer = Objects.requireNonNullElse(fagsakRelasjonEventPubliserer, FagsakRelasjonEventPubliserer.NULL_EVENT_PUB);
        this.fagsakRelasjonRepository = fagsakRelasjonRepository;
    }

    public FagsakRelasjonTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                  FagsakRelasjonEventPubliserer fagsakRelasjonEventPubliserer) {
        this(repositoryProvider.getFagsakRepository(), fagsakRelasjonEventPubliserer, repositoryProvider.getFagsakRelasjonRepository());
    }

    public FagsakRelasjonTjeneste(BehandlingRepositoryProvider repositoryProvider) {
        this(repositoryProvider, null);
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
                      FagsakRelasjon fagsakRelasjon,
                      Stønadskontoberegning stønadskontoberegning) {
        var fagsak = finnFagsak(fagsakId);
        fagsakRelasjonRepository.lagre(fagsak, stønadskontoberegning);
        fagsakRelasjonEventPubliserer.fireEvent(fagsakRelasjon);
    }

    public FagsakRelasjon opprettRelasjon(Fagsak fagsak) {
        var fagsakRelasjon = fagsakRelasjonRepository.opprettRelasjon(fagsak);
        fagsakRelasjonEventPubliserer.fireEvent(fagsakRelasjon);
        return fagsakRelasjon;
    }

    public void oppdaterDekningsgrad(Long fagsakId, Dekningsgrad dekningsgrad) {
        var fagsakRelasjon = fagsakRelasjonRepository.oppdaterDekningsgrad(fagsakRepository.finnEksaktFagsakReadOnly(fagsakId), dekningsgrad, null);
        fagsakRelasjonEventPubliserer.fireEvent(fagsakRelasjon);
    }

    public void opprettRelasjon(Fagsak fagsak, FagsakRelasjon fagsakRelasjon) {
        var fr = fagsakRelasjonRepository.opprettRelasjon(fagsak, fagsakRelasjon);
        fagsakRelasjonEventPubliserer.fireEvent(fr);
    }

    public void kobleFagsaker(Fagsak fagsakEn, Fagsak fagsakTo) {
        var fagsakRelasjonOpt = fagsakRelasjonRepository.kobleFagsaker(fagsakEn, fagsakTo);
        fagsakRelasjonOpt.ifPresent(fagsakRelasjon -> fagsakRelasjonEventPubliserer.fireEvent(fagsakRelasjon));
    }

    public void fraKobleFagsaker(Fagsak fagsakEn, Fagsak fagsakTo) {
        var fagsakRelasjonOpt = fagsakRelasjonRepository.fraKobleFagsaker(fagsakEn, fagsakTo);
        fagsakRelasjonOpt.ifPresent(fagsakRelasjon -> fagsakRelasjonEventPubliserer.fireEvent(fagsakRelasjon));
    }

    private Fagsak finnFagsak(long fagsakId) {
        return fagsakRepository.finnEksaktFagsak(fagsakId);
    }

    public void oppdaterMedAvslutningsdato(FagsakRelasjon relasjon, LocalDate avsluttningsdato, FagsakRelasjonLås lås,
                                           Optional<FagsakLås> fagsak1Lås, Optional<FagsakLås> fagsak2Lås) {
        var fagsakRelasjonOpt = fagsakRelasjonRepository.oppdaterMedAvsluttningsdato(relasjon, avsluttningsdato, lås, fagsak1Lås,
                fagsak2Lås);
        fagsakRelasjonOpt.ifPresent(fagsakRelasjon -> fagsakRelasjonEventPubliserer.fireEvent(fagsakRelasjon));
    }

    public Optional<FagsakRelasjon> nullstillOverstyrtStønadskontoberegning(Fagsak fagsak) {
        return fagsakRelasjonRepository.nullstillOverstyrtStønadskontoberegning(fagsak);
    }

    public List<Fagsak> finnFagsakerForAvsluttning(LocalDate dato) {
        return fagsakRelasjonRepository.finnFagsakerForAvsluttning(dato);
    }
}
