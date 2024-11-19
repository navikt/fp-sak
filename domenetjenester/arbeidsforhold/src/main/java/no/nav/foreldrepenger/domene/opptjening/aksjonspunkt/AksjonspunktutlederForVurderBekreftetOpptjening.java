package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import static java.util.Collections.emptyList;
import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.JA;
import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.NEI;
import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettListeForAksjonspunkt;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.VURDER_PERIODER_MED_OPPTJENING;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Organisasjonstype;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;

@ApplicationScoped
public class AksjonspunktutlederForVurderBekreftetOpptjening {

    private static final List<AksjonspunktResultat> INGEN_AKSJONSPUNKTER = emptyList();
    private static final Logger LOG = LoggerFactory.getLogger(AksjonspunktutlederForVurderBekreftetOpptjening.class);
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

    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        var behandlingId = param.getBehandlingId();
        var inntektArbeidYtelseGrunnlagOptional = iayTjeneste.finnGrunnlag(behandlingId);
        var fastsattOpptjeningOptional = opptjeningRepository.finnOpptjening(behandlingId);
        if (inntektArbeidYtelseGrunnlagOptional.isEmpty() || fastsattOpptjeningOptional.isEmpty()) {
            return INGEN_AKSJONSPUNKTER;
        }
        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseGrunnlagOptional.get();
        var opptjeningPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(fastsattOpptjeningOptional.get().getFom(),
                fastsattOpptjeningOptional.get().getTom());

        var skjæringstidspunkt = param.getSkjæringstidspunkt().getUtledetSkjæringstidspunkt();
        if (finnesDetArbeidsforholdMedStillingsprosentLik0(param.getAktørId(), inntektArbeidYtelseGrunnlag, opptjeningPeriode,
                skjæringstidspunkt) == JA) {
            LOG.info("Utleder AP 5051 fra stillingsprosent 0: behandlingId={}", behandlingId);
            return opprettListeForAksjonspunkt(VURDER_PERIODER_MED_OPPTJENING);
        }

        if (finnesDetArbeidsforholdLagtTilAvSaksbehandler(param.getRef(), inntektArbeidYtelseGrunnlag, skjæringstidspunkt) == JA) {
            LOG.info("Utleder AP 5051 fra arbeidsforhold lagt til av saksbehandler: behandlingId={}", behandlingId);
            return opprettListeForAksjonspunkt(VURDER_PERIODER_MED_OPPTJENING);

        }
        return INGEN_AKSJONSPUNKTER;
    }

    private Utfall finnesDetArbeidsforholdLagtTilAvSaksbehandler(BehandlingReferanse referanse, InntektArbeidYtelseGrunnlag grunnlag,
            LocalDate skjæringstidspunkt) {
        var aktørId = referanse.aktørId();
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(aktørId))
                .før(skjæringstidspunkt);

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

        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(aktørId))
                .før(skjæringstidspunkt);

        var yrkesaktiviteter = filter.getYrkesaktiviteter();
        if (!yrkesaktiviteter.isEmpty()) {
            for (var yrkesaktivitet : yrkesaktiviteter.stream()
                    .filter(it -> ArbeidType.AA_REGISTER_TYPER.contains(it.getArbeidType())).toList()) {
                if (girAksjonspunkt(filter, opptjeningPeriode, yrkesaktivitet)) {
                    return JA;
                }
            }
        }
        return NEI;
    }

    // Denne kalles bare dersom det ikke er nok annen opptjening. Trenger ikke se på perioder av 0% og N% i samme yrkesaktivitet
    private boolean girAksjonspunkt(YrkesaktivitetFilter filter, DatoIntervallEntitet opptjeningPeriode, Yrkesaktivitet yrkesaktivitet) {
        if (filter.getAnsettelsesPerioder(yrkesaktivitet).stream().noneMatch(asp -> opptjeningPeriode.overlapper(asp.getPeriode()))) {
            return false;
        }
        if (yrkesaktivitet.getArbeidsgiver().getErVirksomhet() && Organisasjonstype.erKunstig(yrkesaktivitet.getArbeidsgiver().getOrgnr())) {
            return true;
        }
        return filter.getAktivitetsAvtalerForArbeid(yrkesaktivitet).stream()
            .anyMatch(aa -> girAksjonspunktForAktivitetsavtale(opptjeningPeriode, aa));
    }

    private boolean girAksjonspunktForAktivitetsavtale(DatoIntervallEntitet opptjeningPeriode, AktivitetsAvtale aktivitetsAvtale) {
        return (aktivitetsAvtale.getProsentsats() == null || aktivitetsAvtale.getProsentsats().getVerdi().compareTo(BigDecimal.ZERO) == 0)
            && opptjeningPeriode.overlapper(aktivitetsAvtale.getPeriode());
    }

    // Ansettelsesperioder skal være knekt opp iht 0%-perioder slik at de overlapper helt eller ikke overlapper.
    boolean girAksjonspunktForAnsettelsesperiode(YrkesaktivitetFilter filter, Long behandlingId, Yrkesaktivitet registerAktivitet,
                                                 Yrkesaktivitet overstyrtAktivitet, AktivitetsAvtale ansettelsesPeriode) {
        if (overstyrtAktivitet != null && overstyrtAktivitet.getArbeidsgiver() != null && OrgNummer.erKunstig(overstyrtAktivitet.getArbeidsgiver().getOrgnr())) {
            return true;
        }
        var opptjening = opptjeningRepository.finnOpptjening(behandlingId);
        if (opptjening.isEmpty() || registerAktivitet == null) {
            return false;
        }
        var opptjeningPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(opptjening.get().getFom(), opptjening.get().getTom());
        if (!opptjeningPeriode.overlapper(ansettelsesPeriode.getPeriode())) {
            return false;
        }
        if (registerAktivitet.getArbeidsgiver().getErVirksomhet() && Organisasjonstype.erKunstig(registerAktivitet.getArbeidsgiver().getOrgnr())) {
            return true;
        }
        return filter.getAktivitetsAvtalerForArbeid(registerAktivitet).stream()
            .anyMatch(p -> p.getPeriode().overlapper(ansettelsesPeriode.getPeriode()) && girAksjonspunktForAktivitetsavtale(opptjeningPeriode, p));
    }
}
