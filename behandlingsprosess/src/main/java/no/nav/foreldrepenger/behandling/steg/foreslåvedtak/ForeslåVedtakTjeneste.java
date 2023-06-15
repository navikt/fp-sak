package no.nav.foreldrepenger.behandling.steg.foreslåvedtak;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.domene.vedtak.impl.KlageAnkeVedtakTjeneste;

@ApplicationScoped
class ForeslåVedtakTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(ForeslåVedtakTjeneste.class);

    private SjekkMotEksisterendeOppgaverTjeneste sjekkMotEksisterendeOppgaverTjeneste;
    private FagsakRepository fagsakRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private KlageAnkeVedtakTjeneste klageAnkeVedtakTjeneste;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;

    protected ForeslåVedtakTjeneste() {
        // CDI proxy
    }

    @Inject
    ForeslåVedtakTjeneste(FagsakRepository fagsakRepository,
                          BehandlingsresultatRepository behandlingsresultatRepository,
                          KlageAnkeVedtakTjeneste klageAnkeVedtakTjeneste,
                          SjekkMotEksisterendeOppgaverTjeneste sjekkMotEksisterendeOppgaverTjeneste,
                          DokumentBehandlingTjeneste dokumentBehandlingTjeneste) {
        this.sjekkMotEksisterendeOppgaverTjeneste = sjekkMotEksisterendeOppgaverTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.klageAnkeVedtakTjeneste = klageAnkeVedtakTjeneste;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
    }

    public BehandleStegResultat foreslåVedtak(Behandling behandling) {
        return foreslåVedtak(behandling, List.of());
    }

    public BehandleStegResultat foreslåVedtak(Behandling behandling, Collection<AksjonspunktDefinisjon> aksjonspunkterFraSteg) {
        var fagsakId = behandling.getFagsakId();
        var fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
        if (fagsak.erStengt()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner = new ArrayList<>(aksjonspunkterFraSteg);
        if (KlageAnkeVedtakTjeneste.behandlingErKlageEllerAnke(behandling)) {
            if (klageAnkeVedtakTjeneste.erKlageResultatHjemsendt(behandling) || klageAnkeVedtakTjeneste.erBehandletAvKabal(behandling)) {
                behandling.nullstillToTrinnsBehandling();
                return BehandleStegResultat.utførtUtenAksjonspunkter();
            }
        } else {
            aksjonspunktDefinisjoner
                    .addAll(sjekkMotEksisterendeOppgaverTjeneste.sjekkMotEksisterendeGsakOppgaver(behandling.getAktørId(), behandling));
        }

        var vedtakUtenTotrinnskontroll = behandling
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
                LOG.info("To-trinn satt på behandling={}", behandling.getId());
            }
            aksjonspunktDefinisjoner.add(AksjonspunktDefinisjon.FORESLÅ_VEDTAK);
        } else {
            behandling.nullstillToTrinnsBehandling();
            LOG.info("To-trinn fjernet på behandling={}", behandling.getId());
            if (skalOppretteForeslåVedtakManuelt(behandling, aksjonspunktDefinisjoner)) {
                aksjonspunktDefinisjoner.add(AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT);
            } else {
                dokumentBehandlingTjeneste.nullstillVedtakFritekstHvisFinnes(behandling.getId());
            }
        }
    }

    private boolean skalOppretteForeslåVedtakManuelt(Behandling behandling, List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner) {
        if (behandling.erRevurdering() && behandling.erManueltOpprettet()) {
            return true;
        }
        if (behandling.harNoenBehandlingÅrsaker(BehandlingÅrsakType.årsakerRelatertTilDød())) {
            return true;
        }
        return FagsakYtelseType.SVANGERSKAPSPENGER.equals(behandling.getFagsakYtelseType()) && behandling.erYtelseBehandling()
            && !erOpphørEllerUendretUtenAndreAksjonspunkt(behandling, aksjonspunktDefinisjoner);
    }

    private boolean skalUtføreTotrinnsbehandling(Behandling behandling) {
        return !behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL) &&
                behandling.harAksjonspunktMedTotrinnskontroll();
    }

    private boolean erOpphørEllerUendretUtenAndreAksjonspunkt(Behandling behandling, List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner) {
        // TODO: Dra med til kon aksjonspunkt rundt tilrettelegging/vilkår.
        var aksjonspunktSomSkalGiManueltVedtak = behandling.getAksjonspunkter().stream()
            .filter(Aksjonspunkt::erUtført)
            .map(Aksjonspunkt::getAksjonspunktDefinisjon)
            .filter(ad -> !AksjonspunktType.AUTOPUNKT.equals(ad.getAksjonspunktType()))
            .filter(ad -> !AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT.equals(ad))
            .toList();
        return behandling.erRevurdering() && aksjonspunktDefinisjoner.isEmpty() && aksjonspunktSomSkalGiManueltVedtak.isEmpty() &&
            behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())
                .filter(br -> br.isBehandlingsresultatOpphørt() || br.isBehandlingsresultatIkkeEndret())
                .isPresent();
    }

}
