package no.nav.foreldrepenger.web.app.tjenester.fordeling.inntektsmelding;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingMangelTjeneste;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingStatus;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

/**
 * Vurderer, for en enkelt {@link ForespørselStatusRequest}, om fp-sak fortsatt trenger inntektsmelding fra
 * arbeidsgiveren på saken.
 * <p>
 * MIDLERTIDIG. Fjernes sammen med resten av endepunktet når ryddejobben er ferdig kjørt.
 */
@ApplicationScoped
public class ForespørselStatusVurderingTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(ForespørselStatusVurderingTjeneste.class);

    private FagsakTjeneste fagsakTjeneste;
    private BehandlingRepository behandlingRepository;
    private ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste;

    ForespørselStatusVurderingTjeneste() {
        // CDI
    }

    @Inject
    public ForespørselStatusVurderingTjeneste(FagsakTjeneste fagsakTjeneste,
                                              BehandlingRepository behandlingRepository,
                                              ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste) {
        this.fagsakTjeneste = fagsakTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.arbeidsforholdInntektsmeldingMangelTjeneste = arbeidsforholdInntektsmeldingMangelTjeneste;
    }

    public boolean vurder(ForespørselStatusRequest request) {
        var fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(new Saksnummer(request.fagsakSaksnummer()), false).orElse(null);
        if (fagsak == null) {
            LOG.info("Fant ikke fagsak for saksnummer={}, bør undersøke denne nærmere før eventuell lukking", request.fagsakSaksnummer());
            return true;
        }
        if (FagsakStatus.AVSLUTTET.equals(fagsak.getStatus())) {
            return false;
        }

        var behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId()).orElseThrow();
        var arbeidsforholdStatuser = arbeidsforholdInntektsmeldingMangelTjeneste.finnStatusForInntektsmeldingArbeidsforhold(
            BehandlingReferanse.fra(behandling));
        return trengerFortsattInntektsmelding(arbeidsforholdStatuser, request.orgnummer());
    }

    private static boolean trengerFortsattInntektsmelding(List<ArbeidsforholdInntektsmeldingStatus> statuser, String orgnummer) {
        return statuser.stream()
            .filter(status -> status.arbeidsgiver().getIdentifikator().equals(orgnummer))
            .map(ArbeidsforholdInntektsmeldingStatus::inntektsmeldingStatus)
            .anyMatch(InntektsmeldingStatus.IKKE_MOTTAT::equals);
    }
}
