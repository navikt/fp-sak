package no.nav.foreldrepenger.web.app.tjenester.fordeling.inntektsmelding;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingMangelTjeneste;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingStatus;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.tjenester.fordeling.inntektsmelding.ForespørselStatusRequest.Forespørsel;
import no.nav.foreldrepenger.web.app.tjenester.fordeling.inntektsmelding.ForespørselStatusResponse.Årsak;

/**
 * Vurderer, per element i {@link ForespørselStatusRequest}, om fp-sak fortsatt trenger inntektsmelding
 * fra arbeidsgiveren på saken. Se {@link ForespørselStatusRequest} for bakgrunn.
 *
 * MIDLERTIDIG. Skal fjernes sammen med resten av batch-endepunktet når ryddejobben i fp-inntektsmelding
 * er ferdig kjørt.
 */
@ApplicationScoped
public class ForespørselStatusVurderingTjeneste {

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

    public List<ForespørselStatusResponse> vurder(ForespørselStatusRequest request) {
        return request.forespørsler().stream()
            .collect(Collectors.groupingBy(Forespørsel::fagsakSaksnummer))
            .entrySet().stream()
            .flatMap(saksnummerOgForespørsler -> vurderForSaksnummer(saksnummerOgForespørsler.getKey(), saksnummerOgForespørsler.getValue()).stream())
            .toList();
    }

    private List<ForespørselStatusResponse> vurderForSaksnummer(String saksnummerVerdi, List<Forespørsel> forespørslerForSak) {
        var fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(new Saksnummer(saksnummerVerdi), false).orElse(null);
        var fagsakÅrsak = utledÅrsakForFagsak(fagsak);
        if (fagsakÅrsak.isPresent()) {
            return lagSvarForAlleForespørsler(forespørslerForSak, fagsakÅrsak.get());
        }

        var behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId()).orElse(null);
        var behandlingÅrsak = utledÅrsakForBehandling(behandling);
        if (behandlingÅrsak.isPresent()) {
            return lagSvarForAlleForespørsler(forespørslerForSak, behandlingÅrsak.get());
        }

        var arbeidsforholdStatuser = arbeidsforholdInntektsmeldingMangelTjeneste.finnStatusForInntektsmeldingArbeidsforhold(BehandlingReferanse.fra(behandling));
        return forespørslerForSak.stream()
            .map(forespørsel -> lagSvar(forespørsel, utledÅrsakForInntektsmelding(arbeidsforholdStatuser, forespørsel.orgnummer())))
            .toList();
    }

    private Optional<Årsak> utledÅrsakForFagsak(Fagsak fagsak) {
        if (fagsak == null) {
            return Optional.of(Årsak.SAK_IKKE_FUNNET);
        }
        if (FagsakStatus.AVSLUTTET.equals(fagsak.getStatus())) {
            return Optional.of(Årsak.SAK_AVSLUTTET);
        }
        return Optional.empty();
    }

    private Optional<Årsak> utledÅrsakForBehandling(Behandling behandling) {
        if (behandling == null) {
            return Optional.of(Årsak.INGEN_BEHANDLING);
        }
        if (behandling.erAvsluttet()) {
            return Optional.of(Årsak.BEHANDLING_AVSLUTTET);
        }
        return Optional.empty();
    }

    private static Årsak utledÅrsakForInntektsmelding(List<ArbeidsforholdInntektsmeldingStatus> statuser, String orgnummer) {
        var imStatuser = statuser.stream()
            .filter(status -> status.arbeidsgiver().getIdentifikator().equals(orgnummer))
            .map(ArbeidsforholdInntektsmeldingStatus::inntektsmeldingStatus)
            .collect(Collectors.toSet());
        if (imStatuser.isEmpty()) {
            return Årsak.IM_ALDRI_PÅKREVD;
        }
        if (imStatuser.contains(InntektsmeldingStatus.IKKE_MOTTAT)) {
            return Årsak.IM_MANGLER;
        }
        return imStatuser.contains(InntektsmeldingStatus.AVKLART_IKKE_PÅKREVD) ? Årsak.IM_AVKLART_IKKE_PÅKREVD : Årsak.IM_MOTTATT;
    }

    private static List<ForespørselStatusResponse> lagSvarForAlleForespørsler(List<Forespørsel> forespørsler, Årsak årsak) {
        return forespørsler.stream()
            .map(forespørsel -> lagSvar(forespørsel, årsak))
            .toList();
    }

    private static ForespørselStatusResponse lagSvar(Forespørsel forespørsel, Årsak årsak) {
        return ForespørselStatusResponse.av(forespørsel.fagsakSaksnummer(), forespørsel.orgnummer(), årsak);
    }
}
