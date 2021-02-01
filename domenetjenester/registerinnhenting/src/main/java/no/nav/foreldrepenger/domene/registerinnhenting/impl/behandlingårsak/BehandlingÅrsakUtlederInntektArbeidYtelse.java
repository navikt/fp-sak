package no.nav.foreldrepenger.domene.registerinnhenting.impl.behandlingårsak;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;
import no.nav.foreldrepenger.domene.arbeidsforhold.AktørYtelseEndring;
import no.nav.foreldrepenger.domene.arbeidsforhold.IAYGrunnlagDiff;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
@GrunnlagRef("InntektArbeidYtelseGrunnlag")
class BehandlingÅrsakUtlederInntektArbeidYtelse implements BehandlingÅrsakUtleder {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    @Inject
    public BehandlingÅrsakUtlederInntektArbeidYtelse(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    @Override
    public Set<EndringResultatType> utledEndringsResultat(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        LocalDate skjæringstidspunkt = ref.getSkjæringstidspunkt().getUtledetSkjæringstidspunkt();
        Saksnummer saksnummer = ref.getSaksnummer();

        InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag1 = inntektArbeidYtelseTjeneste.hentGrunnlagPåId(ref.getBehandlingId(), (UUID) grunnlagId1);
        InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag2 = inntektArbeidYtelseTjeneste.hentGrunnlagPåId(ref.getBehandlingId(), (UUID) grunnlagId2);

        Set<EndringResultatType> endringResultatTyper = new HashSet<>();

        IAYGrunnlagDiff iayGrunnlagDiff = new IAYGrunnlagDiff(inntektArbeidYtelseGrunnlag1, inntektArbeidYtelseGrunnlag2);
        boolean erAktørArbeidEndret = iayGrunnlagDiff.erEndringPåAktørArbeidForAktør(skjæringstidspunkt, ref.getAktørId());
        boolean erAktørInntektEndret = iayGrunnlagDiff.erEndringPåAktørInntektForAktør(skjæringstidspunkt, ref.getAktørId());
        boolean erInntektsmeldingEndret = iayGrunnlagDiff.erEndringPåInntektsmelding();
        AktørYtelseEndring aktørYtelseEndring = iayGrunnlagDiff.endringPåAktørYtelseForAktør(saksnummer, skjæringstidspunkt, ref.getAktørId());

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
