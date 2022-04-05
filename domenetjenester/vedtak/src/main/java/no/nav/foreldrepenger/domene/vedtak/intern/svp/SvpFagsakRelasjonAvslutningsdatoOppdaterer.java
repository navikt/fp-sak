package no.nav.foreldrepenger.domene.vedtak.intern.svp;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLås;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.laas.FagsakRelasjonLås;
import no.nav.foreldrepenger.domene.uttak.saldo.MaksDatoUttakTjeneste;
import no.nav.foreldrepenger.domene.vedtak.intern.FagsakRelasjonAvslutningsdatoOppdaterer;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
public class SvpFagsakRelasjonAvslutningsdatoOppdaterer implements FagsakRelasjonAvslutningsdatoOppdaterer {

    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    private UttakInputTjeneste uttakInputTjeneste;
    private MaksDatoUttakTjeneste maksDatoUttakTjeneste;


    @Inject
    public SvpFagsakRelasjonAvslutningsdatoOppdaterer(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                                      UttakInputTjeneste uttakInputTjeneste,
                                                      @FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER) MaksDatoUttakTjeneste svpMaksDatoUttakTjeneste,
                                                      FagsakRelasjonTjeneste fagsakRelasjonTjeneste) {
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = behandlingRepositoryProvider.getBehandlingsresultatRepository();
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.maksDatoUttakTjeneste = svpMaksDatoUttakTjeneste;
    }

    @Override
    public void oppdaterFagsakRelasjonAvsluttningsdato(FagsakRelasjon relasjon,
                                                Long fagsakId,
                                                FagsakRelasjonLås lås,
                                                Optional<FagsakLås> fagsak1Lås,
                                                Optional<FagsakLås> fagsak2Lås) {
        var avslutningsdato = finnAvslutningsdato(fagsakId, relasjon);
        fagsakRelasjonTjeneste.oppdaterMedAvsluttningsdato(relasjon, avslutningsdato, lås, fagsak1Lås, fagsak2Lås);
    }

    LocalDate finnAvslutningsdato(Long fagsakId, FagsakRelasjon fagsakRelasjon) {
        var avsluttningsdato = avsluttningsdatoFraEksisterendeFagsakRelasjon(fagsakRelasjon);
        var sisteUttaksdato = hentSisteUttaksdatoForFagsak(fagsakId, avsluttningsdato);
        return erAvsluttningsdatoIkkeSattEllerEtter(avsluttningsdato, sisteUttaksdato) ? sisteUttaksdato : avsluttningsdato;
    }

    private LocalDate hentSisteUttaksdatoForFagsak(Long fagsakId, LocalDate avslutningsdato) {
        return behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId).map(behandling -> {
            var avsluttningsdato = avsluttningsdatoHvisBehandlingAvslåttEllerOpphørt(behandling);
            var uttakInput = uttakInputTjeneste.lagInput(behandling);
            var maxdatoUttak = maksDatoUttakTjeneste.beregnMaksDatoUttak(uttakInput);
            return maxdatoUttak.filter(d -> erAvsluttningsdatoIkkeSattEllerEtter(avsluttningsdato, d)).isPresent() ?
                maxdatoUttak.get().plusDays(1) : avsluttningsdato;
        }).orElse(LocalDate.now().plusDays(1));
    }

    private LocalDate avsluttningsdatoFraEksisterendeFagsakRelasjon(FagsakRelasjon fagsakRelasjon) {
        if (fagsakRelasjon.getAvsluttningsdato() != null && fagsakRelasjon.getAvsluttningsdato().isAfter(LocalDate.now())) {
            return fagsakRelasjon.getAvsluttningsdato();
        }
        return null;
    }

    private LocalDate avsluttningsdatoHvisBehandlingAvslåttEllerOpphørt(Behandling behandling) {

        var behandlingsresultatAvslåttOrOpphørt = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())
            .filter(Behandlingsresultat::isBehandlingsresultatAvslåttOrOpphørt).isPresent();
        return behandlingsresultatAvslåttOrOpphørt ? LocalDate.now().plusDays(1) : null;
    }

    private boolean erAvsluttningsdatoIkkeSattEllerEtter(LocalDate avsluttningsdato, LocalDate nyAvsluttningsdato) {
        return avsluttningsdato == null || nyAvsluttningsdato.isBefore(avsluttningsdato);
    }

}

