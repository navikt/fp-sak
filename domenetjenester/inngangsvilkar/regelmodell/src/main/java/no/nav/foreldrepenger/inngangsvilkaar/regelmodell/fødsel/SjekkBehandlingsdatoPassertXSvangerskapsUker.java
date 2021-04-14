package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.fødsel;

import java.time.Period;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.RuleReasonRefImpl;
import no.nav.fpsak.nare.specification.LeafSpecification;

/**
 * Regel definerer hvorvidt svangerskapsuke X (p.t. 22) er passert ifht. behandlingsdato
 */
public class SjekkBehandlingsdatoPassertXSvangerskapsUker extends LeafSpecification<FødselsvilkårGrunnlag> {

    static final String ID = SjekkBehandlingsdatoPassertXSvangerskapsUker.class.getSimpleName();

    static final RuleReasonRefImpl IKKE_OPPFYLT_PASSERT_TIDLIGSTE_SVANGERSKAPSUKE_KAN_BEHANDLES = new RuleReasonRefImpl("1001",
            "Behandlingsdato {0} er < tidligste behandlingstidspunkt ({1}) (svangerskapsuke {2})");

    /** Angitt maks lengde termin brukt ved beregning av svangerskapsuke. */
    static final int TERMIN_LENGDE_UKER = 40;

    /**
     * P.t. definerer loven første svangerskapsuke som kan søkes om i uke 22. Hvis endrer seg må dette flyttes til ny
     * regel eller Sats.
     */
    private static final int TIDLIGSTE_SVANGERSKAPS_UKE = 22;

    SjekkBehandlingsdatoPassertXSvangerskapsUker() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(FødselsvilkårGrunnlag t) {
        var dagensdato = t.getDagensdato();
        var termindato = t.getBekreftetTermindato();

        if (dagensdato == null) {
            throw new IllegalArgumentException("Mangler behandlingsdato i :" + t);
        }
        if (termindato == null) {
            throw new IllegalArgumentException("Mangler termindato i :" + t);
        }
        /**
         * Presisering fra fag: Fra og med man er i svangerskapsuke 22 kan man søke og få innvilget ES.
         * Det betyr at fra og med jordmor/lege skriver at man er f.eks. er 22+1 uker på vei så kan man søke.
         * For at man skal kunne komme frem til riktig dato for dette i VL Foreldrepenger må man ta
         * termindato minus 14 uker (fra søndag til søndag) og 4 dager. F.eks.
         * Termindato 4. juni 2018, kan man søke og få innvilget ES f.o.m. Torsdag 22. februar.
         * Med termindato 20. mai skal man kunne søke fra og med onsdag 7. februar.
         */
        var fratrekkFraTermindatoNedTilTidligsteUke = Period
                .ofDays((TERMIN_LENGDE_UKER - TIDLIGSTE_SVANGERSKAPS_UKE) * 7 + 4);

        var tidligstSvangerskapsDato = termindato.minus(fratrekkFraTermindatoNedTilTidligsteUke);

        if (tidligstSvangerskapsDato.isBefore(dagensdato)) {
            return ja();
        }
        return nei(IKKE_OPPFYLT_PASSERT_TIDLIGSTE_SVANGERSKAPSUKE_KAN_BEHANDLES, dagensdato, tidligstSvangerskapsDato,
                TIDLIGSTE_SVANGERSKAPS_UKE);
    }
    @Override
    public String beskrivelse() {
        return "Sjekk behandlingsdato har passert X svangerskapsuker";
    }
}
