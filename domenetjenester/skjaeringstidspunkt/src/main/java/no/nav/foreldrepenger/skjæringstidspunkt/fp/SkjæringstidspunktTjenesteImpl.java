package no.nav.foreldrepenger.skjæringstidspunkt.fp;

import java.time.LocalDate;
import java.time.Period;
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
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktRegisterinnhentingTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.MinsterettBehandling2022;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.UtsettelseBehandling2021;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.UtsettelseCore2021;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.konfig.Tid;

@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class SkjæringstidspunktTjenesteImpl implements SkjæringstidspunktTjeneste , SkjæringstidspunktRegisterinnhentingTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(SkjæringstidspunktTjenesteImpl.class);

    private static final Period MAX_STØNADSPERIODE = Period.ofYears(3);

    private FamilieHendelseRepository familieGrunnlagRepository;
    private SkjæringstidspunktUtils utlederUtils;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private FpUttakRepository fpUttakRepository;
    private OpptjeningRepository opptjeningRepository;
    private SøknadRepository søknadRepository;
    private BehandlingRepository behandlingRepository;
    private YtelseMaksdatoTjeneste ytelseMaksdatoTjeneste;
    private UtsettelseBehandling2021 utsettelse2021;
    private MinsterettBehandling2022 minsterett2022;

    SkjæringstidspunktTjenesteImpl() {
        // CDI
    }

    @Inject
    public SkjæringstidspunktTjenesteImpl(BehandlingRepositoryProvider repositoryProvider,
                                          YtelseMaksdatoTjeneste ytelseMaksdatoTjeneste,
                                          SkjæringstidspunktUtils utlederUtils,
                                          UtsettelseBehandling2021 utsettelse2021,
                                          MinsterettBehandling2022 minsterett2022) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.fpUttakRepository = repositoryProvider.getFpUttakRepository();
        this.opptjeningRepository = repositoryProvider.getOpptjeningRepository();
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.familieGrunnlagRepository = repositoryProvider.getFamilieHendelseRepository();
        this.utlederUtils = utlederUtils;
        this.ytelseMaksdatoTjeneste = ytelseMaksdatoTjeneste;
        this.utsettelse2021 = utsettelse2021;
        this.minsterett2022 = minsterett2022;
    }

    @Override
    public LocalDate utledSkjæringstidspunktForRegisterInnhenting(Long behandlingId) {
        final var familieHendelseAggregat = familieGrunnlagRepository.hentAggregatHvisEksisterer(behandlingId);

        return familieHendelseAggregat.map(utlederUtils::utledSkjæringstidspunktRegisterinnhenting).orElse(LocalDate.now());
    }

    @Override
    public Skjæringstidspunkt getSkjæringstidspunkter(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var sammenhengendeUttak = utsettelse2021.kreverSammenhengendeUttak(behandling);
        var utenMinsterett = minsterett2022.utenMinsterett(behandling);
        var førsteUttaksdatoOpt = Optional.ofNullable(førsteUttaksdag(behandling, sammenhengendeUttak));
        var førsteUttaksdato = førsteUttaksdatoOpt.orElseGet(LocalDate::now); // Mangler grunnlag for å angi dato, bruker midlertidig dagens dato pga Dtos etc.
        var familieHendelseGrunnlag = familieGrunnlagRepository.hentAggregatHvisEksisterer(behandlingId);
        var førsteUttaksdatoFødselsjustert = førsteDatoHensyntattTidligFødsel(behandling, familieHendelseGrunnlag, førsteUttaksdato);
        var gjelderFødsel = familieHendelseGrunnlag.map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getGjelderFødsel).orElse(true);

        var builder = Skjæringstidspunkt.builder()
            .medKreverSammenhengendeUttak(sammenhengendeUttak)
            .medUtenMinsterett(utenMinsterett)
            .medFørsteUttaksdato(førsteUttaksdato)
            .medFørsteUttaksdatoGrunnbeløp(førsteUttaksdatoFødselsjustert)
            .medFørsteUttaksdatoSøknad(førsteUttaksdatoOpt.orElse(null))
            .medGjelderFødsel(gjelderFødsel);

        var opptjening = opptjeningRepository.finnOpptjening(behandlingId);
        if (opptjening.filter(Opptjening::erOpptjeningPeriodeVilkårOppfylt).isPresent()) {
            var skjæringstidspunktOpptjening = opptjening.get().getTom().plusDays(1);
            return builder.medSkjæringstidspunktOpptjening(skjæringstidspunktOpptjening)
                .medUtledetSkjæringstidspunkt(skjæringstidspunktOpptjening)
                .medUtledetMedlemsintervall(utledYtelseintervall(behandling, skjæringstidspunktOpptjening, sammenhengendeUttak))
                .build();
        }

        Optional<LocalDate> morsMaksDato = !sammenhengendeUttak ? Optional.empty() :
            ytelseMaksdatoTjeneste.beregnMorsMaksdato(behandling.getFagsak().getSaksnummer(), behandling.getFagsak().getRelasjonsRolleType());
        var utledetSkjæringstidspunkt = utlederUtils.utledSkjæringstidspunktFraBehandling(behandling, førsteUttaksdato,
            familieHendelseGrunnlag, morsMaksDato, utenMinsterett);

        return builder.medUtledetSkjæringstidspunkt(utledetSkjæringstidspunkt)
            .medUtledetMedlemsintervall(utledYtelseintervall(behandling, utledetSkjæringstidspunkt, sammenhengendeUttak))
            .build();
    }

    private LocalDate førsteUttaksdag(Behandling behandling, boolean kreverSammenhengendeUttak) {
        final var ytelseFordelingAggregat = hentYtelseFordelingAggregatFor(behandling.getId());

        final var avklartStartDato = ytelseFordelingAggregat.flatMap(YtelseFordelingAggregat::getAvklarteDatoer)
            .map(AvklarteUttakDatoerEntitet::getFørsteUttaksdato);

        return avklartStartDato.orElseGet(() -> førsteØnskedeUttaksdag(behandling, ytelseFordelingAggregat, kreverSammenhengendeUttak));
    }

    private Optional<YtelseFordelingAggregat> hentYtelseFordelingAggregatFor(Long behandlingId) {
        return ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId);
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
            // Sjekk utsettelse av startdato og returner da første uttaksdato i ny søknad
            var utsattStartdato = getUtsattStartdato(kreverSammenhengendeUttak, førsteUttaksdagIForrigeVedtak, oppgittFordeling);
            if (utsattStartdato.isPresent()) {
                return utsattStartdato.get();
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
            // Har ikke grunnlag for å avgjøre skjæringstidspunkt enda
            return førsteØnskedeUttaksdagIBehandling.orElse(null);
        }
        return førsteØnskedeUttaksdagIBehandling
            .or(() -> unntaksTilfellerFriUtsettelse(behandling))
            .orElseThrow(() -> finnerIkkeStpException(behandling.getId()));
    }

    /*
     * Håndtering av søknader med kun utsettelse - fyll på med tilfelle
     * - Mor / fødsel - da vil STP være gitt av fødselsdato og første uttaksdato er seneste fødselsdato med mindre utsettelse sykdom/innleggelse/skade
     */
    private Optional<LocalDate> unntaksTilfellerFriUtsettelse(Behandling behandling) {
        if (RelasjonsRolleType.MORA.equals(behandling.getRelasjonsRolleType())) {
            return familieGrunnlagRepository.hentAggregatHvisEksisterer(behandling.getId())
                .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
                .filter(FamilieHendelseEntitet::getGjelderFødsel)
                .map(FamilieHendelseEntitet::getSkjæringstidspunkt);
        }
        return Optional.empty();
    }

    private TekniskException finnerIkkeStpException(Long behandlingId) {
        return new TekniskException("FP-931232",
            "Finner ikke skjæringstidspunkt for foreldrepenger som forventet for behandling=" + behandlingId);
    }

    private LocalDateInterval utledYtelseintervall(Behandling behandling, LocalDate skjæringsTidspunkt, boolean kreverSammenhengendeUttak) {
        var sistedato = sisteØnskedeUttaksdag(behandling, hentYtelseFordelingAggregatFor(behandling.getId()), skjæringsTidspunkt, kreverSammenhengendeUttak);
        var bruktomdato = sistedato.isAfter(skjæringsTidspunkt.plus(MAX_STØNADSPERIODE).minusDays(1)) ?
            skjæringsTidspunkt.plus(MAX_STØNADSPERIODE).minusDays(1) : sistedato;
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

    private Long originalBehandling(Behandling behandling) {
        return behandling.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalArgumentException("Revurdering må ha original behandling"));
    }

    private Optional<LocalDate> finnFørsteDatoIUttakResultat(Behandling behandling, boolean kreverSammenhengendeUttak) {
        var uttakResultatPerioder = fpUttakRepository.hentUttakResultatHvisEksisterer(originalBehandling(behandling))
            .map(UttakResultatEntitet::getGjeldendePerioder)
            .map(UttakResultatPerioderEntitet::getPerioder)
            .orElse(Collections.emptyList());
        return UtsettelseCore2021.finnFørsteDatoFraUttakResultat(uttakResultatPerioder, kreverSammenhengendeUttak);
    }

    private Optional<LocalDate> finnSisteDatoIUttakResultat(Behandling behandling, boolean kreverSammenhengendeUttak) {
        var uttakResultatPerioder = fpUttakRepository.hentUttakResultatHvisEksisterer(originalBehandling(behandling))
            .map(UttakResultatEntitet::getGjeldendePerioder)
            .map(UttakResultatPerioderEntitet::getPerioder)
            .orElse(Collections.emptyList());
        return UtsettelseCore2021.finnSisteDatoFraUttakResultat(uttakResultatPerioder, kreverSammenhengendeUttak);
    }

    private LocalDate førsteDatoHensyntattTidligFødsel(Behandling behandling, Optional<FamilieHendelseGrunnlagEntitet> grunnlag, LocalDate førsteUttaksdato) {
        return grunnlag.map(g -> UtsettelseCore2021.førsteUttaksDatoForBeregning(behandling.getRelasjonsRolleType(), g, førsteUttaksdato))
            .orElse(førsteUttaksdato);
    }

    private static Optional<LocalDate> getUtsattStartdato(boolean kreverSammenhengendeUttak, Optional<LocalDate> førsteUttaksdagIForrigeVedtak,
                                                          Optional<OppgittFordelingEntitet> oppgittFordeling) {
        final var førsteSøkteUttaksdag = UtsettelseCore2021.finnFørsteDatoFraSøknad(oppgittFordeling, kreverSammenhengendeUttak);
        final var førsteSøkteUtsettelsedag = UtsettelseCore2021.finnFørsteUtsettelseDatoFraSøknad(oppgittFordeling, kreverSammenhengendeUttak);
        if (!kreverSammenhengendeUttak && førsteUttaksdagIForrigeVedtak.isPresent()
            && førsteSøkteUttaksdag.isPresent() && førsteSøkteUtsettelsedag.isPresent() &&
            !førsteSøkteUtsettelsedag.get().isAfter(førsteUttaksdagIForrigeVedtak.get())) {
            return førsteSøkteUttaksdag;
        }
        return Optional.empty();
    }
}
