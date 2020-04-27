package no.nav.foreldrepenger.domene.vedtak;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
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
    private static final BigDecimal PROSENT96 = new BigDecimal(96L);

    private BeregningsresultatRepository beregningsresultatRepository;
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
                                                          BehandlingOverlappInfotrygdRepository overlappRepository) {
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.aktørConsumer = aktørConsumer;
        this.infotrygdPSGrTjeneste = infotrygdPSGrTjeneste;
        this.infotrygdSPGrTjeneste = infotrygdSPGrTjeneste;
        this.overlappRepository = overlappRepository;
    }

    public void vurderOglagreEventueltOverlapp(Behandling behandling) {
        try {
            List<BehandlingOverlappInfotrygd> listeMedOverlapp = vurderOmOverlappInfotrygd(behandling);
            listeMedOverlapp.forEach(behandlingOverlappInfotrygd -> overlappRepository.lagre(behandlingOverlappInfotrygd));
        } catch (Exception e) {
            log.info("Identifisering av overlapp i Infotrygd feilet ", e);
        }
    }

    public List<BehandlingOverlappInfotrygd> vurderOmOverlappInfotrygd(Behandling behandling) {
        LocalDate førsteUttaksDatoFP = finnFørsteUttakUtbetaltDato(behandling.getId());
        if (Tid.TIDENES_ENDE.equals(førsteUttaksDatoFP)) {
            return Collections.emptyList();
        }
        //Henter alle utbetalingsperioder på behandling som er iverksatt
        LocalDateTimeline<Boolean> perioderFp = hentPerioderFp(behandling.getId());
        LocalDateTimeline<Boolean> perioderFpGradert = hentGradertePerioderFp(behandling.getId());
        //sjekker om noen av vedtaksperiodene i Infotrygd på sykepenger eller pleiepenger overlapper med perioderFp
        return harFPYtelserSomOverlapperIT(behandling, førsteUttaksDatoFP, perioderFp, perioderFpGradert);
    }

    public List<BehandlingOverlappInfotrygd> harFPYtelserSomOverlapperIT(Behandling behandling, LocalDate førsteUttaksdatoFp, LocalDateTimeline<Boolean> perioderFP, LocalDateTimeline<Boolean> perioderFpGradert) {
        List<BehandlingOverlappInfotrygd> overlappene = new ArrayList<>();
        var ident = getFnrFraAktørId(behandling.getAktørId());

        List<Grunnlag> infotrygdPSGrunnlag = infotrygdPSGrTjeneste.hentGrunnlag(ident.getIdent(), førsteUttaksdatoFp.minusWeeks(1), førsteUttaksdatoFp.plusYears(3));
        List<Grunnlag> infotrygdSPGrunnlag = infotrygdSPGrTjeneste.hentGrunnlag(ident.getIdent(), førsteUttaksdatoFp.minusWeeks(1), førsteUttaksdatoFp.plusYears(3));

        finnOverlappene(behandling, perioderFP, "SP", finnTidslinjeFraGrunnlagene(infotrygdSPGrunnlag), overlappene);
        finnOverlappene(behandling, perioderFP, "BS", finnTidslinjeFraGrunnlagene(infotrygdPSGrunnlag), overlappene);

        // Logger i tillegg overlapp FP-gradert / BS+SP-gradert - i påvente av videre epos om gradert samhandling
        if (!perioderFpGradert.getDatoIntervaller().isEmpty()) {
            finnOverlappene(behandling, perioderFP, "SPG", finnGradertTidslinjeFraGrunnlagene(infotrygdSPGrunnlag), overlappene);
            finnOverlappene(behandling, perioderFP, "BSG", finnGradertTidslinjeFraGrunnlagene(infotrygdPSGrunnlag), overlappene);
        }

        return overlappene;
    }

    private void finnOverlappene(Behandling behandling, LocalDateTimeline<Boolean> perioderFP, String tema,
                                 LocalDateTimeline<Boolean> tlGrunnlag, List<BehandlingOverlappInfotrygd> overlappene) {

        tlGrunnlag.getDatoIntervaller()
            .forEach(grunnlagPeriode -> perioderFP.getDatoIntervaller().stream()
                .filter(grunnlagPeriode::overlaps)
                .map(vlPeriode -> opprettOverlappIT(behandling, tema, grunnlagPeriode, vlPeriode))
                .forEach(overlappene::add)
        );
    }

    private PersonIdent getFnrFraAktørId(AktørId aktørId) {
        return aktørConsumer.hentPersonIdentForAktørId(aktørId.getId()).map(PersonIdent::new).orElseThrow();
    }

    private LocalDateTimeline<Boolean> hentPerioderFp(Long behandlingId) {
        Optional<BeregningsresultatEntitet> berResultat = beregningsresultatRepository.hentBeregningsresultat(behandlingId);

        var segments = berResultat.map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
            .filter(beregningsresultatPeriode -> beregningsresultatPeriode.getDagsats() > 0)
            .map(p -> new LocalDateSegment<>(p.getBeregningsresultatPeriodeFom(), VirkedagUtil.tomSøndag(p.getBeregningsresultatPeriodeTom()), Boolean.TRUE))
            .collect(Collectors.toList());

        return helgeJusterTidslinje(new LocalDateTimeline<>(segments, StandardCombinators::alwaysTrueForMatch).compress());
    }

    private LocalDateTimeline<Boolean> hentGradertePerioderFp(Long behandlingId) {
        Optional<BeregningsresultatEntitet> berResultat = beregningsresultatRepository.hentBeregningsresultat(behandlingId);

        var segments = berResultat.map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
            .filter(beregningsresultatPeriode -> beregningsresultatPeriode.getDagsats() > 0)
            .filter(bp -> bp.getLavestUtbetalingsgrad().map(ug -> PROSENT96.compareTo(ug) > 0).orElse(Boolean.FALSE))
            .map(p -> new LocalDateSegment<>(p.getBeregningsresultatPeriodeFom(), VirkedagUtil.tomSøndag(p.getBeregningsresultatPeriodeTom()), Boolean.TRUE))
            .collect(Collectors.toList());

        return helgeJusterTidslinje(new LocalDateTimeline<>(segments, StandardCombinators::alwaysTrueForMatch).compress());
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

    private LocalDateTimeline<Boolean> finnGradertTidslinjeFraGrunnlagene(List<Grunnlag> grunnlag) {
        List<LocalDateSegment<Boolean>> segmenter = grunnlag.stream()
            .map(Grunnlag::getVedtak)
            .flatMap(Collection::stream)
            .filter(v -> v.getUtbetalingsgrad() > 0)
            .filter(v -> v.getUtbetalingsgrad() < 96)
            .map(p-> new LocalDateSegment<>(p.getPeriode().getFom(), VirkedagUtil.tomSøndag(p.getPeriode().getTom()), Boolean.TRUE))
            .collect(Collectors.toList());

        return helgeJusterTidslinje(new LocalDateTimeline<>(segmenter, StandardCombinators::alwaysTrueForMatch).compress());
    }

    private LocalDate finnFørsteUttakUtbetaltDato(Long behandlingId) {
        Optional<BeregningsresultatEntitet> berResultat = beregningsresultatRepository.hentBeregningsresultat(behandlingId);
        Optional<LocalDate> minFom = berResultat.map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
            .filter(beregningsresultatPeriode -> beregningsresultatPeriode.getDagsats() > 0)
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)
            .min(Comparator.naturalOrder());
        return minFom.orElse(Tid.TIDENES_ENDE);
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
