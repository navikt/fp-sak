package no.nav.foreldrepenger.web.app.tjenester.fordeling.inntektsmelding;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingMangelTjeneste;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingStatus;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.tjenester.fordeling.inntektsmelding.ForespørselStatusRequest.Forespørsel;
import no.nav.foreldrepenger.web.app.tjenester.fordeling.inntektsmelding.ForespørselStatusRequest.YtelseType;
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
        // Validerer batch-invariant (ett fagsakSaksnummer per kall) før noe domenelogikk kjøres.
        // Se ForespørselStatusRequest#enesteFagsakSaksnummer for begrunnelse (ABAC/PDP-begrensning).
        request.enesteFagsakSaksnummer();
        return request.forespørsler().stream().map(this::vurderEn).toList();
    }

    private ForespørselStatusResponse vurderEn(Forespørsel forespørsel) {
        var saksnummer = new Saksnummer(forespørsel.fagsakSaksnummer());
        var forventetYtelseType = mapYtelseType(forespørsel.ytelsetype());

        var fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, false);
        if (fagsak.isEmpty() || fagsak.get().getYtelseType() != forventetYtelseType) {
            return svar(forespørsel, Årsak.SAK_IKKE_FUNNET);
        }
        if (FagsakStatus.AVSLUTTET.equals(fagsak.get().getStatus())) {
            return svar(forespørsel, Årsak.SAK_AVSLUTTET);
        }

        var behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.get().getId());
        if (behandling.isEmpty()) {
            return svar(forespørsel, Årsak.INGEN_BEHANDLING);
        }

        var årsakForAvsluttetBehandling = vurderAvsluttetBehandling(behandling.get());
        if (årsakForAvsluttetBehandling.isPresent()) {
            return svar(forespørsel, årsakForAvsluttetBehandling.get());
        }

        return svar(forespørsel, vurderArbeidsforhold(behandling.get(), forespørsel.orgnummer()));
    }

    private Optional<Årsak> vurderAvsluttetBehandling(Behandling behandling) {
        if (!behandling.erAvsluttet()) {
            return Optional.empty();
        }
        var behandlingsresultat = behandling.getBehandlingsresultat();
        if (behandlingsresultat == null) {
            return Optional.empty();
        }
        if (behandlingsresultat.isBehandlingsresultatAvslått()) {
            return Optional.of(Årsak.BEHANDLING_AVSLÅTT);
        }
        if (behandlingsresultat.isBehandlingHenlagt()) {
            return Optional.of(Årsak.BEHANDLING_HENLAGT);
        }
        return Optional.empty();
    }

    private Årsak vurderArbeidsforhold(Behandling behandling, String orgnummer) {
        var statuser = arbeidsforholdInntektsmeldingMangelTjeneste.finnStatusForInntektsmeldingArbeidsforhold(BehandlingReferanse.fra(behandling));
        var statuserForArbeidsgiver = statuser.stream().filter(status -> status.arbeidsgiver().getIdentifikator().equals(orgnummer)).toList();

        if (statuserForArbeidsgiver.isEmpty()) {
            return Årsak.ORGNR_IKKE_PÅKREVD;
        }
        if (statuserForArbeidsgiver.stream()
            .anyMatch(status -> status.inntektsmeldingStatus() == ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.IKKE_MOTTAT)) {
            return Årsak.MANGLER_INNTEKTSMELDING;
        }
        if (statuserForArbeidsgiver.stream()
            .allMatch(status -> status.inntektsmeldingStatus() == ArbeidsforholdInntektsmeldingStatus.InntektsmeldingStatus.MOTTATT)) {
            return Årsak.INNTEKTSMELDING_MOTTATT;
        }
        return Årsak.AVKLART_IKKE_PÅKREVD;
    }

    private static ForespørselStatusResponse svar(Forespørsel forespørsel, Årsak årsak) {
        return ForespørselStatusResponse.av(forespørsel.fagsakSaksnummer(), forespørsel.orgnummer(), årsak);
    }

    private static FagsakYtelseType mapYtelseType(YtelseType ytelseType) {
        return switch (ytelseType) {
            case FORELDREPENGER -> FagsakYtelseType.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> FagsakYtelseType.SVANGERSKAPSPENGER;
        };
    }
}
