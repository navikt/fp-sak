package no.nav.foreldrepenger.behandling.aksjonspunkt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;

public class OppdateringResultat {

    private static final String MULTI_ENDRING = "Kan ikke fjerne vilkårtype med resultat som er lagt til i samme transisjon";

    private final List<VilkårType> vilkårTyperNyeIkkeVurdert = new ArrayList<>();
    private final List<VilkårOppdateringResultat> vilkårUtfallSomSkalLeggesTil = new ArrayList<>();
    private final List<VilkårType> vilkårTyperSomSkalFjernes = new ArrayList<>(); // Eksisterer for å håndtere vilkåropprydding for Omsorg
    private OverhoppKontroll overhoppKontroll;
    private boolean beholdAksjonspunktÅpent = false;
    private boolean totrinnsKontroll = false;
    private AksjonspunktOppdateringTransisjon transisjon;
    private final List<OppdateringAksjonspunktResultat> ekstraAksjonspunktResultat = new ArrayList<>();

    private OppdateringResultat(OverhoppKontroll overhoppKontroll, AksjonspunktOppdateringTransisjon transisjon, boolean totrinn) {
        this.overhoppKontroll = overhoppKontroll;
        this.transisjon = transisjon;
        this.totrinnsKontroll = totrinn;
    }

    private OppdateringResultat(OverhoppKontroll overhoppKontroll) {
        this.overhoppKontroll = overhoppKontroll;
    }

    /**
     * Klassisk resultat - uten spesiell håndtering annet enn å sette Aksjonspunkt
     * til UTFO
     */
    public static OppdateringResultat utenOverhopp() {
        return new OppdateringResultat(OverhoppKontroll.UTEN_OVERHOPP);
    }

    /**
     * Brukes i tilfelle med behov for tilstandsavhengig håndtering av resultat
     */
    public static Builder utenTransisjon() {
        return new Builder();
    }

    /**
     * Brukes typisk ved avslag på Vilår for å hoppe fram til uttak/vedtak
     */
    public static OppdateringResultat medFremoverHopp(AksjonspunktOppdateringTransisjon transisjon) {
        return new OppdateringResultat(OverhoppKontroll.FREMOVERHOPP, transisjon, false);
    }

    /**
     * Brukes typisk ved avslag på Vilår for å hoppe fram til uttak/vedtak men
     * setter totrinnskontroll
     */
    public static OppdateringResultat medFremoverHoppTotrinn(AksjonspunktOppdateringTransisjon transisjon) {
        return new OppdateringResultat(OverhoppKontroll.FREMOVERHOPP, transisjon, true);
    }

    public AksjonspunktOppdateringTransisjon getTransisjon() {
        return transisjon;
    }

    public OverhoppKontroll getOverhoppKontroll() {
        return overhoppKontroll;
    }

    public boolean skalUtføreAksjonspunkt() {
        return !beholdAksjonspunktÅpent;
    }

    public boolean kreverTotrinnsKontroll() {
        return totrinnsKontroll;
    }

    public List<VilkårType> getVilkårTyperNyeIkkeVurdert() {
        return vilkårTyperNyeIkkeVurdert;
    }

    public List<OppdateringAksjonspunktResultat> getEkstraAksjonspunktResultat() {
        return ekstraAksjonspunktResultat;
    }

    public List<VilkårOppdateringResultat> getVilkårUtfallSomSkalLeggesTil() {
        return vilkårUtfallSomSkalLeggesTil;
    }

    public List<VilkårType> getVilkårTyperSomSkalFjernes() {
        return vilkårTyperSomSkalFjernes;
    }

    public static class Builder {
        private final OppdateringResultat resultat;

        public Builder() {
            resultat = new OppdateringResultat(OverhoppKontroll.UTEN_OVERHOPP);
        }

        /*
         * Lar aksjonspunkt bli stående i OPPRETTET etter oppdatering
         */
        public Builder medBeholdAksjonspunktÅpent(boolean holdÅpent) {
            resultat.beholdAksjonspunktÅpent = holdÅpent;
            return this;
        }

        public Builder leggTilIkkeVurdertVilkår(VilkårType vilkårType) {
            Objects.requireNonNull(vilkårType);
            if (resultat.vilkårTyperSomSkalFjernes.contains(vilkårType) ||
                resultat.vilkårUtfallSomSkalLeggesTil.stream().anyMatch(v -> v.getVilkårType().equals(vilkårType))) {
                throw new IllegalStateException(MULTI_ENDRING);
            }
            resultat.vilkårTyperNyeIkkeVurdert.add(vilkårType);
            return this;
        }

        public Builder leggTilManueltOppfyltVilkår(VilkårType vilkårType) {
            Objects.requireNonNull(vilkårType);
            if (resultat.vilkårTyperNyeIkkeVurdert.contains(vilkårType) || resultat.vilkårTyperSomSkalFjernes.contains(vilkårType)) {
                throw new IllegalStateException(MULTI_ENDRING);
            }
            resultat.vilkårUtfallSomSkalLeggesTil.add(new VilkårOppdateringResultat(vilkårType, VilkårUtfallType.OPPFYLT));
            return this;
        }

        public Builder leggTilManueltAvslåttVilkår(VilkårType vilkårType, Avslagsårsak avslagsårsak) {
            Objects.requireNonNull(vilkårType);
            Objects.requireNonNull(avslagsårsak);
            if (Avslagsårsak.UDEFINERT.equals(avslagsårsak)) {
                throw new IllegalArgumentException("Mangler gyldig avslagsårsak");
            }
            if (resultat.vilkårTyperNyeIkkeVurdert.contains(vilkårType) || resultat.vilkårTyperSomSkalFjernes.contains(vilkårType)) {
                throw new IllegalStateException(MULTI_ENDRING);
            }
            resultat.vilkårUtfallSomSkalLeggesTil.add(new VilkårOppdateringResultat(vilkårType, avslagsårsak));
            return this;
        }

        public Builder leggTilAvslåttVilkårRegistrering(VilkårType vilkårType, Avslagsårsak avslagsårsak) {
            Objects.requireNonNull(vilkårType);
            Objects.requireNonNull(avslagsårsak);
            if (Avslagsårsak.UDEFINERT.equals(avslagsårsak)) {
                throw new IllegalArgumentException("Mangler gyldig avslagsårsak");
            }
            if (resultat.vilkårTyperNyeIkkeVurdert.contains(vilkårType) || resultat.vilkårTyperSomSkalFjernes.contains(vilkårType)) {
                throw new IllegalStateException(MULTI_ENDRING);
            }
            resultat.vilkårTyperNyeIkkeVurdert.add(vilkårType);
            resultat.vilkårUtfallSomSkalLeggesTil.add(new VilkårOppdateringResultat(vilkårType, avslagsårsak));
            return this;
        }

        public Builder fjernVilkårType(VilkårType vilkårType) {
            Objects.requireNonNull(vilkårType);
            if (resultat.vilkårTyperNyeIkkeVurdert.contains(vilkårType) ||
                resultat.vilkårUtfallSomSkalLeggesTil.stream().anyMatch(v -> v.getVilkårType().equals(vilkårType))) {
                throw new IllegalStateException(MULTI_ENDRING);
            }
            resultat.vilkårTyperSomSkalFjernes.add(vilkårType);
            return this;
        }

        /*
         * Brukes KUN ved behov for oppdatering av registerdata, fx dersom kjernedato
         * for registerinnhenting flyttes mer enn 12M
         */
        public Builder medOppdaterGrunnlag() {
            resultat.overhoppKontroll = OverhoppKontroll.OPPDATER;
            return this;
        }

        /*
         * Sentral håndtering av totrinn.
         */
        public Builder medTotrinn() {
            resultat.totrinnsKontroll = true;
            return this;
        }

        /*
         * Sentral håndtering av totrinn.
         */
        public Builder medTotrinnHvis(boolean erTotrinn) {
            resultat.totrinnsKontroll = erTotrinn;
            return this;
        }

        /*
         * Brukes i spesielle tilfelle rundt foreslå vedtak og avslag vilkår
         */
        public Builder medFremoverHopp(AksjonspunktOppdateringTransisjon transisjon) {
            resultat.overhoppKontroll = OverhoppKontroll.FREMOVERHOPP;
            resultat.transisjon = transisjon;
            return this;
        }

        /*
         * Brukes dersom man absolutt må endre status på andre aksjonspunkt enn det aktuelle for oppdatering/overstyring.
         * NB: Vil legge til dersom ikke finnes fra før. Bruk helst andre mekanismer.
         * Aksepterer ikke autopunkt - hvis det er behov så ønsker vi en diskusjon
         */
        public Builder medEkstraAksjonspunktResultat(AksjonspunktDefinisjon aksjonspunktDefinisjon, AksjonspunktStatus nyStatus) {
            if (aksjonspunktDefinisjon.erAutopunkt()) {
                throw new IllegalArgumentException("Utviklerfeil: aksjonspunkt er autopunkt " + aksjonspunktDefinisjon);
            }
            resultat.ekstraAksjonspunktResultat.add(new OppdateringAksjonspunktResultat(aksjonspunktDefinisjon, nyStatus));
            return this;
        }

        public OppdateringResultat build() {
            return resultat;
        }
    }

    @Override
    public String toString() {
        return "OppdateringResultat{" +
                "transisjon=" + transisjon +
                ", overhoppKontroll=" + overhoppKontroll +
                '}';
    }
}
