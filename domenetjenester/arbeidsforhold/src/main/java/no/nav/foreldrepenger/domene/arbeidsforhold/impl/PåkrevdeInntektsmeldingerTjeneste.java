package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.VurderArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@ApplicationScoped
public class PåkrevdeInntektsmeldingerTjeneste {

    private static final Logger LOGGER = LoggerFactory.getLogger(VurderArbeidsforholdTjeneste.class);

    private InntektsmeldingRegisterTjeneste inntektsmeldingArkivTjeneste;
    private SøknadRepository søknadRepository;

    PåkrevdeInntektsmeldingerTjeneste() {
        // CDI
    }

    @Inject
    public PåkrevdeInntektsmeldingerTjeneste(InntektsmeldingRegisterTjeneste inntektsmeldingArkivTjeneste,
                                             SøknadRepository søknadRepository) {
        this.inntektsmeldingArkivTjeneste = inntektsmeldingArkivTjeneste;
        this.søknadRepository = søknadRepository;
    }

    public void leggTilArbeidsforholdHvorPåkrevdeInntektsmeldingMangler(BehandlingReferanse behandlingReferanse,
                                                                 Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result) {
        Boolean erEndringssøknad = erEndringssøknad(behandlingReferanse);
        final Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> manglendeInntektsmeldinger = inntektsmeldingArkivTjeneste
            .utledManglendeInntektsmeldingerFraGrunnlagForVurdering(behandlingReferanse, erEndringssøknad);
        if (!erEndringssøknad) {
            for (Map.Entry<Arbeidsgiver, Set<InternArbeidsforholdRef>> entry : manglendeInntektsmeldinger.entrySet()) {
                LeggTilResultat.leggTil(result, AksjonspunktÅrsak.MANGLENDE_INNTEKTSMELDING, entry.getKey(), entry.getValue());
                LOGGER.info("Mangler inntektsmelding: arbeidsgiver={}, arbeidsforholdRef={}", entry.getKey(), entry.getValue());
            }
        }
    }

    private Boolean erEndringssøknad(BehandlingReferanse referanse) {
        return søknadRepository.hentSøknadHvisEksisterer(referanse.getBehandlingId())
            .map(SøknadEntitet::erEndringssøknad)
            .orElse(false);
    }

}
