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
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
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
                arbeidsforholdInformasjonOpt = inntektArbeidYtelseTjeneste.hentGrunnlagPåId(behandling.getId(), iayGrunnlagUuid.get()).getArbeidsforholdInformasjon();
            } else {
                arbeidsforholdInformasjonOpt = inntektArbeidYtelseTjeneste.finnGrunnlag(behandling.getId())
                    .flatMap(InntektArbeidYtelseGrunnlag::getArbeidsforholdInformasjon);
            }

            if (arbeidsforholdInformasjonOpt.isPresent()) {
                var arbeidsforholdInformasjon = arbeidsforholdInformasjonOpt.get();
                var overstyringer = arbeidsforholdInformasjon.getOverstyringer();
                return overstyringer.stream()
                    .map(this::lagArbeidsforholdDto)
                    .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    private TotrinnsArbeidsforholdDto lagArbeidsforholdDto(ArbeidsforholdOverstyring arbeidsforhold) {
        var ref = arbeidsforhold.getArbeidsforholdRef().getReferanse();
        var handling = arbeidsforhold.getHandling();
        var brukPermisjon = skalPermisjonBrukes(arbeidsforhold);
        if (OrgNummer.erKunstig(arbeidsforhold.getArbeidsgiver().getIdentifikator()) && arbeidsforhold.getArbeidsgiverNavn() != null) {
            return new TotrinnsArbeidsforholdDto(arbeidsforhold.getArbeidsgiver().getIdentifikator(), arbeidsforhold.getArbeidsgiverNavn(),
                arbeidsforhold.getArbeidsgiver().getOrgnr(), ref, handling, brukPermisjon);
        }
        if (arbeidsforhold.getArbeidsgiver().erAktørId()) {
            var arbeidsgiverOpplysninger = arbeidsgiverTjeneste.hent(arbeidsforhold.getArbeidsgiver());
            if (arbeidsgiverOpplysninger != null) {
                var navn = arbeidsgiverOpplysninger.getNavn();
                var fødselsdato = arbeidsgiverOpplysninger.getIdentifikator();
                return new TotrinnsArbeidsforholdDto(arbeidsforhold.getArbeidsgiver().getIdentifikator(), navn, fødselsdato, ref, handling, brukPermisjon);
            }
        }
        if (arbeidsforhold.getArbeidsgiver().getErVirksomhet()) {
            var orgnr = arbeidsforhold.getArbeidsgiver().getOrgnr();
            var navn = arbeidsgiverTjeneste.hentVirksomhet(orgnr).getNavn();
            return new TotrinnsArbeidsforholdDto(arbeidsforhold.getArbeidsgiver().getIdentifikator(), navn, orgnr, ref, handling, brukPermisjon);
        }
        throw new IllegalStateException("Klarer ikke identifisere arbeidsgiver under iverksettelse av totrinnskontroll");
    }

    private Boolean skalPermisjonBrukes(ArbeidsforholdOverstyring arbeidsforhold) {
        var bekreftetPermisjonOpt = arbeidsforhold.getBekreftetPermisjon();
        if (bekreftetPermisjonOpt.isPresent()) {
            var bekreftetPermisjon = bekreftetPermisjonOpt.get();
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
