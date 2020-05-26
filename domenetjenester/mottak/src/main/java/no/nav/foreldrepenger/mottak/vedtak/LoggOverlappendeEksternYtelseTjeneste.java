package no.nav.foreldrepenger.mottak.vedtak;


import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.abakus.vedtak.ytelse.v1.YtelseV1;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingOverlappInfotrygd;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingOverlappInfotrygdRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

/*
 * Finn og logg overlappende vedtak når fpsak mottar vedtak om andre ytelser fra fx k9-sak
 */
@ApplicationScoped
public class LoggOverlappendeEksternYtelseTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(LoggOverlappendeEksternYtelseTjeneste.class);

    private BeregningsresultatRepository tilkjentYtelseRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingOverlappInfotrygdRepository overlappRepository;


    LoggOverlappendeEksternYtelseTjeneste() {
        // for CDI
    }

    @Inject
    public LoggOverlappendeEksternYtelseTjeneste(BeregningsresultatRepository beregningsresultatRepository,
                                                 BehandlingOverlappInfotrygdRepository overlappRepository,
                                                 BehandlingRepository behandlingRepository) {
        this.tilkjentYtelseRepository = beregningsresultatRepository;
        this.behandlingRepository = behandlingRepository;
        this.overlappRepository = overlappRepository;
    }

    public void loggOverlappUtenGradering(YtelseV1 ytelse, LocalDate minYtelseDato, LocalDateTimeline<Boolean> ytelseTidslinje, List<Fagsak> sakerForBruker) {
        var ytelseTemaSaksnummer = ytelse.getType().getKode() + ytelse.getSaksnummer();
        sakerForBruker.stream()
            .map(f -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(f.getId()))
            .flatMap(Optional::stream)
            .map(b -> sjekkOverlappFor(ytelseTemaSaksnummer, minYtelseDato, ytelseTidslinje, b))
            .flatMap(Collection::stream)
            .forEach(overlappRepository::lagre);
    }

    private List<BehandlingOverlappInfotrygd> sjekkOverlappFor(String tema, LocalDate minYtelseDato, LocalDateTimeline<Boolean> ytelseTidslinje, Behandling behandling) {
        List<LocalDateSegment<Boolean>> fpsegments = tilkjentYtelseRepository.hentBeregningsresultat(behandling.getId())
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(List.of()).stream()
            .filter(p -> p.getDagsats() > 0)
            .filter(p -> p.getBeregningsresultatPeriodeTom().isAfter(minYtelseDato.minusDays(1)))
            .map(p -> new LocalDateSegment<>(VirkedagUtil.fomVirkedag(p.getBeregningsresultatPeriodeFom()),
                VirkedagUtil.tomVirkedag(p.getBeregningsresultatPeriodeTom()), Boolean.TRUE))
            .collect(Collectors.toList());

        if (fpsegments.isEmpty())
            return Collections.emptyList();
        var fpTidslinje = new LocalDateTimeline<>(fpsegments, StandardCombinators::alwaysTrueForMatch).compress();

        var filter = fpTidslinje.intersection(ytelseTidslinje, StandardCombinators::alwaysTrueForMatch).compress();
        if (!filter.getDatoIntervaller().isEmpty())
            LOG.info("Vedtatt-Ytelse logger overlapp mellom mottatt ytelse {} og sak {}", tema, behandling.getFagsak().getSaksnummer().getVerdi());

        return filter.getDatoIntervaller().stream()
            .map(periode -> opprettOverlappEntitet(behandling, tema, periode))
            .collect(Collectors.toList());
    }

    private BehandlingOverlappInfotrygd opprettOverlappEntitet(Behandling behandling, String tema, LocalDateInterval periode) {
        return BehandlingOverlappInfotrygd.builder()
            .medSaksnummer(behandling.getFagsak().getSaksnummer())
            .medBehandlingId(behandling.getId())
            .medPeriodeInfotrygd(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(periode.getFomDato(), periode.getTomDato()))
            .medPeriodeVL(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(periode.getFomDato(), periode.getTomDato()))
            .medYtelseInfotrygd(tema)
            .build();
    }

}
