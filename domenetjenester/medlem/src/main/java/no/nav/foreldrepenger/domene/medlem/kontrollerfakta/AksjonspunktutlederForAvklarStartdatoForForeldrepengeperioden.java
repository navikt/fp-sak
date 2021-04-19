package no.nav.foreldrepenger.domene.medlem.kontrollerfakta;

import static java.util.Collections.emptyList;
import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.JA;
import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.NEI;
import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettListeForAksjonspunkt;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

@ApplicationScoped
public class AksjonspunktutlederForAvklarStartdatoForForeldrepengeperioden implements AksjonspunktUtleder {

    private static final List<AksjonspunktResultat> INGEN_AKSJONSPUNKTER = emptyList();
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    AksjonspunktutlederForAvklarStartdatoForForeldrepengeperioden() {
    }

    @Inject
    AksjonspunktutlederForAvklarStartdatoForForeldrepengeperioden(InntektArbeidYtelseTjeneste iayTjeneste,
                                                                  YtelsesFordelingRepository ytelsesFordelingRepository) {
        this.iayTjeneste = iayTjeneste;
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
    }

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        if (BehandlingType.REVURDERING.equals(param.getBehandlingType())) {
            return INGEN_AKSJONSPUNKTER;
        }

        var behandlingId = param.getBehandlingId();
        var inntektArbeidYtelseGrunnlag = iayTjeneste.finnGrunnlag(behandlingId).orElse(null);
        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId).orElse(null);

        if (ytelseFordelingAggregat == null || inntektArbeidYtelseGrunnlag == null) {
            return INGEN_AKSJONSPUNKTER;
        }
        var inntektsmeldinger = inntektArbeidYtelseGrunnlag.getInntektsmeldinger().orElse(null);
        var harAlleredeAvklartStartDato = ytelseFordelingAggregat.getAvklarteDatoer().map(AvklarteUttakDatoerEntitet::getFørsteUttaksdato).isPresent();
        if (inntektsmeldinger == null || harAlleredeAvklartStartDato) {
            return INGEN_AKSJONSPUNKTER;
        }

        var skjæringstidspunkt = param.getSkjæringstidspunkt().getFørsteUttaksdatoFødseljustert();

        var filter = new YrkesaktivitetFilter(inntektArbeidYtelseGrunnlag.getArbeidsforholdInformasjon(), inntektArbeidYtelseGrunnlag.getAktørArbeidFraRegister(param.getAktørId()))
            .før(skjæringstidspunkt);

        if (filter.getYrkesaktiviteter().isEmpty()) {
            return INGEN_AKSJONSPUNKTER;
        }

        var startdatoOppgittAvBruker = skjæringstidspunkt;

        if (samsvarerStartdatoerFraInntektsmeldingOgBruker(startdatoOppgittAvBruker, inntektsmeldinger) == NEI) {
            if (erMinstEttArbeidsforholdLøpende(filter, skjæringstidspunkt) == JA) {
                if (samsvarerAlleLøpendeArbeidsforholdMedStartdatoFraBruker(filter, inntektsmeldinger, startdatoOppgittAvBruker) == NEI) {
                    return opprettListeForAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_STARTDATO_FOR_FORELDREPENGEPERIODEN);
                }
            }
        }

        return INGEN_AKSJONSPUNKTER;
    }

    private Utfall samsvarerAlleLøpendeArbeidsforholdMedStartdatoFraBruker(YrkesaktivitetFilter filter, InntektsmeldingAggregat inntektsmeldingAggregat,
                                                                           LocalDate startdatoOppgittAvBruker) {
        return filter.getYrkesaktiviteter()
            .stream()
            .filter(Yrkesaktivitet::erArbeidsforhold)
            .anyMatch(yrkesaktivitet -> samsvarerIkkeMellomLøpendeArbeidsforholdOgStartdatoFrabruker(filter.getAnsettelsesPerioder(yrkesaktivitet),
                inntektsmeldingAggregat, startdatoOppgittAvBruker, yrkesaktivitet)) ? NEI : JA;
    }

    private boolean samsvarerIkkeMellomLøpendeArbeidsforholdOgStartdatoFrabruker(Collection<AktivitetsAvtale> aktivitetsAvtaler,
                                                                                 InntektsmeldingAggregat inntektsmeldingAggregat,
                                                                                 LocalDate startdatoOppgittAvBruker,
                                                                                 Yrkesaktivitet yrkesaktivitet) {
        var løpendeAnsettelse = aktivitetsAvtaler.stream()
            .map(AktivitetsAvtale::getPeriode)
            .map(DatoIntervallEntitet::getTomDato)
            .anyMatch(startdatoOppgittAvBruker::isBefore);
        return løpendeAnsettelse && inntektsmeldingAggregat.getInntektsmeldingerFor(yrkesaktivitet.getArbeidsgiver()).stream()
                    .anyMatch(inntektsmelding -> !samsvarerOppgittOgInntektsmeldingDato(startdatoOppgittAvBruker, inntektsmelding));
    }

    private boolean samsvarerOppgittOgInntektsmeldingDato(LocalDate startdatoOppgittAvBruker, Inntektsmelding inntektsmelding) {
        return endreDatoHvisLørdagEllerSøndag(inntektsmelding.getStartDatoPermisjon().orElseThrow())
            .equals(endreDatoHvisLørdagEllerSøndag(startdatoOppgittAvBruker));
    }

    Utfall erMinstEttArbeidsforholdLøpende(YrkesaktivitetFilter filter, LocalDate skjæringstidspunkt) {
        var minstEttLøpende = filter.getYrkesaktiviteter().stream()
            .filter(Yrkesaktivitet::erArbeidsforhold)
            .map(filter::getAnsettelsesPerioder)
            .flatMap(Collection::stream)
            .map(AktivitetsAvtale::getPeriode)
            .map(DatoIntervallEntitet::getTomDato)
            .anyMatch(skjæringstidspunkt::isBefore);
        return minstEttLøpende ? JA : NEI;
    }

    Utfall samsvarerStartdatoerFraInntektsmeldingOgBruker(LocalDate startdatoOppgittAvBruker, InntektsmeldingAggregat inntektsmeldingAggregat) {
        return inntektsmeldingAggregat.getInntektsmeldingerSomSkalBrukes().stream()
            .anyMatch(im -> !samsvarerOppgittOgInntektsmeldingDato(startdatoOppgittAvBruker, im)) ? NEI : JA;

    }

    LocalDate endreDatoHvisLørdagEllerSøndag(LocalDate dato) {
        if (dato.getDayOfWeek().equals(DayOfWeek.SATURDAY)) {
            return dato.plusDays(2L);
        }
        if (dato.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
            return dato.plusDays(1L);
        }
        return dato;
    }

}
