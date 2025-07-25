package no.nav.foreldrepenger.skjæringstidspunkt.fp;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.Optional;
import java.util.function.BiFunction;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.skjæringstidspunkt.FamilieHendelseMapper;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.MinsterettBehandling2022;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.MinsterettCore2022;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.UtsettelseCore2021;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.konfig.Tid;

@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class SkjæringstidspunktTjenesteImpl implements SkjæringstidspunktTjeneste {

    private static final Period MAX_STØNADSPERIODE = Period.ofYears(3);

    private FamilieHendelseRepository familieGrunnlagRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private FpUttakRepository fpUttakRepository;
    private OpptjeningRepository opptjeningRepository;
    private SøknadRepository søknadRepository;
    private BehandlingRepository behandlingRepository;
    private YtelseMaksdatoTjeneste ytelseMaksdatoTjeneste;
    private MinsterettBehandling2022 minsterett2022;

    SkjæringstidspunktTjenesteImpl() {
        // CDI
    }

    @Inject
    public SkjæringstidspunktTjenesteImpl(BehandlingRepositoryProvider repositoryProvider,
                                          YtelseMaksdatoTjeneste ytelseMaksdatoTjeneste,
                                          MinsterettBehandling2022 minsterett2022) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.fpUttakRepository = repositoryProvider.getFpUttakRepository();
        this.opptjeningRepository = repositoryProvider.getOpptjeningRepository();
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.familieGrunnlagRepository = repositoryProvider.getFamilieHendelseRepository();
        this.ytelseMaksdatoTjeneste = ytelseMaksdatoTjeneste;
        this.minsterett2022 = minsterett2022;
    }

    @Override
    public Skjæringstidspunkt getSkjæringstidspunkter(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var utenMinsterett = minsterett2022.utenMinsterett(behandling);
        var familieHendelseGrunnlag = familieGrunnlagRepository.hentAggregatHvisEksisterer(behandlingId);
        // Finn første uttaksdag, men bruk dagens dato dersom søknad mangler (vent på søknad, papirsøknad)
        var førsteUttaksdato = førsteUttaksdag(behandling, familieHendelseGrunnlag, utenMinsterett);

        var førsteUttaksdatoFødselsjustert = førsteDatoHensyntattTidligFødsel(behandling, familieHendelseGrunnlag, førsteUttaksdato, utenMinsterett);

        var builder = Skjæringstidspunkt.builder()
            .medUtenMinsterett(utenMinsterett)
            .medFørsteUttaksdato(førsteUttaksdato)
            .medFørsteUttaksdatoGrunnbeløp(førsteUttaksdatoFødselsjustert)
            .medFørsteUttaksdatoSøknad(førsteUttaksdato);
        return ferdigstillSkjæringstidspunkt(familieHendelseGrunnlag, builder, behandling, utenMinsterett, førsteUttaksdato, this::utledYtelseintervall);
    }

    @Override
    public Skjæringstidspunkt getSkjæringstidspunkterForAvsluttetBehandling(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var utenMinsterett = minsterett2022.utenMinsterett(behandling);

        var familieHendelseGrunnlag = familieGrunnlagRepository.hentAggregatHvisEksisterer(behandlingId);
        var førsteUttaksdato = finnFørsteDatoIUttakResultat(behandlingId)
            .or(() -> Optional.ofNullable(førsteUttaksdag(behandling, familieHendelseGrunnlag, utenMinsterett)))
            .orElse(null);
        var førsteUttaksdatoFødselsjustert = førsteDatoHensyntattTidligFødsel(behandling, familieHendelseGrunnlag, førsteUttaksdato, utenMinsterett);

        var builder = Skjæringstidspunkt.builder()
            .medUtenMinsterett(utenMinsterett)
            .medFørsteUttaksdato(førsteUttaksdato)
            .medFørsteUttaksdatoGrunnbeløp(førsteUttaksdatoFødselsjustert)
            .medFørsteUttaksdatoSøknad(førsteUttaksdato);
        return ferdigstillSkjæringstidspunkt(familieHendelseGrunnlag, builder, behandling, utenMinsterett, førsteUttaksdato, this::utledYtelseintervallAvsluttetBehandling);
    }

    private Skjæringstidspunkt ferdigstillSkjæringstidspunkt(Optional<FamilieHendelseGrunnlagEntitet> familieHendelseGrunnlag,
                                                             Skjæringstidspunkt.Builder builder,
                                                             Behandling behandling,
                                                             boolean utenMinsterett,
                                                             LocalDate førsteUttaksdato,
                                                             BiFunction<Behandling, LocalDateInterval, LocalDateInterval> intervallUtleder) {
        familieHendelseGrunnlag.map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseMapper::mapTilFamilieHendelseDato)
            .ifPresent(builder::medFamilieHendelseDato);
        hentYtelseFordelingAggregatFor(behandling.getId()).map(YtelseFordelingAggregat::getGjeldendeFordeling)
            .map(OppgittFordelingEntitet::ønskerJustertVedFødsel)
            .map(valg -> valg && !utenMinsterett)
            .ifPresent(builder::medUttakSkalJusteresTilFødselsdato);

        var skjæringstidspunktOpptjening = getFastsattSkjæringstidspunkt(behandling.getId());
        if (skjæringstidspunktOpptjening != null) {
            return builder.medSkjæringstidspunktOpptjening(skjæringstidspunktOpptjening)
                .medUtledetSkjæringstidspunkt(skjæringstidspunktOpptjening)
                .medUttaksintervall(intervallUtleder.apply(behandling, maxstønadsperiode(skjæringstidspunktOpptjening, familieHendelseGrunnlag)))
                .medKreverSammenhengendeUttak(UtsettelseCore2021.kreverSammenhengendeUttak(skjæringstidspunktOpptjening))
                .build();
        } else if (førsteUttaksdato != null) {
            Optional<LocalDate> morsMaksdato = UtsettelseCore2021.kreverSammenhengendeUttak(familieHendelseGrunnlag.orElse(null)) ?
                ytelseMaksdatoTjeneste.beregnMorsMaksdato(behandling.getSaksnummer(), behandling.getFagsak().getRelasjonsRolleType())
                    .filter(UtsettelseCore2021::kreverSammenhengendeUttakMorsMaxdato) : Optional.empty();
            var utledetSkjæringstidspunkt = SkjæringstidspunktUtils.utledSkjæringstidspunktFraBehandling(behandling, førsteUttaksdato,
                familieHendelseGrunnlag, morsMaksdato, utenMinsterett);

            return builder.medUtledetSkjæringstidspunkt(utledetSkjæringstidspunkt)
                .medUttaksintervall(intervallUtleder.apply(behandling, maxstønadsperiode(utledetSkjæringstidspunkt, familieHendelseGrunnlag)))
                .medKreverSammenhengendeUttak(UtsettelseCore2021.kreverSammenhengendeUttak(utledetSkjæringstidspunkt))
                .build();
        } else {
            throw finnerIkkeStpException(behandling.getId());
        }
    }

    private LocalDate getFastsattSkjæringstidspunkt(Long behandlingId) {
        return opptjeningRepository.finnOpptjening(behandlingId)
            .filter(Opptjening::erOpptjeningPeriodeVilkårOppfylt)
            .map(Opptjening::getTom)
            .map(d -> d.plusDays(1))
            .orElse(null);
    }

    private LocalDate førsteUttaksdag(Behandling behandling, Optional<FamilieHendelseGrunnlagEntitet> fhGrunnlag, boolean utenMinsterett) {
        var ytelseFordelingAggregat = hentYtelseFordelingAggregatFor(behandling.getId());

        var avklartStartDato = ytelseFordelingAggregat.flatMap(YtelseFordelingAggregat::getAvklarteDatoer)
            .map(AvklarteUttakDatoerEntitet::getFørsteUttaksdato);

        var førsteUttaksdato = avklartStartDato
            .or(() -> stpForFlyttbareFedreOgMedmødre(behandling, ytelseFordelingAggregat, fhGrunnlag, utenMinsterett))
            .orElseGet(() -> førsteØnskedeUttaksdag(behandling, ytelseFordelingAggregat));
        if (førsteUttaksdato != null) {
            return førsteUttaksdato;
        } else if (fhGrunnlag.isPresent()) {
            return MinsterettCore2022.førsteUttaksDatoForBeregning(behandling.getRelasjonsRolleType(), fhGrunnlag.get(), førsteUttaksdato, utenMinsterett);
        } else if (manglerSøknadIFørstegangsbehandling(behandling)) {
            return LocalDate.now();
        } else {
            throw finnerIkkeStpException(behandling.getId());
        }
    }

    private Optional<LocalDate> stpForFlyttbareFedreOgMedmødre(Behandling behandling, Optional<YtelseFordelingAggregat> aggregat, Optional<FamilieHendelseGrunnlagEntitet> fhGrunnlag, boolean utenMinsterett) {
        if (utenMinsterett || RelasjonsRolleType.MORA.equals(behandling.getRelasjonsRolleType()) || aggregat.isEmpty() ||
            !aggregat.get().getGjeldendeFordeling().ønskerJustertVedFødsel() || fhGrunnlag.isEmpty()) {
            return Optional.empty();
        }
        return fhGrunnlag.map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon).flatMap(FamilieHendelseEntitet::getFødselsdato).map(VirkedagUtil::fomVirkedag);
    }

    private Optional<YtelseFordelingAggregat> hentYtelseFordelingAggregatFor(Long behandlingId) {
        return ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId);
    }

    private LocalDate førsteØnskedeUttaksdag(Behandling behandling, Optional<YtelseFordelingAggregat> ytelseFordelingAggregat) {
        var oppgittFordeling = ytelseFordelingAggregat.map(YtelseFordelingAggregat::getOppgittFordeling);

        var førsteØnskedeUttaksdagIBehandling = UtsettelseCore2021.finnFørsteDatoFraSøknad(oppgittFordeling);

        if (behandling.erRevurdering()) {
            var originalBehandlingId = originalBehandling(behandling);
            var originalBehandling = behandlingRepository.hentBehandling(originalBehandlingId);
            // Forutsetning: at man ikke oppretter revurdering uten søknad (manuell/im) på sak uten innvilget uttaksperioder.
            var førsteUttaksdagIForrigeVedtak = finnFørsteDatoIUttakResultat(originalBehandlingId);
            if (førsteUttaksdagIForrigeVedtak.isEmpty() && førsteØnskedeUttaksdagIBehandling.isEmpty()) {
                // Rekursjon til forrige behandling
                var fastsattSkjæringstidspunkt = getFastsattSkjæringstidspunkt(originalBehandlingId);
                return fastsattSkjæringstidspunkt != null ? fastsattSkjæringstidspunkt :
                    førsteØnskedeUttaksdag(originalBehandling, hentYtelseFordelingAggregatFor(originalBehandlingId));
            } else {
                // Sjekk utsettelse av startdato og returner da første uttaksdato i ny søknad - eller tidligste dato
                return getUtsattStartdato(førsteUttaksdagIForrigeVedtak, oppgittFordeling).orElseGet(
                    () -> utledTidligste(førsteØnskedeUttaksdagIBehandling.orElse(Tid.TIDENES_ENDE),
                        førsteUttaksdagIForrigeVedtak.orElse(Tid.TIDENES_ENDE)));
            }
        } else if (BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType())) {
            if (manglerSøknadIFørstegangsbehandling(behandling)) {
                // Har ikke grunnlag for å avgjøre skjæringstidspunkt enda - heller ikke familiehendelse
                return førsteØnskedeUttaksdagIBehandling.orElse(null);
            } else {
                return førsteØnskedeUttaksdagIBehandling
                    .or(() -> unntaksTilfellerFriUtsettelse(behandling))
                    .or(() -> Optional.ofNullable(getFastsattSkjæringstidspunkt(behandling.getId())))
                    .orElse(null);
            }
        } else {
            throw new IllegalArgumentException("Ikke gyldig behandlingstype: " + behandling.getType());
        }
    }

    /*
     * Håndtering av søknader med kun utsettelse - fyll på med tilfelle
     * - Mor / fødsel - da vil STP være gitt av fødselsdato og første uttaksdato er seneste fødselsdato med mindre utsettelse sykdom/innleggelse/skade
     */
    private Optional<LocalDate> unntaksTilfellerFriUtsettelse(Behandling behandling) {
        if (RelasjonsRolleType.MORA.equals(behandling.getRelasjonsRolleType())) {
            return familieGrunnlagRepository.hentAggregatHvisEksisterer(behandling.getId())
                .map(g -> UtsettelseCore2021.førsteUttaksDatoForBeregning(RelasjonsRolleType.MORA, g, null));
        }
        return Optional.empty();
    }

    private TekniskException finnerIkkeStpException(Long behandlingId) {
        return new TekniskException("FP-931232",
            "Finner ikke skjæringstidspunkt for foreldrepenger som forventet for behandling=" + behandlingId);
    }

    private LocalDateInterval utledYtelseintervall(Behandling behandling, LocalDateInterval maxstønadsperiode) {
        var sistedato = sisteØnskedeUttaksdag(behandling, hentYtelseFordelingAggregatFor(behandling.getId()), maxstønadsperiode.getFomDato());
        var bruktomdato = sistedato.isAfter(maxstønadsperiode.getTomDato().minusDays(1)) ?
            maxstønadsperiode.getTomDato().minusDays(1) : sistedato;
        return new LocalDateInterval(maxstønadsperiode.getFomDato(), bruktomdato.isAfter(maxstønadsperiode.getFomDato()) ? bruktomdato : maxstønadsperiode.getFomDato());
    }

    private LocalDateInterval utledYtelseintervallAvsluttetBehandling(Behandling behandling, LocalDateInterval maxstønadsperiode) {
        var sistedato = finnSisteDatoIUttakResultat(behandling.getId())
            .orElseGet(() -> maxstønadsperiode.getTomDato().minusDays(1));
        var bruktomdato = sistedato.isAfter(maxstønadsperiode.getTomDato().minusDays(1)) ?
            maxstønadsperiode.getTomDato().minusDays(1) : sistedato;
        return new LocalDateInterval(maxstønadsperiode.getFomDato(), bruktomdato.isAfter(maxstønadsperiode.getFomDato()) ? bruktomdato : maxstønadsperiode.getFomDato());
    }

    private LocalDateInterval maxstønadsperiode(LocalDate skjæringstidspunkt, Optional<FamilieHendelseGrunnlagEntitet> familieHendelseGrunnlag) {
        var max = familieHendelseGrunnlag.map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getSkjæringstidspunkt)
            .orElse(skjæringstidspunkt)
            .plus(MAX_STØNADSPERIODE);
        var fom = skjæringstidspunkt.isBefore(max) ? skjæringstidspunkt : max;
        var tom = max.isAfter(skjæringstidspunkt) ? max : skjæringstidspunkt;
        return new LocalDateInterval(fom, tom);
    }

    private LocalDate sisteØnskedeUttaksdag(Behandling behandling, Optional<YtelseFordelingAggregat> ytelseFordelingAggregat,
                                            LocalDate skjæringsTidspunkt) {
        var oppgittFordeling = ytelseFordelingAggregat.map(YtelseFordelingAggregat::getOppgittFordeling);

        var sisteØnskedeUttaksdagIBehandling = UtsettelseCore2021.finnSisteDatoFraSøknad(oppgittFordeling);

        if (behandling.erRevurdering()) {
            var sisteUttaksdagIForrigeVedtak = finnSisteDatoIUttakResultat(originalBehandling(behandling));
            if (sisteUttaksdagIForrigeVedtak.isEmpty() && sisteØnskedeUttaksdagIBehandling.isEmpty()) {
                var originalBehandling = behandlingRepository.hentBehandling(behandling.getOriginalBehandlingId().orElseThrow());
                var forrigeØnsket = sisteØnskedeUttaksdag(originalBehandling, hentYtelseFordelingAggregatFor(originalBehandling.getId()), skjæringsTidspunkt);
                return utledSeneste(forrigeØnsket, skjæringsTidspunkt);
            } else {
                return utledSeneste(sisteØnskedeUttaksdagIBehandling.orElse(Tid.TIDENES_BEGYNNELSE),
                    sisteUttaksdagIForrigeVedtak.orElse(Tid.TIDENES_BEGYNNELSE));
            }
        } else {
            return sisteØnskedeUttaksdagIBehandling.orElse(skjæringsTidspunkt);
        }
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

    private Optional<LocalDate> finnFørsteDatoIUttakResultat(Long behandlingId) {
        var uttakResultatPerioder = fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingId)
            .map(UttakResultatEntitet::getGjeldendePerioder)
            .map(UttakResultatPerioderEntitet::getPerioder)
            .orElse(Collections.emptyList());
        return UtsettelseCore2021.finnFørsteDatoFraUttakResultat(uttakResultatPerioder);
    }

    private Optional<LocalDate> finnSisteDatoIUttakResultat(Long behandlingId) {
        var uttakResultatPerioder = fpUttakRepository.hentUttakResultatHvisEksisterer(behandlingId)
            .map(UttakResultatEntitet::getGjeldendePerioder)
            .map(UttakResultatPerioderEntitet::getPerioder)
            .orElse(Collections.emptyList());
        return UtsettelseCore2021.finnSisteDatoFraUttakResultat(uttakResultatPerioder);
    }

    private LocalDate førsteDatoHensyntattTidligFødsel(Behandling behandling, Optional<FamilieHendelseGrunnlagEntitet> grunnlag,
                                                       LocalDate førsteUttaksdato, boolean utenMinsterett) {
        if (førsteUttaksdato == null) {
            return null;
        }
        return grunnlag.map(g -> MinsterettCore2022.førsteUttaksDatoForBeregning(behandling.getRelasjonsRolleType(), g, førsteUttaksdato, utenMinsterett))
            .orElse(førsteUttaksdato);
    }

    private static Optional<LocalDate> getUtsattStartdato(Optional<LocalDate> førsteUttaksdagIForrigeVedtak,
                                                          Optional<OppgittFordelingEntitet> oppgittFordeling) {
        var førsteSøkteUttaksdag = UtsettelseCore2021.finnFørsteDatoFraSøknad(oppgittFordeling);
        var førsteSøkteUtsettelsedag = UtsettelseCore2021.finnFørsteUtsettelseDatoFraSøknad(oppgittFordeling);
        if (førsteUttaksdagIForrigeVedtak.isPresent() && !UtsettelseCore2021.kreverSammenhengendeUttak(førsteUttaksdagIForrigeVedtak.get()) &&
            førsteSøkteUttaksdag.isPresent() && førsteSøkteUtsettelsedag.isPresent() &&
            !førsteSøkteUtsettelsedag.get().isAfter(førsteUttaksdagIForrigeVedtak.get())) {
            return førsteSøkteUttaksdag;
        }
        return Optional.empty();
    }
}
