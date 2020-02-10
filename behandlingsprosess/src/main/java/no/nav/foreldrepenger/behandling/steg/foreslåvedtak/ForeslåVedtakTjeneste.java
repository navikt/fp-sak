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
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;

@ApplicationScoped
class ForeslåVedtakTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(ForeslåVedtakTjeneste.class);

    private SjekkMotEksisterendeOppgaverTjeneste sjekkMotEksisterendeOppgaverTjeneste;
    private FagsakRepository fagsakRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private KlageRepository klageRepository;
    private AnkeRepository ankeRepository;

    protected ForeslåVedtakTjeneste() {
        // CDI proxy
    }

    @Inject
    ForeslåVedtakTjeneste(FagsakRepository fagsakRepository,
                          AnkeRepository ankeRepository,
                          KlageRepository klageRepository,
                          BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                          SjekkMotEksisterendeOppgaverTjeneste sjekkMotEksisterendeOppgaverTjeneste) {
        this.sjekkMotEksisterendeOppgaverTjeneste = sjekkMotEksisterendeOppgaverTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.klageRepository = klageRepository;
        this.ankeRepository = ankeRepository;
    }

    public BehandleStegResultat foreslåVedtak(Behandling behandling, BehandlingskontrollKontekst kontekst) {
        long fagsakId = behandling.getFagsakId();
        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
        if (fagsak.getSkalTilInfotrygd()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner = new ArrayList<>();
        if (!BehandlingType.KLAGE.equals(behandling.getType()) && !BehandlingType.ANKE.equals(behandling.getType())) {
            aksjonspunktDefinisjoner = sjekkMotEksisterendeOppgaverTjeneste.sjekkMotEksisterendeGsakOppgaver(behandling.getAktørId(), behandling);
        } else {
            if (erKlageResultatHjemsendt(behandling)) {
                behandling.nullstillToTrinnsBehandling();
                settForeslåOgFatterVedtakAksjonspunkterAvbrutt(behandling, kontekst);
                return BehandleStegResultat.utførtUtenAksjonspunkter();
            }
            if (erKlageEllerAnkeGodkjentHosMedunderskriver(behandling)) {
                behandling.nullstillToTrinnsBehandling();
                settForeslåOgFatterVedtakAksjonspunkterAvbrutt(behandling, kontekst);
                aksjonspunktDefinisjoner.add(AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL);
                return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunktDefinisjoner);
            }
        }

        Optional<Aksjonspunkt> vedtakUtenTotrinnskontroll = behandling
            .getÅpentAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL);
        if (vedtakUtenTotrinnskontroll.isPresent()) {
            behandling.nullstillToTrinnsBehandling();
            return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunktDefinisjoner);
        }

        håndterToTrinn(behandling, kontekst, aksjonspunktDefinisjoner);

        return aksjonspunktDefinisjoner.isEmpty() ? BehandleStegResultat.utførtUtenAksjonspunkter()
            : BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunktDefinisjoner);
    }

    private void håndterToTrinn(Behandling behandling, BehandlingskontrollKontekst kontekst, List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner) {
        if (skalUtføreTotrinnsbehandling(behandling)) {
            if (!behandling.isToTrinnsBehandling()) {
                behandling.setToTrinnsBehandling();
                logger.info("To-trinn satt på behandling={}", behandling.getId());
            }
            aksjonspunktDefinisjoner.add(AksjonspunktDefinisjon.FORESLÅ_VEDTAK);
        } else {
            behandling.nullstillToTrinnsBehandling();
            logger.info("To-trinn fjernet på behandling={}", behandling.getId());
            settForeslåOgFatterVedtakAksjonspunkterAvbrutt(behandling, kontekst);
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
        return behandling.getFagsakYtelseType().gjelderEngangsstønad() &&
            behandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
    }

    private boolean erKlageResultatHjemsendt(Behandling behandling) {
        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            Optional<KlageVurderingResultat> klageVurderingResultat = klageRepository.hentGjeldendeKlageVurderingResultat(behandling);
            return klageVurderingResultat.filter(kvr -> KlageVurdering.HJEMSENDE_UTEN_Å_OPPHEVE.equals(kvr.getKlageVurdering())).isPresent();
        }
        return false;
    }

    private boolean erKlageEllerAnkeGodkjentHosMedunderskriver(Behandling behandling) {
        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            Optional<KlageVurderingResultat> klageVurderingResultat = klageRepository.hentGjeldendeKlageVurderingResultat(behandling);
            return klageVurderingResultat.isPresent() && klageVurderingResultat.get().getKlageVurdertAv().equals(KlageVurdertAv.NK)
                && klageVurderingResultat.get().isGodkjentAvMedunderskriver();
        } else if (BehandlingType.ANKE.equals(behandling.getType())) {
            Optional<AnkeVurderingResultatEntitet> ankeVurderingResultat = ankeRepository.hentAnkeVurderingResultat(behandling.getId());
            return ankeVurderingResultat.isPresent() && ankeVurderingResultat.get().godkjentAvMedunderskriver();
        }
        return false;
    }

    private void settForeslåOgFatterVedtakAksjonspunkterAvbrutt(Behandling behandling, BehandlingskontrollKontekst kontekst) {
        // TODO: Hører ikke hjemme her. Bør bruke generisk stegresultat eller flyttes. Hva er use-case for disse tilfellene?
        //  Er det grunn til å tro at disse finnes når man er i FORVED-steg - de skal utledes i steget?
        List<Aksjonspunkt> skalAvbrytes = new ArrayList<>();
        behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.FORESLÅ_VEDTAK).ifPresent(skalAvbrytes::add);
        behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.FATTER_VEDTAK).ifPresent(skalAvbrytes::add);
        if (!skalAvbrytes.isEmpty()) {
            behandlingskontrollTjeneste.lagreAksjonspunkterAvbrutt(kontekst, behandling.getAktivtBehandlingSteg(), skalAvbrytes);
        }
    }
}
