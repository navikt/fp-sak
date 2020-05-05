package no.nav.foreldrepenger.domene.vedtak.infotrygd.overlapp;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingOverlappInfotrygd;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingOverlappInfotrygdRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.vedtak.infotrygd.rest.InfotrygdFPGrunnlag;
import no.nav.foreldrepenger.domene.vedtak.infotrygd.rest.InfotrygdSVPGrunnlag;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.vedtak.felles.integrasjon.aktør.klient.AktørConsumerMedCache;
import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.Grunnlag;

/*
Når Foreldrepenger-sak, enten førstegang eller revurdering, innvilges sjekker vi for overlapp med pleiepenger eller sykepenger i Infortrygd på personen.
Overlapp er tilstede dersom noen av de vedtatte periodende i Infotrygd overlapper med noen av utbetalingsperiodene på iverksatt foreldrepenge-behandling
Ved overlapp lagres informasjonen til databasetabellen BEHANDLING_OVERLAPP_INFOTRYGD
Det er manuell håndtering av funnene videre.
Håndtering av overlapp av Foreldrepenger-foreldrepenger håndteres av klassen VurderOpphørAvYtelser som trigges av en prosesstask.
 */
@ApplicationScoped
public class LoggHistoriskOverlappFPInfotrygdVLTjeneste {
    private BeregningsresultatRepository beregningsresultatRepository;
    private BehandlingRepository behandlingRepository;
    private AktørConsumerMedCache aktørConsumer;
    private InfotrygdSVPGrunnlag infotrygdSVPGrTjeneste;
    private InfotrygdFPGrunnlag infotrygdFPGrTjeneste;
    private BehandlingOverlappInfotrygdRepository overlappRepository;
    private static final Logger log = LoggerFactory.getLogger(LoggHistoriskOverlappFPInfotrygdVLTjeneste.class);


    LoggHistoriskOverlappFPInfotrygdVLTjeneste() {
        // for CDI
    }

    @Inject
    public LoggHistoriskOverlappFPInfotrygdVLTjeneste(BeregningsresultatRepository beregningsresultatRepository,
                                                      BehandlingRepository behandlingRepository,
                                                      AktørConsumerMedCache aktørConsumer,
                                                      InfotrygdSVPGrunnlag infotrygdSVPGrTjeneste,
                                                      InfotrygdFPGrunnlag infotrygdFPGrTjeneste,
                                                      BehandlingOverlappInfotrygdRepository overlappRepository) {
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.behandlingRepository = behandlingRepository;
        this.aktørConsumer = aktørConsumer;
        this.infotrygdSVPGrTjeneste = infotrygdSVPGrTjeneste;
        this.infotrygdFPGrTjeneste = infotrygdFPGrTjeneste;
        this.overlappRepository = overlappRepository;
    }

    public void vurderOglagreEventueltOverlapp(String prefix, Long behandlingId, AktørId annenPart, LocalDate minFraQuery) {
        try {
            utledPerioderMedOverlapp(prefix, behandlingId, annenPart, minFraQuery)
                .forEach(overlappRepository::lagre);
        } catch (Exception e) {
            log.info("Identifisering av overlapp i Infotrygd feilet ", e);
        }
    }

    public List<BehandlingOverlappInfotrygd> vurderEventueltOverlapp(Long behandlingId, AktørId annenPart, LocalDate minFraQuery) {
        try {
            return utledPerioderMedOverlapp("I", behandlingId, annenPart, minFraQuery);
        } catch (Exception e) {
            log.info("Identifisering av overlapp i Infotrygd feilet ", e);
        }
        return Collections.emptyList();
    }

    private List<BehandlingOverlappInfotrygd> utledPerioderMedOverlapp(String prefix, Long behandlingId, AktørId annenPart, LocalDate minFraQuery) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        List<BehandlingOverlappInfotrygd> resultat = new ArrayList<>();

        if (FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType())) {
            // Her skal vi oppdage alle ytelser som strekker seg forbi førsteDatoVL - uansett overlapp eller ei.
            LocalDate førsteDatoVL = VirkedagUtil.fomVirkedag(minFraQuery);
            LocalDateInterval førstePeriodeVL = hentPerioderFp(behandlingId, førsteDatoVL);

            harFPYtelserSomOverlapperIT(førsteDatoVL, behandling.getAktørId()).getDatoIntervaller().stream().findFirst()
                .ifPresent(grunnlagPeriode -> resultat.add(opprettOverlappIT(behandling, prefix + "F1", grunnlagPeriode, førstePeriodeVL)));
            if (RelasjonsRolleType.erMor(behandling.getFagsak().getRelasjonsRolleType()) && annenPart != null)  {
                harFPYtelserSomOverlapperIT(førsteDatoVL, annenPart).getDatoIntervaller().stream().findFirst()
                    .ifPresent(grunnlagPeriode -> resultat.add(opprettOverlappIT(behandling, prefix + "F2", grunnlagPeriode, førstePeriodeVL)));
            }
            if (RelasjonsRolleType.erMor(behandling.getFagsak().getRelasjonsRolleType())) {
                harSVPYtelserSomOverlapperIT(førsteDatoVL, behandling.getAktørId()).getDatoIntervaller().stream().findFirst()
                    .ifPresent(grunnlagPeriode -> resultat.add(opprettOverlappIT(behandling, prefix + "SV", grunnlagPeriode, førstePeriodeVL)));
            }
        } else if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(behandling.getFagsakYtelseType())) {
            // Her skal vi oppdage alle ytelser med netto overlapp - kun utbetalinger
            LocalDateTimeline<Boolean> perioderVL = hentPerioderSVP(behandlingId);
            LocalDate førsteDatoVL = perioderVL.getDatoIntervaller().stream().map(LocalDateInterval::getFomDato).min(Comparator.naturalOrder()).orElse(null);
            if (førsteDatoVL != null) {
                harFPYtelserSomOverlapperIT(førsteDatoVL, behandling.getAktørId())
                    .intersection(perioderVL, StandardCombinators::alwaysTrueForMatch).compress().getDatoIntervaller().stream().findFirst()
                    .ifPresent(grunnlagPeriode -> resultat.add(opprettOverlappIT(behandling, prefix + "F1", grunnlagPeriode, grunnlagPeriode)));
                if (RelasjonsRolleType.erMor(behandling.getFagsak().getRelasjonsRolleType())) {
                    harSVPYtelserSomOverlapperIT(førsteDatoVL, behandling.getAktørId())
                        .intersection(perioderVL, StandardCombinators::alwaysTrueForMatch).compress().getDatoIntervaller().stream().findFirst()
                        .ifPresent(grunnlagPeriode -> resultat.add(opprettOverlappIT(behandling, prefix + "SV", grunnlagPeriode, grunnlagPeriode)));
                }
            }
        }
        return resultat;
    }

    private LocalDateTimeline<Boolean> harFPYtelserSomOverlapperIT(LocalDate førsteDatoVL, AktørId finnForAktørId) {
        var ident = getFnrFraAktørId(finnForAktørId);

        List<Grunnlag> infotrygdFPGrunnlag = infotrygdFPGrTjeneste.hentGrunnlag(ident.getIdent(), førsteDatoVL.minusWeeks(4), førsteDatoVL.plusYears(3));

        return finnTidslinjeFraGrunnlagene(infotrygdFPGrunnlag, førsteDatoVL);
    }

    private LocalDateTimeline<Boolean> harSVPYtelserSomOverlapperIT(LocalDate førsteDatoVL, AktørId finnForAktørId) {
        var ident = getFnrFraAktørId(finnForAktørId);

        List<Grunnlag> infotrygdSVPGrunnlag = infotrygdSVPGrTjeneste.hentGrunnlag(ident.getIdent(), førsteDatoVL.minusWeeks(4), førsteDatoVL.plusYears(3));

        return finnTidslinjeFraGrunnlagene(infotrygdSVPGrunnlag, førsteDatoVL);
    }


    private PersonIdent getFnrFraAktørId(AktørId aktørId) {
        return aktørConsumer.hentPersonIdentForAktørId(aktørId.getId()).map(PersonIdent::new).orElseThrow();
    }

    private LocalDateInterval hentPerioderFp(Long behandlingId, LocalDate minUttakDato) {
        var minimum = new LocalDateInterval(minUttakDato, minUttakDato);

        var segmenter = new ArrayList<>(List.of(new LocalDateSegment<>(minimum, Boolean.TRUE)));
        beregningsresultatRepository.hentBeregningsresultat(behandlingId)
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
            .map(p -> new LocalDateSegment<>(p.getBeregningsresultatPeriodeFom(), VirkedagUtil.tomSøndag(p.getBeregningsresultatPeriodeTom()), Boolean.TRUE))
            .forEach(segmenter::add);

        return helgeJusterTidslinje(new LocalDateTimeline<>(segmenter, StandardCombinators::alwaysTrueForMatch).compress()).getDatoIntervaller().stream()
            .min(Comparator.comparing(LocalDateInterval::getFomDato)).orElse(minimum);
    }

    private LocalDateTimeline<Boolean> hentPerioderSVP(Long behandlingId) {
        var segments = beregningsresultatRepository.hentBeregningsresultat(behandlingId)
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
            .filter(beregningsresultatPeriode -> beregningsresultatPeriode.getDagsats() > 0)
            .map(p -> new LocalDateSegment<>(p.getBeregningsresultatPeriodeFom(), VirkedagUtil.tomSøndag(p.getBeregningsresultatPeriodeTom()), Boolean.TRUE))
            .collect(Collectors.toList());

        return helgeJusterTidslinje(new LocalDateTimeline<>(segments, StandardCombinators::alwaysTrueForMatch).compress());
    }

    private LocalDateTimeline<Boolean> finnTidslinjeFraGrunnlagene(List<Grunnlag> grunnlag, LocalDate førsteUttaksdatoFp) {
        List<LocalDateSegment<Boolean>> segmenter = grunnlag.stream()
            .map(Grunnlag::getVedtak)
            .flatMap(Collection::stream)
            .filter(v -> !v.getPeriode().getTom().isBefore(førsteUttaksdatoFp))
            .filter(v -> v.getUtbetalingsgrad() > 0)
            .map(p-> new LocalDateSegment<>(p.getPeriode().getFom(), VirkedagUtil.tomSøndag(p.getPeriode().getTom()), Boolean.TRUE))
            .collect(Collectors.toList());

        return helgeJusterTidslinje(new LocalDateTimeline<>(segmenter, StandardCombinators::alwaysTrueForMatch).compress());
    }

    private BehandlingOverlappInfotrygd opprettOverlappIT(Behandling behandling, String tema, LocalDateInterval periodeInfotrygd, LocalDateInterval periodeVL) {
        return BehandlingOverlappInfotrygd.builder()
            .medSaksnummer(behandling.getFagsak().getSaksnummer())
            .medBehandlingId(behandling.getId())
            .medPeriodeInfotrygd(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(periodeInfotrygd.getFomDato(), periodeInfotrygd.getTomDato()))
            .medPeriodeVL(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(periodeVL.getFomDato(), periodeVL.getTomDato()))
            .medYtelseInfotrygd(tema)
            .build();
    }

    private LocalDateTimeline<Boolean> helgeJusterTidslinje(LocalDateTimeline<Boolean> tidslinje) {
        var segments = tidslinje.getDatoIntervaller().stream()
            .map(p -> new LocalDateSegment<>(VirkedagUtil.fomVirkedag(p.getFomDato()), VirkedagUtil.tomVirkedag(p.getTomDato()), Boolean.TRUE))
            .collect(Collectors.toList());
        return new LocalDateTimeline<>(segments, StandardCombinators::alwaysTrueForMatch).compress();
    }
}
