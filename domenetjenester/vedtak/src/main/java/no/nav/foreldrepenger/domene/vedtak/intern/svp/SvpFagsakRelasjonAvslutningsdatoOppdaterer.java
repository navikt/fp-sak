package no.nav.foreldrepenger.domene.vedtak.intern.svp;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.saldo.MaksDatoUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.foreldrepenger.domene.vedtak.intern.FagsakRelasjonAvslutningsdatoOppdaterer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.Optional;

@ApplicationScoped
@FagsakYtelseTypeRef("SVP")
public class SvpFagsakRelasjonAvslutningsdatoOppdaterer extends FagsakRelasjonAvslutningsdatoOppdaterer {

    @Inject
    public SvpFagsakRelasjonAvslutningsdatoOppdaterer(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                                      StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste,
                                                      UttakInputTjeneste uttakInputTjeneste,
                                                      @FagsakYtelseTypeRef("SVP") MaksDatoUttakTjeneste svpMaksDatoUttakTjeneste,
                                                      FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                                                      ForeldrepengerUttakTjeneste fpUttakTjeneste) {
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = behandlingRepositoryProvider.getBehandlingsresultatRepository();
        this.fpUttakTjeneste = fpUttakTjeneste;
        this.familieHendelseRepository = behandlingRepositoryProvider.getFamilieHendelseRepository();
        this.stønadskontoSaldoTjeneste = stønadskontoSaldoTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.maksDatoUttakTjeneste = svpMaksDatoUttakTjeneste;
    }

    protected LocalDate finnAvslutningsdato(Long fagsakId, FagsakRelasjon fagsakRelasjon) {
        LocalDate avsluttningsdato = avsluttningsdatoFraEksisterendeFagsakRelasjon(fagsakRelasjon);
        LocalDate sisteUttaksdato = hentSisteUttaksdatoForFagsak(fagsakId);
        return (erAvsluttningsdatoIkkeSattEllerEtter(avsluttningsdato, sisteUttaksdato))? sisteUttaksdato : avsluttningsdato;
    }

    private LocalDate hentSisteUttaksdatoForFagsak(Long fagsakId) {
        return behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId).map(behandling -> {
            LocalDate avsluttningsdato = avsluttningsdatoHvisBehandlingAvslåttEllerOpphørt(behandling, null);
            var uttakInput = uttakInputTjeneste.lagInput(behandling);
            Optional<LocalDate> maxdatoUttak = maksDatoUttakTjeneste.beregnMaksDatoUttak(uttakInput);
            return (maxdatoUttak.isPresent() && erAvsluttningsdatoIkkeSattEllerEtter(avsluttningsdato, maxdatoUttak.get()))? maxdatoUttak.get().plusDays(1) : avsluttningsdato;
        }).orElse(LocalDate.now().plusDays(1));
    }

}

