package no.nav.foreldrepenger.behandling.revurdering;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkBegrunnelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
public class BerørtBehandlingTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(BerørtBehandlingTjeneste.class);

    private StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste;
    private HistorikkRepository historikkRepository;
    private ForeldrepengerUttakTjeneste uttakTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;

    @Inject
    public BerørtBehandlingTjeneste(StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste,
                                    BehandlingRepositoryProvider repositoryProvider,
                                    UttakInputTjeneste uttakInputTjeneste,
                                    ForeldrepengerUttakTjeneste uttakTjeneste,
                                    YtelseFordelingTjeneste ytelseFordelingTjeneste) {
        this.stønadskontoSaldoTjeneste = stønadskontoSaldoTjeneste;
        this.historikkRepository = repositoryProvider.getHistorikkRepository();
        this.uttakTjeneste = uttakTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
    }

    BerørtBehandlingTjeneste() {
        // CDI
    }

    /**
     * Finner ut om det skal opprettes en berørt behandling på med forelders sak.
     *
     * @param brukersGjeldendeBehandlingsresultat brukers behandlingsresultat
     * @param behandlingId                        brukers siste vedtatte behandling
     * @param behandlingIdAnnenPart               medforelders siste vedtatte
     *                                            behandling.
     * @return true dersom berørt behandling skal opprettes, ellers false.
     */
    public boolean skalBerørtBehandlingOpprettes(Optional<Behandlingsresultat> brukersGjeldendeBehandlingsresultat,
                                                 Long behandlingId,
                                                 Long behandlingIdAnnenPart) {
        var gammel = gammel(brukersGjeldendeBehandlingsresultat, behandlingId, behandlingIdAnnenPart);
        try {
            var ny = skalBerørtBehandlingOpprettesNy(brukersGjeldendeBehandlingsresultat.orElseThrow(), behandlingId,
                behandlingIdAnnenPart);
            if (gammel != ny) {
                LOG.info("Ny utregning av berørt behandling gammel:{}, ny:{}", gammel, ny);
            }

        } catch (Throwable t) {
            LOG.info("Ny utregning av berørt behandling feilet", t);
        }
        return gammel;
    }

    boolean skalBerørtBehandlingOpprettesNy(Behandlingsresultat behandlingsresultat,
                                            Long behandlingId,
                                            Long behandlingIdAnnenpart) {
        var uttakInput = uttakInputTjeneste.lagInput(behandlingsresultat.getBehandlingId());
        ForeldrepengerGrunnlag foreldrepengerGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        //Må sjekke konsekvens pga overlapp med samtidig uttak
        if (foreldrepengerGrunnlag.isTapendeBehandling() || harKonsekvens(behandlingsresultat, KonsekvensForYtelsen.INGEN_ENDRING)) {
            return false;
        }

        if (behandlingsresultat.isEndretStønadskonto() || stønadskontoSaldoTjeneste.erNegativSaldoPåNoenKonto(uttakInput)) {
            return true;
        }

        var annenpartsUttak = hentUttak(behandlingIdAnnenpart);
        if (annenpartsUttak.isEmpty()) {
            return false;
        }

        var brukersUttak = hentUttak(behandlingId);
        if (brukersUttak.isEmpty()) {
            return false;
        }

        var endringsdato = ytelseFordelingTjeneste.hentAggregat(behandlingId).getGjeldendeEndringsdato();
        if (harOverlappendeAktivtUttakEtterEndringsdato(brukersUttak.get(), annenpartsUttak.get(), endringsdato)) {
            return true;
        }

        return hullIFellesUttaksplanEtterEndringsdato(brukersUttak.get(), annenpartsUttak.get(), endringsdato);
    }

    private boolean harOverlappendeAktivtUttakEtterEndringsdato(ForeldrepengerUttak brukersUttak,
                                                                ForeldrepengerUttak annenpartsUttak,
                                                                LocalDate endringsdato) {
        //Går ut i fra at brukers uttak alltid er splittet opp (har knekkpunkt) på endringsdato
        var brukersUttakEtterEndringsdato = uttakEtterDato(brukersUttak, endringsdato);
        return harOverlappIAktivtUttak(brukersUttakEtterEndringsdato, annenpartsUttak.getGjeldendePerioder());
    }

    private List<ForeldrepengerUttakPeriode> uttakEtterDato(ForeldrepengerUttak uttak, LocalDate dato) {
        return uttak.getGjeldendePerioder()
            .stream()
            .filter(periode -> !periode.getTidsperiode().getFomDato().isBefore(dato))
            .collect(Collectors.toList());
    }

    private boolean harOverlappIAktivtUttak(List<ForeldrepengerUttakPeriode> brukersUttak,
                                            List<ForeldrepengerUttakPeriode> annenpartsUttak) {
        return brukersUttak.stream().anyMatch(periode -> overlappendePeriode(periode, annenpartsUttak).isPresent());
    }

    private Optional<ForeldrepengerUttakPeriode> overlappendePeriode(ForeldrepengerUttakPeriode periode,
                                                                     List<ForeldrepengerUttakPeriode> perioder) {
        return perioder.stream()
            .filter(periode2 -> periode.getTidsperiode().overlaps(periode2.getTidsperiode()) && periode.harAktivtUttak()
                && periode2.harAktivtUttak())
            .findFirst();
    }

    private boolean hullIFellesUttaksplanEtterEndringsdato(ForeldrepengerUttak brukersUttak,
                                                           ForeldrepengerUttak annenpartsUttak,
                                                           LocalDate endringsdato) {
        var brukersUttakEtterEndringsdato = uttakEtterDato(brukersUttak, endringsdato);
        return brukersUttakEtterEndringsdato.stream().anyMatch(p -> lagerHullIFellespUttaksplan(p, annenpartsUttak));
    }

    private boolean lagerHullIFellespUttaksplan(ForeldrepengerUttakPeriode periode,
                                                ForeldrepengerUttak annenpartsUttak) {
        if (periode.harAktivtUttak()) {
            return false;
        }
        if (!harUttakEtterDato(periode.getTom(), annenpartsUttak)) {
            return false;
        }
        var annenpartsOverlappendePeriode = overlappendePeriode(periode, annenpartsUttak.getGjeldendePerioder());
        return annenpartsOverlappendePeriode.isEmpty() || !annenpartsOverlappendePeriode.get().harAktivtUttak();
    }

    private boolean harUttakEtterDato(LocalDate dato, ForeldrepengerUttak uttak) {
        return uttak.getGjeldendePerioder().stream().anyMatch(p -> p.getTom().isAfter(dato));
    }

    private boolean gammel(Optional<Behandlingsresultat> brukersGjeldendeBehandlingsresultat,
                           Long behandlingId,
                           Long behandlingIdAnnenPart) {
        if (brukersGjeldendeBehandlingsresultat.isEmpty() || (behandlingId == null)) {
            return false;
        }
        var uttakInput = uttakInputTjeneste.lagInput(brukersGjeldendeBehandlingsresultat.get().getBehandlingId());
        ForeldrepengerGrunnlag foreldrepengerGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        if ((foreldrepengerGrunnlag == null) || foreldrepengerGrunnlag.isTapendeBehandling()) {
            return false;
        }
        var brukersUttaksPerioder = hentUttak(behandlingId);
        var medforeldersUttaksPerioder = hentUttak(behandlingIdAnnenPart);

        return skalBerørtBehandlingOpprettes(brukersGjeldendeBehandlingsresultat, brukersUttaksPerioder,
            medforeldersUttaksPerioder);
    }

    /**
     * Finner ut om det skal opprettes en berørt behandling på med forelders sak.
     *
     * @param optionalBehandlingsresultat brukers behandlingsresultat
     * @param brukersUttaksPerioder       brukers uttaksplan.
     * @param medforeldersUttaksPerioder  medforelders uttaksplan.
     * @return true dersom berørt behandling skal opprettes, ellers false.
     */
    boolean skalBerørtBehandlingOpprettes(Optional<Behandlingsresultat> optionalBehandlingsresultat,
                                          Optional<ForeldrepengerUttak> brukersUttaksPerioder,
                                          Optional<ForeldrepengerUttak> medforeldersUttaksPerioder) {
        if (optionalBehandlingsresultat.isEmpty()) {
            return false;
        }
        Behandlingsresultat behandlingsresultat = optionalBehandlingsresultat.get();
        var uttakInput = uttakInputTjeneste.lagInput(behandlingsresultat.getBehandlingId());
        return skalRevurderingFøreTilBerørtBehandling(behandlingsresultat, brukersUttaksPerioder,
            medforeldersUttaksPerioder, uttakInput);
    }

    private Optional<ForeldrepengerUttak> hentUttak(Long behandling) {
        return uttakTjeneste.hentUttakHvisEksisterer(behandling);
    }

    private boolean skalRevurderingFøreTilBerørtBehandling(Behandlingsresultat behandlingsresultat,
                                                           Optional<ForeldrepengerUttak> brukersUttaksPerioder,
                                                           Optional<ForeldrepengerUttak> medforeldersUttaksPerioder,
                                                           UttakInput uttakInput) {
        if (behandlingsresultat.isBehandlingsresultatOpphørt() && harMedforelderPerioderEtterBrukersOpphør(
            brukersUttaksPerioder, medforeldersUttaksPerioder)) {
            var harKonsekvens = harKonsekvens(behandlingsresultat, KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER);
            if (harKonsekvens) {
                log("OPPHØR");
            }
            return harKonsekvens;
        }
        if (behandlingsresultat.isBehandlingsresultatForeldrepengerEndret()
            || behandlingsresultat.isBehandlingsresultatInnvilget()) {
            if (harKonsekvens(behandlingsresultat, KonsekvensForYtelsen.ENDRING_I_BEREGNING) || harKonsekvens(
                behandlingsresultat, KonsekvensForYtelsen.ENDRING_I_UTTAK)) {
                if (behandlingsresultat.isEndretStønadskonto()) {
                    log("ENDRET_KONTO");
                    return true;
                }
            }
            if (stønadskontoSaldoTjeneste.erNegativSaldoPåNoenKonto(uttakInput)) {
                log("NEGATIV_KONTO");
                return true;
            }
            var harOverlappendePerioder = harOverlappendePerioder(brukersUttaksPerioder, medforeldersUttaksPerioder);
            if (harOverlappendePerioder) {
                log("OVERLAPP_UTTAK");
            }
            return harOverlappendePerioder;
        }
        return false;
    }

    private void log(String årsak) {
        LOG.info("Berørt behandling opprettes pga {}", årsak);
    }

    private boolean harMedforelderPerioderEtterBrukersOpphør(Optional<ForeldrepengerUttak> brukersUttaksplan,
                                                             Optional<ForeldrepengerUttak> motpartsUttaksplan) {
        if (brukersUttaksplan.isPresent() && motpartsUttaksplan.isPresent()) {
            if (harBrukerFørsteUttak(brukersUttaksplan.get(), motpartsUttaksplan.get())) {
                return true;
            }
            Optional<LocalDate> medforeldrersSisteDag = sisteUttaksdato(motpartsUttaksplan.get());
            Optional<LocalDate> brukersFørsteDag = førsteUttaksdatoUtenAvslåttePerioder(brukersUttaksplan.get());
            if (medforeldrersSisteDag.isPresent() && brukersFørsteDag.isPresent()) {
                return !brukersFørsteDag.get().isAfter(medforeldrersSisteDag.get());
            }
        }
        return false;
    }

    private boolean harBrukerFørsteUttak(ForeldrepengerUttak brukersUttaksplan,
                                         ForeldrepengerUttak motpartsUttaksplan) {
        return brukersUttaksplan.getGjeldendePerioder()
            .stream()
            .map(p -> p.getFom())
            .min(LocalDate::compareTo)
            .get()
            .isBefore(motpartsUttaksplan.getGjeldendePerioder()
                .stream()
                .map(p -> p.getFom())
                .min(LocalDate::compareTo)
                .get());
    }

    private boolean harOverlappendePerioder(Optional<ForeldrepengerUttak> brukersUttaksplan,
                                            Optional<ForeldrepengerUttak> motpartsUttaksplan) {
        if (brukersUttaksplan.isPresent() && motpartsUttaksplan.isPresent()) {
            LocalDate førsteForeldrersSisteDag = førsteForeldrersSisteDag(brukersUttaksplan.get(),
                motpartsUttaksplan.get());
            LocalDate andreForeldrersFørsteDag = andreForeldrersFørsteDag(brukersUttaksplan.get(),
                motpartsUttaksplan.get());
            if ((førsteForeldrersSisteDag != null) && (andreForeldrersFørsteDag != null)) {
                return !andreForeldrersFørsteDag.isAfter(førsteForeldrersSisteDag);
            }
        }
        return false;
    }

    private LocalDate førsteForeldrersSisteDag(ForeldrepengerUttak brukersUttaksplan,
                                               ForeldrepengerUttak motpartsUttaksplan) {
        Optional<LocalDate> førsteForeldrersSisteDag = brukersUttaksplan.finnFørsteUttaksdato()
            .isAfter(motpartsUttaksplan.finnFørsteUttaksdato()) ? sisteUttaksdato(motpartsUttaksplan) : sisteUttaksdato(
            brukersUttaksplan);
        return førsteForeldrersSisteDag.orElse(null);
    }

    private LocalDate andreForeldrersFørsteDag(ForeldrepengerUttak brukersUttaksplan,
                                               ForeldrepengerUttak motpartsUttaksplan) {
        Optional<LocalDate> andreForeldrersFørsteDag = brukersUttaksplan.finnFørsteUttaksdato()
            .isAfter(motpartsUttaksplan.finnFørsteUttaksdato()) ? førsteUttaksdatoUtenAvslåttePerioder(
            brukersUttaksplan) : førsteUttaksdatoUtenAvslåttePerioder(motpartsUttaksplan);
        return andreForeldrersFørsteDag.orElse(null);
    }

    private Optional<LocalDate> førsteUttaksdatoUtenAvslåttePerioder(ForeldrepengerUttak uttaksplan) {
        return uttaksplan.getGjeldendePerioder()
            .stream()
            .filter(u -> !PeriodeResultatType.AVSLÅTT.equals(u.getResultatType()))
            .map(p -> p.getFom())
            .min(LocalDate::compareTo);
    }

    private Optional<LocalDate> sisteUttaksdato(ForeldrepengerUttak uttaksplan) {
        return uttaksplan.getGjeldendePerioder()
            .stream()
            .filter(
                periode -> PeriodeResultatType.INNVILGET.equals(periode.getResultatType()) || periode.getAktiviteter()
                    .stream()
                    .anyMatch(a -> a.getTrekkdager().merEnn0()))
            .map(p -> p.getTidsperiode().getTomDato())
            .max(LocalDate::compareTo);
    }

    public boolean harKonsekvens(Behandlingsresultat behandlingsresultat, KonsekvensForYtelsen konsekvens) {
        return behandlingsresultat.getKonsekvenserForYtelsen().contains(konsekvens);
    }

    public void opprettHistorikkinnslagOmRevurdering(Behandling behandling,
                                                     BehandlingÅrsakType revurderingÅrsak,
                                                     HistorikkBegrunnelseType historikkBegrunnelseType,
                                                     HistorikkinnslagType historikkinnslagType) {
        Historikkinnslag revurderingsInnslag = new Historikkinnslag();
        revurderingsInnslag.setBehandling(behandling);
        revurderingsInnslag.setType(historikkinnslagType);
        revurderingsInnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        HistorikkInnslagTekstBuilder historiebygger = new HistorikkInnslagTekstBuilder().medHendelse(
            historikkinnslagType);
        if (revurderingÅrsak != null) {
            historiebygger.medBegrunnelse(revurderingÅrsak);
        } else {
            historiebygger.medBegrunnelse(historikkBegrunnelseType);
        }
        historiebygger.build(revurderingsInnslag);

        historikkRepository.lagre(revurderingsInnslag);
    }

    public void opprettHistorikkinnslagForVenteFristRelaterteInnslag(Behandling behandling,
                                                                     HistorikkinnslagType historikkinnslagType,
                                                                     LocalDateTime frist,
                                                                     Venteårsak venteårsak) {
        HistorikkInnslagTekstBuilder builder = new HistorikkInnslagTekstBuilder();
        if (frist != null) {
            builder.medHendelse(historikkinnslagType, frist.toLocalDate());
        } else {
            builder.medHendelse(historikkinnslagType);
        }
        if (!Venteårsak.UDEFINERT.equals(venteårsak)) {
            builder.medÅrsak(venteårsak);
        }
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        historikkinnslag.setType(historikkinnslagType);
        historikkinnslag.setBehandlingId(behandling.getId());
        historikkinnslag.setFagsakId(behandling.getFagsakId());
        builder.build(historikkinnslag);
        historikkRepository.lagre(historikkinnslag);
    }
}
