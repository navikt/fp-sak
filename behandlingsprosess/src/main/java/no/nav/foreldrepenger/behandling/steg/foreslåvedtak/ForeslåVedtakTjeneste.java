package no.nav.foreldrepenger.behandling.steg.foreslåvedtak;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.vedtak.impl.KlageAnkeVedtakTjeneste;

@ApplicationScoped
class ForeslåVedtakTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(ForeslåVedtakTjeneste.class);

    private SjekkMotEksisterendeOppgaverTjeneste sjekkMotEksisterendeOppgaverTjeneste;
    private FagsakRepository fagsakRepository;
    private KlageAnkeVedtakTjeneste klageAnkeVedtakTjeneste;

    protected ForeslåVedtakTjeneste() {
        // CDI proxy
    }

    @Inject
    ForeslåVedtakTjeneste(FagsakRepository fagsakRepository,
                          KlageAnkeVedtakTjeneste klageAnkeVedtakTjeneste,
                          SjekkMotEksisterendeOppgaverTjeneste sjekkMotEksisterendeOppgaverTjeneste) {
        this.sjekkMotEksisterendeOppgaverTjeneste = sjekkMotEksisterendeOppgaverTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.klageAnkeVedtakTjeneste = klageAnkeVedtakTjeneste;
    }

    public BehandleStegResultat foreslåVedtak(Behandling behandling, BehandlingskontrollKontekst kontekst) {
        long fagsakId = behandling.getFagsakId();
        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
        if (fagsak.getSkalTilInfotrygd()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner = new ArrayList<>();
        if (!BehandlingType.KLAGE.equals(behandling.getType()) && !BehandlingType.ANKE.equals(behandling.getType())) {
            aksjonspunktDefinisjoner.addAll(sjekkMotEksisterendeOppgaverTjeneste.sjekkMotEksisterendeGsakOppgaver(behandling.getAktørId(), behandling));
        } else if (klageAnkeVedtakTjeneste.erKlageResultatHjemsendt(behandling)) {
            behandling.nullstillToTrinnsBehandling();
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }  else if (klageAnkeVedtakTjeneste.erGodkjentHosMedunderskriver(behandling)) {
            behandling.nullstillToTrinnsBehandling();
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL));
        }

        Optional<Aksjonspunkt> vedtakUtenTotrinnskontroll = behandling
            .getÅpentAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL);
        if (vedtakUtenTotrinnskontroll.isPresent()) {
            behandling.nullstillToTrinnsBehandling();
            return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunktDefinisjoner);
        }

        håndterToTrinn(behandling, aksjonspunktDefinisjoner);

        return aksjonspunktDefinisjoner.isEmpty() ? BehandleStegResultat.utførtUtenAksjonspunkter()
            : BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunktDefinisjoner);
    }

    private void håndterToTrinn(Behandling behandling, List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner) {
        if (skalUtføreTotrinnsbehandling(behandling)) {
            if (!behandling.isToTrinnsBehandling()) {
                behandling.setToTrinnsBehandling();
                logger.info("To-trinn satt på behandling={}", behandling.getId());
            }
            aksjonspunktDefinisjoner.add(AksjonspunktDefinisjon.FORESLÅ_VEDTAK);
        } else {
            behandling.nullstillToTrinnsBehandling();
            logger.info("To-trinn fjernet på behandling={}", behandling.getId());
            if (skalOppretteForeslåVedtakManuelt(behandling)) {
                aksjonspunktDefinisjoner.add(AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT);
            }
        }
    }

    private boolean skalOppretteForeslåVedtakManuelt(Behandling behandling) {
        return BehandlingType.REVURDERING.equals(behandling.getType()) &&
            !erRevurderingEtterFødselHendelseES(behandling) && behandling.erManueltOpprettet();
    }

    private boolean skalUtføreTotrinnsbehandling(Behandling behandling) {
        return !behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL) &&
            behandling.harAksjonspunktMedTotrinnskontroll();
    }

    private boolean erRevurderingEtterFødselHendelseES(Behandling behandling) {
        return FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType()) &&
            behandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
    }
}
