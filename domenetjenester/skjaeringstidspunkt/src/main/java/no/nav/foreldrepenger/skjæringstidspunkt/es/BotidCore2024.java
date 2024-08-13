package no.nav.foreldrepenger.skjæringstidspunkt.es;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.FamilieHendelseDato;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.konfig.KonfigVerdi;

/*
 * OBSOBSOBS: Ikrafttredelsesdato og overgangsgangsordning er vedtatt i Stortinget og sanksjonert i Statsråd
 * Klasse for styring av ikrafttredelese nytt regelverk for krav om 12 måneder forutgående medlemskap for engangsstønad
 * Metode for å gi ikrafttredelsesdato avhengig av miljø
 * Metode for å vurdere om en Familiehendelse skal vurderes etter nye eller gamle regler. Vil bli oppdatert
 * Støtter ikke ikrafttredelse senere enn loven tilsier
 * TODO: Slett denne etter at siste dato er passert
 */
@ApplicationScoped
public class BotidCore2024 {


    private static final Logger LOG = LoggerFactory.getLogger(BotidCore2024.class);
    private static final Environment ENV = Environment.current();

    private static final String PROP_NAME_DATO = "dato.for.botid";
    private static final LocalDate DATO_FOR_PROD = LocalDate.of(2024, Month.OCTOBER,1); // LA STÅ

    private LocalDate ikrafttredelseDato = DATO_FOR_PROD;

    private LocalDate ikrafttredelseDatoMedOvergangsordning;

    BotidCore2024() {
        // CDI
    }

    @Inject
    public BotidCore2024(@KonfigVerdi(value = PROP_NAME_DATO, required = false) LocalDate ikrafttredelse,
                         @KonfigVerdi(value = "terminbekreftelse.tidligst.utstedelse.før.termin", defaultVerdi = "P18W3D") Period tidligsteUtstedelseAvTerminBekreftelse) {
        this.ikrafttredelseDato = bestemDato(ikrafttredelse);
        this.ikrafttredelseDatoMedOvergangsordning = ikrafttredelseDato.plus(tidligsteUtstedelseAvTerminBekreftelse);
    }

    public boolean ikkeBotidskrav(FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        if (familieHendelseGrunnlag == null || familieHendelseGrunnlag.getGjeldendeVersjon() == null) {
            return LocalDate.now().isBefore(ikrafttredelseDato);
        }
        var bekreftetFamilieHendelse = familieHendelseGrunnlag.getGjeldendeBekreftetVersjon();
        var datoFraBekreftetFamilieHendelse = bekreftetFamilieHendelse.flatMap(this::getTerminEllerFamiliehendelsedato);
        if (bekreftetFamilieHendelse.isPresent() && datoFraBekreftetFamilieHendelse.isPresent()) {
            return skalIkkeKreveBotid(bekreftetFamilieHendelse.get(), datoFraBekreftetFamilieHendelse.get());
        }
        var gjeldendeFamilieHendelse = familieHendelseGrunnlag.getGjeldendeVersjon();
        var datoFraFamilieHendelse = getTerminEllerFamiliehendelsedato(gjeldendeFamilieHendelse);
        return datoFraFamilieHendelse.map(date -> skalIkkeKreveBotid(gjeldendeFamilieHendelse, date))
            .orElseGet(() -> LocalDate.now().isBefore(ikrafttredelseDato));
    }

    private boolean skalIkkeKreveBotid(FamilieHendelseEntitet familieHendelse, LocalDate familieHendelseDato) {
        if (familieHendelse.getGjelderFødsel() && familieHendelse.getTermindato().isPresent()) {
            return familieHendelse.getTermindato().get().isBefore(ikrafttredelseDatoMedOvergangsordning);
        } else {
            return familieHendelseDato.isBefore(ikrafttredelseDato);
        }
    }

    private Optional<LocalDate> getTerminEllerFamiliehendelsedato(FamilieHendelseEntitet familieHendelse) {
        if (familieHendelse.getGjelderFødsel()) {
            return familieHendelse.getTermindato().or(familieHendelse::getFødselsdato);
        } else {
            return familieHendelse.getAdopsjon().map(AdopsjonEntitet::getOmsorgsovertakelseDato);
        }
    }

    public boolean ikkeBotidskrav(FamilieHendelseDato familieHendelseDato) {
        if (familieHendelseDato == null || familieHendelseDato.familieHendelseDato() == null) {
            return LocalDate.now().isBefore(ikrafttredelseDato);
        }
        if (familieHendelseDato.gjelderFødsel() && familieHendelseDato.termindato() != null) {
            return familieHendelseDato.termindato().isBefore(ikrafttredelseDatoMedOvergangsordning);
        } else {
            return familieHendelseDato.familieHendelseDato().isBefore(ikrafttredelseDato);
        }
    }

    private static LocalDate bestemDato(LocalDate ikrafttredelseFraKonfig) {
        if (ENV.isProd()) {
            return BotidCore2024.DATO_FOR_PROD;
        } else if (ikrafttredelseFraKonfig != null && ikrafttredelseFraKonfig.isAfter(BotidCore2024.DATO_FOR_PROD)) {
            throw new IllegalArgumentException("Støtter ikke forsinket iverksettelse i test");
        } else {
            LOG.info("BOTID CORE 2024 ikrafttredelse {}", ikrafttredelseFraKonfig);
            return ikrafttredelseFraKonfig == null ? BotidCore2024.DATO_FOR_PROD : ikrafttredelseFraKonfig;

        }
    }


}
