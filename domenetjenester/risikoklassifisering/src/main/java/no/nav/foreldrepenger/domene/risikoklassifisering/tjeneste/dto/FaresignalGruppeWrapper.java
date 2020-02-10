package no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto;

import java.util.ArrayList;
import java.util.List;

import no.nav.foreldrepenger.domene.risikoklassifisering.modell.Kontrollresultat;

public class FaresignalGruppeWrapper {
    private Kontrollresultat kontrollresultat;
    private List<String> faresignaler = new ArrayList<>();

    public Kontrollresultat getKontrollresultat() {
        return kontrollresultat;
    }

    public List<String> getFaresignaler() {
        return faresignaler;
    }

    private void leggTilFaresignal(String faresignal) {
        faresignaler.add(faresignal);
    }

    private void valider() {
        if (Kontrollresultat.UDEFINERT.equals(kontrollresultat)) {
            throw new IllegalStateException("Kontrollresultat fra fprisk kan ikke være udefinert");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        FaresignalGruppeWrapper mal;

        private Builder() {
            mal = new FaresignalGruppeWrapper();
        }

        public Builder medKontrollresultat(Kontrollresultat kontrollresultat) {
            mal.kontrollresultat = kontrollresultat;
            return this;
        }

        public Builder leggTilFaresignal(String faresignal) {
            mal.leggTilFaresignal(faresignal);
            return this;
        }

        public FaresignalGruppeWrapper build() {
            mal.valider();
            return mal;
        }
    }
}
