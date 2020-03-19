package no.nav.foreldrepenger.skjæringstidspunkt.fp;

import static no.nav.foreldrepenger.behandlingslager.uttak.IkkeOppfyltÅrsak.SØKNADSFRIST;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt.Builder;
import no.nav.foreldrepenger.behandling.YtelseMaksdatoTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktFeil;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktRegisterinnhentingTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.konfig.Tid;
import no.nav.vedtak.util.FPDateUtil;

@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class SkjæringstidspunktTjenesteImpl implements SkjæringstidspunktTjeneste , SkjæringstidspunktRegisterinnhentingTjeneste {

    private FamilieHendelseRepository familieGrunnlagRepository;
    private SkjæringstidspunktUtils utlederUtils;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private UttakRepository uttakRepository;
    private OpptjeningRepository opptjeningRepository;
    private SøknadRepository søknadRepository;
    private BehandlingRepository behandlingRepository;
    private YtelseMaksdatoTjeneste ytelseMaksdatoTjeneste;

    SkjæringstidspunktTjenesteImpl() {
        // CDI
    }

    @Inject
    public SkjæringstidspunktTjenesteImpl(BehandlingRepositoryProvider repositoryProvider,
                                          YtelseMaksdatoTjeneste beregnMorsMaksdatoTjeneste,
                                          SkjæringstidspunktUtils utlederUtils) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.uttakRepository = repositoryProvider.getUttakRepository();
        this.opptjeningRepository = repositoryProvider.getOpptjeningRepository();
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.familieGrunnlagRepository = repositoryProvider.getFamilieHendelseRepository();
        this.utlederUtils = utlederUtils;
        this.ytelseMaksdatoTjeneste = beregnMorsMaksdatoTjeneste;
    }

    @Override
    public LocalDate utledSkjæringstidspunktForRegisterInnhenting(Long behandlingId) {
        final Optional<FamilieHendelseGrunnlagEntitet> familieHendelseAggregat = familieGrunnlagRepository.hentAggregatHvisEksisterer(behandlingId);

        return familieHendelseAggregat.map(utlederUtils::utledSkjæringstidspunktRegisterinnhenting).orElse(FPDateUtil.iDag());
    }

    @Override
    public Skjæringstidspunkt getSkjæringstidspunkter(Long behandlingId) {
        Builder builder = Skjæringstidspunkt.builder();

        LocalDate førsteUttaksdato = førsteUttaksdag(behandlingId);
        builder.medFørsteUttaksdato(førsteUttaksdato);

        Optional<Opptjening> opptjening = opptjeningRepository.finnOpptjening(behandlingId);
        if (opptjening.map(Opptjening::erOpptjeningPeriodeVilkårOppfylt).orElse(Boolean.FALSE)) {
            LocalDate skjæringstidspunktOpptjening = opptjening.get().getTom().plusDays(1);
            builder.medSkjæringstidspunktOpptjening(skjæringstidspunktOpptjening);
            builder.medUtledetSkjæringstidspunkt(skjæringstidspunktOpptjening);
            return builder.build();
        }

        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Optional<FamilieHendelseGrunnlagEntitet> familieHendelseGrunnlag = familieGrunnlagRepository.hentAggregatHvisEksisterer(behandlingId);
        Optional<LocalDate> morsMaksDato = ytelseMaksdatoTjeneste.beregnMorsMaksdato(behandling.getFagsak().getSaksnummer(), behandling.getFagsak().getRelasjonsRolleType());
        builder.medUtledetSkjæringstidspunkt(utlederUtils.utledSkjæringstidspunktFraBehandling(behandling, førsteUttaksdato, familieHendelseGrunnlag, morsMaksDato));

        return builder.build();
    }

    private LocalDate førsteUttaksdag(Long behandlingId) {
        final Optional<YtelseFordelingAggregat> ytelseFordelingAggregat = hentYtelseFordelingAggregatFor(behandlingId);

        final Optional<LocalDate> avklartStartDato = ytelseFordelingAggregat.flatMap(YtelseFordelingAggregat::getAvklarteDatoer)
            .map(AvklarteUttakDatoerEntitet::getFørsteUttaksdato);

        return avklartStartDato.orElseGet(() -> førsteØnskedeUttaksdag(behandlingId, ytelseFordelingAggregat));
    }

    private Optional<YtelseFordelingAggregat> hentYtelseFordelingAggregatFor(Long behandlingId) {
        return ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId);
    }

    private LocalDate førsteØnskedeUttaksdag(Long behandlingId, Optional<YtelseFordelingAggregat> ytelseFordelingAggregat) {
        Optional<OppgittFordelingEntitet> oppgittFordeling = ytelseFordelingAggregat.map(YtelseFordelingAggregat::getOppgittFordeling);

        final Optional<LocalDate> førsteØnskedeUttaksdagIBehandling = finnFørsteØnskedeUttaksdagFor(oppgittFordeling);

        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        if (behandling.erRevurdering()) {
            final Optional<LocalDate> førsteUttaksdagIForrigeVedtak = finnFørsteDatoIUttakResultat(behandling);
            if (!førsteUttaksdagIForrigeVedtak.isPresent() && !førsteØnskedeUttaksdagIBehandling.isPresent()) {
                Optional<YtelseFordelingAggregat> ytelseFordelingForOriginalBehandling = hentYtelseFordelingAggregatFor(originalBehandling(behandling).getId());
                return finnFørsteØnskedeUttaksdagFor(ytelseFordelingForOriginalBehandling.map(YtelseFordelingAggregat::getOppgittFordeling))
                    .orElseThrow(() -> SkjæringstidspunktFeil.FACTORY.finnerIkkeSkjæringstidspunktForForeldrepenger(behandlingId).toException());
            } else {
                final LocalDate skjæringstidspunkt = utledTidligste(førsteØnskedeUttaksdagIBehandling.orElse(Tid.TIDENES_ENDE),
                    førsteUttaksdagIForrigeVedtak.orElse(Tid.TIDENES_ENDE));
                if (skjæringstidspunkt.equals(Tid.TIDENES_ENDE)) {
                    // Fant da ikke noe skjæringstidspunkt i tidligere vedtak heller.
                    throw SkjæringstidspunktFeil.FACTORY.finnerIkkeSkjæringstidspunktForForeldrepenger(behandlingId).toException();
                }
                return skjæringstidspunkt;
            }
        } else {
            if (manglerSøknadIFørstegangsbehandling(behandling)) {
                // Har ikke grunnlag for å avgjøre skjæringstidspunkt enda så gir midlertidig dagens dato. for at DTOer skal fungere.
                return førsteØnskedeUttaksdagIBehandling.orElse(FPDateUtil.iDag());
            }
            return førsteØnskedeUttaksdagIBehandling
                .orElseThrow(() -> SkjæringstidspunktFeil.FACTORY.finnerIkkeSkjæringstidspunktForForeldrepenger(behandlingId).toException());
        }
    }

    private boolean manglerSøknadIFørstegangsbehandling(Behandling behandling) {
        return BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType()) && !søknadRepository.hentSøknadHvisEksisterer(behandling.getId()).isPresent();
    }

    private Optional<LocalDate> finnFørsteØnskedeUttaksdagFor(Optional<OppgittFordelingEntitet> oppgittFordeling) {
        return finnFørsteDatoFraOppgittePerioder(oppgittFordeling
            .map(OppgittFordelingEntitet::getOppgittePerioder)
            .orElse(Collections.emptyList()));
    }

    private LocalDate utledTidligste(LocalDate første, LocalDate andre) {
        if (første.isBefore(andre)) {
            return første;
        }
        return andre;
    }

    private Behandling originalBehandling(Behandling behandling) {
        Optional<Behandling> originalBehandling = behandling.getOriginalBehandling();
        if (!originalBehandling.isPresent()) {
            throw new IllegalArgumentException("Revurdering må ha original behandling");
        }
        return originalBehandling.get();
    }

    private Optional<LocalDate> finnFørsteDatoFraOppgittePerioder(List<OppgittPeriodeEntitet> oppgittePerioder) {
        return oppgittePerioder.stream()
            .map(OppgittPeriodeEntitet::getFom)
            .min(Comparator.naturalOrder());
    }

    private Optional<LocalDate> finnFørsteDatoIUttakResultat(Behandling behandling) {
        final Optional<UttakResultatEntitet> uttakResultat = uttakRepository.hentUttakResultatHvisEksisterer(originalBehandling(behandling).getId());
        List<UttakResultatPeriodeEntitet> uttakResultatPerioder = uttakResultat.map(UttakResultatEntitet::getGjeldendePerioder)
            .map(UttakResultatPerioderEntitet::getPerioder)
            .orElse(Collections.emptyList());
        if (erAllePerioderAvslåttOgIngenAvslagPgaSøknadsfrist(uttakResultatPerioder)) {
            return uttakResultatPerioder
                .stream()
                .sorted(Comparator.comparing(UttakResultatPeriodeEntitet::getFom))
                .map(UttakResultatPeriodeEntitet::getFom)
                .findFirst();
        }
        return uttakResultatPerioder
            .stream()
            .filter(it -> it.isInnvilget() || SØKNADSFRIST.equals(it.getPeriodeResultatÅrsak()))
            .sorted(Comparator.comparing(UttakResultatPeriodeEntitet::getFom))
            .map(UttakResultatPeriodeEntitet::getFom)
            .findFirst();
    }

    private boolean erAllePerioderAvslåttOgIngenAvslagPgaSøknadsfrist(List<UttakResultatPeriodeEntitet> uttakResultatPerioder) {
        return uttakResultatPerioder.stream().allMatch(ut -> PeriodeResultatType.AVSLÅTT.equals(ut.getPeriodeResultatType())
            && !SØKNADSFRIST.equals(ut.getPeriodeResultatÅrsak()));
    }
}
