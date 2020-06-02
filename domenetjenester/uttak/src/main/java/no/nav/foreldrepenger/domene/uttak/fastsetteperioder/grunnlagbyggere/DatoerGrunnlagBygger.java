package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Datoer;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Dødsdatoer;

@ApplicationScoped
public class DatoerGrunnlagBygger {

    private PersonopplysningTjeneste personopplysningTjeneste;
    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;

    DatoerGrunnlagBygger() {
        // CDI
    }

    @Inject
    public DatoerGrunnlagBygger(UttaksperiodegrenseRepository uttaksperiodegrenseRepository,
                                PersonopplysningTjeneste personopplysningTjeneste) {
        this.uttaksperiodegrenseRepository = uttaksperiodegrenseRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    public Datoer.Builder byggGrunnlag(UttakInput input) {
        ForeldrepengerGrunnlag ytelsespesifiktGrunnlag = input.getYtelsespesifiktGrunnlag();
        var gjeldendeFamilieHendelse = ytelsespesifiktGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse();
        var ref = input.getBehandlingReferanse();
        return new Datoer.Builder()
            .medFødsel(gjeldendeFamilieHendelse.getFødselsdato().orElse(null))
            .medTermin(gjeldendeFamilieHendelse.getTermindato().orElse(null))
            .medOmsorgsovertakelse(gjeldendeFamilieHendelse.getOmsorgsovertakelse().orElse(null))
            .medFørsteLovligeUttaksdag(førsteLovligeUttaksdag(ref))
            .medDødsdatoer(byggDødsdatoer(ytelsespesifiktGrunnlag, ref));
    }

    private LocalDate førsteLovligeUttaksdag(BehandlingReferanse ref) {
        Uttaksperiodegrense uttaksperiodegrense = uttaksperiodegrenseRepository.hent(ref.getBehandlingId());
        return uttaksperiodegrense.getFørsteLovligeUttaksdag();
    }

    private Dødsdatoer.Builder byggDødsdatoer(ForeldrepengerGrunnlag foreldrepengerGrunnlag, BehandlingReferanse ref) {
        return new Dødsdatoer.Builder()
            .medSøkersDødsdato(søkersDødsdato(ref).orElse(null))
            .medBarnsDødsdato(barnsDødsdato(foreldrepengerGrunnlag).orElse(null))
            .medErAlleBarnDøde(erAlleBarnDøde(foreldrepengerGrunnlag));
    }

    private Optional<LocalDate> søkersDødsdato(BehandlingReferanse ref) {
        PersonopplysningerAggregat personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(ref);
        return personopplysningerAggregat.getSøker().getDødsdato() == null ? Optional.empty() : Optional.of(personopplysningerAggregat.getSøker().getDødsdato());
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
