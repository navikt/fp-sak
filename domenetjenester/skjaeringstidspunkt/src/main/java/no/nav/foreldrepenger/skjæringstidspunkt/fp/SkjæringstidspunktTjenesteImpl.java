package no.nav.foreldrepenger.skjæringstidspunkt.fp;

import static no.nav.foreldrepenger.behandlingslager.uttak.fp.IkkeOppfyltÅrsak.SØKNADSFRIST;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.YtelseMaksdatoTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktRegisterinnhentingTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.UtsettelseCore2021;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.konfig.Tid;
import no.nav.vedtak.util.env.Environment;

@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class SkjæringstidspunktTjenesteImpl implements SkjæringstidspunktTjeneste , SkjæringstidspunktRegisterinnhentingTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(SkjæringstidspunktTjenesteImpl.class);
    private static final boolean ER_PROD = Environment.current().isProd();

    private FamilieHendelseRepository familieGrunnlagRepository;
    private SkjæringstidspunktUtils utlederUtils;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private FpUttakRepository fpUttakRepository;
    private OpptjeningRepository opptjeningRepository;
    private SøknadRepository søknadRepository;
    private BehandlingRepository behandlingRepository;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private YtelseMaksdatoTjeneste ytelseMaksdatoTjeneste;
    private UtsettelseCore2021 utsettelse2021;

    SkjæringstidspunktTjenesteImpl() {
        // CDI
    }

    @Inject
    public SkjæringstidspunktTjenesteImpl(BehandlingRepositoryProvider repositoryProvider,
                                          YtelseMaksdatoTjeneste ytelseMaksdatoTjeneste,
                                          SkjæringstidspunktUtils utlederUtils,
                                          UtsettelseCore2021 utsettelse2021) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.fpUttakRepository = repositoryProvider.getFpUttakRepository();
        this.opptjeningRepository = repositoryProvider.getOpptjeningRepository();
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.familieGrunnlagRepository = repositoryProvider.getFamilieHendelseRepository();
        this.utlederUtils = utlederUtils;
        this.ytelseMaksdatoTjeneste = ytelseMaksdatoTjeneste;
        this.utsettelse2021 = utsettelse2021;
    }

    @Override
    public LocalDate utledSkjæringstidspunktForRegisterInnhenting(Long behandlingId) {
        final var familieHendelseAggregat = familieGrunnlagRepository.hentAggregatHvisEksisterer(behandlingId);

        return familieHendelseAggregat.map(utlederUtils::utledSkjæringstidspunktRegisterinnhenting).orElse(LocalDate.now());
    }

    @Override
    public Skjæringstidspunkt getSkjæringstidspunkter(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var førsteUttaksdato = førsteUttaksdag(behandling);
        var førsteInnvilgetUttaksdato = førsteDatoHensyntattTidligFødsel(behandling, førsteInnvilgetUttaksdag(behandling));
        var sammenhengendeUttak = kreverSammenhengendeUttak(behandling);

        var builder = Skjæringstidspunkt.builder()
            .medKreverSammenhengendeUttak(sammenhengendeUttak)
            .medFørsteUttaksdato(førsteUttaksdato)
            .medFørsteUttaksdatoFødseljustert(førsteDatoHensyntattTidligFødsel(behandling, førsteUttaksdato))
            .medFørsteUttaksdatoGrunnbeløp(førsteInnvilgetUttaksdato);

        var opptjening = opptjeningRepository.finnOpptjening(behandlingId);
        if (opptjening.map(Opptjening::erOpptjeningPeriodeVilkårOppfylt).orElse(Boolean.FALSE)) {
            var skjæringstidspunktOpptjening = opptjening.get().getTom().plusDays(1);
            return builder.medSkjæringstidspunktOpptjening(skjæringstidspunktOpptjening)
                .medUtledetSkjæringstidspunkt(skjæringstidspunktOpptjening)
                .medUtledetMedlemsintervall(utledYtelseintervall(behandling, skjæringstidspunktOpptjening))
                .build();
        }

        var familieHendelseGrunnlag = familieGrunnlagRepository.hentAggregatHvisEksisterer(behandlingId);
        var morsMaksDato = ytelseMaksdatoTjeneste.beregnMorsMaksdato(behandling.getFagsak().getSaksnummer(), behandling.getFagsak().getRelasjonsRolleType());
        var utledetSkjæringstidspunkt = utlederUtils.utledSkjæringstidspunktFraBehandling(behandling, førsteUttaksdato, familieHendelseGrunnlag, morsMaksDato);

        return builder.medUtledetSkjæringstidspunkt(utledetSkjæringstidspunkt)
            .medUtledetMedlemsintervall(utledYtelseintervall(behandling, utledetSkjæringstidspunkt))
            .build();
    }

    private LocalDate førsteUttaksdag(Behandling behandling) {
        final var ytelseFordelingAggregat = hentYtelseFordelingAggregatFor(behandling);

        final var avklartStartDato = ytelseFordelingAggregat.flatMap(YtelseFordelingAggregat::getAvklarteDatoer)
            .map(AvklarteUttakDatoerEntitet::getFørsteUttaksdato);

        return avklartStartDato.orElseGet(() -> førsteØnskedeUttaksdag(behandling, ytelseFordelingAggregat, true));
    }

    private LocalDate førsteInnvilgetUttaksdag(Behandling behandling) {
        final var ytelseFordelingAggregat = hentYtelseFordelingAggregatFor(behandling);

        final var avklartStartDato = ytelseFordelingAggregat.flatMap(YtelseFordelingAggregat::getAvklarteDatoer)
            .map(AvklarteUttakDatoerEntitet::getFørsteUttaksdato);

        return avklartStartDato.orElseGet(() -> førsteØnskedeUttaksdag(behandling, ytelseFordelingAggregat, false));
    }

    private Optional<YtelseFordelingAggregat> hentYtelseFordelingAggregatFor(Behandling behandling) {
        return ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandling.getId());
    }

    private LocalDate førsteØnskedeUttaksdag(Behandling behandling, Optional<YtelseFordelingAggregat> ytelseFordelingAggregat, boolean medAvslåttSøknadsfrist) {
        var oppgittFordeling = ytelseFordelingAggregat.map(YtelseFordelingAggregat::getOppgittFordeling);

        final var førsteØnskedeUttaksdagIBehandling = finnFørsteØnskedeUttaksdagFor(oppgittFordeling);

        if (behandling.erRevurdering()) {
            final var førsteUttaksdagIForrigeVedtak = finnFørsteDatoIUttakResultat(behandling, medAvslåttSøknadsfrist);
            if (førsteUttaksdagIForrigeVedtak.isEmpty() && førsteØnskedeUttaksdagIBehandling.isEmpty()) {
                var ytelseFordelingForOriginalBehandling = hentYtelseFordelingAggregatFor(originalBehandling(behandling));
                return finnFørsteØnskedeUttaksdagFor(ytelseFordelingForOriginalBehandling.map(YtelseFordelingAggregat::getOppgittFordeling))
                    .or(() -> finnFørsteØnskedeUttaksdagFor(ytelseFordelingForOriginalBehandling.map(YtelseFordelingAggregat::getGjeldendeSøknadsperioder)))
                    .orElseThrow(() -> finnerIkkeStpException(behandling.getId()));
            }
            final var skjæringstidspunkt = utledTidligste(førsteØnskedeUttaksdagIBehandling.orElse(Tid.TIDENES_ENDE),
                førsteUttaksdagIForrigeVedtak.orElse(Tid.TIDENES_ENDE));
            if (skjæringstidspunkt.equals(Tid.TIDENES_ENDE)) {
                // Fant da ikke noe skjæringstidspunkt i tidligere vedtak heller.
                throw finnerIkkeStpException(behandling.getId());
            }
            return skjæringstidspunkt;
        }
        if (manglerSøknadIFørstegangsbehandling(behandling)) {
            // Har ikke grunnlag for å avgjøre skjæringstidspunkt enda så gir midlertidig dagens dato. for at DTOer skal fungere.
            return førsteØnskedeUttaksdagIBehandling.orElse(LocalDate.now());
        }
        return førsteØnskedeUttaksdagIBehandling.orElseThrow(() -> finnerIkkeStpException(behandling.getId()));
    }

    private TekniskException finnerIkkeStpException(Long behandlingId) {
        return new TekniskException("FP-931232",
            "Finner ikke skjæringstidspunkt for foreldrepenger som forventet for behandling=" + behandlingId);
    }

    private LocalDateInterval utledYtelseintervall(Behandling behandling, LocalDate skjæringsTidspunkt) {
        var sistedato = sisteØnskedeUttaksdag(behandling, hentYtelseFordelingAggregatFor(behandling), skjæringsTidspunkt);
        var bruktomdato = sistedato.isAfter(skjæringsTidspunkt.plusYears(3)) ? skjæringsTidspunkt.plusYears(3) : sistedato;
        return new LocalDateInterval(skjæringsTidspunkt, bruktomdato.isAfter(skjæringsTidspunkt) ? bruktomdato : skjæringsTidspunkt);
    }

    private LocalDate sisteØnskedeUttaksdag(Behandling behandling, Optional<YtelseFordelingAggregat> ytelseFordelingAggregat, LocalDate skjæringsTidspunkt) {
        var oppgittFordeling = ytelseFordelingAggregat.map(YtelseFordelingAggregat::getOppgittFordeling);

        final var sisteØnskedeUttaksdagIBehandling = finnSisteØnskedeUttaksdagFor(oppgittFordeling);

        if (behandling.erRevurdering()) {
            final var sisteUttaksdagIForrigeVedtak = finnSisteDatoIUttakResultat(behandling);
            if (sisteUttaksdagIForrigeVedtak.isEmpty() && sisteØnskedeUttaksdagIBehandling.isEmpty()) {
                var ytelseFordelingForOriginalBehandling = hentYtelseFordelingAggregatFor(originalBehandling(behandling));
                return finnSisteØnskedeUttaksdagFor(ytelseFordelingForOriginalBehandling.map(YtelseFordelingAggregat::getOppgittFordeling))
                    .or(() -> finnSisteØnskedeUttaksdagFor(ytelseFordelingForOriginalBehandling.map(YtelseFordelingAggregat::getGjeldendeSøknadsperioder)))
                    .orElse(skjæringsTidspunkt);
            }
            final var sistedato = utledSeneste(sisteØnskedeUttaksdagIBehandling.orElse(Tid.TIDENES_BEGYNNELSE),
                sisteUttaksdagIForrigeVedtak.orElse(Tid.TIDENES_BEGYNNELSE));
            return sistedato.equals(Tid.TIDENES_BEGYNNELSE) ? skjæringsTidspunkt : sistedato;
        }
        return sisteØnskedeUttaksdagIBehandling.orElse(skjæringsTidspunkt);
    }

    private boolean manglerSøknadIFørstegangsbehandling(Behandling behandling) {
        return BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType()) && !søknadRepository.hentSøknadHvisEksisterer(behandling.getId()).isPresent();
    }

    private Optional<LocalDate> finnFørsteØnskedeUttaksdagFor(Optional<OppgittFordelingEntitet> oppgittFordeling) {
        return oppgittFordeling.map(OppgittFordelingEntitet::getOppgittePerioder).orElse(Collections.emptyList()).stream()
            .map(OppgittPeriodeEntitet::getFom)
            .min(Comparator.naturalOrder());
    }

    private Optional<LocalDate> finnSisteØnskedeUttaksdagFor(Optional<OppgittFordelingEntitet> oppgittFordeling) {
        return oppgittFordeling.map(OppgittFordelingEntitet::getOppgittePerioder).orElse(Collections.emptyList()).stream()
            .map(OppgittPeriodeEntitet::getTom)
            .max(Comparator.naturalOrder());
    }

    private LocalDate utledTidligste(LocalDate første, LocalDate andre) {
        return første.isBefore(andre) ? første :  andre;
    }

    private LocalDate utledSeneste(LocalDate første, LocalDate andre) {
        return første.isAfter(andre) ? første :  andre;
    }

    private Behandling originalBehandling(Behandling behandling) {
        var originalBehandlingId = behandling.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalArgumentException("Revurdering må ha original behandling"));
        return behandlingRepository.hentBehandling(originalBehandlingId);
    }

    private Optional<LocalDate> finnFørsteDatoIUttakResultat(Behandling behandling, boolean medAvslåttSøknadsfrist) {
        var uttakResultatPerioder = fpUttakRepository.hentUttakResultatHvisEksisterer(originalBehandling(behandling).getId())
            .map(UttakResultatEntitet::getGjeldendePerioder)
            .map(UttakResultatPerioderEntitet::getPerioder)
            .orElse(Collections.emptyList());
        if (erAllePerioderAvslåttOgIngenAvslagPgaSøknadsfrist(uttakResultatPerioder)) {
            return uttakResultatPerioder
                .stream()
                .map(UttakResultatPeriodeEntitet::getFom)
                .min(Comparator.naturalOrder());
        }
        return uttakResultatPerioder
            .stream()
            .filter(it -> it.isInnvilget() || (medAvslåttSøknadsfrist && SØKNADSFRIST.equals(it.getResultatÅrsak())))
            .map(UttakResultatPeriodeEntitet::getFom)
            .min(Comparator.naturalOrder());
    }

    private Optional<LocalDate> finnSisteDatoIUttakResultat(Behandling behandling) {
        return fpUttakRepository.hentUttakResultatHvisEksisterer(originalBehandling(behandling).getId())
            .map(UttakResultatEntitet::getGjeldendePerioder)
            .map(UttakResultatPerioderEntitet::getPerioder)
            .orElse(Collections.emptyList())
            .stream()
            .filter(it -> it.isInnvilget() || SØKNADSFRIST.equals(it.getResultatÅrsak()))
            .map(UttakResultatPeriodeEntitet::getTom)
            .max(Comparator.naturalOrder());
    }

    private boolean erAllePerioderAvslåttOgIngenAvslagPgaSøknadsfrist(List<UttakResultatPeriodeEntitet> uttakResultatPerioder) {
        return uttakResultatPerioder.stream().allMatch(ut -> PeriodeResultatType.AVSLÅTT.equals(ut.getResultatType())
            && !SØKNADSFRIST.equals(ut.getResultatÅrsak()));
    }

    private LocalDate førsteDatoHensyntattTidligFødsel(Behandling behandling, LocalDate førsteUttaksdato) {
        // FAR/MEDMOR skal ikke vurderes
        if (RelasjonsRolleType.MORA.equals(behandling.getFagsak().getRelasjonsRolleType())) {
            // Uttak begynner virkedag på/etter fødsel dersom fødsel inntreffer før søkt uttak.
            var registrertFødselsDato = familieGrunnlagRepository.hentAggregatHvisEksisterer(behandling.getId())
                .flatMap(FamilieHendelseGrunnlagEntitet::getGjeldendeBekreftetVersjon)
                .flatMap(FamilieHendelseEntitet::getFødselsdato)
                .map(VirkedagUtil::fomVirkedag);
            return registrertFødselsDato.filter(førsteUttaksdato::isAfter).orElse(førsteUttaksdato);
        } else {
            return førsteUttaksdato;
        }
    }

    private boolean kreverSammenhengendeUttak(Behandling behandling) {
        var sammenhengendeUttak = familieGrunnlagRepository.hentAggregatHvisEksisterer(behandling.getId())
            .or(() -> finnFHSistVedtatteBehandlingKobletFagsak(behandling))
            .map(utsettelse2021::kreverSammenhengendeUttak).orElse(UtsettelseCore2021.DEFAULT_KREVER_SAMMENHENGENDE_UTTAK);
        if (!sammenhengendeUttak && ER_PROD) {
            LOG.error("Prod uten krav om sammenhengende periode - sjekk om korrekt, saksnummer {}", behandling.getFagsak().getSaksnummer());
        } else if (!sammenhengendeUttak) {
            LOG.info("Non-prod uten krav om sammenhengende periode, saksnummer {} behandling {}", behandling.getFagsak().getSaksnummer(), behandling.getId());
        }
        return sammenhengendeUttak;
    }

    private Optional<FamilieHendelseGrunnlagEntitet> finnFHSistVedtatteBehandlingKobletFagsak(Behandling behandling) {
        return fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(behandling.getFagsak())
            .flatMap(fr -> fr.getRelatertFagsak(behandling.getFagsak()))
            .flatMap(f -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(f.getId()))
            .flatMap(b -> familieGrunnlagRepository.hentAggregatHvisEksisterer(b.getId()));
    }
}
