package no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidInntektsmelding;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsutredningTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingIdVersjonDto;
import no.nav.vedtak.exception.TekniskException;

@ApplicationScoped
public class ArbeidOgInntektsmeldingProsessTjeneste {

    private BehandlingRepository behandlingRepository;
    private BehandlingsutredningTjeneste behandlingsutredningTjeneste;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private BehandlingsprosessTjeneste behandlingsprosessTjeneste;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    ArbeidOgInntektsmeldingProsessTjeneste() {
        // CDI
    }

    @Inject
    ArbeidOgInntektsmeldingProsessTjeneste(BehandlingRepository behandlingRepository,
                                           BehandlingsutredningTjeneste behandlingsutredningTjeneste,
                                           BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                           BehandlingsprosessTjeneste behandlingsprosessTjeneste,
                                           BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingsutredningTjeneste = behandlingsutredningTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
    }

    public void rullTilbakeTilKontrollerArbeidOgInntektsmelding(BehandlingIdVersjonDto dto) {
        var behandling = behandlingRepository.hentBehandling(dto.getBehandlingUuid());

        // Diverse kontroller for å sjekke at vi har lov til å flytte behandlingen bakover i prosessen.
        validerAtOperasjonErLovlig(behandling, false);

        behandlingsutredningTjeneste.kanEndreBehandling(behandling, dto.getBehandlingVersjon());
        behandlingProsesseringTjeneste.reposisjonerBehandlingTilbakeTil(behandling, BehandlingStegType.KONTROLLER_FAKTA_ARBEIDSFORHOLD_INNTEKTSMELDING );
        behandlingsprosessTjeneste.asynkKjørProsess(behandling);
    }

    public void tillTilbakeOgOpprettAksjonspunkt(BehandlingIdVersjonDto dto) {
        var behandling = behandlingRepository.hentBehandling(dto.getBehandlingUuid());

        // Diverse kontroller for å sjekke at vi har lov til å flytte behandlingen bakover i prosessen.
        validerAtOperasjonErLovlig(behandling, true);

        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        behandlingskontrollTjeneste.lagreAksjonspunkterFunnet(kontekst, List.of(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD_INNTEKTSMELDING));
        behandlingProsesseringTjeneste.reposisjonerBehandlingTilbakeTil(behandling,
                BehandlingStegType.KONTROLLER_FAKTA_ARBEIDSFORHOLD_INNTEKTSMELDING);
        behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(behandling);
    }

    private void validerAtOperasjonErLovlig(Behandling behandling, boolean aksjonspunktSkalOpprettes) {
        if (!aksjonspunktSkalOpprettes && !behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD_INNTEKTSMELDING)) {
            throw new IllegalStateException("FEIL: Prøver å tilbakestille en behandling som aldri har hatt aksjonspunkt" +
                    " for arbeidsforhold / inntektsmelding. Ugyldig aksjon.");
        }
        if (aksjonspunktSkalOpprettes && behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD_INNTEKTSMELDING)) {
            var msg = String.format("Behandling %s har allerede aksjonspunkt 5085 for arbeid og inntektsmelding",
                    behandling.getId());
            throw new TekniskException("FP-658124", msg);
        }
        if (behandling.erAvsluttet()) {
            throw new IllegalStateException("FEIL: Prøver å tilbakestille en behandling som er avsluttet. Ugyldig aksjon.");
        } else if (behandling.isBehandlingPåVent()) {
            throw new IllegalStateException("FEIL: Prøver å tilbakestille en behandling som er på vent. Ugyldig aksjon.");
        }
        if (!behandlingskontrollTjeneste.erStegPassert(behandling, BehandlingStegType.KONTROLLER_FAKTA_ARBEIDSFORHOLD_INNTEKTSMELDING)) {
            throw new IllegalStateException("FEIL: Prøver å tilbakestille en behandling før den har kommet lant nok i behandlingsprosessen. Ugyldig aksjon.");
        }
        if (!behandlingskontrollTjeneste.erStegPassert(behandling, BehandlingStegType.KONTROLLER_FAKTA_ARBEIDSFORHOLD_INNTEKTSMELDING)) {
            throw new IllegalStateException("FEIL: Prøver å tilbakestille en behandling før den har kommet lant nok i behandlingsprosessen. Ugyldig aksjon.");
        }
        if (behandlingskontrollTjeneste.erStegPassert(behandling, BehandlingStegType.FORESLÅ_VEDTAK)) {
            throw new IllegalStateException("FEIL: Prøver å tilbakestille en behandling som har passert steg for å foreslå vedtak. Ugyldig aksjon.");
        }
        if (behandling.getStatus().equals(BehandlingStatus.FATTER_VEDTAK) || behandling.getStatus().equals(BehandlingStatus.IVERKSETTER_VEDTAK)) {
            throw new IllegalStateException("FEIL: Prøver å tilbakestille en behandling som er i ferd med å fatte vedtak. Ugyldig aksjon.");
        }
    }

}
