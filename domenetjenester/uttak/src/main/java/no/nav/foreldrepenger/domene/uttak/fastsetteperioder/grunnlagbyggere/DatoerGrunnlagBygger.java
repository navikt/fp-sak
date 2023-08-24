package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakGrunnlagEntitet;
import no.nav.foreldrepenger.domene.uttak.PersonopplysningerForUttak;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Datoer;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Dødsdatoer;

import java.time.LocalDate;
import java.util.Optional;

@ApplicationScoped
public class DatoerGrunnlagBygger {

    private PersonopplysningerForUttak personopplysninger;

    DatoerGrunnlagBygger() {
        // CDI
    }

    @Inject
    public DatoerGrunnlagBygger(PersonopplysningerForUttak personopplysninger) {
        this.personopplysninger = personopplysninger;
    }

    public Datoer.Builder byggGrunnlag(UttakInput input) {
        ForeldrepengerGrunnlag ytelsespesifiktGrunnlag = input.getYtelsespesifiktGrunnlag();
        var gjeldendeFamilieHendelse = ytelsespesifiktGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse();
        var ref = input.getBehandlingReferanse();

        return new Datoer.Builder()
            .fødsel(gjeldendeFamilieHendelse.getFødselsdato().orElse(null))
            .termin(gjeldendeFamilieHendelse.getTermindato().orElse(null))
            .omsorgsovertakelse(gjeldendeFamilieHendelse.getOmsorgsovertakelse().orElse(null))
            .dødsdatoer(byggDødsdatoer(ytelsespesifiktGrunnlag, ref))
            .startdatoNesteStønadsperiode(nesteStønadsperiode(ytelsespesifiktGrunnlag).orElse(null));
    }

    private Dødsdatoer.Builder byggDødsdatoer(ForeldrepengerGrunnlag foreldrepengerGrunnlag, BehandlingReferanse ref) {
        return new Dødsdatoer.Builder()
            .søkersDødsdato(personopplysninger.søkersDødsdato(ref).orElse(null))
            .barnsDødsdato(barnsDødsdato(foreldrepengerGrunnlag).orElse(null))
            .alleBarnDøde(erAlleBarnDøde(foreldrepengerGrunnlag));
    }

    private Optional<LocalDate> nesteStønadsperiode(ForeldrepengerGrunnlag ytelsespesifiktGrunnlag) {
        return ytelsespesifiktGrunnlag.getNesteSakGrunnlag().map(NesteSakGrunnlagEntitet::getStartdato);
    }

    private Optional<LocalDate> barnsDødsdato(ForeldrepengerGrunnlag foreldrepengerGrunnlag) {
        return foreldrepengerGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse().getBarna().stream()
            .map(Barn::getDødsdato)
            .flatMap(Optional::stream)
            .max(LocalDate::compareTo);
    }

    private boolean erAlleBarnDøde(ForeldrepengerGrunnlag fpGrunnlag) {
        return fpGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse().erAlleBarnDøde();
    }
}
