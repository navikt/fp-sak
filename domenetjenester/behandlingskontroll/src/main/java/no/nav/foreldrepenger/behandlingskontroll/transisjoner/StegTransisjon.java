package no.nav.foreldrepenger.behandlingskontroll.transisjoner;

public enum StegTransisjon {

    // Steg-orienterte transisjoner - mer tilstandsmaskinorientert. For tiden ikke egen GJENOPPTATT
    STARTET,
    SUSPENDERT,
    UTFØRT,

    // Prosess-orienterte transisjoner
    RETURNER, // Kan gå på steg eller gjenåpning av aksjonspunkt. For tiden er kun aksjonspunkt støttet. Kaller vedHoppOverBakover
    HENLEGG, // Evt kall denne for Avbrutt. Denne blir via en del omveier mappet om til HOPPPOVER via Event og Behandlingskontroll

    HOPPOVER, // Vil hoppe fram til angitt steg uten å utføre stegene, men vil kalle vedHoppOverFramover
    FLYOVER, // Vil fly fram til angitt steg uten å utføre stegene eller kalle vedHoppOverFramover. Bruk av FLYOVER må selv initialisere data.
    ;

    public boolean direkteTilGittDestinasjon() {
        return this == HOPPOVER || this == FLYOVER;
    }

    public boolean kreverAksjonspunkt() {
        return this == RETURNER;
    }
}
