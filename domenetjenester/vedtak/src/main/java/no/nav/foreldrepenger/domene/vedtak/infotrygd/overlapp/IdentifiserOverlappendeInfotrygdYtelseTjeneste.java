package no.nav.foreldrepenger.domene.vedtak.infotrygd.overlapp;


import java.math.BigDecimal;
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
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingOverlappInfotrygd;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingOverlappInfotrygdRepository;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.vedtak.infotrygd.rest.InfotrygdPSGrunnlag;
import no.nav.foreldrepenger.domene.vedtak.infotrygd.rest.InfotrygdSPGrunnlag;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.vedtak.felles.integrasjon.aktør.klient.AktørConsumerMedCache;
import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.Grunnlag;
import no.nav.vedtak.konfig.Tid;
/*
Når Foreldrepenger-sak, enten førstegang eller revurdering, innvilges sjekker vi for overlapp med pleiepenger eller sykepenger i Infortrygd på personen.
Overlapp er tilstede dersom noen av de vedtatte periodende i Infotrygd overlapper med noen av utbetalingsperiodene på iverksatt foreldrepenge-behandling
Ved overlapp lagres informasjonen til databasetabellen BEHANDLING_OVERLAPP_INFOTRYGD
Det er manuell håndtering av funnene videre.
Håndtering av overlapp av Foreldrepenger-foreldrepenger håndteres av klassen VurderOpphørAvYtelser som trigges av en prosesstask.
 */
@ApplicationScoped
public class IdentifiserOverlappendeInfotrygdYtelseTjeneste {

    private static final Logger log = LoggerFactory.getLogger(IdentifiserOverlappendeInfotrygdYtelseTjeneste.class);
    private static final BigDecimal HUNDRE = new BigDecimal(100);

    private BeregningsresultatRepository beregningsresultatRepository;
    private BehandlingRepository behandlingRepository;
    private AktørConsumerMedCache aktørConsumer;
    private InfotrygdPSGrunnlag infotrygdPSGrTjeneste;
    private InfotrygdSPGrunnlag infotrygdSPGrTjeneste;
    private BehandlingOverlappInfotrygdRepository overlappRepository;


    IdentifiserOverlappendeInfotrygdYtelseTjeneste() {
        // for CDI
    }

    @Inject
    public IdentifiserOverlappendeInfotrygdYtelseTjeneste(BeregningsresultatRepository beregningsresultatRepository,
                                                          AktørConsumerMedCache aktørConsumer,
                                                          InfotrygdPSGrunnlag infotrygdPSGrTjeneste,
                                                          InfotrygdSPGrunnlag infotrygdSPGrTjeneste,
                                                          BehandlingOverlappInfotrygdRepository overlappRepository,
                                                          BehandlingRepository behandlingRepository) {
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.behandlingRepository = behandlingRepository;
        this.aktørConsumer = aktørConsumer;
        this.infotrygdPSGrTjeneste = infotrygdPSGrTjeneste;
        this.infotrygdSPGrTjeneste = infotrygdSPGrTjeneste;
        this.overlappRepository = overlappRepository;
    }

    public void vurderOglagreEventueltOverlapp(Behandling behandling) {
        try {
            List<BehandlingOverlappInfotrygd> listeMedOverlapp = vurderOmOverlappInfotrygd("", behandling, null);
            listeMedOverlapp.forEach(overlappRepository::lagre);
        } catch (Exception e) {
            log.info("Identifisering av overlapp i Infotrygd feilet ", e);
        }
    }

    public void vurderOglagreEventueltOverlapp(String prefix, Long behandlingId, LocalDate minFraQuery) {
        try {
            var behandling = behandlingRepository.hentBehandling(behandlingId);
            LocalDate førsteUttaksDatoFP = VirkedagUtil.fomVirkedag(minFraQuery);
            vurderOmOverlappInfotrygd(prefix, behandling, førsteUttaksDatoFP).forEach(overlappRepository::lagre);
        } catch (Exception e) {
            log.info("Identifisering av overlapp i Infotrygd feilet ", e);
        }
    }

    public List<BehandlingOverlappInfotrygd> vurderEventueltOverlapp(Long behandlingId, LocalDate minFraQuery) {
        try {
            var behandling = behandlingRepository.hentBehandling(behandlingId);
            LocalDate førsteUttaksDatoFP = VirkedagUtil.fomVirkedag(minFraQuery);
            return vurderOmOverlappInfotrygd("I", behandling, førsteUttaksDatoFP);
        } catch (Exception e) {
            log.info("Identifisering av overlapp i Infotrygd feilet ", e);
            return Collections.emptyList();
        }
    }

    public List<BehandlingOverlappInfotrygd> vurderOmOverlappInfotrygd(String prefix, Behandling behandling, LocalDate førsteUttaksDatoFP) {
        //Henter alle utbetalingsperioder på behandling som er iverksatt
        var brPerioder = beregningsresultatRepository.hentBeregningsresultat(behandling.getId())
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList());

        var tidligsteUttakFP = førsteUttaksDatoFP != null ? førsteUttaksDatoFP : finnFørsteUttakUtbetaltDato(brPerioder);
        LocalDateTimeline<Boolean> perioderFP = hentPerioderFp(brPerioder);
        if (perioderFP.getDatoIntervaller().isEmpty())
            return  Collections.emptyList();
        LocalDateTimeline<BigDecimal> perioderFpGradert = hentGradertePerioderFp(brPerioder);
        //sjekker om noen av vedtaksperiodene i Infotrygd på sykepenger eller pleiepenger overlapper med perioderFp

        List<BehandlingOverlappInfotrygd> overlappene = new ArrayList<>();
        var ident = getFnrFraAktørId(behandling.getAktørId());

        List<Grunnlag> infotrygdPSGrunnlag = infotrygdPSGrTjeneste.hentGrunnlag(ident.getIdent(), tidligsteUttakFP.minusWeeks(1), tidligsteUttakFP.plusYears(3));
        List<Grunnlag> infotrygdSPGrunnlag = infotrygdSPGrTjeneste.hentGrunnlag(ident.getIdent(), tidligsteUttakFP.minusWeeks(1), tidligsteUttakFP.plusYears(3));

        if (perioderFpGradert.filterValue(x -> x.compareTo(HUNDRE) < 0).getDatoIntervaller().isEmpty()) {
            finnOverlappene(behandling, perioderFP, prefix + "SP", finnTidslinjeFraGrunnlagene(infotrygdSPGrunnlag), overlappene);
            finnOverlappene(behandling, perioderFP, prefix + "BS", finnTidslinjeFraGrunnlagene(infotrygdPSGrunnlag), overlappene);
        } else {
            finnGradertOverlapp(behandling, perioderFpGradert, prefix + "GS", finnGradertTidslinjeFraGrunnlagene(infotrygdSPGrunnlag), overlappene);
            finnGradertOverlapp(behandling, perioderFpGradert, prefix + "GB", finnGradertTidslinjeFraGrunnlagene(infotrygdPSGrunnlag), overlappene);
        }

        return overlappene;
    }

    private void finnOverlappene(Behandling behandling, LocalDateTimeline<Boolean> perioderFP, String tema,
                                LocalDateTimeline<Boolean> tlGrunnlag, List<BehandlingOverlappInfotrygd> overlappene) {
        var filter = perioderFP.intersection(tlGrunnlag, StandardCombinators::alwaysTrueForMatch).compress();

        filter.getDatoIntervaller()
            .forEach(grunnlagPeriode -> perioderFP.getDatoIntervaller().stream()
                .filter(grunnlagPeriode::overlaps)
                .map(vlPeriode -> opprettOverlappIT(behandling, tema, grunnlagPeriode, vlPeriode))
                .forEach(overlappene::add)
            );
    }

    private void finnGradertOverlapp(Behandling behandling, LocalDateTimeline<BigDecimal> perioderFP, String tema,
                                    LocalDateTimeline<BigDecimal> tlGrunnlag, List<BehandlingOverlappInfotrygd> overlappene) {
        var filter = perioderFP.intersection(tlGrunnlag, StandardCombinators::sum).filterValue(v -> v.compareTo(HUNDRE) > 0);

        filter.getDatoIntervaller()
            .forEach(grunnlagPeriode -> perioderFP.getDatoIntervaller().stream()
                .filter(grunnlagPeriode::overlaps)
                .map(vlPeriode -> opprettOverlappIT(behandling, tema, grunnlagPeriode, vlPeriode))
                .forEach(overlappene::add)
            );
    }

    private LocalDateTimeline<Boolean> hentPerioderFp(Collection<BeregningsresultatPeriode> perioder) {
        var segments = perioder.stream()
            .filter(beregningsresultatPeriode -> beregningsresultatPeriode.getDagsats() > 0)
            .map(p -> new LocalDateSegment<>(p.getBeregningsresultatPeriodeFom(), VirkedagUtil.tomSøndag(p.getBeregningsresultatPeriodeTom()), Boolean.TRUE))
            .collect(Collectors.toList());

        return helgeJusterTidslinje(new LocalDateTimeline<>(segments, StandardCombinators::alwaysTrueForMatch).compress());
    }

    private LocalDateTimeline<BigDecimal> hentGradertePerioderFp(Collection<BeregningsresultatPeriode> perioder) {
        var segments = perioder.stream()
            .map(p -> new LocalDateSegment<>(VirkedagUtil.fomVirkedag(p.getBeregningsresultatPeriodeFom()),
                VirkedagUtil.tomVirkedag(p.getBeregningsresultatPeriodeTom()), p.getKalkulertUtbetalingsgrad()))
            .collect(Collectors.toList());

        return new LocalDateTimeline<>(segments, StandardCombinators::sum).filterValue(v -> v.compareTo(BigDecimal.ZERO) > 0);
    }

    private LocalDateTimeline<Boolean> finnTidslinjeFraGrunnlagene(List<Grunnlag> grunnlag) {
        List<LocalDateSegment<Boolean>> segmenter = grunnlag.stream()
            .map(Grunnlag::getVedtak)
            .flatMap(Collection::stream)
            .filter(v -> v.getUtbetalingsgrad() > 0)
            .map(p-> new LocalDateSegment<>(p.getPeriode().getFom(), VirkedagUtil.tomSøndag(p.getPeriode().getTom()), Boolean.TRUE))
            .collect(Collectors.toList());

        return helgeJusterTidslinje(new LocalDateTimeline<>(segmenter, StandardCombinators::alwaysTrueForMatch).compress());
    }

    private LocalDateTimeline<BigDecimal> finnGradertTidslinjeFraGrunnlagene(List<Grunnlag> grunnlag) {
        var segmenter = grunnlag.stream()
            .map(Grunnlag::getVedtak)
            .flatMap(Collection::stream)
            .filter(v -> v.getUtbetalingsgrad() > 0)
            .map(p-> new LocalDateSegment<>(VirkedagUtil.fomVirkedag(p.getPeriode().getFom()), VirkedagUtil.tomVirkedag(p.getPeriode().getTom()),
                new BigDecimal(p.getUtbetalingsgrad())))
            .collect(Collectors.toList());

        return new LocalDateTimeline<>(segmenter, StandardCombinators::sum);
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
        return new LocalDateTimeline<Boolean>(segments, StandardCombinators::alwaysTrueForMatch).compress();
    }


    private PersonIdent getFnrFraAktørId(AktørId aktørId) {
        return aktørConsumer.hentPersonIdentForAktørId(aktørId.getId()).map(PersonIdent::new).orElseThrow();
    }

    private LocalDate finnFørsteUttakUtbetaltDato(Collection<BeregningsresultatPeriode> perioder) {
        return perioder.stream()
            .filter(beregningsresultatPeriode -> beregningsresultatPeriode.getDagsats() > 0)
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)
            .map(VirkedagUtil::fomVirkedag)
            .min(Comparator.naturalOrder()).orElse(Tid.TIDENES_ENDE);
    }



}
