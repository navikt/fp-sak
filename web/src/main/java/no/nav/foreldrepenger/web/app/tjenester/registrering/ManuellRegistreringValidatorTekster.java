package no.nav.foreldrepenger.web.app.tjenester.registrering;

public class ManuellRegistreringValidatorTekster {

    public static final String PAAKREVD_FELT = "Påkrevd felt";
    public static final String OVERLAPPENDE_PERIODER = "Periodene må ikke overlappe";
    public static final String PERIODER_MANGLER = "Perioder mangler";
    public static final String STARTDATO_FØR_SLUTTDATO = "Startdato må være før sluttdato";
    public static final String MINDRE_ELLER_LIK_LENGDE = "Feltet må være mindre eller lik";

    public static final String TIDLIGERE_DATO = "Må være bak i tid";
    public static final String FØR_ELLER_LIK_DAGENS_DATO = "Må være før eller lik dagens dato";
    public static final String TERMINDATO_OG_FØDSELSDATO = "Ikke fyll ut både fødsel og termin";
    public static final String TERMINDATO_ELLER_FØDSELSDATO = "Fyll ut enten fødsel eller termin";
    public static final String LIKT_ANTALL_BARN_OG_FØDSELSDATOER = "Fødselsdatoer må fylles ut for alle barn";
    public static final String OPPHOLDSSKJEMA_TOMT = "Oppholdsland og datoer må fylles ut";
    public static final String UGYLDIG_FØDSELSNUMMER = "Ugyldig Fødselsnummer";
    public static final String TERMINBEKREFTELSESDATO_FØR_TERMINDATO = "Terminbekreftelsesdato må være før termindato";
    public static final String LIK_ELLER_ETTER_MOTTATT_DATO = "Må være lik eller etter mottatt dato";
    public static final String MANGLER_MORS_AKTIVITET = "Mangler mors aktivitet";

    private ManuellRegistreringValidatorTekster() {
        // Klassen skal ikke instansieres
    }
}
