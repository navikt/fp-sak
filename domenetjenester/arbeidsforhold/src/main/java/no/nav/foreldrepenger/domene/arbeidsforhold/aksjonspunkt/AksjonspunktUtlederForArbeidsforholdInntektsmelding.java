package no.nav.foreldrepenger.domene.arbeidsforhold.aksjonspunkt;

import static java.util.Collections.emptyList;
import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettListeForAksjonspunkt;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;

import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdValg;

import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdMangel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingMangelTjeneste;

@ApplicationScoped
public class AksjonspunktUtlederForArbeidsforholdInntektsmelding implements AksjonspunktUtleder {
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

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        var behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        if (SpesialBehandling.skalGrunnlagBeholdes(behandling)) {
            return INGEN_AKSJONSPUNKTER;
        }
        var mangler = arbeidsforholdInntektsmeldingMangelTjeneste.utledManglerPåArbeidsforholdInntektsmelding(param.getRef());
        LOG.info("Fant {} mangler relatert til arbeid og inntektsmeldinger på saksnummer {}. Alle mangler var: {}", mangler.size(), param.getSaksnummer(), mangler);
        if (behandling.erRevurdering() && !mangler.isEmpty()) {
            var avklarteValg = arbeidsforholdInntektsmeldingMangelTjeneste.hentArbeidsforholdValgForSak(BehandlingReferanse.fra(behandling));
            var alleredeAvklarteMangler = mangler.stream()
                .filter(mangel -> avklarteValg.stream()
                    .anyMatch(valg -> valg.getArbeidsgiver().equals(mangel.arbeidsgiver()) && valg.getArbeidsforholdRef().gjelderFor(mangel.ref())))
                .toList();
            var alleManglerHarEksisterendeAvklaring = alleredeAvklarteMangler.size() == mangler.size()
                && alleredeAvklarteMangler.containsAll(mangler);
            LOG.info("Fant {} eksisterende arbeid-inntekt avklaringer på saksnummer {}. Alle avklaringer var: {}, sjekker om alle mangler har en eksisterende avklaring: {}", avklarteValg.size(), param.getSaksnummer(),
                avklarteValg, alleManglerHarEksisterendeAvklaring);
            return alleManglerHarEksisterendeAvklaring ? INGEN_AKSJONSPUNKTER : opprettListeForAksjonspunkt(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD_INNTEKTSMELDING);

        }
        return mangler.isEmpty() ? INGEN_AKSJONSPUNKTER : opprettListeForAksjonspunkt(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD_INNTEKTSMELDING);
    }
}
