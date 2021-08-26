package no.nav.foreldrepenger.skjæringstidspunkt.fp;

import java.time.LocalDate;
import java.util.Collections;
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
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktRegisterinnhentingTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.UtsettelseCore2021;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.konfig.Tid;

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
        if (Environment.current().isProd() && behandlingId == 1944710) {
            // Midl spesialhåndtering pga annen bug
            var idag = LocalDate.now();
            return Skjæringstidspunkt.builder()
                .medKreverSammenhengendeUttak(true)
                .medFørsteUttaksdato(idag)
                .medFørsteUttaksdatoGrunnbeløp(idag)
                .medUtledetSkjæringstidspunkt(idag)
                .medSkjæringstidspunktOpptjening(idag)
                .medSkjæringstidspunktBeregning(idag)
                .medUtledetMedlemsintervall(new LocalDateInterval(idag, idag))
                .build();
        }
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var sammenhengendeUttak = kreverSammenhengendeUttak(behandling);
        var førsteUttaksdato = førsteUttaksdag(behandling, sammenhengendeUttak);
        var førsteUttaksdatoFødselsjustert = førsteDatoHensyntattTidligFødsel(behandling, førsteUttaksdato);

        var builder = Skjæringstidspunkt.builder()
            .medKreverSammenhengendeUttak(sammenhengendeUttak)
            .medFørsteUttaksdato(førsteUttaksdato)
            .medFørsteUttaksdatoGrunnbeløp(førsteUttaksdatoFødselsjustert);

        var opptjening = opptjeningRepository.finnOpptjening(behandlingId);
        if (opptjening.filter(Opptjening::erOpptjeningPeriodeVilkårOppfylt).isPresent()) {
            var skjæringstidspunktOpptjening = opptjening.get().getTom().plusDays(1);
            return builder.medSkjæringstidspunktOpptjening(skjæringstidspunktOpptjening)
                .medUtledetSkjæringstidspunkt(skjæringstidspunktOpptjening)
                .medUtledetMedlemsintervall(utledYtelseintervall(behandling, skjæringstidspunktOpptjening, sammenhengendeUttak))
                .build();
        }

        var familieHendelseGrunnlag = familieGrunnlagRepository.hentAggregatHvisEksisterer(behandlingId);
        Optional<LocalDate> morsMaksDato = !sammenhengendeUttak ? Optional.empty() :
            ytelseMaksdatoTjeneste.beregnMorsMaksdato(behandling.getFagsak().getSaksnummer(), behandling.getFagsak().getRelasjonsRolleType());
        var utledetSkjæringstidspunkt = utlederUtils.utledSkjæringstidspunktFraBehandling(behandling, førsteUttaksdato,
            familieHendelseGrunnlag, morsMaksDato);

        return builder.medUtledetSkjæringstidspunkt(utledetSkjæringstidspunkt)
            .medUtledetMedlemsintervall(utledYtelseintervall(behandling, utledetSkjæringstidspunkt, sammenhengendeUttak))
            .build();
    }

    private LocalDate førsteUttaksdag(Behandling behandling, boolean kreverSammenhengendeUttak) {
        final var ytelseFordelingAggregat = hentYtelseFordelingAggregatFor(behandling);

        final var avklartStartDato = ytelseFordelingAggregat.flatMap(YtelseFordelingAggregat::getAvklarteDatoer)
            .map(AvklarteUttakDatoerEntitet::getFørsteUttaksdato);

        return avklartStartDato.orElseGet(() -> førsteØnskedeUttaksdag(behandling, ytelseFordelingAggregat, kreverSammenhengendeUttak));
    }

    private Optional<YtelseFordelingAggregat> hentYtelseFordelingAggregatFor(Behandling behandling) {
        return ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandling.getId());
    }

    private LocalDate førsteØnskedeUttaksdag(Behandling behandling, Optional<YtelseFordelingAggregat> ytelseFordelingAggregat, boolean kreverSammenhengendeUttak) {
        var oppgittFordeling = ytelseFordelingAggregat.map(YtelseFordelingAggregat::getOppgittFordeling);

        final var førsteØnskedeUttaksdagIBehandling = UtsettelseCore2021.finnFørsteDatoFraSøknad(oppgittFordeling, kreverSammenhengendeUttak);

        if (behandling.erRevurdering()) {
            // Forutsetning: at man ikke oppretter revurdering uten søknad (manuell/im) på sak uten innvilget uttaksperioder.
            final var førsteUttaksdagIForrigeVedtak = finnFørsteDatoIUttakResultat(behandling, kreverSammenhengendeUttak);
            if (førsteUttaksdagIForrigeVedtak.isEmpty() && førsteØnskedeUttaksdagIBehandling.isEmpty()) {
                var ytelseFordelingForOriginalBehandling = hentYtelseFordelingAggregatFor(originalBehandling(behandling));
                return UtsettelseCore2021.finnFørsteDatoFraSøknad(ytelseFordelingForOriginalBehandling.map(YtelseFordelingAggregat::getOppgittFordeling), kreverSammenhengendeUttak)
                    .or(() -> UtsettelseCore2021.finnFørsteDatoFraSøknad(ytelseFordelingForOriginalBehandling.map(YtelseFordelingAggregat::getGjeldendeSøknadsperioder), kreverSammenhengendeUttak))
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

    private LocalDateInterval utledYtelseintervall(Behandling behandling, LocalDate skjæringsTidspunkt, boolean kreverSammenhengendeUttak) {
        var sistedato = sisteØnskedeUttaksdag(behandling, hentYtelseFordelingAggregatFor(behandling), skjæringsTidspunkt, kreverSammenhengendeUttak);
        var bruktomdato = sistedato.isAfter(skjæringsTidspunkt.plusYears(3)) ? skjæringsTidspunkt.plusYears(3) : sistedato;
        return new LocalDateInterval(skjæringsTidspunkt, bruktomdato.isAfter(skjæringsTidspunkt) ? bruktomdato : skjæringsTidspunkt);
    }

    private LocalDate sisteØnskedeUttaksdag(Behandling behandling, Optional<YtelseFordelingAggregat> ytelseFordelingAggregat,
                                            LocalDate skjæringsTidspunkt, boolean kreverSammenhengendeUttak) {
        var oppgittFordeling = ytelseFordelingAggregat.map(YtelseFordelingAggregat::getOppgittFordeling);

        final var sisteØnskedeUttaksdagIBehandling = UtsettelseCore2021.finnSisteDatoFraSøknad(oppgittFordeling, kreverSammenhengendeUttak);

        if (behandling.erRevurdering()) {
            final var sisteUttaksdagIForrigeVedtak = finnSisteDatoIUttakResultat(behandling, kreverSammenhengendeUttak);
            if (sisteUttaksdagIForrigeVedtak.isEmpty() && sisteØnskedeUttaksdagIBehandling.isEmpty()) {
                var ytelseFordelingForOriginalBehandling = hentYtelseFordelingAggregatFor(originalBehandling(behandling));
                return UtsettelseCore2021.finnSisteDatoFraSøknad(ytelseFordelingForOriginalBehandling.map(YtelseFordelingAggregat::getOppgittFordeling), kreverSammenhengendeUttak)
                    .or(() -> UtsettelseCore2021.finnSisteDatoFraSøknad(ytelseFordelingForOriginalBehandling.map(YtelseFordelingAggregat::getGjeldendeSøknadsperioder), kreverSammenhengendeUttak))
                    .orElse(skjæringsTidspunkt);
            }
            final var sistedato = utledSeneste(sisteØnskedeUttaksdagIBehandling.orElse(Tid.TIDENES_BEGYNNELSE),
                sisteUttaksdagIForrigeVedtak.orElse(Tid.TIDENES_BEGYNNELSE));
            return sistedato.equals(Tid.TIDENES_BEGYNNELSE) ? skjæringsTidspunkt : sistedato;
        }
        return sisteØnskedeUttaksdagIBehandling.orElse(skjæringsTidspunkt);
    }

    private boolean manglerSøknadIFørstegangsbehandling(Behandling behandling) {
        return BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType()) && søknadRepository.hentSøknadHvisEksisterer(behandling.getId()).isEmpty();
    }

    private static LocalDate utledTidligste(LocalDate første, LocalDate andre) {
        return første.isBefore(andre) ? første :  andre;
    }

    private static LocalDate utledSeneste(LocalDate første, LocalDate andre) {
        return første.isAfter(andre) ? første :  andre;
    }

    private Behandling originalBehandling(Behandling behandling) {
        var originalBehandlingId = behandling.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalArgumentException("Revurdering må ha original behandling"));
        return behandlingRepository.hentBehandling(originalBehandlingId);
    }

    private Optional<LocalDate> finnFørsteDatoIUttakResultat(Behandling behandling, boolean kreverSammenhengendeUttak) {
        var uttakResultatPerioder = fpUttakRepository.hentUttakResultatHvisEksisterer(originalBehandling(behandling).getId())
            .map(UttakResultatEntitet::getGjeldendePerioder)
            .map(UttakResultatPerioderEntitet::getPerioder)
            .orElse(Collections.emptyList());
        return UtsettelseCore2021.finnFørsteDatoFraUttakResultat(uttakResultatPerioder, kreverSammenhengendeUttak);
    }

    private Optional<LocalDate> finnSisteDatoIUttakResultat(Behandling behandling, boolean kreverSammenhengendeUttak) {
        var uttakResultatPerioder = fpUttakRepository.hentUttakResultatHvisEksisterer(originalBehandling(behandling).getId())
            .map(UttakResultatEntitet::getGjeldendePerioder)
            .map(UttakResultatPerioderEntitet::getPerioder)
            .orElse(Collections.emptyList());
        return UtsettelseCore2021.finnSisteDatoFraUttakResultat(uttakResultatPerioder, kreverSammenhengendeUttak);
    }

    private LocalDate førsteDatoHensyntattTidligFødsel(Behandling behandling, LocalDate førsteUttaksdato) {
        var grunnlag = familieGrunnlagRepository.hentAggregatHvisEksisterer(behandling.getId());
        return grunnlag.map(g -> UtsettelseCore2021.førsteUttaksDatoForBeregning(behandling.getRelasjonsRolleType(), g, førsteUttaksdato))
            .orElse(førsteUttaksdato);
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
