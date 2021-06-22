package no.nav.foreldrepenger.domene.opptjening;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.OppgittFrilans;
import no.nav.foreldrepenger.domene.iay.modell.OppgittFrilansoppdrag;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjening;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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


        List<OppgittFrilansoppdrag> relevantOppgittFrilans = finnOppgittFrilansSomKrysserStpBG(stpBG, iayGrunnlag);
        List<Yrkesaktivitet> relevantRegisterFrilans = finnFrilansIRegisterSomKrysserSTP(stpBG, iayGrunnlag, ref.getAktørId());
        if (relevantOppgittFrilans.isEmpty() && relevantRegisterFrilans.isEmpty()) {
            return;
        }
        if (relevantOppgittFrilans.isEmpty() || relevantRegisterFrilans.isEmpty()) {
            loggAvvik(ref.getSaksnummer(), relevantOppgittFrilans, relevantRegisterFrilans);
        }
    }

    private void loggAvvik(Saksnummer saksnummer,
                           List<OppgittFrilansoppdrag> relevantOppgittFrilans,
                           List<Yrkesaktivitet> relevantRegisterFrilans) {
        List<DatoIntervallEntitet> oppgittePerioder = relevantOppgittFrilans.stream()
            .map(OppgittFrilansoppdrag::getPeriode)
            .collect(Collectors.toList());

        List<DatoIntervallEntitet> registerPerioder = relevantRegisterFrilans.stream()
            .map(Yrkesaktivitet::getAlleAktivitetsAvtaler)
            .flatMap(Collection::stream)
            .filter(AktivitetsAvtale::erAnsettelsesPeriode)
            .map(AktivitetsAvtale::getPeriode)
            .collect(Collectors.toList());
        LOG.info("FP-654894: Missmatch mellom frilans fra søknad og frilans fra register på saksnr {}." +
            " Frilansperioder fra søknad var {} mens frilansperioder fra register var {}",
            saksnummer, oppgittePerioder, registerPerioder);
    }

    private List<OppgittFrilansoppdrag> finnOppgittFrilansSomKrysserStpBG(LocalDate stpBG, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        List<OppgittFrilansoppdrag> oppgittFrilans = iayGrunnlag.getOppgittOpptjening().flatMap(OppgittOpptjening::getFrilans)
            .map(OppgittFrilans::getFrilansoppdrag)
            .orElse(Collections.emptyList());

        return oppgittFrilans.stream()
            .filter(of -> of.getPeriode().inkluderer(stpBG))
            .collect(Collectors.toList());
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
