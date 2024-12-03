package no.nav.foreldrepenger.behandling.steg.foreslåvedtak;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;

@ApplicationScoped
public class SjekkMotEksisterendeOppgaverTjeneste {
    private OppgaveTjeneste oppgaveTjeneste;

    SjekkMotEksisterendeOppgaverTjeneste() {
        // CDI proxy
    }

    @Inject
    public SjekkMotEksisterendeOppgaverTjeneste(OppgaveTjeneste oppgaveTjeneste) {
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    public List<AksjonspunktDefinisjon> sjekkMotEksisterendeGsakOppgaver(AktørId aktørid, Behandling behandling) {

        if (!SpesialBehandling.skalUttakVurderes(behandling) || sjekkMotEksisterendeGsakOppgaverUtført(behandling)) {
            return List.of();
        }

        List<AksjonspunktDefinisjon> aksjonspunktliste = new ArrayList<>();

        if (oppgaveTjeneste.harÅpneVurderKonsekvensOppgaver(aktørid)) {
            aksjonspunktliste.add(AksjonspunktDefinisjon.VURDERE_ANNEN_YTELSE_FØR_VEDTAK);
        }
        if (oppgaveTjeneste.harÅpneVurderDokumentOppgaver(aktørid)) {
            aksjonspunktliste.add(AksjonspunktDefinisjon.VURDERE_DOKUMENT_FØR_VEDTAK);
        }
        return aksjonspunktliste;
    }

    private boolean sjekkMotEksisterendeGsakOppgaverUtført(Behandling behandling) {
        return behandling.getAksjonspunkter()
            .stream()
            .anyMatch(ap -> ap.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.VURDERE_ANNEN_YTELSE_FØR_VEDTAK) && ap.erUtført()
                || ap.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.VURDERE_DOKUMENT_FØR_VEDTAK) && ap.erUtført());
    }
}
