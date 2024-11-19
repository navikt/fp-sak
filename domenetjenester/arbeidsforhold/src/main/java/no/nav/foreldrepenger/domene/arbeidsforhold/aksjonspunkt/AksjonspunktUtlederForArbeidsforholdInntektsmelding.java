package no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt;

import static java.util.Collections.emptyList;
import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettListeForAksjonspunkt;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingMangelTjeneste;

@ApplicationScoped
public class AksjonspunktUtlederForArbeidsforholdInntektsmelding {
    private static final List<AksjonspunktResultat> INGEN_AKSJONSPUNKTER = emptyList();
    private static final Logger LOG = LoggerFactory.getLogger(AksjonspunktUtlederForArbeidsforholdInntektsmelding.class);

    private BehandlingRepository behandlingRepository;
    private ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste;

    AksjonspunktUtlederForArbeidsforholdInntektsmelding() {
    }

    @Inject
    public AksjonspunktUtlederForArbeidsforholdInntektsmelding(BehandlingRepository behandlingRepository,
                                                               ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.arbeidsforholdInntektsmeldingMangelTjeneste = arbeidsforholdInntektsmeldingMangelTjeneste;
    }

    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        var behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        if (SpesialBehandling.skalGrunnlagBeholdes(behandling)) {
            return INGEN_AKSJONSPUNKTER;
        }
        var uavklarteMangler = arbeidsforholdInntektsmeldingMangelTjeneste.utledUavklarteManglerPåArbeidsforholdInntektsmelding(param.getRef(), param.getSkjæringstidspunkt());
        LOG.info("Fant {} uavklarteMangler relatert til arbeid og inntektsmeldinger på saksnummer {}. Alle uavklarteMangler var: {}", uavklarteMangler.size(), param.getSaksnummer(), uavklarteMangler);
        return uavklarteMangler.isEmpty() ? INGEN_AKSJONSPUNKTER : opprettListeForAksjonspunkt(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD_INNTEKTSMELDING);
    }
}
