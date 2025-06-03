package no.nav.foreldrepenger.behandlingskontroll.impl;

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKontrollRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktType;

/**
 * Håndterer aksjonspunktresultat og oppretter/reaktiverer aksjonspunkt Brukes
 * fra StegVisitor og Behandlingskontroll for lik håndtering
 */
class AksjonspunktResultatOppretter {

    private final Behandling behandling;

    private final AksjonspunktKontrollRepository aksjonspunktKontrollRepository;

    private final Map<AksjonspunktDefinisjon, Aksjonspunkt> eksisterende = new LinkedHashMap<>();

    AksjonspunktResultatOppretter(AksjonspunktKontrollRepository aksjonspunktKontrollRepository, Behandling behandling) {
        this.behandling = Objects.requireNonNull(behandling, "behandling");
        this.aksjonspunktKontrollRepository = aksjonspunktKontrollRepository;
        behandling.getAksjonspunkter().forEach(ap -> this.eksisterende.putIfAbsent(ap.getAksjonspunktDefinisjon(), ap));
    }

    /**
     * Lagrer nye aksjonspunkt, og gjenåpner dem hvis de alleerede står til
     * avbrutt/utført
     */
    List<Aksjonspunkt> opprettAksjonspunkter(List<AksjonspunktResultat> apResultater, BehandlingStegType behandlingStegType) {

        if (!apResultater.isEmpty()) {
            List<Aksjonspunkt> endringAksjonspunkter = new ArrayList<>();
            endringAksjonspunkter.addAll(fjernGjensidigEkskluderendeAksjonspunkter(apResultater));
            endringAksjonspunkter.addAll(leggTilResultatPåBehandling(behandlingStegType, apResultater));
            return endringAksjonspunkter;
        }
        return new ArrayList<>();
    }

    private List<Aksjonspunkt> fjernGjensidigEkskluderendeAksjonspunkter(List<AksjonspunktResultat> nyeApResultater) {
        List<Aksjonspunkt> avbrutteAksjonspunkter = new ArrayList<>();
        var nyeApDef = nyeApResultater.stream().map(AksjonspunktResultat::getAksjonspunktDefinisjon).collect(toSet());
        // Avbryt eksisterende aksjonspunkt dersom det skal opprettes nytt som er i konflikt med eksisterende
        eksisterende.values().stream()
                .filter(Aksjonspunkt::erÅpentAksjonspunkt)
                .filter(ap -> ap.getAksjonspunktDefinisjon().getUtelukkendeApdef().stream().anyMatch(nyeApDef::contains))
                .forEach(ap -> {
                    aksjonspunktKontrollRepository.setTilAvbrutt(ap);
                    avbrutteAksjonspunkter.add(ap);
                });
        return avbrutteAksjonspunkter;
    }

    private List<Aksjonspunkt> leggTilResultatPåBehandling(BehandlingStegType behandlingStegType, List<AksjonspunktResultat> resultat) {
        return resultat.stream()
                .map(ar -> oppdaterAksjonspunktMedResultat(behandlingStegType, ar))
                .toList();
    }

    private Aksjonspunkt oppdaterAksjonspunktMedResultat(BehandlingStegType behandlingStegType, AksjonspunktResultat resultat) {
        if (AksjonspunktType.AUTOPUNKT.equals(resultat.getAksjonspunktDefinisjon().getAksjonspunktType())
            && AksjonspunktStatus.OPPRETTET.equals(resultat.getMålStatus())) {
            aksjonspunktKontrollRepository.forberedSettPåVentMedAutopunkt(behandling, resultat.getAksjonspunktDefinisjon());
        }
        var oppdatert = eksisterende.get(resultat.getAksjonspunktDefinisjon());
        if (oppdatert == null) {
            oppdatert = behandlingStegType == null ? aksjonspunktKontrollRepository.leggTilAksjonspunkt(behandling, resultat.getAksjonspunktDefinisjon())
                : aksjonspunktKontrollRepository.leggTilAksjonspunkt(behandling, resultat.getAksjonspunktDefinisjon(), behandlingStegType);
            eksisterende.putIfAbsent(oppdatert.getAksjonspunktDefinisjon(), oppdatert);
        }
        switch (resultat.getMålStatus()) {
            case OPPRETTET -> reåpne(oppdatert);
            case AVBRUTT -> avbryt(oppdatert);
            case UTFØRT -> utfør(oppdatert);
        }
        if (resultat.getFrist() != null || resultat.getVenteårsak() != null) {
            aksjonspunktKontrollRepository.setFrist(behandling, oppdatert, resultat.getFrist(), resultat.getVenteårsak());
        }
        return oppdatert;
    }

    private void utfør(Aksjonspunkt oppdatert) {
        if (!oppdatert.erUtført()) {
            if (oppdatert.erAvbrutt()) {
                aksjonspunktKontrollRepository.setReåpnet(oppdatert);
            }
            aksjonspunktKontrollRepository.setTilUtført(oppdatert, oppdatert.getBegrunnelse());
        }
    }

    private void reåpne(Aksjonspunkt oppdatert) {
        if (oppdatert.erUtført() || oppdatert.erAvbrutt()) {
            aksjonspunktKontrollRepository.setReåpnet(oppdatert);
        }
    }

    private void avbryt(Aksjonspunkt oppdatert) {
        if (!oppdatert.erAvbrutt()) {
            aksjonspunktKontrollRepository.setTilAvbrutt(oppdatert);
        }
    }

}
