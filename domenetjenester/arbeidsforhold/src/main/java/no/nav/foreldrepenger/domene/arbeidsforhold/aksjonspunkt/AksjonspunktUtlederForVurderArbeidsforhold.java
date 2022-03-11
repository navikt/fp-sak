package no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt;

import static java.util.Collections.emptyList;
import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettListeForAksjonspunkt;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.VurderArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@ApplicationScoped
public class AksjonspunktUtlederForVurderArbeidsforhold implements AksjonspunktUtleder {
    private static final List<AksjonspunktResultat> INGEN_AKSJONSPUNKTER = emptyList();

    private VurderArbeidsforholdTjeneste vurderArbeidsforholdTjeneste;

    private BehandlingRepository behandlingRepository;

    private InntektArbeidYtelseTjeneste iayTjeneste;

    AksjonspunktUtlederForVurderArbeidsforhold() {
    }

    @Inject
    public AksjonspunktUtlederForVurderArbeidsforhold(BehandlingRepository behandlingRepository,
            InntektArbeidYtelseTjeneste iayTjeneste,
            VurderArbeidsforholdTjeneste vurderArbeidsforholdTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.iayTjeneste = iayTjeneste;
        this.vurderArbeidsforholdTjeneste = vurderArbeidsforholdTjeneste;
    }

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        var behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        if (SpesialBehandling.skalGrunnlagBeholdes(behandling)) {
            return INGEN_AKSJONSPUNKTER;
        }

        var iayGrunnlag = iayTjeneste.finnGrunnlag(param.getBehandlingId());
        if (iayGrunnlag.isPresent()) {
            var vurder = hentArbeidsforholdTilVurdering(param, iayGrunnlag.get());
            if (!vurder.isEmpty()) {
                return opprettListeForAksjonspunkt(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD);
            }
        }

        if (ArbeidsforholdUtenRelevantOppgittOpptjening.erUtenRelevantOppgittOpptjening(param, iayGrunnlag)) {
            return opprettListeForAksjonspunkt(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD);
        }

        return INGEN_AKSJONSPUNKTER;
    }

    private Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> hentArbeidsforholdTilVurdering(AksjonspunktUtlederInput param,
            InntektArbeidYtelseGrunnlag iayGrunnlag) {
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> vurder;
        var taStillingTilEndringerIArbeidsforhold = skalTaStillingTilEndringerIArbeidsforhold(param.getRef());
        var sakInntektsmeldinger = taStillingTilEndringerIArbeidsforhold ? iayTjeneste.hentInntektsmeldinger(param.getSaksnummer()) : null;
        vurder = vurderArbeidsforholdTjeneste.vurder(param.getRef(), iayGrunnlag, sakInntektsmeldinger, taStillingTilEndringerIArbeidsforhold);
        return vurder;
    }

    private boolean skalTaStillingTilEndringerIArbeidsforhold(BehandlingReferanse behandlingReferanse) {
        var behandling = behandlingRepository.hentBehandling(behandlingReferanse.getBehandlingId());
        return !Objects.equals(behandlingReferanse.getBehandlingType(), BehandlingType.FØRSTEGANGSSØKNAD) || behandling.harSattStartpunkt();
    }
}
