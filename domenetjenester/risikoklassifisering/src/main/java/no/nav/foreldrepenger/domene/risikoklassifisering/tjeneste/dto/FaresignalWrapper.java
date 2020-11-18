package no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto;

import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.Kontrollresultat;

public class FaresignalWrapper {
    private Kontrollresultat kontrollresultat;
    private FaresignalGruppeWrapper medlFaresignaler;
    private FaresignalGruppeWrapper iayFaresignaler;

    public Kontrollresultat getKontrollresultat() {
        return kontrollresultat;
    }

    public FaresignalGruppeWrapper getMedlFaresignaler() {
        return medlFaresignaler;
    }

    public FaresignalGruppeWrapper getIayFaresignaler() {
        return iayFaresignaler;
    }

    private void valider() {
        Objects.requireNonNull(kontrollresultat);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        FaresignalWrapper mal;

        private Builder () {
            mal = new FaresignalWrapper();
        }

        public Builder medKontrollresultat(Kontrollresultat kontrollresultat) {
            mal.kontrollresultat = kontrollresultat;
            return this;
        }

        public Builder medMedlFaresignaler(FaresignalGruppeWrapper medlFaresignaler) {
            mal.medlFaresignaler = medlFaresignaler;
            return this;
        }

        public Builder medIayFaresignaler(FaresignalGruppeWrapper iayFaresignaler) {
            mal.iayFaresignaler = iayFaresignaler;
            return this;
        }

        public FaresignalWrapper build() {
            mal.valider();
            return mal;
        }
    }
}
