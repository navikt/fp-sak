package no.nav.foreldrepenger.domene.registerinnhenting.impl.behandlingårsak;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;
import no.nav.foreldrepenger.domene.arbeidsforhold.IAYGrunnlagDiff;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;

@ApplicationScoped
@GrunnlagRef(GrunnlagRef.IAY_GRUNNLAG)
class BehandlingÅrsakUtlederInntektArbeidYtelse implements BehandlingÅrsakUtleder {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    @Inject
    public BehandlingÅrsakUtlederInntektArbeidYtelse(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    @Override
    public Set<EndringResultatType> utledEndringsResultat(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        var skjæringstidspunkt = ref.getSkjæringstidspunkt().getUtledetSkjæringstidspunkt();
        var saksnummer = ref.getSaksnummer();

        var inntektArbeidYtelseGrunnlag1 = inntektArbeidYtelseTjeneste.hentGrunnlagPåId(ref.getBehandlingId(), (UUID) grunnlagId1);
        var inntektArbeidYtelseGrunnlag2 = inntektArbeidYtelseTjeneste.hentGrunnlagPåId(ref.getBehandlingId(), (UUID) grunnlagId2);

        Set<EndringResultatType> endringResultatTyper = new HashSet<>();

        var iayGrunnlagDiff = new IAYGrunnlagDiff(inntektArbeidYtelseGrunnlag1, inntektArbeidYtelseGrunnlag2);
        var erAktørArbeidEndret = iayGrunnlagDiff.erEndringPåAktørArbeidForAktør(skjæringstidspunkt, ref.getAktørId());
        var erAktørInntektEndret = iayGrunnlagDiff.erEndringPåAktørInntektForAktør(skjæringstidspunkt, ref.getAktørId());
        var erInntektsmeldingEndret = iayGrunnlagDiff.erEndringPåInntektsmelding();
        var aktørYtelseEndring = iayGrunnlagDiff.endringPåAktørYtelseForAktør(saksnummer, skjæringstidspunkt, ref.getAktørId());

        if (erAktørArbeidEndret || erAktørInntektEndret) {
            endringResultatTyper.add(EndringResultatType.REGISTEROPPLYSNING);
        }
        if (aktørYtelseEndring.erEndret()) {
            endringResultatTyper.add(EndringResultatType.REGISTEROPPLYSNING);
            endringResultatTyper.add(EndringResultatType.OPPLYSNING_OM_YTELSER);
        }
        if (erInntektsmeldingEndret) {
            endringResultatTyper.add(EndringResultatType.INNTEKTSMELDING);
        }
        return endringResultatTyper;
    }
}
