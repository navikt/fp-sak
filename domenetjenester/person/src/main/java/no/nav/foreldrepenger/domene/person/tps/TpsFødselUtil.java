package no.nav.foreldrepenger.domene.person.tps;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public final class TpsFødselUtil {

    private TpsFødselUtil() {
    }

    public static DatoIntervallEntitet forventetFødselIntervall(FamilieHendelseGrunnlagEntitet grunnlag, Period tidsromFør, Period tidsromEtter, SøknadEntitet søknad) {
        Objects.requireNonNull(søknad, "søknad");
        Objects.requireNonNull(grunnlag, "grunnlag");
        Objects.requireNonNull(tidsromFør, "tidsromFør");
        Objects.requireNonNull(tidsromEtter, "tidsromEtter");

        final FamilieHendelseEntitet hendelse = grunnlag.getGjeldendeVersjon();
        Optional<LocalDate> funnetFødselsdato = TpsFødselUtil.finnFødselsdato(hendelse);

        if (FamilieHendelseType.ADOPSJON.equals(hendelse.getType()) || FamilieHendelseType.OMSORG.equals(hendelse.getType())) {
            return funnetFødselsdato.map(TpsFødselUtil::lagIntervallForFødsel)
                .orElseThrow(IllegalStateException::new);
        } else if (FamilieHendelseType.FØDSEL.equals(hendelse.getType())) {
            return funnetFødselsdato.map(TpsFødselUtil::lagIntervallForFødsel)
                .orElseGet(() -> finnIntervallForFødselUtenBekreftetFødsel(grunnlag, tidsromFør, tidsromEtter, søknad));
        } else if (FamilieHendelseType.TERMIN.equals(hendelse.getType())) {
            return lagIntervallForTermin(hendelse, tidsromFør, tidsromEtter, søknad);
        } else {
            throw new IllegalArgumentException("Ukjent FamilieHendelseType " + hendelse.getType());
        }
    }

    private static DatoIntervallEntitet finnIntervallForFødselUtenBekreftetFødsel(FamilieHendelseGrunnlagEntitet grunnlag, Period tidsromFør, Period tidsromEtter, SøknadEntitet søknad) {
        FamilieHendelseEntitet hendelse = grunnlag.getGjeldendeVersjon();
        if (hendelse.getTerminbekreftelse().isPresent()) {
            return lagIntervallForTermin(hendelse, tidsromFør, tidsromEtter, søknad);
        } else {
            Optional<LocalDate> fødselsdato = finnFødselsdato(grunnlag.getSøknadVersjon());
            if (fødselsdato.isPresent()) {
                return lagIntervallForFødsel(fødselsdato.get());
            }

            // Gjelder ventende behandlinger fra fundamentet som er gjennopptatt hvor sb har overstyrt termin
            // med overstyring 0 barn. Overstyrt aggregat har ikke terminbekreftelse.
            if (FamilieHendelseType.TERMIN.equals(grunnlag.getSøknadVersjon().getType())) {
                return lagIntervallForTermin(grunnlag.getSøknadVersjon(), tidsromFør, tidsromEtter, søknad);
            }
            throw new IllegalStateException();
        }
    }

    public static boolean kanFinneForventetFødselIntervall(FamilieHendelseGrunnlagEntitet hendelseGrunnlag, SøknadEntitet søknad) {
        if (hendelseGrunnlag == null) {
            return false;
        }
        final FamilieHendelseEntitet hendelse = hendelseGrunnlag.getGjeldendeVersjon();
        Optional<LocalDate> funnetFødselsdato = TpsFødselUtil.finnFødselsdato(hendelse);
        return funnetFødselsdato.isPresent() || kanFinneTerminDato(hendelse) && kanFinneSøknadsdato(søknad);
    }

    private static boolean kanFinneSøknadsdato(SøknadEntitet søknad) {
        return søknad != null && søknad.getSøknadsdato() != null;
    }

    private static boolean kanFinneTerminDato(FamilieHendelseEntitet hendelse) {
        return hendelse.getTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato).isPresent();
    }

    private static DatoIntervallEntitet lagIntervallForTermin(FamilieHendelseEntitet hendelse,
                                                              Period tidsromFør,
                                                              Period tidsromEtter,
                                                              SøknadEntitet søknad) {
        LocalDate termindato = finnTermindato(hendelse);
        LocalDate søknadsDato = søknad.getSøknadsdato();
        if (søknadsDato.isBefore(termindato)) {
            return DatoIntervallEntitet.fraOgMedTilOgMed(søknadsDato.minus(tidsromFør), termindato.plus(tidsromEtter));
        } else {
            return DatoIntervallEntitet.fraOgMedTilOgMed(termindato.minus(tidsromFør), termindato.plus(tidsromEtter));
        }
    }

    private static DatoIntervallEntitet lagIntervallForFødsel(LocalDate fødselsdato) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(fødselsdato.minusDays(1), fødselsdato.plusDays(1));
    }

    private static Optional<LocalDate> finnFødselsdato(FamilieHendelseEntitet hendelse) {
        return hendelse.getBarna().stream().map(UidentifisertBarn::getFødselsdato).findFirst();
    }

    private static LocalDate finnTermindato(FamilieHendelseEntitet hendelse) {
        return hendelse.getTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato).orElseThrow(IllegalStateException::new);
    }
}
