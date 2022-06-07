package no.nav.foreldrepenger.behandling;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
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
    public FagsakRelasjonTjeneste(FagsakRelasjonRepository fagsakRelasjonRepository,
            FagsakRelasjonEventPubliserer fagsakRelasjonEventPubliserer,
            FagsakRepository fagsakRepository) {

        this.fagsakRelasjonEventPubliserer = fagsakRelasjonEventPubliserer;
        this.fagsakRepository = fagsakRepository;

        if (fagsakRelasjonEventPubliserer != null) {
            this.fagsakRelasjonEventPubliserer = fagsakRelasjonEventPubliserer;
        } else {
            this.fagsakRelasjonEventPubliserer = FagsakRelasjonEventPubliserer.NULL_EVENT_PUB;
        }
        this.fagsakRelasjonRepository = fagsakRelasjonRepository;

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

    public void lagre(long fagsakId, FagsakRelasjon fagsakRelasjon, Long behandlingId, Stønadskontoberegning stønadskontoberegning) {
        var fagsak = finnFagsak(fagsakId);
        fagsakRelasjonRepository.lagre(fagsak, behandlingId, stønadskontoberegning);
        fagsakRelasjonEventPubliserer.fireEvent(fagsakRelasjon);
    }

    public FagsakRelasjon opprettRelasjon(Fagsak fagsak, Dekningsgrad dekningsgrad) {
        var fagsakRelasjon = fagsakRelasjonRepository.opprettRelasjon(fagsak, dekningsgrad);
        fagsakRelasjonEventPubliserer.fireEvent(fagsakRelasjon);
        return fagsakRelasjon;
    }

    public FagsakRelasjon overstyrDekningsgrad(Fagsak fagsak, Dekningsgrad overstyrtVerdi) {
        var fagsakRelasjon = fagsakRelasjonRepository.overstyrDekningsgrad(fagsak, overstyrtVerdi);
        fagsakRelasjonEventPubliserer.fireEvent(fagsakRelasjon);
        return fagsakRelasjon;
    }

    public void opprettEllerOppdaterRelasjon(Fagsak fagsak, Optional<FagsakRelasjon> fagsakRelasjon, Dekningsgrad dekningsgrad) {
        var fagsakRelasjonOpt = fagsakRelasjonRepository.opprettEllerOppdaterRelasjon(fagsak, fagsakRelasjon, dekningsgrad);
        fagsakRelasjonOpt.ifPresent(relasjon -> fagsakRelasjonEventPubliserer.fireEvent(relasjon));
    }

    public void kobleFagsaker(Fagsak fagsakEn, Fagsak fagsakTo, Behandling behandlingEn) {
        var fagsakRelasjonOpt = fagsakRelasjonRepository.kobleFagsaker(fagsakEn, fagsakTo, behandlingEn);
        fagsakRelasjonOpt.ifPresent(fagsakRelasjon -> fagsakRelasjonEventPubliserer.fireEvent(fagsakRelasjon));
    }

    public void fraKobleFagsaker(Fagsak fagsakEn, Fagsak fagsakTo) {
        var fagsakRelasjonOpt = fagsakRelasjonRepository.fraKobleFagsaker(fagsakEn, fagsakTo);
        fagsakRelasjonOpt.ifPresent(fagsakRelasjon -> fagsakRelasjonEventPubliserer.fireEvent(fagsakRelasjon));
    }

    public void nullstillOverstyrtDekningsgrad(Fagsak fagsak) {
        var fr = fagsakRelasjonRepository.nullstillOverstyrtDekningsgrad(fagsak);
        fagsakRelasjonEventPubliserer.fireEvent(fr);
    }

    public void overstyrStønadskontoberegning(long fagsakId, Long behandlingId, Stønadskontoberegning stønadskontoberegning) {
        var fagsak = finnFagsak(fagsakId);
        fagsakRelasjonRepository.overstyrStønadskontoberegning(fagsak, behandlingId, stønadskontoberegning);
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
}
