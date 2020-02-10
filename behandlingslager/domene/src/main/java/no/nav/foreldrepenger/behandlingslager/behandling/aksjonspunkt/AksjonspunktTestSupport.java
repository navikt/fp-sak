package no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.vedtak.util.FPDateUtil;

/**
 * Skal kun brukes av tester som av en eller annen grunn må tukle
 */
public class AksjonspunktTestSupport {


    public AksjonspunktTestSupport() {
    }

    public Aksjonspunkt leggTilAksjonspunkt(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                            BehandlingStegType behandlingStegType) {
        Objects.requireNonNull(behandlingStegType, "behandlingStegType");
        return leggTilAksjonspunkt(behandling, aksjonspunktDefinisjon, Optional.ofNullable(behandlingStegType), Optional.empty(), Optional.empty(),
            Optional.empty());
    }

    public Aksjonspunkt leggTilAksjonspunkt(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return leggTilAksjonspunkt(behandling, aksjonspunktDefinisjon, Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty());
    }

    private Aksjonspunkt leggTilAksjonspunkt(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon,
                                             Optional<BehandlingStegType> behandlingStegType, Optional<LocalDateTime> frist, Optional<Venteårsak> venteÅrsak,
                                             Optional<Boolean> toTrinnskontroll) {
        // sjekk at alle parametere er spesifisert
        Objects.requireNonNull(behandling, "behandling");
        Objects.requireNonNull(aksjonspunktDefinisjon, "aksjonspunktDefinisjon");
        Objects.requireNonNull(behandlingStegType, "behandlingStegType");
        Objects.requireNonNull(frist, "frist");
        Objects.requireNonNull(venteÅrsak, "venteÅrsak");
        Objects.requireNonNull(toTrinnskontroll, "toTrinnskontroll");

        // slå opp for å få riktig konfigurasjon.
        Aksjonspunkt.Builder adBuilder = behandlingStegType.isPresent()
            ? new Aksjonspunkt.Builder(aksjonspunktDefinisjon, behandlingStegType.get())
            : new Aksjonspunkt.Builder(aksjonspunktDefinisjon);

        if (frist.isPresent()) {
            adBuilder.medFristTid(frist.get());
        } else if (aksjonspunktDefinisjon.getFristPeriod() != null) {
            adBuilder.medFristTid(FPDateUtil.nå().plus(aksjonspunktDefinisjon.getFristPeriod()));
        }

        if (venteÅrsak.isPresent()) {
            adBuilder.medVenteårsak(venteÅrsak.get());
        } else {
            adBuilder.medVenteårsak(Venteårsak.UDEFINERT);
        }

        Aksjonspunkt aksjonspunkt = adBuilder.buildFor(behandling);
        return aksjonspunkt;

    }

    public void setToTrinnsBehandlingKreves(Aksjonspunkt aksjonspunkt) {
        AksjonspunktDefinisjon apDef = aksjonspunkt.getAksjonspunktDefinisjon();
        if (apDef.getSkjermlenkeType() == null || SkjermlenkeType.UDEFINERT.equals(apDef.getSkjermlenkeType())) {
            if (AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL.equals(apDef) || AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT.equals(apDef)) {
                return;
            }
        }
        if (!aksjonspunkt.isToTrinnsBehandling()) {
            if (!aksjonspunkt.erÅpentAksjonspunkt()) {
                setReåpnet(aksjonspunkt);
            }
            aksjonspunkt.settToTrinnsFlag();
        }
    }

    public void fjernToTrinnsBehandlingKreves(Aksjonspunkt aksjonspunkt) {
        aksjonspunkt.fjernToTrinnsFlagg();
    }

    public boolean setTilUtført(Aksjonspunkt aksjonspunkt, String begrunnelse) {
        return aksjonspunkt.setStatus(AksjonspunktStatus.UTFØRT, begrunnelse);
    }

    public void setTilAvbrutt(Aksjonspunkt aksjonspunkt) {
        aksjonspunkt.setStatus(AksjonspunktStatus.AVBRUTT, aksjonspunkt.getBegrunnelse());
    }

    public void setReåpnet(Aksjonspunkt aksjonspunkt) {
        aksjonspunkt.setStatus(AksjonspunktStatus.OPPRETTET, aksjonspunkt.getBegrunnelse());
    }

    public void setFrist(Aksjonspunkt ap, LocalDateTime fristTid, Venteårsak venteårsak) {
        ap.setFristTid(fristTid);
        ap.setVenteårsak(venteårsak);
    }
}
