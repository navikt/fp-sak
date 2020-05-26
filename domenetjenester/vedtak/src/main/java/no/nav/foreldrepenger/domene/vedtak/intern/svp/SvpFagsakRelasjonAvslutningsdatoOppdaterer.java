package no.nav.foreldrepenger.domene.vedtak.intern.svp;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
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

    private MaksDatoUttakTjeneste maksDatoUttakTjeneste;

    public SvpFagsakRelasjonAvslutningsdatoOppdaterer() {
        // NOSONAR
    }

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

        Optional<Behandling> behandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId);
        if (behandling.isPresent()) {
            avsluttningsdato = avsluttningsdatoHvisBehandlingAvslåttEllerOpphørt(behandling.get(), avsluttningsdato);
            avsluttningsdato = avsluttningsdatoHvisDetIkkeErStønadsdagerIgjen(behandling.get(), avsluttningsdato);
            if(fagsakRelasjon.getFagsakNrTo().isEmpty()){
                Optional<LocalDate> sisteUttaksdato = hentSisteUttaksdatoForFagsak(behandling.get().getFagsakId());
                if(sisteUttaksdato.isPresent() && erAvsluttningsdatoIkkeSattEllerEtter(avsluttningsdato, sisteUttaksdato.get()))avsluttningsdato = sisteUttaksdato.get();
            }
            avsluttningsdato = avsluttningsdatoHvisDetErStønadsdagerIgjen(behandling.get(), avsluttningsdato);
        }

        if (avsluttningsdato == null) {
            avsluttningsdato = LocalDate.now().plusDays(1);
        }
        return avsluttningsdato;
    }

    private Optional<LocalDate> hentSisteUttaksdatoForFagsak(Long fagsakId) {
        Optional<Behandling> sisteYtelsesvedtak = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId);
        if (sisteYtelsesvedtak.isPresent()) {
            var uttakInput = uttakInputTjeneste.lagInput(sisteYtelsesvedtak.get());
            return maksDatoUttakTjeneste.beregnMaksDatoUttak(uttakInput);
        } else return Optional.empty();
    }
}
