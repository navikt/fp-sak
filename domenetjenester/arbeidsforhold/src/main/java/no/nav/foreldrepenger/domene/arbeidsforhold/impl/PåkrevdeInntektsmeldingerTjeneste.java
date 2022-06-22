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

@ApplicationScoped
public class PåkrevdeInntektsmeldingerTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(PåkrevdeInntektsmeldingerTjeneste.class);

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
        var erEndringssøknad = erEndringssøknad(behandlingReferanse);
        final var manglendeInntektsmeldinger = inntektsmeldingArkivTjeneste
                .utledManglendeInntektsmeldingerFraGrunnlag(behandlingReferanse, erEndringssøknad);
        if (!erEndringssøknad) {
            for (var entry : manglendeInntektsmeldinger.entrySet()) {
                LeggTilResultat.leggTil(result, AksjonspunktÅrsak.MANGLENDE_INNTEKTSMELDING, entry.getKey(), entry.getValue());
                LOG.info("Mangler inntektsmelding: arbeidsgiver={}, arbeidsforholdRef={}", entry.getKey(), entry.getValue());
            }
        }
    }

    private Boolean erEndringssøknad(BehandlingReferanse referanse) {
        return søknadRepository.hentSøknadHvisEksisterer(referanse.behandlingId())
                .map(SøknadEntitet::erEndringssøknad)
                .orElse(false);
    }

}
