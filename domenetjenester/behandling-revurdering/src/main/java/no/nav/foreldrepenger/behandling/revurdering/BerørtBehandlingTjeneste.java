package no.nav.foreldrepenger.behandling.revurdering;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
import no.nav.foreldrepenger.domene.uttak.TidsperiodeForbeholdtMor;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

@ApplicationScoped
public class BerørtBehandlingTjeneste {

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
        if (brukersGjeldendeBehandlingsresultat.isBehandlingHenlagt() || harKonsekvens(
            brukersGjeldendeBehandlingsresultat, KonsekvensForYtelsen.INGEN_ENDRING)) {
            return false;
        }
        var uttakInput = uttakInputTjeneste.lagInput(brukersGjeldendeBehandlingsresultat.getBehandlingId());
        var kreverSammenhengendeUttak = uttakInput.getBehandlingReferanse().getSkjæringstidspunkt().kreverSammenhengendeUttak();
        ForeldrepengerGrunnlag foreldrepengerGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        if (foreldrepengerGrunnlag.isBerørtBehandling()) {
            return false;
        }
        if (brukersGjeldendeBehandlingsresultat.isEndretStønadskonto()
            || stønadskontoSaldoTjeneste.erNegativSaldoPåNoenKonto(uttakInput)) {
            return true;
        }
        var annenpartsUttak = hentUttak(behandlingIdAnnenPart);
        if (annenpartsUttak.isEmpty()) {
            return false;
        }
        var brukersUttak = hentUttak(behandlingId);
        var tidslinjeBruker = lagTidslinje(brukersUttak.orElse(tomtUttak()));
        var tidslinjeAnnenpart = lagTidslinje(annenpartsUttak.get());
        // Slår sammen uttaksplanene med TRUE der en eller begge har uttak.
        var fellesTidslinje = tidslinjeBruker.crossJoin(tidslinjeAnnenpart, StandardCombinators::alwaysTrueForMatch).compress();
        if (fellesTidslinje.isEmpty()) {
            return false;
        }
        // Endring fra en søknadsperiode eller fra start?
        var endringsdato = ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandlingId)
            .flatMap(YtelseFordelingAggregat::getGjeldendeEndringsdatoHvisEksisterer)
            .orElseGet(fellesTidslinje::getMinLocalDate);
        var periodeFomEndringsdato = new LocalDateInterval(endringsdato, fellesTidslinje.getMaxLocalDate());

        // Overlapp fom endringsdato - perioder der tidlinjene overlapper sjekkes mot intervall fom endringsdato
        if (!tidslinjeBruker.intersection(tidslinjeAnnenpart).intersection(periodeFomEndringsdato).isEmpty()) {
            // Vurder egne sjekker for å tillate tilfelle av 200% samtidig uttak - flerbarn og rundt fødsel
            return true;
        }

        // Sikre at periode reservert mor er komplett med uttak, utsettelser, overføringer
        if (foreldrepengerGrunnlag.getFamilieHendelser().gjelderTerminFødsel()) {
            var familieHendelseDato = foreldrepengerGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse().getFamilieHendelseDato();
            var førsteSeksUker = new LocalDateInterval(familieHendelseDato, TidsperiodeForbeholdtMor.tilOgMed(familieHendelseDato));
            if (!fellesTidslinje.isContinuous(førsteSeksUker))
                return true;
        }

        return kreverSammenhengendeUttak && !fellesTidslinje.isContinuous(periodeFomEndringsdato);
    }

    private LocalDateTimeline<Boolean> lagTidslinje(ForeldrepengerUttak uttak) {
        var segmenter = uttak.getGjeldendePerioder().stream()
            .filter(ForeldrepengerUttakPeriode::harAktivtUttak)
            .map(this::lagSegment)
            .toList();
        return new LocalDateTimeline<>(segmenter, StandardCombinators::alwaysTrueForMatch).compress();
    }

    private LocalDateSegment<Boolean> lagSegment(ForeldrepengerUttakPeriode periode) {
        var fom = VirkedagUtil.lørdagSøndagTilMandag(periode.getFom());
        var tom = VirkedagUtil.fredagLørdagTilSøndag(periode.getTom());
        var brukFom = fom.isAfter(tom) ? periode.getFom() : fom;
        return new LocalDateSegment<>(brukFom, tom, Boolean.TRUE);
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
