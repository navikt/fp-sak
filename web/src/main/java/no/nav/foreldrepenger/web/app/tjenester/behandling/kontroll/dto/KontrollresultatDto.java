package no.nav.foreldrepenger.web.app.tjenester.behandling.kontroll.dto;

import no.nav.foreldrepenger.domene.risikoklassifisering.modell.FaresignalVurdering;
import no.nav.foreldrepenger.domene.risikoklassifisering.modell.Kontrollresultat;

public class KontrollresultatDto {
    private Kontrollresultat kontrollresultat;
    private FaresignalgruppeDto iayFaresignaler;
    private FaresignalgruppeDto medlFaresignaler;
    private FaresignalVurdering faresignalVurdering;

    public Kontrollresultat getKontrollresultat() {
        return kontrollresultat;
    }

    public void setKontrollresultat(Kontrollresultat kontrollresultat) {
        this.kontrollresultat = kontrollresultat;
    }

    public FaresignalgruppeDto getIayFaresignaler() {
        return iayFaresignaler;
    }

    public void setIayFaresignaler(FaresignalgruppeDto iayFaresignaler) {
        this.iayFaresignaler = iayFaresignaler;
    }

    public FaresignalgruppeDto getMedlFaresignaler() {
        return medlFaresignaler;
    }

    public void setMedlFaresignaler(FaresignalgruppeDto medlFaresignaler) {
        this.medlFaresignaler = medlFaresignaler;
    }

    public FaresignalVurdering getFaresignalVurdering() {
        return faresignalVurdering;
    }

    public void setFaresignalVurdering(FaresignalVurdering faresignalVurdering) {
        this.faresignalVurdering = faresignalVurdering;
    }

    public static KontrollresultatDto ikkeKlassifisert() {
        KontrollresultatDto dto = new KontrollresultatDto();
        dto.setKontrollresultat(Kontrollresultat.IKKE_KLASSIFISERT);
        return dto;
    }
}
