package no.nav.foreldrepenger.behandling.aksjonspunkt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.TransisjonIdentifikator;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;

public class OppdateringResultat {

    private static final String MULTI_ENDRING = "Kan ikke fjerne vilkårtype med resultat som er lagt til i samme transisjon";

    private BehandlingStegType nesteSteg;
    private final List<VilkårOppdateringResultat> vilkårResultatSomSkalLeggesTil = new ArrayList<>();
    private final List<VilkårType> vilkårTyperSomSkalFjernes = new ArrayList<>(); // Eksisterer for å håndtere vilkåropprydding for Omsorg
    private VilkårResultatType vilkårResultatType;
    private OverhoppKontroll overhoppKontroll;
    private BehandlingResultatType henleggelseResultat;
    private String henleggingsbegrunnelse;
    private boolean beholdAksjonspunktÅpent = false;
    private boolean totrinnsKontroll = false;
    private TransisjonIdentifikator transisjonId;
    private List<AksjonspunktResultatMedStatus> ekstraAksjonspunktResultat = new ArrayList<>();

    private OppdateringResultat(BehandlingStegType nesteSteg, OverhoppKontroll overhoppKontroll, TransisjonIdentifikator transisjonId,
            boolean totrinn) {
        this.overhoppKontroll = overhoppKontroll;
        this.nesteSteg = nesteSteg;
        this.transisjonId = transisjonId;
        this.totrinnsKontroll = totrinn;
    }

    private OppdateringResultat(OverhoppKontroll overhoppKontroll) {
        this.overhoppKontroll = overhoppKontroll;
        this.nesteSteg = null;
    }

    private OppdateringResultat(OverhoppKontroll overhoppKontroll, BehandlingResultatType henleggelseResultat, String henleggingsbegrunnelse) {
        this.overhoppKontroll = overhoppKontroll;
        this.henleggelseResultat = henleggelseResultat;
        this.henleggingsbegrunnelse = henleggingsbegrunnelse;
    }

    /**
     * Klassisk resultat - uten spesiell håndtering annet enn å sette Aksjonspunkt
     * til UTFO
     */
    public static OppdateringResultat utenOveropp() {
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
    public static OppdateringResultat medFremoverHopp(TransisjonIdentifikator transisjonId) {
        return new OppdateringResultat(null, OverhoppKontroll.FREMOVERHOPP, transisjonId, false);
    }

    /**
     * Brukes typisk ved avslag på Vilår for å hoppe fram til uttak/vedtak men
     * setter totrinnskontroll
     */
    public static OppdateringResultat medFremoverHoppTotrinn(TransisjonIdentifikator transisjonId) {
        return new OppdateringResultat(null, OverhoppKontroll.FREMOVERHOPP, transisjonId, true);
    }

    /**
     * Vil avbryte alle åpne aksjonspunkt hoppe til iverksetting og avslutte uten
     * vedtak
     */
    public static OppdateringResultat medHenleggelse(BehandlingResultatType henleggelseResultat, String henleggingsbegrunnelse) {
        return new OppdateringResultat(OverhoppKontroll.HENLEGGELSE, henleggelseResultat, henleggingsbegrunnelse);
    }

    public BehandlingStegType getNesteSteg() {
        return nesteSteg;
    }

    public TransisjonIdentifikator getTransisjon() {
        return transisjonId;
    }

    public OverhoppKontroll getOverhoppKontroll() {
        return overhoppKontroll;
    }

    public BehandlingResultatType getHenleggelseResultat() {
        return henleggelseResultat;
    }

    public String getHenleggingsbegrunnelse() {
        return henleggingsbegrunnelse;
    }

    public boolean skalUtføreAksjonspunkt() {
        return !beholdAksjonspunktÅpent;
    }

    public boolean kreverTotrinnsKontroll() {
        return totrinnsKontroll;
    }

    public List<AksjonspunktResultatMedStatus> getEkstraAksjonspunktResultat() {
        return ekstraAksjonspunktResultat;
    }

    public List<VilkårOppdateringResultat> getVilkårResultatSomSkalLeggesTil() {
        return vilkårResultatSomSkalLeggesTil;
    }

    public List<VilkårType> getVilkårTyperSomSkalFjernes() {
        return vilkårTyperSomSkalFjernes;
    }

    public VilkårResultatType getVilkårResultatType() {
        return vilkårResultatType;
    }

    public static class Builder {
        private OppdateringResultat resultat;

        public Builder() {
            resultat = new OppdateringResultat(OverhoppKontroll.UTEN_OVERHOPP);
        }

        /*
         * Lar aksjonspunkt bli stående i OPPRETTET etter oppdatering
         */
        public Builder medBeholdAksjonspunktÅpent() {
            resultat.beholdAksjonspunktÅpent = true;
            return this;
        }

        public Builder medVilkårResultatType(VilkårResultatType vilkårResultatType) {
            Objects.requireNonNull(vilkårResultatType);
            resultat.vilkårResultatType = vilkårResultatType;
            return this;
        }

        public Builder leggTilVilkårResultat(VilkårType vilkårType, VilkårUtfallType vilkårUtfallType) {
            Objects.requireNonNull(vilkårType);
            Objects.requireNonNull(vilkårUtfallType);
            if (VilkårUtfallType.IKKE_OPPFYLT.equals(vilkårUtfallType)) {
                throw new IllegalArgumentException("Mangler avslagsårsak");
            }
            if (resultat.vilkårTyperSomSkalFjernes.stream().anyMatch(type -> type.equals(vilkårType))) {
                throw new IllegalStateException(MULTI_ENDRING);
            }
            resultat.vilkårResultatSomSkalLeggesTil.add(new VilkårOppdateringResultat(vilkårType, vilkårUtfallType));
            return this;
        }

        public Builder leggTilAvslåttVilkårResultat(VilkårType vilkårType, Avslagsårsak avslagsårsak) {
            Objects.requireNonNull(vilkårType);
            Objects.requireNonNull(avslagsårsak);
            if (Avslagsårsak.UDEFINERT.equals(avslagsårsak)) {
                throw new IllegalArgumentException("Mangler gyldig avslagsårsak");
            }
            if (resultat.vilkårTyperSomSkalFjernes.stream().anyMatch(type -> type.equals(vilkårType))) {
                throw new IllegalStateException(MULTI_ENDRING);
            }
            resultat.vilkårResultatSomSkalLeggesTil.add(new VilkårOppdateringResultat(vilkårType, avslagsårsak));
            return this;
        }

        public Builder fjernVilkårType(VilkårType vilkårType) {
            Objects.requireNonNull(vilkårType);
            if (resultat.vilkårResultatSomSkalLeggesTil.stream().anyMatch(v -> v.getVilkårType().equals(vilkårType))) {
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
        public Builder medFremoverHopp(TransisjonIdentifikator transisjonId) {
            resultat.overhoppKontroll = OverhoppKontroll.FREMOVERHOPP;
            resultat.transisjonId = transisjonId;
            return this;
        }

        /*
         * Brukes dersom man absolutt må endre status på andre aksjonspunkt enn det
         * aktuelle for oppdatering/overstyring. NB: Vil legge til dersom ikke finnes
         * fra før. Bruk helst andre mekanismer.
         */
        public Builder medEkstraAksjonspunktResultat(AksjonspunktDefinisjon aksjonspunktDefinisjon, AksjonspunktStatus nyStatus) {
            resultat.ekstraAksjonspunktResultat.add(new AksjonspunktResultatMedStatus(AksjonspunktResultat.opprettForAksjonspunkt(aksjonspunktDefinisjon), nyStatus));
            return this;
        }

        /*
         * Brukes dersom man absolutt må sette på vent. NB: Vil legge til dersom ikke
         * finnes fra før. Bruk helst andre mekanismer.
         */
        public Builder medEkstraAksjonspunktResultat(AksjonspunktResultat aksjonspunktResultat, AksjonspunktStatus nyStatus) {
            resultat.ekstraAksjonspunktResultat.add(new AksjonspunktResultatMedStatus(aksjonspunktResultat, nyStatus));
            return this;
        }

        public OppdateringResultat build() {
            if (resultat.vilkårResultatSomSkalLeggesTil.stream().map(VilkårOppdateringResultat::getVilkårUtfallType).anyMatch(VilkårUtfallType.IKKE_OPPFYLT::equals)) {
                resultat.vilkårResultatType = VilkårResultatType.AVSLÅTT;
            }
            return resultat;
        }
    }

    @Override
    public String toString() {
        return "OppdateringResultat{" +
                "nesteSteg=" + nesteSteg +
                ", transisjonId=" + transisjonId +
                ", overhoppKontroll=" + overhoppKontroll +
                ", henleggelseResultat=" + henleggelseResultat +
                ", henleggingsbegrunnelse='" + henleggingsbegrunnelse + '\'' +
                '}';
    }
}
