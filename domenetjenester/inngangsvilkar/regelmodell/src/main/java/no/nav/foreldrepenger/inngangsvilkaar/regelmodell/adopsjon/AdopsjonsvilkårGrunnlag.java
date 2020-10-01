package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.adopsjon;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.Kjoenn;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.VilkårGrunnlag;
import no.nav.fpsak.nare.doc.RuleDocumentationGrunnlag;

@RuleDocumentationGrunnlag
public class AdopsjonsvilkårGrunnlag implements VilkårGrunnlag {

    /**
     * Fødselsdato for adoptert barn
     */
    private List<BekreftetAdopsjonBarn> bekreftetAdopsjonBarn = new ArrayList<>();
    /**
     * Om det er ektefelles barn
     */
    private boolean ektefellesBarn;
    /**
     * Søkers kjønn
     */
    private Kjoenn soekersKjonn;
    /**
     * Om mann adopterer alene
     */
    private boolean mannAdoptererAlene;
    /**
     * Dato for omsorgsovertakelse
     */
    private LocalDate omsorgsovertakelsesdato;

    /**
     * Om stønadsperioden for annen forelder er brukt opp
     */
    private boolean erStønadsperiodeBruktOpp;


    public AdopsjonsvilkårGrunnlag(List<BekreftetAdopsjonBarn> bekreftetAdopsjonBarn, boolean ektefellesBarn, Kjoenn soekersKjonn,
                                   boolean mannAdoptererAlene, LocalDate omsorgsovertakelsesdato, boolean erStønadsperiodeBruktOpp) {

        this.ektefellesBarn = ektefellesBarn;
        this.soekersKjonn = soekersKjonn;
        this.mannAdoptererAlene = mannAdoptererAlene;
        this.omsorgsovertakelsesdato = omsorgsovertakelsesdato;
        this.erStønadsperiodeBruktOpp = erStønadsperiodeBruktOpp;
        if (bekreftetAdopsjonBarn != null) {
            for (BekreftetAdopsjonBarn bab : bekreftetAdopsjonBarn) {
                leggTilBekreftetAdopsjonBarn(bab);
            }
        }
    }

    public AdopsjonsvilkårGrunnlag() {
    }

    public boolean isEktefellesBarn() {
        return ektefellesBarn;
    }

    public Kjoenn getSoekersKjonn() {
        return soekersKjonn;
    }

    public boolean isMannAdoptererAlene() {
        return mannAdoptererAlene;
    }

    public LocalDate getOmsorgsovertakelsesdato() {
        return omsorgsovertakelsesdato;
    }

    public List<BekreftetAdopsjonBarn> getBekreftetAdopsjonBarn() {
        return bekreftetAdopsjonBarn;
    }

    public boolean getErStønadsperiodeBruktOpp() {
        return erStønadsperiodeBruktOpp;
    }

    public final void leggTilBekreftetAdopsjonBarn(BekreftetAdopsjonBarn bekreftetAdopsjonBarn) {
        getBekreftetAdopsjonBarn().add(bekreftetAdopsjonBarn);
    }
}
