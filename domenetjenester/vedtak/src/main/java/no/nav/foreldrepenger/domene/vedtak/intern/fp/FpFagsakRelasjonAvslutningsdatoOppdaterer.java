package no.nav.foreldrepenger.domene.vedtak.intern.fp;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.saldo.MaksDatoUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.foreldrepenger.domene.vedtak.intern.FagsakRelasjonAvslutningsdatoOppdaterer;

@ApplicationScoped
@FagsakYtelseTypeRef("FP")
public class FpFagsakRelasjonAvslutningsdatoOppdaterer extends FagsakRelasjonAvslutningsdatoOppdaterer {

    @Inject
    public FpFagsakRelasjonAvslutningsdatoOppdaterer(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                                     StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste,
                                                     UttakInputTjeneste uttakInputTjeneste,
                                                     @FagsakYtelseTypeRef("FP") MaksDatoUttakTjeneste fpMaksDatoUttakTjeneste,
                                                     FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                                                     ForeldrepengerUttakTjeneste fpUttakTjeneste) {
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = behandlingRepositoryProvider.getBehandlingsresultatRepository();
        this.fpUttakTjeneste = fpUttakTjeneste;
        this.familieHendelseRepository = behandlingRepositoryProvider.getFamilieHendelseRepository();
        this.stønadskontoSaldoTjeneste = stønadskontoSaldoTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.maksDatoUttakTjeneste = fpMaksDatoUttakTjeneste;
    }

    protected LocalDate finnAvslutningsdato(Long fagsakId, FagsakRelasjon fagsakRelasjon) {
        var avsluttningsdato = avsluttningsdatoFraEksisterendeFagsakRelasjon(fagsakRelasjon);
        var behandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId);
        if (behandling.isPresent()) {
            avsluttningsdato = avsluttningsdatoHvisBehandlingAvslåttEllerOpphørt(behandling.get(), avsluttningsdato);
            avsluttningsdato = finnAvsluttningsdatoForRelaterteBehandlinger(behandling.get(), avsluttningsdato);
        }

        if (avsluttningsdato == null) {
            avsluttningsdato = LocalDate.now().plusDays(1);
        }
        return avsluttningsdato;
    }

}
