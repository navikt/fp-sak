package no.nav.foreldrepenger.domene.opptjening;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.Inntekt;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.Inntektspost;
import no.nav.foreldrepenger.domene.iay.modell.OppgittFrilans;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjening;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class FrilansAvvikLoggTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(FrilansAvvikLoggTjeneste.class);

    private BeregningTjeneste beregningTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;


    public FrilansAvvikLoggTjeneste() {
        // CDI
    }

    @Inject
    public FrilansAvvikLoggTjeneste(BeregningTjeneste beregningTjeneste,
                                    InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.beregningTjeneste = beregningTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public void loggFrilansavvikVedBehov(BehandlingReferanse ref) {
        var gr = beregningTjeneste.hent(ref.behandlingId());
        var stpBGOpt = gr.flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag).map(Beregningsgrunnlag::getSkjæringstidspunkt);

        if (stpBGOpt.isEmpty()) {
            return;
        }
        var stpBG = stpBGOpt.get();

        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(ref.behandlingId());
        var relevantOppgittFrilans = finnOppgittFrilansFraSøknad(iayGrunnlag);

        var frilansPåSTP = finnFrilansIRegisterSomKrysserSTP(stpBG, iayGrunnlag, ref.aktørId());
        var arbeidsgivereMedInntektFørSTP = finnArbeidsgivereMedInntekterSiste3Mnd(stpBG, iayGrunnlag, ref.aktørId());
        var frilansaktiviteterPåSTPMedInntektSiste3Mnd = frilansPåSTP.stream()
            .filter(ya -> arbeidsgivereMedInntektFørSTP.contains(ya.getArbeidsgiver()))
            .collect(Collectors.toList());

        if (relevantOppgittFrilans.isEmpty()) {
            frilansaktiviteterPåSTPMedInntektSiste3Mnd.forEach(fl -> {
                LOG.info("FP-654895: Saksnr {}. Ikke oppgitt frilans i søknad, men arbeidsgiver {} har gitt utbetaling som frilans siste 3 mnd",
                    ref.saksnummer().getVerdi(), fl.getArbeidsgiver().toString());
            });
        }
        else if (frilansaktiviteterPåSTPMedInntektSiste3Mnd.isEmpty()){
            LOG.info("FP-654896: Saksnr {}. Oppgitt frilans i søknad, men ingen utbetalinger som frilans siste 3 mnd",
                ref.saksnummer().getVerdi());

            // Ingen aktiv inntekt på stp, logg alder på frilansforholdene som er åpne på stp
            frilansPåSTP.forEach(fl -> {
                var startdato = finnStartdato(fl, stpBG);
                startdato.ifPresent(dato -> {
                    LOG.info("FP-654897: Saksnr {}. Oppgitt frilans i søknad uten inntekt siste periode før stp. " +
                            "Åpent frilansforhold hos {} som er {} måneder gammelt (startet {})",
                        ref.saksnummer().getVerdi(), fl.getArbeidsgiver().toString(), alderIMnd(dato), dato);
                });
            });
        }
    }

    private long alderIMnd(LocalDate dato) {
        return ChronoUnit.MONTHS.between(
            YearMonth.from(dato),
            YearMonth.from(LocalDate.now()));
    }

    private Optional<LocalDate> finnStartdato(Yrkesaktivitet fl, LocalDate stpBG) {
        return fl.getAlleAktivitetsAvtaler().stream()
            .filter(AktivitetsAvtale::erAnsettelsesPeriode)
            .filter(aa -> aa.getPeriode().inkluderer(stpBG)).map(aa -> aa.getPeriode().getFomDato())
            .min(Comparator.naturalOrder());
    }

    private List<Arbeidsgiver> finnArbeidsgivereMedInntekterSiste3Mnd(LocalDate stpBG,
                                                                      InntektArbeidYtelseGrunnlag grunnlag,
                                                                      AktørId aktørId) {
        var datoViSjekkerInntektFra = LocalDate.now().isBefore(stpBG.minusWeeks(2))
            ? stpBG.minusMonths(4).withDayOfMonth(1)
            : stpBG.minusMonths(3).withDayOfMonth(1);
        var inntektfilter = new InntektFilter(grunnlag.getAktørInntektFraRegister(aktørId));
        return inntektfilter.getAlleInntekter(InntektsKilde.INNTEKT_BEREGNING).stream()
            .filter(innt -> innt.getArbeidsgiver() != null)
            .filter(innt -> finnesInntektEtterDato(innt.getAlleInntektsposter(), datoViSjekkerInntektFra))
            .map(Inntekt::getArbeidsgiver)
            .collect(Collectors.toList());
    }

    private boolean finnesInntektEtterDato(Collection<Inntektspost> alleInntektsposter, LocalDate dato) {
        return alleInntektsposter.stream().anyMatch(ip -> !ip.getPeriode().getFomDato().isBefore(dato));
    }

    private Optional<OppgittFrilans> finnOppgittFrilansFraSøknad(InntektArbeidYtelseGrunnlag iayGrunnlag) {
        return iayGrunnlag.getOppgittOpptjening().flatMap(OppgittOpptjening::getFrilans);
    }

    private List<Yrkesaktivitet> finnFrilansIRegisterSomKrysserSTP(LocalDate stpBG,
                                                                   InntektArbeidYtelseGrunnlag grunnlag,
                                                                   AktørId aktørId) {
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(),
            grunnlag.getAktørArbeidFraRegister(aktørId)).før(stpBG);
        return filter.getFrilansOppdrag().stream()
            .filter(ya -> erAnsattPåDato(ya.getAlleAktivitetsAvtaler(), stpBG))
            .collect(Collectors.toList());
    }

    private boolean erAnsattPåDato(Collection<AktivitetsAvtale> alleAktivitetsAvtaler, LocalDate stpBG) {
        return alleAktivitetsAvtaler.stream()
            .filter(AktivitetsAvtale::erAnsettelsesPeriode)
            .anyMatch(aa -> aa.getPeriode().inkluderer(stpBG));
    }
}
