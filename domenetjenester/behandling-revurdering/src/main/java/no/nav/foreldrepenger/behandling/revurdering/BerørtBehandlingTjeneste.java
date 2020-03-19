package no.nav.foreldrepenger.behandling.revurdering;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
public class BerørtBehandlingTjeneste {

    private StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste;
    private HistorikkRepository historikkRepository;
    private UttakRepository uttakRepository;
    private UttakInputTjeneste uttakInputTjeneste;

    BerørtBehandlingTjeneste() {
        //CDI
    }

    @Inject
    public BerørtBehandlingTjeneste(StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste,
                                    BehandlingRepositoryProvider repositoryProvider,
                                    UttakInputTjeneste uttakInputTjeneste) {
        this.stønadskontoSaldoTjeneste = stønadskontoSaldoTjeneste;
        this.historikkRepository = repositoryProvider.getHistorikkRepository();
        this.uttakRepository = repositoryProvider.getUttakRepository();
        this.uttakInputTjeneste = uttakInputTjeneste;
    }

    /**
     * Finner ut om det skal opprettes en berørt behandling på med forelders sak.
     *
     * @param optionalBehandlingsresultat brukers behandlingsresultat
     * @param brukersUttaksPerioder       brukers uttaksplan.
     * @param medforeldersUttaksPerioder  medforelders uttaksplan.
     * @return true dersom berørt behandling skal opprettes, ellers false.
     */
    public boolean skalBerørtBehandlingOpprettes(Optional<Behandlingsresultat> optionalBehandlingsresultat,
                                                 Optional<UttakResultatPerioderEntitet> brukersUttaksPerioder,
                                                 Optional<UttakResultatPerioderEntitet> medforeldersUttaksPerioder) {
        if (optionalBehandlingsresultat.isEmpty()) {
            return false;
        }
        Behandlingsresultat behandlingsresultat = optionalBehandlingsresultat.get();
        var uttakInput = uttakInputTjeneste.lagInput(behandlingsresultat.getBehandlingId());
        return skalRevurderingFøreTilBerørtBehandling(behandlingsresultat, brukersUttaksPerioder, medforeldersUttaksPerioder, uttakInput);
    }

    /**
     * Finner ut om det skal opprettes en berørt behandling på med forelders sak.
     *
     * @param brukersGjeldendeBehandlingsresultat brukers behandlingsresultat
     * @param behandlingId                       brukers siste vedtatte behandling
     * @param behandlingIdAnnenPart                    medforelders siste vedtatte behandling.
     * @return true dersom berørt behandling skal opprettes, ellers false.
     */
    public boolean skalBerørtBehandlingOpprettes(Optional<Behandlingsresultat> brukersGjeldendeBehandlingsresultat,
                                                 Long behandlingId,
                                                 Long behandlingIdAnnenPart) {
        if (brukersGjeldendeBehandlingsresultat.isEmpty() || behandlingId == null) {
            return false;
        }
        var uttakInput = uttakInputTjeneste.lagInput(brukersGjeldendeBehandlingsresultat.get().getBehandlingId());
        ForeldrepengerGrunnlag foreldrepengerGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        if (foreldrepengerGrunnlag == null || foreldrepengerGrunnlag.isTapendeBehandling()) {
            return false;
        }
        Optional<UttakResultatPerioderEntitet> brukersUttaksPerioder = finnGjeldendeUttaksPerioder(behandlingId);
        Optional<UttakResultatPerioderEntitet> medforeldersUttaksPerioder = finnGjeldendeUttaksPerioder(behandlingIdAnnenPart);

        return skalBerørtBehandlingOpprettes(brukersGjeldendeBehandlingsresultat, brukersUttaksPerioder, medforeldersUttaksPerioder);
    }

    private Optional<UttakResultatPerioderEntitet> finnGjeldendeUttaksPerioder(Long behandling) {
        var uttaksplan = uttakRepository.hentUttakResultatHvisEksisterer(behandling);
        return uttaksplan.map(UttakResultatEntitet::getGjeldendePerioder);
    }

    private boolean skalRevurderingFøreTilBerørtBehandling(Behandlingsresultat behandlingsresultat,
                                                           Optional<UttakResultatPerioderEntitet> brukersUttaksPerioder,
                                                           Optional<UttakResultatPerioderEntitet> medforeldersUttaksPerioder,
                                                           UttakInput uttakInput) {
        if (behandlingsresultat.isBehandlingsresultatOpphørt() && harMedforelderPerioderEtterBrukersOpphør(brukersUttaksPerioder, medforeldersUttaksPerioder)) {
            return harKonsekvens(behandlingsresultat, KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER);
        }
        if (behandlingsresultat.isBehandlingsresultatForeldrepengerEndret()
            || behandlingsresultat.isBehandlingsresultatInnvilget()) {
            if (harKonsekvens(behandlingsresultat, KonsekvensForYtelsen.ENDRING_I_BEREGNING)
                || harKonsekvens(behandlingsresultat, KonsekvensForYtelsen.ENDRING_I_UTTAK)) {
                if (behandlingsresultat.isEndretStønadskonto()) {
                    return true;
                }
            }
            if (stønadskontoSaldoTjeneste.erNegativSaldoPåNoenKonto(uttakInput)) {
                return true;
            }
            return harOverlappendePerioder(brukersUttaksPerioder, medforeldersUttaksPerioder);
        }
        return false;
    }

    private boolean harMedforelderPerioderEtterBrukersOpphør(Optional<UttakResultatPerioderEntitet> brukersUttaksplan, Optional<UttakResultatPerioderEntitet> motpartsUttaksplan) {
        if (brukersUttaksplan.isPresent() && motpartsUttaksplan.isPresent()) {
            if (harBrukerFørsteUttak(brukersUttaksplan.get(), motpartsUttaksplan.get())) {
                return true;
            } else {
                Optional<LocalDate> medforeldrersSisteDag = sisteUttaksdatoUtenAvslåttePerioder(motpartsUttaksplan.get());
                Optional<LocalDate> brukersFørsteDag = førsteUttaksdatoUtenAvslåttePerioder(brukersUttaksplan.get());
                if (medforeldrersSisteDag.isPresent() && brukersFørsteDag.isPresent()) {
                    return !brukersFørsteDag.get().isAfter(medforeldrersSisteDag.get());
                }
            }
        }
        return false;
    }

    private boolean harBrukerFørsteUttak(UttakResultatPerioderEntitet brukersUttaksplan, UttakResultatPerioderEntitet motpartsUttaksplan) {
        return brukersUttaksplan.getPerioder().stream().map(p -> p.getTidsperiode().getFomDato()).min(LocalDate::compareTo).get()
            .isBefore(motpartsUttaksplan.getPerioder().stream().map(p -> p.getTidsperiode().getFomDato()).min(LocalDate::compareTo).get());
    }

    private boolean harOverlappendePerioder(Optional<UttakResultatPerioderEntitet> brukersUttaksplan, Optional<UttakResultatPerioderEntitet> motpartsUttaksplan) {
        if (brukersUttaksplan.isPresent() && motpartsUttaksplan.isPresent()) {
            LocalDate førsteForeldrersSisteDag = førsteForeldrersSisteDag(brukersUttaksplan.get(), motpartsUttaksplan.get());
            LocalDate andreForeldrersFørsteDag = andreForeldrersFørsteDag(brukersUttaksplan.get(), motpartsUttaksplan.get());
            if (førsteForeldrersSisteDag != null && andreForeldrersFørsteDag != null) {
                return !andreForeldrersFørsteDag.isAfter(førsteForeldrersSisteDag);
            }
        }
        return false;
    }

    private LocalDate førsteForeldrersSisteDag(UttakResultatPerioderEntitet brukersUttaksplan, UttakResultatPerioderEntitet motpartsUttaksplan) {
        Optional<LocalDate> førsteForeldrersSisteDag = brukersUttaksplan.finnFørsteUttaksdato().get().isAfter(motpartsUttaksplan.finnFørsteUttaksdato().get())
            ? sisteUttaksdatoUtenAvslåttePerioder(motpartsUttaksplan) : sisteUttaksdatoUtenAvslåttePerioder(brukersUttaksplan);
        return førsteForeldrersSisteDag.orElse(null);
    }

    private LocalDate andreForeldrersFørsteDag(UttakResultatPerioderEntitet brukersUttaksplan, UttakResultatPerioderEntitet motpartsUttaksplan) {
        Optional<LocalDate> andreForeldrersFørsteDag = brukersUttaksplan.finnFørsteUttaksdato().get().isAfter(motpartsUttaksplan.finnFørsteUttaksdato().get())
            ? førsteUttaksdatoUtenAvslåttePerioder(brukersUttaksplan) : førsteUttaksdatoUtenAvslåttePerioder(motpartsUttaksplan);
        return andreForeldrersFørsteDag.orElse(null);
    }

    private Optional<LocalDate> førsteUttaksdatoUtenAvslåttePerioder(UttakResultatPerioderEntitet uttaksplan) {
        return uttaksplan.getPerioder().stream()
            .filter(u -> !PeriodeResultatType.AVSLÅTT.equals(u.getPeriodeResultatType())).map(p -> p.getTidsperiode().getFomDato()).min(LocalDate::compareTo);
    }

    private Optional<LocalDate> sisteUttaksdatoUtenAvslåttePerioder(UttakResultatPerioderEntitet uttaksplan) {
        return uttaksplan.getPerioder().stream()
            .filter(u -> !PeriodeResultatType.AVSLÅTT.equals(u.getPeriodeResultatType())).map(p -> p.getTidsperiode().getTomDato()).max(LocalDate::compareTo);
    }

    public boolean harKonsekvens(Behandlingsresultat behandlingsresultat, KonsekvensForYtelsen konsekvens) {
        return behandlingsresultat.getKonsekvenserForYtelsen().contains(konsekvens);
    }

    public void opprettHistorikkinnslagOmRevurdering(Behandling behandling, BehandlingÅrsakType revurderingÅrsak,
                                                     HistorikkBegrunnelseType historikkBegrunnelseType, HistorikkinnslagType historikkinnslagType) {
        Historikkinnslag revurderingsInnslag = new Historikkinnslag();
        revurderingsInnslag.setBehandling(behandling);
        revurderingsInnslag.setType(historikkinnslagType);
        revurderingsInnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        HistorikkInnslagTekstBuilder historiebygger = new HistorikkInnslagTekstBuilder()
            .medHendelse(historikkinnslagType);
        if (revurderingÅrsak != null) {
            historiebygger.medBegrunnelse(revurderingÅrsak);
        } else {
            historiebygger.medBegrunnelse(historikkBegrunnelseType);
        }
        historiebygger.build(revurderingsInnslag);

        historikkRepository.lagre(revurderingsInnslag);
    }

    public void opprettHistorikkinnslagForVenteFristRelaterteInnslag(Behandling behandling, HistorikkinnslagType historikkinnslagType, LocalDateTime frist, Venteårsak venteårsak) {
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
