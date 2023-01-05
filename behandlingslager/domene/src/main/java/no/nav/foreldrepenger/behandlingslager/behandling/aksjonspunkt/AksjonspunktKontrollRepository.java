package no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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

    private static final Set<AksjonspunktDefinisjon> IKKE_AKTUELL_TOTRINN = Set.of(AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL, AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT);

    @Inject
    public AksjonspunktKontrollRepository() {
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
        utvidBehandlingsfristVedBehov(behandling, aksjonspunkt);
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
        if (setToTrinn && !aksjonspunkt.isToTrinnsBehandling() && !IKKE_AKTUELL_TOTRINN.contains(aksjonspunkt.getAksjonspunktDefinisjon())) {
            aksjonspunkt.settToTrinnsFlag();
        }
    }

    public void setTilAvbrutt(Aksjonspunkt aksjonspunkt) {
        LOG.info("Setter aksjonspunkt avbrutt: {}", aksjonspunkt.getAksjonspunktDefinisjon());
        aksjonspunkt.setStatus(AksjonspunktStatus.AVBRUTT, aksjonspunkt.getBegrunnelse());
    }

    public boolean setTilUtført(Aksjonspunkt aksjonspunkt, String begrunnelse) {
        LOG.info("Setter aksjonspunkt utført: {}", aksjonspunkt.getAksjonspunktDefinisjon());
        return aksjonspunkt.setStatus(AksjonspunktStatus.UTFØRT, begrunnelse);
    }

    public void setFrist(Behandling behandling, Aksjonspunkt ap, LocalDateTime fristTid, Venteårsak venteårsak) {
        ap.setFristTid(fristTid);
        ap.setVenteårsak(venteårsak);
        utvidBehandlingsfristVedBehov(behandling, ap);
    }

    private void utvidBehandlingsfristVedBehov(Behandling behandling, Aksjonspunkt aksjonspunkt) {
        if (aksjonspunkt.getFristTid() != null && aksjonspunkt.getAksjonspunktDefinisjon().utviderBehandlingsfrist()) {
            var eksisterendeFrist = behandling.getBehandlingstidFrist();
            var fristFraAksjonspunkt = aksjonspunkt.getFristTid().toLocalDate().plusWeeks(behandling.getType().getBehandlingstidFristUker());
            if (eksisterendeFrist == null || fristFraAksjonspunkt.isAfter(eksisterendeFrist)) {
                behandling.setBehandlingstidFrist(fristFraAksjonspunkt);
            }
        }

    }

}
