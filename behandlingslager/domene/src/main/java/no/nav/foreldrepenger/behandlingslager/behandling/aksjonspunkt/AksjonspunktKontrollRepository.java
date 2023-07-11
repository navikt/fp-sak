package no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;

/**
 * Håndter all endring av aksjonspunkt for behandlingskontroll. Skal IKKE brukes utenfor Behandlingskontroll uten avklaring
 */
@ApplicationScoped
public class AksjonspunktKontrollRepository {

    private static final Logger LOG = LoggerFactory.getLogger(AksjonspunktKontrollRepository.class);

    public AksjonspunktKontrollRepository() {
        // CDI
    }

    public Aksjonspunkt settBehandlingPåVent(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                             BehandlingStegType stegType,
                                             LocalDateTime fristTid, Venteårsak venteårsak) {

        LOG.info("Setter behandling på vent for steg={}, aksjonspunkt={}, fristTid={}, venteÅrsak={}", stegType, aksjonspunktDefinisjon, fristTid, venteårsak);

        Aksjonspunkt aksjonspunkt;
        var eksisterendeAksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(aksjonspunktDefinisjon);
        if (eksisterendeAksjonspunkt.isPresent()) {
            // håndter har allerede angit aksjonpunkt, oppdaterer
            aksjonspunkt = eksisterendeAksjonspunkt.get();
            if (!aksjonspunkt.erOpprettet()) {
                this.setReåpnet(aksjonspunkt);
            }
            this.setFrist(behandling, aksjonspunkt, fristTid, venteårsak);
        } else {
            // nytt aksjonspunkt
            aksjonspunkt = this.leggTilAksjonspunkt(behandling, aksjonspunktDefinisjon, null, fristTid, venteårsak);
        }
        aksjonspunkt.setBehandlingSteg(stegType);
        return aksjonspunkt;
    }

    private Aksjonspunkt leggTilAksjonspunkt(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                             BehandlingStegType behandlingStegType, LocalDateTime frist, Venteårsak venteÅrsak) {
        // sjekk at alle parametere er spesifisert
        Objects.requireNonNull(behandling, "behandling");
        Objects.requireNonNull(aksjonspunktDefinisjon, "aksjonspunktDefinisjon");

        // slå opp for å få riktig konfigurasjon.
        var adBuilder = Optional.ofNullable(behandlingStegType)
            .map(stegType -> new Aksjonspunkt.Builder(aksjonspunktDefinisjon, stegType))
            .orElseGet(() -> new Aksjonspunkt.Builder(aksjonspunktDefinisjon));

        if (frist != null) {
            adBuilder.medFristTid(frist);
        } else if (aksjonspunktDefinisjon.getFristPeriod() != null) {
            adBuilder.medFristTid(LocalDateTime.now().plus(aksjonspunktDefinisjon.getFristPeriod()));
        }

        adBuilder.medVenteårsak(venteÅrsak != null ? venteÅrsak : Venteårsak.UDEFINERT);

        var aksjonspunkt = adBuilder.buildFor(behandling);
        oppdaterSaksbehandlerBehandlingsfristVedBehov(behandling, aksjonspunkt);
        LOG.info("Legger til aksjonspunkt: {}", aksjonspunktDefinisjon);
        return aksjonspunkt;
    }

    public Aksjonspunkt leggTilAksjonspunkt(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                            BehandlingStegType behandlingStegType) {
        Objects.requireNonNull(behandlingStegType, "behandlingStegType");
        return leggTilAksjonspunkt(behandling, aksjonspunktDefinisjon, behandlingStegType, null, null);
    }

    public Aksjonspunkt leggTilAksjonspunkt(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return leggTilAksjonspunkt(behandling, aksjonspunktDefinisjon, null, null, null);
    }

    public void setReåpnet(Aksjonspunkt aksjonspunkt) {
        LOG.info("Setter aksjonspunkt reåpnet: {}", aksjonspunkt.getAksjonspunktDefinisjon());
        aksjonspunkt.setStatus(AksjonspunktStatus.OPPRETTET, aksjonspunkt.getBegrunnelse());
    }

    public void setReåpnetMedTotrinn(Aksjonspunkt aksjonspunkt, boolean setToTrinn) {
        LOG.info("Setter aksjonspunkt reåpnet: {}", aksjonspunkt.getAksjonspunktDefinisjon());
        aksjonspunkt.setStatus(AksjonspunktStatus.OPPRETTET, aksjonspunkt.getBegrunnelse());
        setToTrinnsBehandlingKreves(aksjonspunkt, setToTrinn);
    }

    public void setTilAvbrutt(Aksjonspunkt aksjonspunkt) {
        LOG.info("Setter aksjonspunkt avbrutt: {}", aksjonspunkt.getAksjonspunktDefinisjon());
        aksjonspunkt.setStatus(AksjonspunktStatus.AVBRUTT, aksjonspunkt.getBegrunnelse());
    }

    public boolean setTilUtført(Aksjonspunkt aksjonspunkt, String begrunnelse) {
        LOG.info("Setter aksjonspunkt utført: {}", aksjonspunkt.getAksjonspunktDefinisjon());
        return aksjonspunkt.setStatus(AksjonspunktStatus.UTFØRT, begrunnelse);
    }

    public void setToTrinnsBehandlingKreves(Aksjonspunkt aksjonspunkt, boolean totrinn) {
        if (!aksjonspunkt.erÅpentAksjonspunkt() || totrinn && !aksjonspunkt.kanSetteToTrinnsbehandling()) {
            throw new IllegalStateException("Utviklerfeil: kan ikke sette totrinn på aksjonspunkt " + aksjonspunkt);
        }
        aksjonspunkt.setToTrinnsBehandling(totrinn);
    }

    public void setFrist(Behandling behandling, Aksjonspunkt ap, LocalDateTime fristTid, Venteårsak venteårsak) {
        ap.setFristTid(fristTid);
        ap.setVenteårsak(venteårsak);
        oppdaterSaksbehandlerBehandlingsfristVedBehov(behandling, ap);
    }

    public void forberedSettPåVentMedAutopunkt(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        if (AksjonspunktType.AUTOPUNKT.equals(aksjonspunktDefinisjon.getAksjonspunktType())) {
            behandling.setAnsvarligSaksbehandler(null);
        }
    }

    private void oppdaterSaksbehandlerBehandlingsfristVedBehov(Behandling behandling, Aksjonspunkt aksjonspunkt) {
        // Sett tom saksbehandler dersom settes på vent
        if (aksjonspunkt.erAutopunkt() && aksjonspunkt.erOpprettet() && behandling.getAnsvarligSaksbehandler() != null) {
            try {
                throw new IllegalStateException("Sett på vent");
            } catch (Exception e) {
                LOG.info("SETTPÅVENT med saksbehandler - hvorfor kom vi hit???", e);
                behandling.setAnsvarligSaksbehandler(null);
            }
        }
        // Oppdater behandlingens tidsfrist for enkelte autopunkt
        if (aksjonspunkt.getFristTid() != null && aksjonspunkt.erOpprettet() && aksjonspunkt.getAksjonspunktDefinisjon().utviderBehandlingsfrist()) {
            var eksisterendeFrist = behandling.getBehandlingstidFrist();
            var fristFraAksjonspunkt = aksjonspunkt.getFristTid().toLocalDate().plusWeeks(behandling.getType().getBehandlingstidFristUker());
            if (eksisterendeFrist == null || fristFraAksjonspunkt.isAfter(eksisterendeFrist)) {
                behandling.setBehandlingstidFrist(fristFraAksjonspunkt);
            }
        }

    }

}
