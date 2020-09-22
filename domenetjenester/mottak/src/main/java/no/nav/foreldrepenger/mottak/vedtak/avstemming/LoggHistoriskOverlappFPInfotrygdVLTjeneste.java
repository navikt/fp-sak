package no.nav.foreldrepenger.mottak.vedtak.avstemming;


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

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OverlappVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.OverlappVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev.OverlappData;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.mottak.vedtak.rest.InfotrygdFPGrunnlag;
import no.nav.foreldrepenger.mottak.vedtak.rest.InfotrygdSVPGrunnlag;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.vedtak.felles.integrasjon.aktør.klient.AktørConsumerMedCache;
import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.Grunnlag;

/*
 * Logging av overlapp mellom vedtak i VL og Infotrygd for avstemmingsformål
 */
@ApplicationScoped
public class LoggHistoriskOverlappFPInfotrygdVLTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(LoggHistoriskOverlappFPInfotrygdVLTjeneste.class);
    private static final long HUNDRE = 100L;

    private BeregningsresultatRepository beregningsresultatRepository;
    private AktørConsumerMedCache aktørConsumer;
    private InfotrygdSVPGrunnlag infotrygdSVPGrTjeneste;
    private InfotrygdFPGrunnlag infotrygdFPGrTjeneste;
    private OverlappVedtakRepository overlappRepository;

    LoggHistoriskOverlappFPInfotrygdVLTjeneste() {
        // for CDI
    }

    @Inject
    public LoggHistoriskOverlappFPInfotrygdVLTjeneste(BeregningsresultatRepository beregningsresultatRepository,
                                                      AktørConsumerMedCache aktørConsumer,
                                                      InfotrygdSVPGrunnlag infotrygdSVPGrTjeneste,
                                                      InfotrygdFPGrunnlag infotrygdFPGrTjeneste,
                                                      OverlappVedtakRepository overlappRepository) {
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.aktørConsumer = aktørConsumer;
        this.infotrygdSVPGrTjeneste = infotrygdSVPGrTjeneste;
        this.infotrygdFPGrTjeneste = infotrygdFPGrTjeneste;
        this.overlappRepository = overlappRepository;
    }

    public void vurderOglagreEventueltOverlapp(String hendelse, OverlappData overlappData) {
        try {
            utledPerioderMedOverlapp(overlappData).stream().map(b -> b.medHendelse(hendelse)).forEach(overlappRepository::lagre);
        } catch (Exception e) {
            LOG.info("Identifisering av overlapp i Infotrygd feilet ", e);
        }
    }

    private List<OverlappVedtak.Builder> utledPerioderMedOverlapp(OverlappData overlappData) {
        List<OverlappVedtak.Builder> resultat = new ArrayList<>();

        if (FagsakYtelseType.FORELDREPENGER.equals(overlappData.getYtelseType())) {
            // Her skal vi oppdage alle ytelser som strekker seg forbi førsteDatoVL .
            LocalDate førsteDatoVL = VirkedagUtil.fomVirkedag(overlappData.getTidligsteDato());

            // Brukers FP-saker i Infotrygd
            infotrygdFpPerioderEtterDato(førsteDatoVL, overlappData.getAktørId()).getDatoIntervaller()
                .forEach(grunnlagPeriode -> resultat.add(opprettOverlappIT(overlappData, FagsakYtelseType.FORELDREPENGER.getKode(), "FOM" + førsteDatoVL, grunnlagPeriode)));
            // Annenparts FP-saker i Infotrygd
            if (RelasjonsRolleType.erMor(overlappData.getRolle()) && overlappData.getAnnenPartAktørId() != null)  {
                infotrygdFpPerioderEtterDato(førsteDatoVL, overlappData.getAnnenPartAktørId()).getDatoIntervaller()
                    .forEach(grunnlagPeriode -> resultat.add(opprettOverlappIT(overlappData, FagsakYtelseType.FORELDREPENGER.getKode(), "AnPART" + førsteDatoVL, grunnlagPeriode)));
            }
            // Brukers SVP-saker i Infotrygd
            if (RelasjonsRolleType.erMor(overlappData.getRolle())) {
                infotrygdSvpPerioderEtterDato(førsteDatoVL, overlappData.getAktørId()).getDatoIntervaller()
                    .forEach(grunnlagPeriode -> resultat.add(opprettOverlappIT(overlappData, FagsakYtelseType.SVANGERSKAPSPENGER.getKode(), "FOM" + førsteDatoVL, grunnlagPeriode)));
            }
        } else if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(overlappData.getYtelseType())) {
            // Her skal vi oppdage alle ytelser med netto overlapp - kun utbetalinger
            LocalDateTimeline<Boolean> perioderVL = hentPerioderSVP(overlappData.getBehandlingId());
            LocalDate førsteDatoVL = perioderVL.getDatoIntervaller().stream().map(LocalDateInterval::getFomDato).min(Comparator.naturalOrder()).orElse(null);
            if (førsteDatoVL != null) {
                infotrygdFpPerioderEtterDato(førsteDatoVL, overlappData.getAktørId())
                    .intersection(perioderVL, StandardCombinators::alwaysTrueForMatch).compress().getDatoIntervaller()
                    .forEach(grunnlagPeriode -> resultat.add(opprettOverlappIT(overlappData, FagsakYtelseType.FORELDREPENGER.getKode(), null, grunnlagPeriode)));
                infotrygdSvpPerioderEtterDato(førsteDatoVL, overlappData.getAktørId())
                    .intersection(perioderVL, StandardCombinators::alwaysTrueForMatch).compress().getDatoIntervaller()
                    .forEach(grunnlagPeriode -> resultat.add(opprettOverlappIT(overlappData, FagsakYtelseType.SVANGERSKAPSPENGER.getKode(), null, grunnlagPeriode)));
            }
        }
        return resultat;
    }

    private LocalDateTimeline<Boolean> infotrygdFpPerioderEtterDato(LocalDate førsteDatoVL, AktørId finnForAktørId) {
        var ident = getFnrFraAktørId(finnForAktørId);

        List<Grunnlag> infotrygdFPGrunnlag = infotrygdFPGrTjeneste.hentGrunnlag(ident.getIdent(), førsteDatoVL.minusWeeks(4), førsteDatoVL.plusYears(3));

        return finnTidslinjeFraGrunnlagene(infotrygdFPGrunnlag, førsteDatoVL);
    }

    private LocalDateTimeline<Boolean> infotrygdSvpPerioderEtterDato(LocalDate førsteDatoVL, AktørId finnForAktørId) {
        var ident = getFnrFraAktørId(finnForAktørId);

        List<Grunnlag> infotrygdSVPGrunnlag = infotrygdSVPGrTjeneste.hentGrunnlag(ident.getIdent(), førsteDatoVL.minusWeeks(4), førsteDatoVL.plusYears(3));

        return finnTidslinjeFraGrunnlagene(infotrygdSVPGrunnlag, førsteDatoVL);
    }


    private PersonIdent getFnrFraAktørId(AktørId aktørId) {
        return aktørConsumer.hentPersonIdentForAktørId(aktørId.getId()).map(PersonIdent::new).orElseThrow();
    }

    private LocalDateTimeline<Boolean> hentPerioderSVP(Long behandlingId) {
        var segments = beregningsresultatRepository.hentBeregningsresultat(behandlingId)
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
            .filter(beregningsresultatPeriode -> beregningsresultatPeriode.getDagsats() > 0)
            .map(p -> new LocalDateSegment<>(p.getBeregningsresultatPeriodeFom(), p.getBeregningsresultatPeriodeTom(), Boolean.TRUE))
            .collect(Collectors.toList());

        return new LocalDateTimeline<>(segments, StandardCombinators::alwaysTrueForMatch).compress();
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

    private OverlappVedtak.Builder opprettOverlappIT(OverlappData overlappData, String ytelse, String referanse, LocalDateInterval periodeInfotrygd) {
        return OverlappVedtak.builder()
            .medSaksnummer(overlappData.getSaksnummer())
            .medBehandlingId(overlappData.getBehandlingId())
            .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(periodeInfotrygd.getFomDato(), periodeInfotrygd.getTomDato()))
            .medFagsystem(Fagsystem.INFOTRYGD.getKode())
            .medYtelse(ytelse)
            .medReferanse(referanse)
            .medUtbetalingsprosent(HUNDRE);
    }

    private LocalDateTimeline<Boolean> helgeJusterTidslinje(LocalDateTimeline<Boolean> tidslinje) {
        var segments = tidslinje.getDatoIntervaller().stream()
            .map(p -> new LocalDateSegment<>(p.getFomDato(), VirkedagUtil.tomVirkedag(p.getTomDato()), Boolean.TRUE))
            .collect(Collectors.toList());
        return new LocalDateTimeline<>(segments, StandardCombinators::alwaysTrueForMatch).compress();
    }
}
