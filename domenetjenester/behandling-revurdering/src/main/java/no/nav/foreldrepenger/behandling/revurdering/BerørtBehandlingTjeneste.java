package no.nav.foreldrepenger.behandling.revurdering;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkBegrunnelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.TidsperiodeFarRundtFødsel;
import no.nav.foreldrepenger.domene.uttak.TidsperiodeForbeholdtMor;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

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
    public boolean skalBerørtBehandlingOpprettes(Behandlingsresultat brukersGjeldendeBehandlingsresultat,
                                                 Long behandlingId,
                                                 Long behandlingIdAnnenPart) {
        //Må sjekke konsekvens pga overlapp med samtidig uttak
        if (brukersGjeldendeBehandlingsresultat.isBehandlingHenlagt()
            || harKonsekvens(brukersGjeldendeBehandlingsresultat, KonsekvensForYtelsen.INGEN_ENDRING)) {
            LOG.info("Skal opprette berørt: Henlagt/IngenEndring");
            return false;
        }
        var uttakInput = uttakInputTjeneste.lagInput(brukersGjeldendeBehandlingsresultat.getBehandlingId());
        var kreverSammenhengendeUttak = uttakInput.getBehandlingReferanse().getSkjæringstidspunkt().kreverSammenhengendeUttak();
        ForeldrepengerGrunnlag foreldrepengerGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        if (foreldrepengerGrunnlag.isBerørtBehandling()) {
            LOG.info("Skal opprette berørt: Berørt");
            return false;
        }
        if (brukersGjeldendeBehandlingsresultat.isEndretStønadskonto()
            || stønadskontoSaldoTjeneste.erNegativSaldoPåNoenKonto(uttakInput)) {
            LOG.info("Skal opprette berørt: EndretKonto/NegativKonto");
            return true;
        }
        var brukersUttak = hentUttak(behandlingId).orElse(tomtUttak());
        var annenpartsUttak = hentUttak(behandlingIdAnnenPart);
        if (annenpartsUttak.isEmpty() || finnMinAktivDato(brukersUttak, annenpartsUttak.get()).isEmpty()) {
            LOG.info("Skal opprette berørt: Empty");
            return false;
        }

        // Endring fra en søknadsperiode eller fra start?
        var endringsdato = ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandlingId)
            .flatMap(YtelseFordelingAggregat::getGjeldendeEndringsdatoHvisEksisterer)
            .orElseGet(() -> finnMinAktivDato(brukersUttak, annenpartsUttak.get()).orElseThrow());
        var periodeTom = finnMaxAktivDato(brukersUttak, annenpartsUttak.get()).filter(endringsdato::isBefore).orElse(endringsdato);
        var periodeFomEndringsdato = new LocalDateInterval(endringsdato, periodeTom);

        if (overlappUtenomAkseptertSamtidigUttak(uttakInput, periodeFomEndringsdato, brukersUttak, annenpartsUttak.get())) {
            LOG.info("Skal opprette berørt: OverlappUtenSamtidig");
            return true;
        }

        var fellesTidslinjeForSammenheng = tidslinjeForSammenhengendeUttaksplan(brukersUttak, annenpartsUttak.get());
        // Sikre at periode reservert mor er komplett med uttak, utsettelser, overføringer
        if (foreldrepengerGrunnlag.getFamilieHendelser().gjelderTerminFødsel()) {
            var familieHendelseDato = foreldrepengerGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse().getFamilieHendelseDato();
            var førsteSeksUker = new LocalDateInterval(familieHendelseDato, TidsperiodeForbeholdtMor.tilOgMed(familieHendelseDato));
            if (!fellesTidslinjeForSammenheng.isContinuous(førsteSeksUker))
                LOG.info("Skal opprette berørt: Første 6 uker");
                return true;
        }

        var opprett = kreverSammenhengendeUttak && !fellesTidslinjeForSammenheng.isContinuous(periodeFomEndringsdato);
        LOG.info("Skal opprette berørt: Sammenhengende etter uke 6 {}", opprett);
        return opprett;
    }

    private boolean overlappUtenomAkseptertSamtidigUttak(UttakInput uttakInput, LocalDateInterval periodeFomEndringsdato,
                                                         ForeldrepengerUttak brukersUttak, ForeldrepengerUttak annenpartsUttak) {
        var tidslinjeBruker = lagTidslinje(brukersUttak, p -> true, this::helgFomMandagSegment);
        var tidslinjeAnnenpart = lagTidslinje(annenpartsUttak, p -> true, this::helgFomMandagSegment);
        // Tidslinje der begge har uttak - fom endringsdato.
        var tidslinjeOverlappendeUttakFomEndringsdato = tidslinjeAnnenpart.intersection(tidslinjeBruker).intersection(periodeFomEndringsdato);
        if (tidslinjeOverlappendeUttakFomEndringsdato.isEmpty()) {
            return false;
        }

        var tidslinjeBrukerSamtidig = lagTidslinje(brukersUttak, ForeldrepengerUttakPeriode::isSamtidigUttak, this::helgFomMandagSegment);
        var tidslinjeAnnenpartSamtidig = lagTidslinje(annenpartsUttak, ForeldrepengerUttakPeriode::isSamtidigUttak, this::helgFomMandagSegment);
        var tidslinjeSamtidigUttak = slåSammenTidslinjer(tidslinjeBrukerSamtidig, tidslinjeAnnenpartSamtidig);

        var kunOverlappRundtFødsel = TidsperiodeFarRundtFødsel.intervallFarRundtFødsel(uttakInput)
            .map(tidslinjeSamtidigUttak::intersection)
            .map(i -> tidslinjeOverlappendeUttakFomEndringsdato.disjoint(i).isEmpty())
            .orElse(Boolean.FALSE);
        return !kunOverlappRundtFødsel;
    }

    private LocalDateTimeline<Boolean> tidslinjeForSammenhengendeUttaksplan(ForeldrepengerUttak brukersUttak, ForeldrepengerUttak annenpartsUttak) {
        var tidslinjeBruker = lagTidslinje(brukersUttak, p -> true, this::helgTomSøndagSegment);
        var tidslinjeAnnenpart = lagTidslinje(annenpartsUttak, p -> true, this::helgTomSøndagSegment);
        return slåSammenTidslinjer(tidslinjeBruker, tidslinjeAnnenpart);
    }

    private LocalDateTimeline<Boolean> slåSammenTidslinjer(LocalDateTimeline<Boolean> tidslinje1, LocalDateTimeline<Boolean> tidslinje2) {
        // Slår sammen uttaksplanene med TRUE der en eller begge har uttak.
        return tidslinje1.crossJoin(tidslinje2, StandardCombinators::alwaysTrueForMatch).compress();
    }

    private LocalDateTimeline<Boolean> lagTidslinje(ForeldrepengerUttak uttak, Predicate<ForeldrepengerUttakPeriode> periodefilter,
                                                    Function<ForeldrepengerUttakPeriode, LocalDateSegment<Boolean>> segmentMapper) {
        var segmenter = uttak.getGjeldendePerioder().stream()
            .filter(ForeldrepengerUttakPeriode::harAktivtUttak)
            .filter(periodefilter)
            .map(segmentMapper)
            .toList();
        return new LocalDateTimeline<>(segmenter, StandardCombinators::alwaysTrueForMatch).compress();
    }

    private LocalDateSegment<Boolean> helgFomMandagSegment(ForeldrepengerUttakPeriode periode) {
        var fom = VirkedagUtil.lørdagSøndagTilMandag(periode.getFom());
        var tom = periode.getTom();
        var brukFom = fom.isAfter(tom) ? periode.getFom() : fom;
        return new LocalDateSegment<>(brukFom, tom, Boolean.TRUE);
    }

    private LocalDateSegment<Boolean> helgTomSøndagSegment(ForeldrepengerUttakPeriode periode) {
        var fom = periode.getFom();
        var tom = VirkedagUtil.fredagLørdagTilSøndag(periode.getTom());
        return new LocalDateSegment<>(fom, tom, Boolean.TRUE);
    }

    private Optional<LocalDate> finnMinAktivDato(ForeldrepengerUttak bruker, ForeldrepengerUttak annenpart) {
        return Stream.concat(bruker.getGjeldendePerioder().stream(), annenpart.getGjeldendePerioder().stream())
            .filter(ForeldrepengerUttakPeriode::harAktivtUttak)
            .map(ForeldrepengerUttakPeriode::getFom)
            .min(Comparator.naturalOrder());
    }

    private Optional<LocalDate> finnMaxAktivDato(ForeldrepengerUttak bruker, ForeldrepengerUttak annenpart) {
        return Stream.concat(bruker.getGjeldendePerioder().stream(), annenpart.getGjeldendePerioder().stream())
            .filter(ForeldrepengerUttakPeriode::harAktivtUttak)
            .map(ForeldrepengerUttakPeriode::getTom)
            .max(Comparator.naturalOrder());
    }

    private Optional<ForeldrepengerUttak> hentUttak(Long behandling) {
        return uttakTjeneste.hentUttakHvisEksisterer(behandling);
    }

    public boolean harKonsekvens(Behandlingsresultat behandlingsresultat, KonsekvensForYtelsen konsekvens) {
        return behandlingsresultat.getKonsekvenserForYtelsen().contains(konsekvens);
    }

    public void opprettHistorikkinnslagOmRevurdering(Behandling behandling,
                                                     HistorikkBegrunnelseType historikkBegrunnelseType) {
        opprettHistorikkinnslag(behandling, historikkBegrunnelseType);
    }

    public void opprettHistorikkinnslagOmRevurdering(Behandling behandling, BehandlingÅrsakType behandlingÅrsakType) {
        opprettHistorikkinnslag(behandling, behandlingÅrsakType);
    }

    private void opprettHistorikkinnslag(Behandling behandling, Kodeverdi begrunnelse) {
        var revurderingsInnslag = new Historikkinnslag();
        revurderingsInnslag.setBehandling(behandling);
        var historikkinnslagType = HistorikkinnslagType.REVURD_OPPR;
        revurderingsInnslag.setType(historikkinnslagType);
        revurderingsInnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        var historiebygger = new HistorikkInnslagTekstBuilder().medHendelse(historikkinnslagType);
        historiebygger.medBegrunnelse(begrunnelse);

        historiebygger.build(revurderingsInnslag);

        historikkRepository.lagre(revurderingsInnslag);
    }

    private ForeldrepengerUttak tomtUttak() {
        return new ForeldrepengerUttak(List.of());
    }
}
