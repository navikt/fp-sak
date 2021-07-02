package no.nav.foreldrepenger.domene.opptjening;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
public class FrilansAvvikLoggTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(FrilansAvvikLoggTjeneste.class);

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;


    public FrilansAvvikLoggTjeneste() {
        // CDI
    }

    @Inject
    public FrilansAvvikLoggTjeneste(BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                    InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public void loggFrilansavvikVedBehov(BehandlingReferanse ref) {
        Optional<BeregningsgrunnlagEntitet> bg = beregningsgrunnlagRepository.hentBeregningsgrunnlagForId(ref.getBehandlingId());
        Optional<LocalDate> stpBGOpt = bg.map(BeregningsgrunnlagEntitet::getSkjæringstidspunkt);

        if (stpBGOpt.isEmpty()) {
            return;
        }
        LocalDate stpBG = stpBGOpt.get();
        InntektArbeidYtelseGrunnlag iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(ref.getBehandlingId());

        Optional<OppgittFrilans> relevantOppgittFrilans = finnOppgittFrilansFraSøknad(iayGrunnlag);

        List<Yrkesaktivitet> registerFrilansPåSTP = finnFrilansIRegisterSomKrysserSTP(stpBG, iayGrunnlag, ref.getAktørId());
        List<Yrkesaktivitet> registerFrilansSiste12MndIkkePåSTP = finnFrilansIRegisterSomErAktivtSiste12MndMenIkkePåStp(stpBG, iayGrunnlag, ref.getAktørId());
        if (!registerFrilansPåSTP.isEmpty() || !registerFrilansSiste12MndIkkePåSTP.isEmpty()) {
            loggFrilansLister(ref.getSaksnummer(), registerFrilansPåSTP, registerFrilansSiste12MndIkkePåSTP);
        }
        List<Inntektspost> inntektsposterFrilansSiste12Mnd = inntektsposterFrilansSiste12Mnd(stpBG, iayGrunnlag, ref.getAktørId());
        if (relevantOppgittFrilans.isEmpty() && registerFrilansPåSTP.isEmpty()) {
            if (!inntektsposterFrilansSiste12Mnd.isEmpty()) {
                // Har ikke frilans i hverken søknad eller register, men har hatt frilansinntekt siste 12 mnd.
                loggInntektUtenAktivFrilans(ref.getSaksnummer(), inntektsposterFrilansSiste12Mnd);
                return;
            }
            return;
        }
        if (inntektsposterFrilansSiste12Mnd.isEmpty()) {
            // Har frilans i enten søknad eller register men er uten frilansinntekt siste 12 mnd
            LOG.info("FP-654893: Saksnr {} har frilans i enten søknad / register men er uten frilansinntekt siste 12 mnd", ref.getSaksnummer().getVerdi());
            return;
        }
        if (relevantOppgittFrilans.isEmpty() || registerFrilansPåSTP.isEmpty()) {
            loggAvvik(ref.getSaksnummer(), relevantOppgittFrilans, registerFrilansPåSTP);
        }
    }

    private void loggFrilansLister(Saksnummer saksnummer,
                                   List<Yrkesaktivitet> registerFrilansPåSTP,
                                   List<Yrkesaktivitet> registerFrilansSiste12MndIkkePåSTP) {
        LOG.info("FP-654891: Saksnr {}. Antall frilansaktiviteter siste 12 mnd ikke aktivt på stp: {}.  Antall frilansaktiviteter på stp: {}",
            saksnummer.getVerdi(), registerFrilansSiste12MndIkkePåSTP.size(), registerFrilansPåSTP.size());
    }

    private List<Yrkesaktivitet> finnFrilansIRegisterSomErAktivtSiste12MndMenIkkePåStp(LocalDate stpBG, InntektArbeidYtelseGrunnlag grunnlag, AktørId aktørId) {
        YrkesaktivitetFilter filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(),
            grunnlag.getAktørArbeidFraRegister(aktørId)).før(stpBG);
        return filter.getFrilansOppdrag().stream()
            .filter(ya -> erAnsattFørDato(ya.getAlleAktivitetsAvtaler(), stpBG))
            .filter(ya -> !erAnsattPåDato(ya.getAlleAktivitetsAvtaler(), stpBG))
            .collect(Collectors.toList());

    }

    private boolean erAnsattFørDato(Collection<AktivitetsAvtale> alleAktivitetsAvtaler, LocalDate stpBG) {
        return alleAktivitetsAvtaler.stream()
            .filter(AktivitetsAvtale::erAnsettelsesPeriode)
            .anyMatch(aa -> aa.getPeriode().getFomDato().isBefore(stpBG));
    }

    private void loggInntektUtenAktivFrilans(Saksnummer saksnummer,
                                             List<Inntektspost> inntektsposterFrilansSiste12Mnd) {
        Inntektspost sisteInntektspost = inntektsposterFrilansSiste12Mnd.stream()
            .max(Comparator.comparing(ip -> ip.getPeriode().getFomDato()))
            .orElseThrow();
        Arbeidsgiver ag = sisteInntektspost.getInntekt().getArbeidsgiver();
        LOG.info("FP-654892: Saksnr {} har hatt frilansinntekt siste 12 mnd men har ikke aktivt frilansforhold på stp. " +
            "Siste inntektspost var hos {} i perioden {}", saksnummer.getVerdi(), ag, sisteInntektspost.getPeriode());
    }

    private List<Inntektspost> inntektsposterFrilansSiste12Mnd(LocalDate stpBG,
                                                               InntektArbeidYtelseGrunnlag grunnlag,
                                                               AktørId aktørId) {
        YrkesaktivitetFilter filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(),
            grunnlag.getAktørArbeidFraRegister(aktørId)).før(stpBG);
        List<Arbeidsgiver> alleFrilansarbeidsgivere = filter.getFrilansOppdrag().stream()
            .map(Yrkesaktivitet::getArbeidsgiver)
            .collect(Collectors.toList());

        InntektFilter inntektfilter = new InntektFilter(grunnlag.getAktørInntektFraRegister(aktørId));
        return inntektfilter.getAlleInntekter(InntektsKilde.INNTEKT_BEREGNING).stream()
            .filter(ip -> alleFrilansarbeidsgivere.contains(ip.getArbeidsgiver()))
            .map(Inntekt::getAlleInntektsposter)
            .filter(alleInntektsposter -> finnesInntektEtterDato(alleInntektsposter, stpBG.minusMonths(12).withDayOfMonth(1)))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    private boolean finnesInntektEtterDato(Collection<Inntektspost> alleInntektsposter, LocalDate dato) {
        return alleInntektsposter.stream().anyMatch(ip -> ip.getPeriode().getFomDato().isAfter(dato));
    }

    private void loggAvvik(Saksnummer saksnummer,
                           Optional<OppgittFrilans> relevantOppgittFrilans,
                           List<Yrkesaktivitet> relevantRegisterFrilans) {
        List<DatoIntervallEntitet> registerPerioder = relevantRegisterFrilans.stream()
            .map(Yrkesaktivitet::getAlleAktivitetsAvtaler)
            .flatMap(Collection::stream)
            .filter(AktivitetsAvtale::erAnsettelsesPeriode)
            .map(AktivitetsAvtale::getPeriode)
            .collect(Collectors.toList());
        LOG.info("FP-654894: Missmatch mellom frilans fra søknad og frilans fra register på saksnr {}." +
            " Frilans er oppgitt i søknaden: {}. Frilans på skjæringstidspunkt i register: {}",
            saksnummer.getVerdi(), relevantOppgittFrilans.isPresent(), registerPerioder);
    }

    private Optional<OppgittFrilans> finnOppgittFrilansFraSøknad(InntektArbeidYtelseGrunnlag iayGrunnlag) {
        return iayGrunnlag.getOppgittOpptjening().flatMap(OppgittOpptjening::getFrilans);
    }

    private List<Yrkesaktivitet> finnFrilansIRegisterSomKrysserSTP(LocalDate stpBG,
                                                                   InntektArbeidYtelseGrunnlag grunnlag,
                                                                   AktørId aktørId) {
        YrkesaktivitetFilter filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(),
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
