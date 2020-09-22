package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import static java.util.Collections.emptyList;
import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.JA;
import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.NEI;
import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettListeForAksjonspunkt;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Organisasjonstype;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.Inntektspost;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;

@ApplicationScoped
public class AksjonspunktutlederForVurderBekreftetOpptjening implements AksjonspunktUtleder {

    private static final List<AksjonspunktResultat> INGEN_AKSJONSPUNKTER = emptyList();
    private static final Logger logger = LoggerFactory.getLogger(AksjonspunktutlederForVurderBekreftetOpptjening.class);
    private OpptjeningRepository opptjeningRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    AksjonspunktutlederForVurderBekreftetOpptjening() {
        // CDI
    }

    @Inject
    public AksjonspunktutlederForVurderBekreftetOpptjening(OpptjeningRepository opptjeningRepository,
                                                           InntektArbeidYtelseTjeneste iayTjeneste) {
        this.iayTjeneste = iayTjeneste;
        this.opptjeningRepository = opptjeningRepository;
    }

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        Long behandlingId = param.getBehandlingId();
        Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlagOptional = iayTjeneste.finnGrunnlag(behandlingId);
        Optional<Opptjening> fastsattOpptjeningOptional = opptjeningRepository.finnOpptjening(behandlingId);
        if (!inntektArbeidYtelseGrunnlagOptional.isPresent() || !fastsattOpptjeningOptional.isPresent()) {
            return INGEN_AKSJONSPUNKTER;
        }
        InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag = inntektArbeidYtelseGrunnlagOptional.get();
        DatoIntervallEntitet opptjeningPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(fastsattOpptjeningOptional.get().getFom(),
            fastsattOpptjeningOptional.get().getTom());

        LocalDate skjæringstidspunkt = param.getSkjæringstidspunkt().getUtledetSkjæringstidspunkt();
        if (finnesDetArbeidsforholdMedStillingsprosentLik0(param.getAktørId(), inntektArbeidYtelseGrunnlag, opptjeningPeriode, skjæringstidspunkt) == JA) {
            logger.info("Utleder AP 5051 fra stillingsprosent 0: behandlingId={}", behandlingId);
            return opprettListeForAksjonspunkt(VURDER_PERIODER_MED_OPPTJENING);
        }

        if (finnesDetBekreftetFrilans(param.getAktørId(), inntektArbeidYtelseGrunnlag, opptjeningPeriode, skjæringstidspunkt) == JA) {
            logger.info("Utleder AP 5051 fra bekreftet frilans: behandlingId={}", behandlingId);
            return opprettListeForAksjonspunkt(VURDER_PERIODER_MED_OPPTJENING);
        }

        if (finnesDetArbeidsforholdLagtTilAvSaksbehandler(param.getRef(), inntektArbeidYtelseGrunnlag, skjæringstidspunkt) == JA) {
            logger.info("Utleder AP 5051 fra arbeidsforhold lagt til av saksbehandler: behandlingId={}", behandlingId);
            return opprettListeForAksjonspunkt(VURDER_PERIODER_MED_OPPTJENING);

        }
        return INGEN_AKSJONSPUNKTER;
    }

    private Utfall finnesDetArbeidsforholdLagtTilAvSaksbehandler(BehandlingReferanse referanse, InntektArbeidYtelseGrunnlag grunnlag,
                                                                 LocalDate skjæringstidspunkt) {
        AktørId aktørId = referanse.getAktørId();
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(aktørId)).før(skjæringstidspunkt);

        var yrkesaktiviteter = filter.getYrkesaktiviteter();
        if (!yrkesaktiviteter.isEmpty()) {
            return yrkesaktiviteter
                .stream()
                .anyMatch(ya -> {
                    var arbeidsgiver = ya.getArbeidsgiver();
                    return arbeidsgiver.getErVirksomhet() && Organisasjonstype.erKunstig(arbeidsgiver.getOrgnr());
                })
                    ? JA
                    : NEI;
        }
        return NEI;
    }

    private Utfall finnesDetArbeidsforholdMedStillingsprosentLik0(AktørId aktørId, InntektArbeidYtelseGrunnlag grunnlag,
                                                                  DatoIntervallEntitet opptjeningPeriode, LocalDate skjæringstidspunkt) {

        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(aktørId)).før(skjæringstidspunkt);

        var yrkesaktiviteter = filter.getYrkesaktiviteter();
        if (!yrkesaktiviteter.isEmpty()) {
            for (Yrkesaktivitet yrkesaktivitet : yrkesaktiviteter.stream()
                .filter(it -> ArbeidType.AA_REGISTER_TYPER.contains(it.getArbeidType())).collect(Collectors.toList())) {
                if (girAksjonspunkt(filter, opptjeningPeriode, yrkesaktivitet)) {
                    return JA;
                }
            }
        }
        return NEI;
    }

    private boolean girAksjonspunkt(YrkesaktivitetFilter filter, DatoIntervallEntitet opptjeningPeriode, Yrkesaktivitet yrkesaktivitet) {
        if (filter.getAnsettelsesPerioder(yrkesaktivitet).stream().noneMatch(asp -> opptjeningPeriode.overlapper(asp.getPeriode()))) {
            return false;
        }
        var avtaler = filter.getAktivitetsAvtalerForArbeid(yrkesaktivitet);
        for (var aktivitetsAvtale : avtaler) {
            if ((aktivitetsAvtale.getProsentsats() == null || aktivitetsAvtale.getProsentsats().getVerdi().compareTo(BigDecimal.ZERO) == 0)
                && opptjeningPeriode.overlapper(aktivitetsAvtale.getPeriode())) {
                return true;
            }
        }
        return yrkesaktivitet.getArbeidsgiver().getErVirksomhet() && Organisasjonstype.erKunstig(yrkesaktivitet.getArbeidsgiver().getOrgnr());
    }

    private Utfall finnesDetBekreftetFrilans(AktørId aktørId, InntektArbeidYtelseGrunnlag grunnlag, DatoIntervallEntitet opptjeningPeriode,
                                             LocalDate skjæringstidspunkt) {

        var filterInntekt = grunnlag.getAktørInntektFraRegister(aktørId).map(ai -> new InntektFilter(ai).før(skjæringstidspunkt).filterPensjonsgivende()).orElse(InntektFilter.EMPTY);

        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(aktørId)).før(skjæringstidspunkt);
        for (Yrkesaktivitet yrkesaktivitet : filter.getFrilansOppdrag()) {
            if (harFrilansavtaleForPeriode(yrkesaktivitet, opptjeningPeriode)
                && harInntektFraVirksomhetForPeriode(filterInntekt, yrkesaktivitet, opptjeningPeriode)) {
                return JA;
            }
        }
        return NEI;
    }

    private boolean harFrilansavtaleForPeriode(Yrkesaktivitet frilans, DatoIntervallEntitet opptjeningsPeriode) {
        return new YrkesaktivitetFilter(null, List.of(frilans)).getAktivitetsAvtalerForArbeid().stream()
            .anyMatch(aa -> opptjeningsPeriode.overlapper(aa.getPeriode()));
    }

    private boolean harInntektFraVirksomhetForPeriode(InntektFilter filterInntekt, Yrkesaktivitet yrkesaktivitet, DatoIntervallEntitet opptjeningsPeriode) {
        return filterInntekt.filterPensjonsgivende()
            .filter(yrkesaktivitet.getArbeidsgiver())
            .getFiltrertInntektsposter().stream()
            .anyMatch(i -> harInntektpostForPeriode(i, opptjeningsPeriode));
    }

    private boolean harInntektpostForPeriode(Inntektspost ip, DatoIntervallEntitet opptjeningsPeriode) {
        return opptjeningsPeriode.overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(ip.getPeriode().getFomDato(), ip.getPeriode().getTomDato()));
    }

    boolean girAksjonspunktForArbeidsforhold(YrkesaktivitetFilter filter, Long behandlingId, Yrkesaktivitet registerAktivitet, Yrkesaktivitet overstyrtAktivitet) {
        if (overstyrtAktivitet != null && overstyrtAktivitet.getArbeidsgiver() != null && OrgNummer.erKunstig(overstyrtAktivitet.getArbeidsgiver().getOrgnr())) {
            return true;
        }
        final Optional<Opptjening> opptjening = opptjeningRepository.finnOpptjening(behandlingId);
        if (opptjening.isEmpty() || registerAktivitet == null) {
            return false;
        }
        final DatoIntervallEntitet opptjeningPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(opptjening.get().getFom(), opptjening.get().getTom());
        return girAksjonspunkt(filter, opptjeningPeriode, registerAktivitet);
    }
}
