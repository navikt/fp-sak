package no.nav.foreldrepenger.domene.vedtak.intern.fp;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.saldo.MaksDatoUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.foreldrepenger.domene.vedtak.intern.UtledeAvslutningsdatoFagsak;
import no.nav.foreldrepenger.regler.uttak.UttakParametre;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
public class FpUtledeAvslutningsdato implements UtledeAvslutningsdatoFagsak {

    private static final int SØKNADSFRIST_I_MÅNEDER = 3;
    static final int PADDING = 13;

    private final FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private final BehandlingRepository behandlingRepository;
    private final BehandlingsresultatRepository behandlingsresultatRepository;
    private final StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste;
    private final UttakInputTjeneste uttakInputTjeneste;
    private final MaksDatoUttakTjeneste maksDatoUttakTjeneste;


    @Inject
    public FpUtledeAvslutningsdato(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                   StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste,
                                   UttakInputTjeneste uttakInputTjeneste,
                                   @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) MaksDatoUttakTjeneste fpMaksDatoUttakTjeneste,
                                   FagsakRelasjonTjeneste fagsakRelasjonTjeneste) {
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = behandlingRepositoryProvider.getBehandlingsresultatRepository();
        this.stønadskontoSaldoTjeneste = stønadskontoSaldoTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.maksDatoUttakTjeneste = fpMaksDatoUttakTjeneste;
    }



    public LocalDate utledAvslutningsdato(Long fagsakId, FagsakRelasjon fagsakRelasjon) {
        var avslutningsdato = avslutningsdatoFraEksisterendeFagsakRelasjon(fagsakRelasjon).orElse(null);

        var behandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId).orElse(null);

        if (behandling != null) {
            var uttakInput = uttakInputTjeneste.lagInput(behandling);
            ForeldrepengerGrunnlag fpGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
            var familieHendelser = fpGrunnlag.getFamilieHendelser();

            if (familieHendelser != null ) {
                var familieHendelse = familieHendelser.getGjeldendeFamilieHendelse();
                var nesteSakGrunnlag = fpGrunnlag.getNesteSakGrunnlag().orElse(null);
                var saldoUtregning = stønadskontoSaldoTjeneste.finnSaldoUtregning(uttakInput);

                if (familieHendelse.erAlleBarnDøde()) {
                    return leggPåSøknadsfristMåneder(hentSisteDødsdatoOgEnDag(familieHendelse).plusWeeks(UttakParametre.ukerTilgjengeligEtterDødsfall(LocalDate.now())));
                }

                //Nytt barn (ny stønadsperiode)
                if (nesteSakGrunnlag != null) {
                    //minsterett ved 2 tette fødsler når begge barna er født innenfor 48 uker,
                    // avslutningsdato må ta høyde for om minsteretten er oppbrukt eller ikke
                    if (!saldoUtregning.restSaldoEtterNesteStønadsperiode().merEnn0()) {
                        return leggPåSøknadsfristMåneder(nesteSakGrunnlag.getStartdato());
                    }
                }

                var stønadRest = stønadskontoSaldoTjeneste.finnStønadRest(saldoUtregning);
                var sisteUttaksdatoFraBeggeParterMedRestdager = maksDatoUttakTjeneste.beregnMaksDatoUttakSakskompleks(uttakInput, stønadRest);
                var skjæringstidspunkt = familieHendelse.getFamilieHendelseDato();
                var maksDatoFraStp = leggPåMaksSøknadsfrist(skjæringstidspunkt);

                if (sisteUttaksdatoFraBeggeParterMedRestdager.isEmpty()) {
                    return ingenAvslutningsdatoEllerNyDatoErFør(avslutningsdato, maksDatoFraStp) ? maksDatoFraStp : avslutningsdato;
                }

                var sisteUttaksdatoFraBeggeParterMedRestOgEnDag = sisteUttaksdatoFraBeggeParterMedRestdager.get().plusDays(1);

                if (behandlingErOpphørt(behandling) && !harKoblingTilAnnenPart(behandling.getFagsak())) {
                    return leggPåSøknadsfristMåneder(sisteUttaksdatoFraBeggeParterMedRestOgEnDag);
                }
                return avslutningsdatoVedInnvilget(uttakInput, stønadRest,
                    sisteUttaksdatoFraBeggeParterMedRestOgEnDag, maksDatoFraStp);
            }
        }
        return Optional.ofNullable(avslutningsdato).orElseGet(() -> LocalDate.now().plusDays(1));
    }

    private LocalDate hentSisteDødsdatoOgEnDag(FamilieHendelse familieHendelse) {
        return familieHendelse
            .getBarna()
            .stream()
            .map(Barn::getDødsdato)
            .flatMap(Optional::stream)
            .max(Comparator.naturalOrder())
            .map(dd -> dd.plusDays(1))
            .orElseThrow();
    }

    private LocalDate avslutningsdatoVedInnvilget(UttakInput uttakInput,
                                                  int stønadRest,
                                                  LocalDate sisteUttaksdatoFraBeggeParterMedRestOgEnDag,
                                                  LocalDate maksDatoFraStp) {
        var friUtsettelse = !uttakInput.getSkjæringstidspunkt().kreverSammenhengendeUttak();
        if (friUtsettelse) {
            if (stønadRest <= 0)
            {
                return leggPåSøknadsfristMåneder(sisteUttaksdatoFraBeggeParterMedRestOgEnDag);
            } else {
                return maksDatoFraStp;
            }
        } else {
            var sisteUttaksdatoMedsøknadsfrist = leggPåSøknadsfristMåneder(sisteUttaksdatoFraBeggeParterMedRestOgEnDag);
            return maksDatoFraStp.isBefore(sisteUttaksdatoMedsøknadsfrist)? maksDatoFraStp : sisteUttaksdatoMedsøknadsfrist;
        }
    }

    private boolean harKoblingTilAnnenPart(Fagsak fagsak) {
        return fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak).filter(fagsakRelasjon -> fagsakRelasjon.getFagsakNrTo().isPresent()).isPresent();
    }

    private LocalDate leggPåSøknadsfristMåneder(LocalDate fraDato) {
        // Lastbalansering
        var padding = System.nanoTime() % PADDING;
        return fraDato.plusMonths(SØKNADSFRIST_I_MÅNEDER).with(TemporalAdjusters.lastDayOfMonth()).plusDays(padding);
    }

    private static LocalDate leggPåMaksSøknadsfrist(LocalDate fraDato) {
        return fraDato.plusYears(UttakParametre.årMaksimalStønadsperiode(LocalDate.now()));
    }

    private static Optional<LocalDate> avslutningsdatoFraEksisterendeFagsakRelasjon(FagsakRelasjon fagsakRelasjon) {
        return Optional.ofNullable(fagsakRelasjon.getAvsluttningsdato()).filter(ad -> ad.isAfter(LocalDate.now()));

    }

    private boolean behandlingErOpphørt(Behandling behandling) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).filter(Behandlingsresultat::isBehandlingsresultatOpphørt).isPresent();
    }
    //hvorfor gjør vi denne sjekken mot eksisterende avslutningsdato for fri utsettelse og ikke sammenhengende?
    private static boolean ingenAvslutningsdatoEllerNyDatoErFør(LocalDate avsluttningsdato, LocalDate nyAvsluttningsdato) {
        return avsluttningsdato == null || nyAvsluttningsdato.isBefore(avsluttningsdato);
    }

}
