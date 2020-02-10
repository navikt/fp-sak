package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.app;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.BekreftetPermisjon;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.Totrinnsvurdering;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto.TotrinnsArbeidsforholdDto;

@ApplicationScoped
public class TotrinnArbeidsforholdDtoTjeneste {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    protected TotrinnArbeidsforholdDtoTjeneste() {
        // for CDI proxy
    }

    @Inject
    public TotrinnArbeidsforholdDtoTjeneste(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                            ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
    }

    public List<TotrinnsArbeidsforholdDto> hentArbeidsforhold(Behandling behandling,
                                                              Totrinnsvurdering aksjonspunkt,
                                                              Optional<UUID> iayGrunnlagUuid) {

        if (aksjonspunkt.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD)) {
            Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjonOpt;

            if (iayGrunnlagUuid.isPresent()) {
                arbeidsforholdInformasjonOpt = inntektArbeidYtelseTjeneste.hentGrunnlagForGrunnlagId(behandling.getId(), iayGrunnlagUuid.get()).getArbeidsforholdInformasjon();
            } else {
                arbeidsforholdInformasjonOpt = inntektArbeidYtelseTjeneste.finnGrunnlag(behandling.getId())
                    .flatMap(InntektArbeidYtelseGrunnlag::getArbeidsforholdInformasjon);
            }

            if (arbeidsforholdInformasjonOpt.isPresent()) {
                ArbeidsforholdInformasjon arbeidsforholdInformasjon = arbeidsforholdInformasjonOpt.get();
                List<ArbeidsforholdOverstyring> overstyringer = arbeidsforholdInformasjon.getOverstyringer();
                return overstyringer.stream()
                    .map(this::lagArbeidsforholdDto)
                    .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    private TotrinnsArbeidsforholdDto lagArbeidsforholdDto(ArbeidsforholdOverstyring arbeidsforhold) {
        String ref = arbeidsforhold.getArbeidsforholdRef().getReferanse();
        ArbeidsforholdHandlingType handling = arbeidsforhold.getHandling();
        Boolean brukPermisjon = skalPermisjonBrukes(arbeidsforhold);
        if (arbeidsforhold.getArbeidsgiver().erAktørId()) {
            ArbeidsgiverOpplysninger arbeidsgiverOpplysninger = arbeidsgiverTjeneste.hent(arbeidsforhold.getArbeidsgiver());
            if (arbeidsgiverOpplysninger != null) {
                String navn = arbeidsgiverOpplysninger.getNavn();
                String fødselsdato = arbeidsgiverOpplysninger.getIdentifikator();
                return new TotrinnsArbeidsforholdDto(navn, fødselsdato, ref, handling, brukPermisjon);
            }
        }
        if (arbeidsforhold.getArbeidsgiver().getErVirksomhet()) {
            String orgnr = arbeidsforhold.getArbeidsgiver().getOrgnr();
            String navn = arbeidsgiverTjeneste.hentVirksomhet(orgnr).getNavn();
            return new TotrinnsArbeidsforholdDto(navn, orgnr, ref, handling, brukPermisjon);
        }
        throw new IllegalStateException("Klarer ikke identifisere arbeidsgiver under iverksettelse av totrinnskontroll");
    }

    private Boolean skalPermisjonBrukes(ArbeidsforholdOverstyring arbeidsforhold) {
        Optional<BekreftetPermisjon> bekreftetPermisjonOpt = arbeidsforhold.getBekreftetPermisjon();
        if (bekreftetPermisjonOpt.isPresent()) {
            BekreftetPermisjon bekreftetPermisjon = bekreftetPermisjonOpt.get();
            if (BekreftetPermisjonStatus.BRUK_PERMISJON.equals(bekreftetPermisjon.getStatus())){
                return true;
            }
            if (BekreftetPermisjonStatus.IKKE_BRUK_PERMISJON.equals(bekreftetPermisjon.getStatus()) ||
                BekreftetPermisjonStatus.UGYLDIGE_PERIODER.equals(bekreftetPermisjon.getStatus())) {
                return false;
            }
        }
        return null;
    }
}
