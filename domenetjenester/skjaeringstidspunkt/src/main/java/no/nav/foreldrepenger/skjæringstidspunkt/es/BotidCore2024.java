package no.nav.foreldrepenger.skjæringstidspunkt.es;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.util.Optional;

import no.nav.foreldrepenger.behandling.FamilieHendelseDato;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.skjæringstidspunkt.FamilieHendelseKonstanter;
import no.nav.foreldrepenger.skjæringstidspunkt.FamilieHendelseMapper;

/*
 * Klasse for styring av ikrafttredelese nytt regelverk for krav om 12 måneder forutgående medlemskap for engangsstønad
 * Metode for å gi ikrafttredelsesdato avhengig av miljø
 * Metode for å vurdere om en Familiehendelse skal vurderes etter nye eller gamle regler. Vil bli oppdatert
 * Støtter ikke ikrafttredelse senere enn loven tilsier
 */
public class BotidCore2024 {

    public static final LocalDate IKRAFT_FRA_DATO = LocalDate.of(2024, Month.OCTOBER,1); // LA STÅ
    public static final Period FORUTGÅENDE_MEDLEMSKAP_TIDSPERIODE = Period.ofMonths(12);

    private static final LocalDate IKRAFT_OVERGANGSPERIODE_DATO = IKRAFT_FRA_DATO.plus(FamilieHendelseKonstanter.TERMINBEKREFTELSE_TIDLIGST_UTSTEDT);

    private BotidCore2024() {
    }

    public static boolean ikkeBotidskrav(FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        var familieHendelseDato = Optional.ofNullable(familieHendelseGrunnlag)
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseMapper::mapTilFamilieHendelseDato)
            .orElse(null);
        return ikkeBotidskrav(familieHendelseDato);
    }

    public static boolean ikkeBotidskrav(FamilieHendelseDato familieHendelseDato) {
        if (familieHendelseDato == null || familieHendelseDato.familieHendelseDato() == null) {
            return false;
        } else if (familieHendelseDato.gjelderFødsel() && familieHendelseDato.termindato() != null) {
            return familieHendelseDato.termindato().isBefore(IKRAFT_OVERGANGSPERIODE_DATO);
        } else {
            return familieHendelseDato.familieHendelseDato().isBefore(IKRAFT_FRA_DATO);
        }
    }

}
