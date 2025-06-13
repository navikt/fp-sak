package no.nav.foreldrepenger.behandlingskontroll.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktkontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKontrollRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;

/**
 * ALLE ENDRINGER I DENNE KLASSEN SKAL KLARERES OG KODE-REVIEWES MED ANSVARLIG
 * APPLIKASJONSARKITEKT (SE UTVIKLERHÅNDBOK).
 */
@ApplicationScoped
public class AksjonspunktkontrollTjenesteImpl implements AksjonspunktkontrollTjeneste {

    private AksjonspunktKontrollRepository aksjonspunktKontrollRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollEventPubliserer eventPubliserer;

    AksjonspunktkontrollTjenesteImpl() {
        // for CDI proxy
    }

    /**
     * SE KOMMENTAR ØVERST
     */
    @Inject
    public AksjonspunktkontrollTjenesteImpl(BehandlingskontrollServiceProvider serviceProvider) {
        this.behandlingRepository = serviceProvider.getBehandlingRepository();
        this.aksjonspunktKontrollRepository = serviceProvider.getAksjonspunktKontrollRepository();
        this.eventPubliserer = serviceProvider.getEventPubliserer();
    }

    @Override
    public List<Aksjonspunkt> lagreAksjonspunkterFunnet(Behandling behandling, BehandlingLås skriveLås,
                                                        List<AksjonspunktDefinisjon> aksjonspunkter) {
        var aksjonspunktResultater = aksjonspunkter.stream().map(AksjonspunktResultat::opprettForAksjonspunkt).toList();
        var endret = internLagreAksjonspunktResultat(behandling, skriveLås, null, aksjonspunktResultater);
        return endret.stream()
            .filter(a -> aksjonspunkter.contains(a.getAksjonspunktDefinisjon()))
            .toList();
    }

    @Override
    public List<Aksjonspunkt> lagreAksjonspunkterFunnet(Behandling behandling, BehandlingLås skriveLås,
                                                        BehandlingStegType behandlingStegType,
                                                        List<AksjonspunktDefinisjon> aksjonspunkter) {
        var aksjonspunktResultater = aksjonspunkter.stream().map(AksjonspunktResultat::opprettForAksjonspunkt).toList();
        var endret = internLagreAksjonspunktResultat(behandling, skriveLås, behandlingStegType, aksjonspunktResultater);
        return endret.stream()
            .filter(a -> aksjonspunkter.contains(a.getAksjonspunktDefinisjon()))
            .toList();
    }

    @Override
    public void lagreAksjonspunkterUtført(Behandling behandling, BehandlingLås skriveLås, Aksjonspunkt aksjonspunkt, String begrunnelse) {
        Objects.requireNonNull(aksjonspunkt);
        if (aksjonspunkt.erAutopunkt()) {
            throw new IllegalArgumentException("Utviklerfeil: aksjonspunkt er autopunkt " + aksjonspunkt.getAksjonspunktDefinisjon());
        }
        List<Aksjonspunkt> utførte = new ArrayList<>();

        if (!aksjonspunkt.erUtført() || !Objects.equals(aksjonspunkt.getBegrunnelse(), begrunnelse)) {
            aksjonspunktKontrollRepository.setTilUtført(aksjonspunkt, begrunnelse);
            utførte.add(aksjonspunkt);
        }

        behandlingRepository.lagre(behandling, skriveLås);
        aksjonspunkterEndretStatus(behandling, utførte);
    }

    @Override
    public void lagreAksjonspunkterAvbrutt(Behandling behandling, BehandlingLås skriveLås, List<Aksjonspunkt> aksjonspunkter) {
        var aksjonspunktResultater = aksjonspunkter.stream()
            .filter(a -> !a.erAvbrutt())
            .map(a -> AksjonspunktResultat.statusForAksjonspunkt(a.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT))
            .toList();
        internLagreAksjonspunktResultat(behandling, skriveLås, null, aksjonspunktResultater);
    }

    @Override
    public void lagreAksjonspunkterReåpnet(Behandling behandling, BehandlingLås skriveLås, List<Aksjonspunkt> aksjonspunkter) {
        var aksjonspunktResultater = aksjonspunkter.stream()
            .filter(a -> !a.erOpprettet())
            .map(Aksjonspunkt::getAksjonspunktDefinisjon)
            .map(AksjonspunktResultat::opprettForAksjonspunkt)
            .toList();
        internLagreAksjonspunktResultat(behandling, skriveLås, null, aksjonspunktResultater);
    }


    @Override
    public void lagreAksjonspunktResultat(Behandling behandling, BehandlingLås skriveLås, BehandlingStegType behandlingStegType,
            List<AksjonspunktResultat> aksjonspunktResultater) {
        internLagreAksjonspunktResultat(behandling, skriveLås, behandlingStegType, aksjonspunktResultater);
    }

    @Override
    public void setAksjonspunkterToTrinn(Behandling behandling, BehandlingLås skriveLås, List<Aksjonspunkt> aksjonspunkter, boolean totrinn) {
        var skalEndres = aksjonspunkter.stream()
            .filter(a -> a.isToTrinnsBehandling() != totrinn)
            .toList();
        if (skalEndres.isEmpty()) {
            return;
        }
        for (var aksjonspunkt : aksjonspunkter) {
            if (!aksjonspunkt.erÅpentAksjonspunkt()) {
                aksjonspunktKontrollRepository.setReåpnet(aksjonspunkt);
            }
            aksjonspunktKontrollRepository.setToTrinnsBehandlingKreves(aksjonspunkt, totrinn);
        }
        behandlingRepository.lagre(behandling, skriveLås);
        aksjonspunkterEndretStatus(behandling, skalEndres);
    }

    private List<Aksjonspunkt> internLagreAksjonspunktResultat(Behandling behandling, BehandlingLås skriveLås,
                                                               BehandlingStegType behandlingStegType,
                                                               List<AksjonspunktResultat> aksjonspunktResultater) {
        if (aksjonspunktResultater.isEmpty()) {
            return List.of();
        }
        if (aksjonspunktResultater.stream().map(AksjonspunktResultat::getAksjonspunktDefinisjon).anyMatch(AksjonspunktDefinisjon::erAutopunkt)) {
            throw new IllegalArgumentException("Utviklerfeil: aksjonspunktResultater inneholder autopunkt "
                + aksjonspunktResultater.stream().map(AksjonspunktResultat::getAksjonspunktDefinisjon).toList());
        }
        var apHåndterer = new AksjonspunktResultatOppretter(aksjonspunktKontrollRepository, behandling);
        var endret = apHåndterer.opprettAksjonspunkter(aksjonspunktResultater, behandlingStegType);
        behandlingRepository.lagre(behandling, skriveLås);
        aksjonspunkterEndretStatus(behandling, endret);
        return endret;
    }

    void aksjonspunkterEndretStatus(Behandling behandling, List<Aksjonspunkt> aksjonspunkter) {
        // handlinger som skal skje når funnet
        if (!aksjonspunkter.isEmpty()) {
            eventPubliserer.fireEvent(new AksjonspunktStatusEvent(behandling, aksjonspunkter));
        }
    }
}
