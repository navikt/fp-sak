package no.nav.foreldrepenger.behandling.aksjonspunkt;

import java.util.ArrayList;
import java.util.List;

import no.nav.foreldrepenger.behandlingskontroll.transisjoner.TransisjonIdentifikator;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.vedtak.util.Tuple;

public class OppdateringResultat {

    private BehandlingStegType nesteSteg;
    private AksjonspunktStatus nesteStatus = AksjonspunktStatus.UTFØRT;
    private OverhoppKontroll overhoppKontroll;
    private BehandlingResultatType henleggelseResultat;
    private String henleggingsbegrunnelse;
    private boolean beholdAksjonspunktÅpent = false;
    private boolean avbrytAksjonspunkt = false;
    private boolean totrinnsKontroll = false;
    private TransisjonIdentifikator transisjonId;
    private List<Tuple<AksjonspunktDefinisjon, AksjonspunktStatus>> ekstraAksjonspunktResultat = new ArrayList<>();

    private OppdateringResultat(BehandlingStegType nesteSteg, OverhoppKontroll overhoppKontroll, TransisjonIdentifikator transisjonId, boolean totrinn) {
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
     * Klassisk resultat - uten spesiell håndtering annet enn å sette Aksjonspunkt til UTFO
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
     * Brukes typisk ved avslag på Vilår for å hoppe fram til uttak/vedtak men setter totrinnskontroll
     */
    public static OppdateringResultat medFremoverHoppTotrinn(TransisjonIdentifikator transisjonId) {
        return new OppdateringResultat(null, OverhoppKontroll.FREMOVERHOPP, transisjonId, true);
    }

    /**
     * Vil avbryte alle åpne aksjonspunkt hoppe til iverksetting og avslutte uten vedtak
     */
    public static OppdateringResultat medHenleggelse(BehandlingResultatType henleggelseResultat, String henleggingsbegrunnelse) {
        return new OppdateringResultat(OverhoppKontroll.HENLEGGELSE, henleggelseResultat, henleggingsbegrunnelse);
    }

    public BehandlingStegType getNesteSteg() {
        return nesteSteg;
    }

    public AksjonspunktStatus getNesteAksjonspunktStatus() {
        return nesteStatus;
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
        return !beholdAksjonspunktÅpent && !avbrytAksjonspunkt;
    }

    public boolean skalAvbryteAksjonspunkt() {
        return avbrytAksjonspunkt;
    }

    public boolean kreverTotrinnsKontroll() {
        return totrinnsKontroll;
    }

    public List<Tuple<AksjonspunktDefinisjon, AksjonspunktStatus>> getEkstraAksjonspunktResultat() {
        return ekstraAksjonspunktResultat;
    }

    public static class Builder {
        private OppdateringResultat resultat;

        Builder() {
            resultat = new OppdateringResultat(OverhoppKontroll.UTEN_OVERHOPP);
        }

        /*
         * Lar aksjonspunkt bli stående i  OPPRETTET etter oppdatering
         */
        public Builder medBeholdAksjonspunktÅpent() {
            resultat.nesteStatus = AksjonspunktStatus.OPPRETTET;
            resultat.beholdAksjonspunktÅpent = true;
            return this;
        }

        /*
         * Sett aksjonspunkt til AVBRUTT etter oppdatering
         */
        public Builder medAvbruttAksjonspunkt() {
            resultat.nesteStatus = AksjonspunktStatus.AVBRUTT;
            return this;
        }

        /*
         * Brukes KUN ved behov for oppdatering av registerdata, fx dersom kjernedato for registerinnhenting flyttes mer enn 12M
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
         * Brukes dersom man absolutt må endre status på andre aksjonspunkt enn det aktuelle for oppdatering/overstyring.
         * NB: Vil legge til dersom ikke finnes fra før. Bruk helst andre mekanismer.
         */
        public Builder medEkstraAksjonspunktResultat(AksjonspunktDefinisjon aksjonspunktDefinisjon, AksjonspunktStatus nyStatus) {
            resultat.ekstraAksjonspunktResultat.add(new Tuple<>(aksjonspunktDefinisjon, nyStatus));
            return this;
        }

        public OppdateringResultat build() {
            return resultat;
        }
    }

    @Override
    public String toString() {
        return "OppdateringResultat{" +
            "nesteSteg=" + nesteSteg +
            ", nesteStatus" + nesteStatus +
            ", transisjonId=" + transisjonId +
            ", overhoppKontroll=" + overhoppKontroll +
            ", henleggelseResultat=" + henleggelseResultat +
            ", henleggingsbegrunnelse='" + henleggingsbegrunnelse + '\'' +
            '}';
    }
}
