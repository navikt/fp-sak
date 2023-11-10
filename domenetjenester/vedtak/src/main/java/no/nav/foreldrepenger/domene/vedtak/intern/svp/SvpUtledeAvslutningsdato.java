package no.nav.foreldrepenger.domene.vedtak.intern.svp;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.input.SvangerskapspengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.saldo.MaksDatoUttakTjeneste;
import no.nav.foreldrepenger.domene.vedtak.intern.UtledeAvslutningsdatoFagsak;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
public class SvpUtledeAvslutningsdato implements UtledeAvslutningsdatoFagsak {

    private BehandlingRepository behandlingRepository;
    private static final int SØKNADSFRIST_I_MÅNEDER = 3;

    static final int PADDING = 13;

    private UttakInputTjeneste uttakInputTjeneste;
    private MaksDatoUttakTjeneste maksDatoUttakTjeneste;


    @Inject
    public SvpUtledeAvslutningsdato(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                    UttakInputTjeneste uttakInputTjeneste,
                                    @FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER) MaksDatoUttakTjeneste svpMaksDatoUttakTjeneste) {
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.maksDatoUttakTjeneste = svpMaksDatoUttakTjeneste;
    }


   public LocalDate utledAvslutningsdato(Long fagsakId, FagsakRelasjon fagsakRelasjon) {
        var avslutningsdato = avslutningsdatoFraEksisterendeFagsakRelasjon(fagsakRelasjon).orElse(null);
        var behandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId).orElse(null);

        if (behandling != null) {
            var uttakInput = uttakInputTjeneste.lagInput(behandling);
            SvangerskapspengerGrunnlag svpGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
            var nesteSakGrunnlag = svpGrunnlag.nesteSakEntitet().orElse(null);

            //opphør nytt barn
            if (nesteSakGrunnlag != null) {
                return leggPåSøknadsfrist(nesteSakGrunnlag.getStartdato());
            }
            return sisteUttaksdatoOgEnDag(uttakInput).map(this::leggPåSøknadsfrist).orElse(LocalDate.now().plusDays(1));
        }
        return Optional.ofNullable(avslutningsdato).orElseGet(() -> LocalDate.now().plusDays(1));
    }

    private Optional<LocalDate> sisteUttaksdatoOgEnDag(UttakInput uttakInput) {
        return maksDatoUttakTjeneste.beregnMaksDatoUttak(uttakInput)
            .map( su -> su.plusDays(1));
    }

    private Optional<LocalDate> avslutningsdatoFraEksisterendeFagsakRelasjon(FagsakRelasjon fagsakRelasjon) {
        return Optional.ofNullable(fagsakRelasjon.getAvsluttningsdato()).filter(ad -> ad.isAfter(LocalDate.now()));
    }

    private LocalDate leggPåSøknadsfrist(LocalDate sisteUttaksdato) {
        // Lastbalansering
        var padding = System.nanoTime() % PADDING;
        return sisteUttaksdato.plusMonths(SØKNADSFRIST_I_MÅNEDER).with(TemporalAdjusters.lastDayOfMonth()).plusDays(padding);
    }
}

