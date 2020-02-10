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
import no.nav.foreldrepenger.behandlingslager.laas.FagsakRelasjonLås;
import no.nav.foreldrepenger.behandlingslager.uttak.Stønadskontoberegning;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
public class FagsakRelasjonTjeneste {

    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private FagsakRelasjonEventPubliserer fagsakRelasjonEventPubliserer;

    FagsakRelasjonTjeneste() {
        // for CDI proxy
    }

    @Inject
    public FagsakRelasjonTjeneste(FagsakRelasjonRepository fagsakRelasjonRepository,
                                  FagsakRelasjonEventPubliserer fagsakRelasjonEventPubliserer) {

        this.fagsakRelasjonEventPubliserer = fagsakRelasjonEventPubliserer;

        if (fagsakRelasjonEventPubliserer != null) {
            this.fagsakRelasjonEventPubliserer = fagsakRelasjonEventPubliserer;
        } else {
            this.fagsakRelasjonEventPubliserer = FagsakRelasjonEventPubliserer.NULL_EVENT_PUB;
        }
        this.fagsakRelasjonRepository = fagsakRelasjonRepository;

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

    public void lagre(Fagsak fagsak, FagsakRelasjon fagsakRelasjon, Long behandlingId, Stønadskontoberegning stønadskontoberegning) {
        fagsakRelasjonRepository.lagre(fagsak, behandlingId, stønadskontoberegning);
        fagsakRelasjonEventPubliserer.fireEvent(fagsakRelasjon);
    }

    public FagsakRelasjon opprettRelasjon(Fagsak fagsak, Dekningsgrad dekningsgrad) {
        FagsakRelasjon fagsakRelasjon = fagsakRelasjonRepository.opprettRelasjon(fagsak, dekningsgrad);
        fagsakRelasjonEventPubliserer.fireEvent(fagsakRelasjon);
        return fagsakRelasjon;
    }

    public FagsakRelasjon overstyrDekningsgrad(Fagsak fagsak, Dekningsgrad overstyrtVerdi) {
        FagsakRelasjon fagsakRelasjon = fagsakRelasjonRepository.overstyrDekningsgrad(fagsak, overstyrtVerdi);
        fagsakRelasjonEventPubliserer.fireEvent(fagsakRelasjon);
        return fagsakRelasjon;
    }

    public void opprettEllerOppdaterRelasjon(Fagsak fagsak, Optional<FagsakRelasjon> fagsakRelasjon, Dekningsgrad dekningsgrad) {
        Optional<FagsakRelasjon> fagsakRelasjonOpt = fagsakRelasjonRepository.opprettEllerOppdaterRelasjon(fagsak, fagsakRelasjon, dekningsgrad);
        if (fagsakRelasjonOpt.isPresent()) {
            fagsakRelasjonEventPubliserer.fireEvent(fagsakRelasjonOpt.get());
        }
    }

    public void kobleFagsaker(Fagsak fagsakEn, Fagsak fagsakTo, Behandling behandlingEn) {
        Optional<FagsakRelasjon> fagsakRelasjonOpt = fagsakRelasjonRepository.kobleFagsaker(fagsakEn, fagsakTo, behandlingEn);
        if (fagsakRelasjonOpt.isPresent()) {
            fagsakRelasjonEventPubliserer.fireEvent(fagsakRelasjonOpt.get());
        }
    }

    public void fraKobleFagsaker(Fagsak fagsakEn, Fagsak fagsakTo) {
        Optional<FagsakRelasjon> fagsakRelasjonOpt = fagsakRelasjonRepository.fraKobleFagsaker(fagsakEn, fagsakTo);
        if (fagsakRelasjonOpt.isPresent()) {
            fagsakRelasjonEventPubliserer.fireEvent(fagsakRelasjonOpt.get());
        }
    }

    public void nullstillOverstyrtDekningsgrad(Fagsak fagsak) {
        FagsakRelasjon fr = fagsakRelasjonRepository.nullstillOverstyrtDekningsgrad(fagsak);
        fagsakRelasjonEventPubliserer.fireEvent(fr);
    }


    public void overstyrStønadskontoberegning(Fagsak fagsak, Long behandlingId, Stønadskontoberegning stønadskontoberegning) {
        fagsakRelasjonRepository.overstyrStønadskontoberegning(fagsak, behandlingId, stønadskontoberegning);
    }

    public void oppdaterMedAvsluttningsdato(FagsakRelasjon relasjon, LocalDate avsluttningsdato, FagsakRelasjonLås lås, Optional<FagsakLås> fagsak1Lås, Optional<FagsakLås> fagsak2Lås) {
        Optional<FagsakRelasjon> fagsakRelasjonOpt = fagsakRelasjonRepository.oppdaterMedAvsluttningsdato(relasjon, avsluttningsdato, lås, fagsak1Lås, fagsak2Lås);
        if (fagsakRelasjonOpt.isPresent()) {
            fagsakRelasjonEventPubliserer.fireEvent(fagsakRelasjonOpt.get());
        }
    }
}
