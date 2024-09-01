package no.nav.foreldrepenger.domene.registerinnhenting.impl.behandlingårsak;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
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
    public Set<EndringResultatType> utledEndringsResultat(BehandlingReferanse ref, Skjæringstidspunkt stp, Object grunnlagId1, Object grunnlagId2) {
        var skjæringstidspunkt = stp.getUtledetSkjæringstidspunkt();
        var saksnummer = ref.saksnummer();

        var inntektArbeidYtelseGrunnlag1 = inntektArbeidYtelseTjeneste.hentGrunnlagPåId(ref.behandlingId(), (UUID) grunnlagId1);
        var inntektArbeidYtelseGrunnlag2 = inntektArbeidYtelseTjeneste.hentGrunnlagPåId(ref.behandlingId(), (UUID) grunnlagId2);

        Set<EndringResultatType> endringResultatTyper = new HashSet<>();

        var iayGrunnlagDiff = new IAYGrunnlagDiff(inntektArbeidYtelseGrunnlag1, inntektArbeidYtelseGrunnlag2);
        var erAktørArbeidEndret = iayGrunnlagDiff.erEndringPåAktørArbeidForAktør(skjæringstidspunkt, ref.aktørId());
        var erAktørInntektEndret = iayGrunnlagDiff.erEndringPåAktørInntektForAktør(skjæringstidspunkt, ref.aktørId());
        var erInntektsmeldingEndret = iayGrunnlagDiff.erEndringPåInntektsmelding();
        var aktørYtelseEndring = iayGrunnlagDiff.endringPåAktørYtelseForAktør(saksnummer, skjæringstidspunkt, ref.aktørId());

        if (erAktørArbeidEndret || erAktørInntektEndret) {
            endringResultatTyper.add(EndringResultatType.REGISTEROPPLYSNING);
        }
        if (aktørYtelseEndring.erEndret()) {
            endringResultatTyper.add(EndringResultatType.OPPLYSNING_OM_YTELSER);
        }
        if (erInntektsmeldingEndret) {
            endringResultatTyper.add(EndringResultatType.INNTEKTSMELDING);
        }
        return endringResultatTyper;
    }
}
