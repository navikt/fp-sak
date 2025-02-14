package no.nav.foreldrepenger.kompletthet;

import java.time.LocalDateTime;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;

public record KompletthetResultat(boolean erOppfylt, LocalDateTime ventefrist, Venteårsak venteårsak) {

    public static KompletthetResultat oppfylt() {
        return new KompletthetResultat(true, null, null);
    }

    public static KompletthetResultat ikkeOppfylt(LocalDateTime ventefrist, Venteårsak venteårsak) {
        return new KompletthetResultat(false, ventefrist, venteårsak);
    }

    public static KompletthetResultat fristUtløpt() {
        return new KompletthetResultat(false, null, null);
    }

    public boolean erFristUtløpt() {
        return !erOppfylt() && ventefrist() == null;
    }
}
