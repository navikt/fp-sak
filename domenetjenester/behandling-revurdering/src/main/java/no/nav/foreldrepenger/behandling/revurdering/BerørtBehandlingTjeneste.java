package no.nav.foreldrepenger.behandling.revurdering;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.UtregnetStønadskontoTjeneste;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.EndringsdatoBerørtUtleder;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

@ApplicationScoped
public class BerørtBehandlingTjeneste {

    private StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste;
    private ForeldrepengerUttakTjeneste uttakTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;

    @Inject
    public BerørtBehandlingTjeneste(StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste,
                                    UttakInputTjeneste uttakInputTjeneste,
                                    ForeldrepengerUttakTjeneste uttakTjeneste,
                                    YtelseFordelingTjeneste ytelseFordelingTjeneste) {
        this.stønadskontoSaldoTjeneste = stønadskontoSaldoTjeneste;
        this.uttakTjeneste = uttakTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
    }

    BerørtBehandlingTjeneste() {
        // CDI
    }

    public enum BerørtÅrsak { ORDINÆR, KONTO_REDUSERT, OPPHØR, FERIEPENGER }

    /**
     * Finner ut om det skal opprettes en berørt behandling på med forelders sak.
     *
     * @param vedtattBehandlingsresultat brukers behandlingsresultat
     * @param vedtattBehandling                          brukers siste vedtatte behandling
     * @param sistVedtattBehandlingIdAnnenPart               medforelders siste vedtatte
     *                                            behandling.
     * @return true dersom berørt behandling skal opprettes, ellers false.
     */
    public Optional<BerørtÅrsak> skalBerørtBehandlingOpprettes(Behandlingsresultat vedtattBehandlingsresultat,
                                                               Behandling vedtattBehandling,
                                                               Long sistVedtattBehandlingIdAnnenPart) {
        //Må sjekke konsekvens pga overlapp med samtidig uttak
        if (ikkeAktuellForVurderBerørt(vedtattBehandling, vedtattBehandlingsresultat)) {
            return Optional.empty();
        }
        var uttakInput = uttakInputTjeneste.lagInput(vedtattBehandling.getId());

        var vedtattUttak = hentUttak(vedtattBehandling.getId()).orElse(tomtUttak());
        var annenpartsSistVedtatteUttak = hentUttak(sistVedtattBehandlingIdAnnenPart);
        var vedtattYfa = ytelseFordelingTjeneste.hentAggregat(vedtattBehandling.getId());
        if (annenpartsSistVedtatteUttak.isEmpty() || finnMinAktivDato(annenpartsSistVedtatteUttak.get()).isEmpty() || finnMinAktivDato(vedtattUttak,
            annenpartsSistVedtatteUttak.get()).isEmpty() || !harLikDekningsgrad(sistVedtattBehandlingIdAnnenPart, vedtattYfa)) { //Skal opprette egen dekningsgrad revurdering hvis ulik dekningsgrad
            return Optional.empty();
        }

        return EndringsdatoBerørtUtleder.utledEndringsdatoForBerørtBehandling(vedtattUttak, vedtattYfa,
            stønadskontoSaldoTjeneste.erNegativSaldoPåNoenKonto(uttakInput),
            annenpartsSistVedtatteUttak,
            uttakInput,
            "Skal opprette berørt").isPresent() ? Optional.of(utledÅrsak(vedtattUttak, annenpartsSistVedtatteUttak.get())) : Optional.empty();
    }

    private boolean harLikDekningsgrad(Long sistVedtattBehandlingIdAnnenPart, YtelseFordelingAggregat vedtattYfa) {
        return Objects.equals(vedtattYfa.getGjeldendeDekningsgrad(),
            ytelseFordelingTjeneste.hentAggregat(sistVedtattBehandlingIdAnnenPart).getGjeldendeDekningsgrad());
    }

    private BerørtÅrsak utledÅrsak(ForeldrepengerUttak brukersUttak,
                                   ForeldrepengerUttak annenpartsUttak) {
        var harEndretStrukturEllerRedusertAntallStønadsdager = UtregnetStønadskontoTjeneste
            .harEndretStrukturEllerRedusertAntallStønadsdager(annenpartsUttak.getStønadskontoBeregning(), brukersUttak.getStønadskontoBeregning());
        return harEndretStrukturEllerRedusertAntallStønadsdager ? BerørtÅrsak.KONTO_REDUSERT : BerørtÅrsak.ORDINÆR;
    }

    private boolean ikkeAktuellForVurderBerørt(Behandling behandling, Behandlingsresultat behandlingsresultat) {
        // Vurder å inkludere
        return SpesialBehandling.erBerørtBehandling(behandling) && SpesialBehandling.skalUttakVurderes(behandling)
            || behandlingsresultat.isBehandlingHenlagt() || harKonsekvens(behandlingsresultat, KonsekvensForYtelsen.INGEN_ENDRING);
    }

    private boolean isAktivtUttak(ForeldrepengerUttakPeriode p) {
        return p.harAktivtUttak() || p.isInnvilgetOpphold();
    }


    private Optional<LocalDate> finnMinAktivDato(ForeldrepengerUttak uttak) {
        return uttak.getGjeldendePerioder().stream()
            .filter(this::isAktivtUttak)
            .map(ForeldrepengerUttakPeriode::getFom)
            .min(Comparator.naturalOrder());
    }

    private Optional<LocalDate> finnMinAktivDato(ForeldrepengerUttak bruker, ForeldrepengerUttak annenpart) {
        return Stream.concat(finnMinAktivDato(bruker).stream(), finnMinAktivDato(annenpart).stream())
            .min(Comparator.naturalOrder());
    }

    private Optional<ForeldrepengerUttak> hentUttak(Long behandling) {
        return uttakTjeneste.hentHvisEksisterer(behandling);
    }

    public static boolean harKonsekvens(Behandlingsresultat behandlingsresultat, KonsekvensForYtelsen konsekvens) {
        return behandlingsresultat.getKonsekvenserForYtelsen().contains(konsekvens);
    }

    private ForeldrepengerUttak tomtUttak() {
        return new ForeldrepengerUttak(List.of());
    }
}
